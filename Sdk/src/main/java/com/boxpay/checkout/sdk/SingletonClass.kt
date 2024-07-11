package com.boxpay.checkout.sdk

import com.boxpay.checkout.sdk.ViewModels.CallBackFunctions

class SingletonClass private constructor() {
    var callBackFunctions: CallBackFunctions? = null

//    companion object {
//        @get:Synchronized
//        var getInstance: SingletonClass? = null
//            get() {
//                if (field == null) {
//                    field = SingletonClass()
//                }
//                return field
//            }
//            private set
//    }

    companion object {
        @Volatile
        private var instance: SingletonClass? = null

        fun getInstance(): SingletonClass {
            return instance ?: synchronized(this) {
                instance ?: SingletonClass().also { instance = it }
            }
        }
    }
    fun getYourObject(): CallBackFunctions? {
        return callBackFunctions
    }
    fun setYourObject(yourObject: CallBackFunctions?) {
        this.callBackFunctions = yourObject
    }

}