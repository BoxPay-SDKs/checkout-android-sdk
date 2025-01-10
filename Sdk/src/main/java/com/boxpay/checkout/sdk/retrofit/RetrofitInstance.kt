package com.boxpay.checkout.sdk.retrofit

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): Retrofit {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val baseUrl = "https://" + sharedPreferences.getString("baseUrl", "apis.boxpay.in") + "/"

        if (retrofit == null || retrofit!!.baseUrl().toString() != baseUrl) {
            val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

        return retrofit!!
    }

    // API service can be accessed via this function
    fun getApi(context: Context): ApiInterface {
        return getInstance(context).create(ApiInterface::class.java)
    }
}
