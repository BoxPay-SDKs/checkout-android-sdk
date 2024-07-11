package com.boxpay.checkout.sdk.ViewModels

import android.util.Log

class callBackFunctionForLoadingState(val onBottomSheetOpened : () -> Unit) {
    fun onBottomSheetOpenedPrivate(){
        Log.d("result for callback","checkingPurpose")
        onBottomSheetOpened()
    }


}