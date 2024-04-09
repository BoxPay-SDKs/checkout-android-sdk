package com.example.tray.ViewModels

class SingletonClassForLoadingState private constructor() {
    var callBackFunctions: callBackFunctionForLoadingState? = null

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
        private var instance: SingletonClassForLoadingState? = null

        fun getInstance(): SingletonClassForLoadingState {
            return instance ?: synchronized(this) {
                instance ?: SingletonClassForLoadingState().also { instance = it }
            }
        }
    }
    fun getYourObject(): callBackFunctionForLoadingState? {
        return callBackFunctions
    }
    fun setYourObject(yourObject: callBackFunctionForLoadingState?) {
        this.callBackFunctions = yourObject
    }

}