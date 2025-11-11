package com.boxpay.checkout.sdk.ViewModels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferRequest
import com.boxpay.checkout.sdk.dataclasses.ApplyInstantOfferResponse
import com.boxpay.checkout.sdk.dataclasses.FetchPaymentMethodPostOffer
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
    val isCodeApplied = mutableStateOf(false)
    val discountAmount = mutableStateOf("")
    private val _getInstantOfferList = MutableStateFlow<List<GetInstantOffersResponse>?>(emptyList())
    val getInstantOfferList : StateFlow<List<GetInstantOffersResponse>?> get() = _getInstantOfferList

    private val _appliedInstantOffer = MutableStateFlow<ApplyInstantOfferResponse?>(null)
    val appliedInstantOffer : StateFlow<ApplyInstantOfferResponse?> get() = _appliedInstantOffer

    private val _updatedPaymentMethods = MutableStateFlow<List<FetchPaymentMethodPostOffer>?>(emptyList())
    val updatedPaymentMethods : StateFlow<List<FetchPaymentMethodPostOffer>?> get() = _updatedPaymentMethods

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

    fun removeInstantOffer() {
        _appliedInstantOffer.value = null
    }

    fun updatePaymentMethods(
        code : String? = null,
        maxAmount: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _updatedPaymentMethods.value = instantOfferRepo.updatePaymentMethod(code = code, amount = maxAmount)
        }
    }
}