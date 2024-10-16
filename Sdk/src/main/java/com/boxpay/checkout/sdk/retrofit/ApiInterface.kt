package com.boxpay.checkout.sdk.retrofit

import com.boxpay.checkout.sdk.dataclasses.DCCRequest
import com.boxpay.checkout.sdk.dataclasses.DCCResponse
import com.boxpay.checkout.sdk.dataclasses.SessionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiInterface {
    @POST(ApiUrls.DCC)
    fun getUserDCC(@Body dccRequest: DCCRequest, @Path("token") token: String): Call<DCCResponse>

    @GET("v0/checkout/sessions/{token}")
    fun createCheckoutSession(
        @Path("token") token: String
    ): Call<SessionResponse>
}