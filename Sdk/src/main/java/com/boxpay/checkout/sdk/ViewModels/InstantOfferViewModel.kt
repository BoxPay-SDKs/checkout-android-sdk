package com.boxpay.checkout.sdk.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferRequest
import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferResponse
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersRequest
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersResponse
import com.boxpay.checkout.sdk.dataclasses.OfferSearchRequest
import com.boxpay.checkout.sdk.repositories.InstantOfferRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InstantOfferViewModel(context: Context) : ViewModel() {
    private val instantOfferRepo = InstantOfferRepo(context = context)
    private val _getInstantOfferList = MutableStateFlow<List<GetInstantOffersResponse>?>(emptyList())
    val getInstantOfferList : StateFlow<List<GetInstantOffersResponse>?> get() = _getInstantOfferList

    private val _appliedInstantOffer = MutableStateFlow<ApplyInstantOfferResponse?>(null)
    val appliedInstantOffer : StateFlow<ApplyInstantOfferResponse?> get() = _appliedInstantOffer

    fun getInstantOffer(
        type: String,
        currency: String,
        minAmount: Int,
        maxAmount: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _getInstantOfferList.value = instantOfferRepo.getInstantOffer(
                GetInstantOffersRequest(type, currency, minAmount, maxAmount)
            )
        }
    }

    fun applyInstantOffer(
        currency: String,
        maxAmount: Int,
        minAmount: Int,
        selectedCode : List<String>
    ) {
        viewModelScope.launch(context = Dispatchers.IO) {
            _appliedInstantOffer.value = instantOfferRepo.applyInstantOffers(
                applyInstantOfferRequest = ApplyInstantOfferRequest(
                    offerSearchRequest = OfferSearchRequest(
                        currency = currency,
                        maxAmount = maxAmount,
                        minAmount = minAmount,
                        offers = selectedCode
                    )
                )
            )
        }
    }
}