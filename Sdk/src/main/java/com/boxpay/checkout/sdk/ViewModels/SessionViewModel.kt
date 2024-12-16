package com.boxpay.checkout.sdk.ViewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.boxpay.checkout.sdk.dataclasses.SessionResponse
import com.boxpay.checkout.sdk.repositories.SessionRepo

class SessionViewModel : ViewModel() {
    private val sessionRepo = SessionRepo()
    fun createCheckoutSession(token: String): MutableLiveData<SessionResponse?> {
        return sessionRepo.createCheckoutSession(token)
    }
}