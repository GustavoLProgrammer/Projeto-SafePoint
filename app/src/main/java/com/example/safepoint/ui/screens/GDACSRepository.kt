package com.example.safepoint.ui.screens

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class GdacsResponse(val features: List<GdacsFeature>)
data class GdacsFeature(val properties: GdacsProperties, val geometry: GdacsGeometry)
data class GdacsProperties(val eventtype: String, val name: String, val alertlevel: String)
data class GdacsGeometry(val coordinates: List<Double>)

interface GdacsApi {
    @GET("gdacsapi/api/events/geteventlist/SEARCH")
    suspend fun getDesastres(
        @Query("eventlist") tipos: String = "EQ;TC;FL;VO;DR;WF",
        @Query("alertlevel") nivel: String = "Green;Orange;Red",
        @Query("limit") limite: Int = 100
    ): GdacsResponse
}

object GdacsRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://www.gdacs.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GdacsApi::class.java)

    suspend fun buscarDesastres(tipos: String = "EQ;TC;FL;VO;DR;WF"): GdacsResponse =
        api.getDesastres(tipos = tipos)
}