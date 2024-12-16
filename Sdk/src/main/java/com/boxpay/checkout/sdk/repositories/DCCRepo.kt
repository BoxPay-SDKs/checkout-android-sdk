package com.boxpay.checkout.sdk.repositories

import androidx.lifecycle.MutableLiveData
import com.boxpay.checkout.sdk.dataclasses.DCCRequest
import com.boxpay.checkout.sdk.dataclasses.DCCResponse
import com.boxpay.checkout.sdk.retrofit.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DCCRepo {
    private val apiService = RetrofitInstance.api
    private val dccResponseMutableLiveData = MutableLiveData<DCCResponse?>()

    fun getDCC(dccRequest: DCCRequest, token: String) : MutableLiveData<DCCResponse?> {

        apiService.getUserDCC(dccRequest, token).enqueue(object : Callback<DCCResponse>{
            override fun onResponse(call: Call<DCCResponse>, response: Response<DCCResponse>) {
                if (response.isSuccessful && response.body() != null){
                    dccResponseMutableLiveData.postValue(response.body())
                }else{
                    dccResponseMutableLiveData.postValue(null)
                }
            }

            override fun onFailure(call: Call<DCCResponse>, t: Throwable) {
                dccResponseMutableLiveData.postValue(null)
            }
        })
        return dccResponseMutableLiveData
    }
}