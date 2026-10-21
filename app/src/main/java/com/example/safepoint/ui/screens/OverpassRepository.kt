package com.example.safepoint.ui.screens

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class OverpassResponse(val elements: List<OverpassElement>)
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String>? = null
)

interface OverpassApi {
    @GET("api/interpreter")
    suspend fun buscarPontos(@Query("data") query: String): OverpassResponse
}

object OverpassRepository {

    // Cliente OkHttp customizado com User-Agent e Accept
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request: Request = chain.request().newBuilder()
                .header("User-Agent", "SafePointApp/1.0 (Android; contato@safepoint.app)")
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OverpassApi::class.java)

    suspend fun buscarPontosSeguranca(
        lat: Double,
        lng: Double,
        categoria: String,
        raioMetros: Int = 5000
    ): OverpassResponse {
        val query = """
            [out:json][timeout:25];
            (
              node["amenity"="$categoria"](around:$raioMetros,$lat,$lng);
              way["amenity"="$categoria"](around:$raioMetros,$lat,$lng);
            );
            out center;
        """.trimIndent()
        return api.buscarPontos(query)
    }
}