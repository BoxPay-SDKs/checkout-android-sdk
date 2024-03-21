package com.example.tray

import FailureScreenSharedViewModel


class FailureScreenCallBackSingletonClass private constructor() {
    var callBackFunctions: FailureScreenSharedViewModel? = null

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
        private var instance: FailureScreenCallBackSingletonClass? = null

        fun getInstance(): FailureScreenCallBackSingletonClass {
            return instance ?: synchronized(this) {
                instance ?: FailureScreenCallBackSingletonClass().also { instance = it }
            }
        }
    }
    fun getYourObject(): FailureScreenSharedViewModel? {
        return callBackFunctions
    }
    fun setYourObject(yourObject: FailureScreenSharedViewModel?) {
        this.callBackFunctions = yourObject
    }
}