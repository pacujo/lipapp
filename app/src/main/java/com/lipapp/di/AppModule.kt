package com.lipapp.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.lipapp.data.api.ApiInterceptor
import com.lipapp.data.api.LipserviceApi
import com.lipapp.data.api.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiInterceptor: ApiInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(apiInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideLipserviceApi(retrofit: Retrofit): LipserviceApi =
        retrofit.create(LipserviceApi::class.java)
}
