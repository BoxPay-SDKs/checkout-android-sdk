package com.boxpay.checkout.sdk

import android.content.Context
import com.crossplatform.BoxPayActivity
import com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler
import com.crossplatform.sdk.data.model.SDKPaymentResponse

class BoxPayCheckout(
    private val context: Context,
    private val token: String,
    val onPaymentResult: (SDKPaymentResponse) -> Unit,
    private val customerShopperToken: String = "",
    private val configurationOptions: Map<ConfigurationOptions, Boolean>? = null,
    private val uiConfiguration : UIConfiguration? = null
) {
    constructor(
        context: Context,
        token: String,
        onPaymentResult: (SDKPaymentResponse) -> Unit,
        configurationOptions: Map<ConfigurationOptions, Boolean>,
        customerShopperToken: String = "",
        uiConfiguration: UIConfiguration?
    ) : this(
        context,
        token,
        onPaymentResult,
        customerShopperToken,
        configurationOptions,
        uiConfiguration
    )

    fun display() {
        SDKPaymentResponseHandler.set(onPaymentResult)

        val intent = BoxPayActivity.createIntent(
            context = context,
            token = token,
            isTestEnv = configurationOptions?.get(ConfigurationOptions.ENABLE_SANDBOX_ENV) == true,
            shopperToken = customerShopperToken,
            showQROnLoad = configurationOptions?.get(ConfigurationOptions.SHOW_UPI_QR_ON_LOAD) == true,
            isSICheckBoxEnabled = configurationOptions?.get(ConfigurationOptions.IS_SI_CHECKBOX_ENABLED) == true,
            isSICheckBoxChecked = configurationOptions?.get(ConfigurationOptions.IS_SI_CHECKBOX_CHECKED) == true,
            isFailedScreenVisible = configurationOptions?.get(ConfigurationOptions.SHOW_BOXPAY_FAILED_SCREEN) == true,
            isSuccessScreenVisible = configurationOptions?.get(ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN) == true,
            ctaBorderRadius = uiConfiguration?.ctaBorderRadius ?: 12,
            focusedTextInputBorderColor = uiConfiguration?.focusedTextInputBorderColor ?: "",
            unfocusedTextInputBorderColor = uiConfiguration?.unfocusedTextInputBorderColor ?: ""
        )
        context.startActivity(intent)
    }
}

enum class ConfigurationOptions {
    ENABLE_SANDBOX_ENV,
    SHOW_BOXPAY_SUCCESS_SCREEN,
    SHOW_BOXPAY_FAILED_SCREEN,
    SHOW_UPI_QR_ON_LOAD,
    IS_SI_CHECKBOX_ENABLED,
    IS_SI_CHECKBOX_CHECKED,
}

data class UIConfiguration (
    val ctaBorderRadius: Int,
    val focusedTextInputBorderColor : String,
    val unfocusedTextInputBorderColor : String
)