package com.boxpay.checkout.sdk.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _dismissBottomSheetEvent = MutableLiveData<Boolean>()
    private val _isOtpCancelReturned = MutableLiveData<Boolean>()
    val isOtpCancelReturned : LiveData<Boolean> = _isOtpCancelReturned
    val dismissBottomSheetEvent: LiveData<Boolean> = _dismissBottomSheetEvent

    fun dismissBottomSheet() {
        _dismissBottomSheetEvent.value = true
    }

    fun bottomSheetDismissed() {
        _dismissBottomSheetEvent.value = false
    }

    fun isOtpCanceled() {
        _isOtpCancelReturned.value = true
    }

    fun isNotOtpCancel() {
        _isOtpCancelReturned.value = false
    }
}