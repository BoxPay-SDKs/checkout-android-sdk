package com.example.tray.ViewModels

class callBackFunctionForLoadingState(val onBottomSheetOpened : () -> Unit) {
    fun onBottomSheetOpenedPrivate(){
        onBottomSheetOpened()
    }


}