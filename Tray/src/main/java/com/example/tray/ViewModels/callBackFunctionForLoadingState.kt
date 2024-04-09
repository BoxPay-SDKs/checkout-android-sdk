package com.example.tray.ViewModels

import android.util.Log
import com.example.tray.paymentResult.PaymentResultObject

class callBackFunctionForLoadingState(val onBottomSheetOpened : () -> Unit) {
    fun onBottomSheetOpenedPrivate(){
        Log.d("result for callback","checkingPurpose")
        onBottomSheetOpened()
    }


}