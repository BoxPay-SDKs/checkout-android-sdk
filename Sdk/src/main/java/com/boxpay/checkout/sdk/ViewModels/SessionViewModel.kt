package com.boxpay.checkout.sdk.ViewModels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.boxpay.checkout.sdk.dataclasses.SessionResponse
import com.boxpay.checkout.sdk.repositories.SessionRepo

class SessionViewModel(context: Context) : ViewModel() {

    private val sessionRepo = SessionRepo(context)

    fun createCheckoutSession(token: String): MutableLiveData<SessionResponse?> {
        return sessionRepo.createCheckoutSession(token)
    }
}
