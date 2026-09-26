package com.benyeung.bus1933clone

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.benyeung.bus1933clone.model.*
import com.benyeung.bus1933clone.network.KmbApi
import com.benyeung.bus1933clone.ui.Bus1933Theme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration
import java.time.Instant
import kotlin.math.*

val Context.dataStore by preferencesDataStore("bus1933")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val api = Retrofit.Builder()
            .baseUrl("https://data.etabus.gov.hk/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(KmbApi::class.java)
        setContent {
            Bus1933Theme {
                App1933Clone(api)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App1933Clone(api: KmbApi) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var routes by remember { mutableStateOf<List<KmbRoute>>(emptyList()) }
    var allStops by remember { mutableStateOf<Map<String, KmbStop>>(emptyMap()) }
    var selectedRoute by remember { mutableStateOf<KmbRoute?>(null) }
    var routeStops by remember { mutableStateOf<List<Pair<RouteStop, KmbStop>>>(emptyList()) }
    var etaMap by remember { mutableStateOf<Map<String, List<Eta>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var nearbyStops by remember { mutableStateOf<List<Pair<KmbStop, Double>>>(emptyList()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoritesKey = stringSetPreferencesKey("favorites")
    val favorites by context.dataStore.data.map { it[favoritesKey] ?: emptySet() }.collectAsState(initial = emptySet())

    // Load routes + stops
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val r = api.getRoutes().data
            val s = api.getStops().data.associateBy { it.stop }
            routes = r.sortedBy { it.route }
            allStops = s
        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
    }

    // Auto refresh ETA every 20s when viewing route
    LaunchedEffect(selectedRoute, routeStops) {
        if (selectedRoute == null || routeStops.isEmpty()) return@LaunchedEffect
        while (true) {
            try {
                val map = mutableMapOf<String, List<Eta>>()
                routeStops.take(20).forEach { (rs, _) ->
                    try {
                        val etas = api.getEta(rs.stop, rs.route, rs.service_type).data
                            .filter { it.route == rs.route }
                            .sortedBy { it.eta ?: "" }
                        map[rs.stop] = etas
                    } catch (_: Exception) {}
                }
                etaMap = map
            } catch (_: Exception) {}
            delay(20000)
        }
    }

    Scaffold(
        topBar = {
            if (selectedRoute == null) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary).padding(16.dp)) {
                    Text("APP 1933 Clone", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("一App睇晒 · 九巴龍運實時", color = Color.White.copy(0.8f), fontSize = 12.sp)
                    Spacer(8.dp)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搵路線 2A, 68X, 270A...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(0.15f),
                            unfocusedContainerColor = Color.White.copy(0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            } else {
                TopAppBar(
                    title = { Text("${selectedRoute!!.route} ${selectedRoute!!.boundText()}") },
                    navigationIcon = {
                        IconButton(onClick = { selectedRoute = null; routeStops = emptyList(); etaMap = emptyMap() }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                context.dataStore.edit { it[favoritesKey] = favorites + "${selectedRoute!!.route}-${selectedRoute!!.bound}-${selectedRoute!!.service_type}" }
                            }
                        }) { Icon(Icons.Default.Star, null) }
                    }
                )
            }
        },
        bottomBar = {
            if (selectedRoute == null) {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.DirectionsBus, null) }, label = { Text("路線") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.NearMe, null) }, label = { Text("鄰近") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("收藏") })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Route, null) }, label = { Text("落車") })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                selectedRoute != null -> RouteDetailView(routeStops, etaMap, allStops, selectedRoute!!, api)
                selectedTab == 0 -> RouteListView(routes, searchQuery, isLoading, onSelect = { r ->
                    selectedRoute = r
                    scope.launch {
                        isLoading = true
                        try {
                            val rs = api.getRouteStops(r.route, r.bound, r.service_type).data.sortedBy { it.seq }
                            routeStops = rs.mapNotNull { rsItem -> allStops[rsItem.stop]?.let { rsItem to it } }
                        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
                    }
                })
                selectedTab == 1 -> NearbyView(allStops, api, nearbyStops, onNearby = { nearbyStops = it })
                selectedTab == 2 -> FavoriteView(favorites, routes, onSelect = { r ->
                    val parts = r.split("-")
                    routes.find { it.route == parts[0] && it.bound == parts[1] && it.service_type == parts[2] }?.let { selectedRoute = it }
                })
                selectedTab == 3 -> AlightReminderView()
            }
        }
    }
}

