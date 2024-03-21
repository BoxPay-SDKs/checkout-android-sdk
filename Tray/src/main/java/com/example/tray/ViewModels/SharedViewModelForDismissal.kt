package com.example.tray.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _dismissBottomSheetEvent = MutableLiveData<Boolean>()
    val dismissBottomSheetEvent: LiveData<Boolean> = _dismissBottomSheetEvent

    fun dismissBottomSheet() {
        _dismissBottomSheetEvent.value = true
    }

    fun bottomSheetDismissed() {
        _dismissBottomSheetEvent.value = false
    }
}