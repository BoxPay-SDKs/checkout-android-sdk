package com.boxpay.checkout.sdk.retrofit

import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferRequest
import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferResponse
import com.boxpay.checkout.sdk.dataclasses.DCCRequest
import com.boxpay.checkout.sdk.dataclasses.DCCResponse
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersRequest
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersResponse
import com.boxpay.checkout.sdk.dataclasses.SessionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiInterface {
    @POST(ApiUrls.DCC)
    fun getUserDCC(@Body dccRequest: DCCRequest, @Path("token") token: String): Call<DCCResponse>

    @GET(ApiUrls.CREATE_SESSION)
    fun createCheckoutSession(
        @Path("token") token: String
    ): Call<SessionResponse>

    @POST(ApiUrls.GET_INSTANT_OFFERS)
    fun getInstantOffers(
        @Body instantOffersRequest: GetInstantOffersRequest,
        @Path("token") token: String
    ) : Call<GetInstantOffersResponse>

    @POST(ApiUrls.APPLY_INSTANT_OFFER)
    fun applyInstantOffers(
        @Body applyInstantOfferRequest: ApplyInstantOfferRequest,
        @Path("token") token: String
    ) : Call<ApplyInstantOfferResponse>
}