package com.example.tray.ViewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tray.dataclasses.TransactionData

class SharedViewModelTransactionDetails : ViewModel() {
    private val transactionData = MutableLiveData<TransactionData>()

    fun setTransactionData(data: TransactionData) {
        transactionData.value = data
    }

    fun getTransactionData() = transactionData
}