package com.lexnicholls.lovecounter

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class ExchangeRateResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

interface CurrencyApi {
    @GET("latest/{base}")
    suspend fun getRates(@Path("base") base: String): ExchangeRateResponse
}

object CurrencyClient {
    private const val BASE_URL = "https://api.exchangerate-api.com/v4/"

    val api: CurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }
}
