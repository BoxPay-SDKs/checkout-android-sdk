package com.boxpay.checkout.sdk.ViewModels

class callBackFunctionForLoadingState(val onBottomSheetOpened : () -> Unit) {
    fun onBottomSheetOpenedPrivate(){
        onBottomSheetOpened()
    }


}