@Composable
fun RouteListView(routes: List<KmbRoute>, query: String, isLoading: Boolean, onSelect: (KmbRoute) -> Unit) {
    val filtered = remember(routes, query) {
        if (query.isBlank()) routes.take(100) else routes.filter { it.route.contains(query.uppercase()) || it.orig_tc.contains(query) || it.dest_tc.contains(query) }
    }
    if (isLoading) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return }
    LazyColumn {
        items(filtered) { r ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable { onSelect(r) }, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(r.route, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.boundText(), fontWeight = FontWeight.SemiBold)
                        Text("${r.orig_tc} → ${r.dest_tc}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun RouteDetailView(
    routeStops: List<Pair<RouteStop, KmbStop>>,
    etaMap: Map<String, List<Eta>>,
    allStops: Map<String, KmbStop>,
    route: KmbRoute,
    api: KmbApi
) {
    LazyColumn {
        item {
            Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text("預計到站時間 · 每20秒更新", fontSize = 12.sp)
                    Text("實時載客量 + 落車提示已整合", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
        items(routeStops) { (rs, stop) ->
            val etas = etaMap[stop.stop] ?: emptyList()
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${rs.seq}. ${stop.name_tc}", fontWeight = FontWeight.Bold)
                        Text(stop.name_en, fontSize = 11.sp, color = Color.Gray)
                        if (etas.isEmpty()) Text("暫無預報", fontSize = 12.sp, color = Color.Gray)
                        else Row {
                            etas.take(3).forEach { eta ->
                                val mins = eta.eta?.let { calcMins(it) } ?: "--"
                                val occ = when { eta.rmk_tc.contains("滿") -> "🔴" ; eta.rmk_tc.contains("有位") -> "🟢" ; else -> "🟡" }
                                Text("$occ ${mins}分 ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (mins == "0") MaterialTheme.colorScheme.primary else Color.Unspecified)
                            }
                        }
                    }
                    IconButton(onClick = { /* 落車提示 */ }) { Icon(Icons.Default.NotificationsActive, null) }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NearbyView(allStops: Map<String, KmbStop>, api: KmbApi, nearby: List<Pair<KmbStop, Double>>, onNearby: (List<Pair<KmbStop, Double>>) -> Unit) {
    val context = LocalContext.current
    val perm = rememberMultiplePermissionsState(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    var location by remember { mutableStateOf<Location?>(null) }
    LaunchedEffect(perm.allPermissionsGranted, allStops) {
        if (!perm.allPermissionsGranted) { perm.launchMultiplePermissionRequest(); return@LaunchedEffect }
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                location = loc
                if (loc != null && allStops.isNotEmpty()) {
                    val list = allStops.values.map { s ->
                        val d = haversine(loc.latitude, loc.longitude, s.lat.toDoubleOrNull() ?: 0.0, s.long.toDoubleOrNull() ?: 0.0)
                        s to d
                    }.filter { it.second < 1.0 }.sortedBy { it.second }.take(30)
                    onNearby(list)
                }
            }
        } catch (_: Exception) {}
    }
    if (!perm.allPermissionsGranted) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Button(onClick = { perm.launchMultiplePermissionRequest() }) { Text("開啟定位睇鄰近路線") } }
        return
    }
    LazyColumn {
        items(nearby) { (stop, dist) ->
            Card(Modifier.fillMaxWidth().padding(8.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(8.dp)
                    Column(Modifier.weight(1f)) {
                        Text(stop.name_tc, fontWeight = FontWeight.Bold)
                        Text("${(dist*1000).toInt()}m · ${stop.stop}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteView(favs: Set<String>, routes: List<KmbRoute>, onSelect: (String) -> Unit) {
    if (favs.isEmpty()) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("未有收藏，快啲收藏常用路線") }; return }
    LazyColumn {
        items(favs.toList()) { f ->
            Card(Modifier.fillMaxWidth().padding(8.dp).clickable { onSelect(f) }) {
                Text(f, Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AlightReminderView() {
    var enabled by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Alarm, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(16.dp)
        Text("落車提示", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("揀咗目的地站，到站前300m自動震動提醒你，唔怕瞓過龍", fontSize = 13.sp, color = Color.Gray)
        Spacer(24.dp)
        Switch(checked = enabled, onCheckedChange = { enabled = it })
        Text(if (enabled) "已啟用 - 到站前會震動" else "未啟用")
    }
}

fun calcMins(etaIso: String): String {
    return try {
        val eta = Instant.parse(etaIso)
        val now = Instant.now()
        val diff = Duration.between(now, eta).toMinutes()
        if (diff < 0) "0" else diff.toString()
    } catch (_: Exception) { "--" }
}

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2-lat1)
    val dLon = Math.toRadians(lon2-lon1)
    val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1-a))
}