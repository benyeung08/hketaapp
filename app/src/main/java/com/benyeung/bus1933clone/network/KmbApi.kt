package com.benyeung.bus1933clone.network

import com.benyeung.bus1933clone.model.*
import retrofit2.http.GET
import retrofit2.http.Path

interface KmbApi {
    @GET("v1/transport/kmb/route/")
    suspend fun getRoutes(): KmbApiResponse<KmbRoute>

    @GET("v1/transport/kmb/stop")
    suspend fun getStops(): KmbApiResponse<KmbStop>

    @GET("v1/transport/kmb/route-stop/{route}/{bound}/{serviceType}")
    suspend fun getRouteStops(
        @Path("route") route: String,
        @Path("bound") bound: String,
        @Path("serviceType") serviceType: String
    ): KmbApiResponse<RouteStop>

    @GET("v1/transport/kmb/stop/{stopId}")
    suspend fun getStop(@Path("stopId") stopId: String): KmbApiResponse<KmbStop>

    @GET("v1/transport/kmb/stop-eta/{stopId}")
    suspend fun getStopEta(@Path("stopId") stopId: String): KmbApiResponse<Eta>

    @GET("v1/transport/kmb/route-eta/{route}/{serviceType}")
    suspend fun getRouteEta(
        @Path("route") route: String,
        @Path("serviceType") serviceType: String
    ): KmbApiResponse<Eta>

    @GET("v1/transport/kmb/eta/{stopId}/{route}/{serviceType}")
    suspend fun getEta(
        @Path("stopId") stopId: String,
        @Path("route") route: String,
        @Path("serviceType") serviceType: String
    ): KmbApiResponse<Eta>
}