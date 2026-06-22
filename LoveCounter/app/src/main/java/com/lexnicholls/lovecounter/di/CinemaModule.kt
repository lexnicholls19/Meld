package com.lexnicholls.lovecounter.di

import com.lexnicholls.lovecounter.data.remote.CinemaApiService
import com.lexnicholls.lovecounter.data.repository.CinemaRepositoryImpl
import com.lexnicholls.lovecounter.domain.repository.CinemaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CinemaModule {

    @Binds
    @Singleton
    abstract fun bindCinemaRepository(
        cinemaRepositoryImpl: CinemaRepositoryImpl
    ): CinemaRepository

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = if (com.lexnicholls.lovecounter.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
        }

        @Provides
        @Singleton
        fun provideCinemaApiService(okHttpClient: OkHttpClient): CinemaApiService {
            return Retrofit.Builder()
                .baseUrl("https://meld-cinema-proxy.meld-cinema.workers.dev/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CinemaApiService::class.java)
        }
    }
}
