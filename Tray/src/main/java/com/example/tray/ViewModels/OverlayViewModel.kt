package com.example.tray.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OverlayViewModel : ViewModel() {
    private val _showOverlay = MutableLiveData<Boolean>()
    val showOverlay: LiveData<Boolean> get() = _showOverlay

    init {
        // Set the initial value of showOverlay to true
        _showOverlay.value = true
    }

    // Method to update the value of showOverlay
    fun setShowOverlay(value: Boolean) {
        _showOverlay.value = value
    }
}