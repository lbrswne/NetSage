package com.netsage.app.api

import com.netsage.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiFactory {
    // 开发环境：Android 模拟器访问宿主机
    private const val BASE_URL_DEBUG = "http://10.0.2.2:8000/"
    // 生产环境：改成圣上自己的线上域名（必须 https，末尾保留 /）
    private const val BASE_URL_RELEASE = "https://your-domain.com/"

    fun createApi(): NetSageApi {
        val baseUrl = if (BuildConfig.DEBUG) BASE_URL_DEBUG else BASE_URL_RELEASE

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(NetSageApi::class.java)
    }
}