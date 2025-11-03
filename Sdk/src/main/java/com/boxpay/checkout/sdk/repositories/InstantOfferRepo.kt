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

    fun getInstantOffer(instantOffersRequest: GetInstantOffersRequest) : List<GetInstantOffersResponse>? {
        return try {
            val response = apiService.getInstantOffers(
                instantOffersRequest = instantOffersRequest,
                token = getSessionToken(context)
            ).execute()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyInstantOffers(applyInstantOfferRequest: ApplyInstantOfferRequest) : ApplyInstantOfferResponse? {
        return try {
            val response = apiService.applyInstantOffers(
                applyInstantOfferRequest = applyInstantOfferRequest,
                token = getSessionToken(context)
            ).execute()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}