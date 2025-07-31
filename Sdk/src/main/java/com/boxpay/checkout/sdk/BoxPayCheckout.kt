package com.boxpay.checkout.sdk

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.sdk.ViewModels.CallBackFunctions
import com.boxpay.checkout.sdk.enum.AnalyticsEvents
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.enum.ConfigurationOptions
import com.boxpay.checkout.sdk.utils.callUIAnalytics
import com.boxpay.checkout.sdk.utils.getAnalyticsUrl

class BoxPayCheckout(
    private val context: Context,
    private val token: String,
    val onPaymentResult: ((PaymentResultObject) -> Unit)?,
    private val customerShopperToken: String = "",
    private val configurationOptions: Map<ConfigurationOptions, Any>? = null
) {
    constructor(
        context: Context,
        token: String,
        onPaymentResult: ((PaymentResultObject) -> Unit)?,
        configurationOptions: Map<ConfigurationOptions, Any>,
        customerShopperToken: String = "",
    ) : this(
        context,
        token,
        onPaymentResult,
        customerShopperToken,
        configurationOptions
    )

    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun display() {
        if (configurationOptions != null) {
            if (configurationOptions[ConfigurationOptions.ENABLE_SANDBOX_ENV] == true) {
                editor.putString("baseUrl", "test-apis.boxpay.tech")
            } else {
                editor.putString("baseUrl", "apis.boxpay.in")
            }
            editor.putBoolean(
                "isSuccessScreenVisible",
                configurationOptions[ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN] == true
            )
        } else {
            editor.putBoolean("isSuccessScreenVisible", false)
            editor.putString("baseUrl", "apis.boxpay.in")
        }
        editor.apply()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()!!
        Thread.setDefaultUncaughtExceptionHandler(SDKCrashHandler(context,
            token, getAnalyticsUrl(context), defaultHandler))

        try {
            if (token.isNotEmpty()) {
                callUIAnalytics(
                    context = context,
                    message = "",
                    screenName = "BoxPayCheckout",
                    uiEvent = AnalyticsEvents.CHECKOUT_LOADED
                )
                putTransactionDetailsInSharedPreferences()
                openBottomSheet()
            } else {
                callUIAnalytics(
                    context = context,
                    message = "Token is null or empty",
                    screenName = "BoxPayCheckout",
                    uiEvent = AnalyticsEvents.SDK_CRASH
                )
            }
        } catch (e: Exception) {
            callUIAnalytics(
                context = context,
                message = e.message ?: "",
                screenName = "BoxPayCheckout",
                uiEvent = AnalyticsEvents.SDK_CRASH
            )
        }
    }

    private fun openBottomSheet() {
        try {
            initializingCallBackFunctions()
            if (context is Activity) {
                val activity = context as AppCompatActivity
                val fragmentManager = activity.supportFragmentManager
                val bottomSheet = MainBottomSheet()
                bottomSheet.setContext(activity.applicationContext)
                bottomSheet.loadQrDirect(configurationOptions?.get(ConfigurationOptions.SHOW_UPI_QR_ON_LOAD) == true)
                bottomSheet.show(fragmentManager, "MainBottomSheet")
            }
        } catch (e: Exception) {
            callUIAnalytics(
                context = context,
                message = e.message ?: "",
                screenName = "BoxPayCheckout",
                uiEvent = AnalyticsEvents.SDK_CRASH
            )
        }
    }

    private fun initializingCallBackFunctions() {
        val callBackFunctions = onPaymentResult?.let { CallBackFunctions(it) }
        SingletonClass.getInstance().callBackFunctions = callBackFunctions
    }


    private fun putTransactionDetailsInSharedPreferences() {
        editor.putString("token", token)
        editor.putString("shopperToken", customerShopperToken)
        editor.apply()
    }
}