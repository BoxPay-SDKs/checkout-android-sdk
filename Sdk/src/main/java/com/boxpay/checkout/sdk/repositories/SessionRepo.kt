package com.boxpay.checkout.sdk.repositories

import androidx.lifecycle.MutableLiveData
import com.boxpay.checkout.sdk.dataclasses.SessionResponse
import com.boxpay.checkout.sdk.retrofit.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SessionRepo {
    private val apiService = RetrofitInstance.api
    private val sessionResponseMutableLiveData = MutableLiveData<SessionResponse?>()

    fun createCheckoutSession(token: String) : MutableLiveData<SessionResponse?> {

        apiService.createCheckoutSession(token).enqueue(object : Callback<SessionResponse> {
            override fun onResponse(call: Call<SessionResponse>, response: Response<SessionResponse>) {
                if (response.isSuccessful && response.body() != null){
                    sessionResponseMutableLiveData.postValue(response.body())
                }
            }

            override fun onFailure(call: Call<SessionResponse>, t: Throwable) {
                sessionResponseMutableLiveData.postValue(null)
            }
        })
        return sessionResponseMutableLiveData
    }
}