package com.benyeung.bus1933clone.model

data class KmbRoute(
    val route: String,
    val bound: String,
    val service_type: String,
    val orig_en: String,
    val orig_tc: String,
    val orig_sc: String,
    val dest_en: String,
    val dest_tc: String,
    val dest_sc: String
)

data class KmbStop(
    val stop: String,
    val name_en: String,
    val name_tc: String,
    val name_sc: String,
    val lat: String,
    val long: String
)

data class RouteStop(
    val route: String,
    val bound: String,
    val service_type: String,
    val seq: Int,
    val stop: String
)

data class Eta(
    val co: String,
    val route: String,
    val dir: String,
    val service_type: Int,
    val seq: Int,
    val dest_tc: String,
    val dest_en: String,
    val eta: String?,
    val eta_seq: Int,
    val rmk_tc: String,
    val data_timestamp: String?
)

data class KmbApiResponse<T>(val type: String, val version: String, val generated_timestamp: String, val data: List<T>)

fun KmbRoute.displayName(): String = route
fun KmbRoute.boundText(): String = if (bound == "O") "往 ${dest_tc}" else "往 ${dest_tc} (回程)"

fun KmbRoute.toDirection(): String = if (bound == "O") "outbound" else "inbound"