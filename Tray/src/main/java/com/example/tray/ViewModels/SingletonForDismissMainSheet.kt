package com.example.tray.ViewModels

import com.example.tray.ViewModels.CallBackFunctions

class SingletonForDismissMainSheet private constructor() {
    var callBackFunctions: CallbackForDismissMainSheet? = null

    companion object {
        @Volatile
        private var instance: SingletonForDismissMainSheet? = null

        fun getInstance(): SingletonForDismissMainSheet {
            return instance ?: synchronized(this) {
                instance ?: SingletonForDismissMainSheet().also { instance = it }
            }
        }
    }
    fun getYourObject(): CallbackForDismissMainSheet? {
        return callBackFunctions
    }
    fun setYourObject(yourObject: CallbackForDismissMainSheet?) {
        this.callBackFunctions = yourObject
    }

}