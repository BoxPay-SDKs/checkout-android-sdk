package com.boxpay.checkout.sdk.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.boxpay.checkout.sdk.repositories.InstantOfferRepo

class InstantOfferViewModel(context: Context) : ViewModel() {
    private val instantOfferRepo = InstantOfferRepo(context = context)

    fun getInstantOffer() {

    }

    fun applyInstantOffer() {

    }
}