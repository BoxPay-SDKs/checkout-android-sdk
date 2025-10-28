package com.boxpay.checkout.sdk.repositories

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferRequest
import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferResponse
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersRequest
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersResponse
import com.boxpay.checkout.sdk.retrofit.RetrofitInstance
import com.boxpay.checkout.sdk.utils.getSessionToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InstantOfferRepo (val context: Context) {
    private val apiService = RetrofitInstance.getApi(context)
    private val getInstantOffersResponseLiveData = MutableLiveData<GetInstantOffersResponse?>()
    private val applyInstantOffersResponseLiveData = MutableLiveData<ApplyInstantOfferResponse?>()

    fun getInstantOffer(instantOffersRequest: GetInstantOffersRequest) {
        apiService.getInstantOffers(instantOffersRequest = instantOffersRequest, token = getSessionToken(context = context)).enqueue(object : Callback<GetInstantOffersResponse> {
            override fun onResponse(call: Call<GetInstantOffersResponse>, response: Response<GetInstantOffersResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    getInstantOffersResponseLiveData.postValue(response.body())
                }
            }

            override fun onFailure(call: Call<GetInstantOffersResponse>, t: Throwable) {
                getInstantOffersResponseLiveData.postValue(null)
            }
        })
    }

    fun applyInstantOffers(applyInstantOfferRequest: ApplyInstantOfferRequest) {
        apiService.applyInstantOffers(applyInstantOfferRequest = applyInstantOfferRequest, token = getSessionToken(context = context)).enqueue(object : Callback<ApplyInstantOfferResponse> {
            override fun onResponse(call: Call<ApplyInstantOfferResponse>, response: Response<ApplyInstantOfferResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    applyInstantOffersResponseLiveData.postValue(response.body())
                }
            }

            override fun onFailure(call: Call<ApplyInstantOfferResponse>, t: Throwable) {
                applyInstantOffersResponseLiveData.postValue(null)
            }
        })
    }
}