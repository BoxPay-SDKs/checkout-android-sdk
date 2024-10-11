package com.boxpay.checkout.sdk.ViewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.boxpay.checkout.sdk.dataclasses.DCCRequest
import com.boxpay.checkout.sdk.dataclasses.DCCResponse
import com.boxpay.checkout.sdk.repositories.DCCRepo

class DCCViewModel : ViewModel() {
    private val dccRepo = DCCRepo()
    fun getDCC(dccRequest: DCCRequest, token:String) : MutableLiveData<DCCResponse?>{
        return dccRepo.getDCC(dccRequest,token)
    }
}