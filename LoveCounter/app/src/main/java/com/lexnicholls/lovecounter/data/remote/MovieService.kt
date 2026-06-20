package com.lexnicholls.lovecounter

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class TmdbSearchResponse(
    val results: List<TmdbMovie>
)

data class TmdbMovie(
    val id: Int,
    val title: String?,
    val name: String?, // For series
    val overview: String?,
    val poster_path: String?,
    val release_date: String?,
    val first_air_date: String?
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
}

interface TmdbApi {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String = "es-ES"
    ): TmdbSearchResponse
}

object MovieClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    // Note: You should get your own API key at themoviedb.org
    const val API_KEY = "1f4b9c20587d4829110a511c75c64a7e"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val originalHttpUrl = original.url

            val url = originalHttpUrl.newBuilder()
                .setQueryParameter("api_key", API_KEY)
                .build()

            val requestBuilder = original.newBuilder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "LoveCounterApp/1.0")

            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    val api: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }
}
