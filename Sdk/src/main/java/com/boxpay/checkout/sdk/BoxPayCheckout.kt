package com.boxpay.checkout.sdk

import android.content.Context
import android.view.View
import com.crossplatform.BoxPayActivity
import com.crossplatform.BoxPayElementsView
import com.crossplatform.sdk.data.handler.BoxPayElementsHandler
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
            isTestEnv = flag(ConfigurationOptions.ENABLE_SANDBOX_ENV),
            shopperToken = customerShopperToken,
            showQROnLoad = flag(ConfigurationOptions.SHOW_UPI_QR_ON_LOAD),
            isSICheckBoxEnabled = flag(ConfigurationOptions.IS_SI_CHECKBOX_ENABLED),
            isSICheckBoxChecked = flag(ConfigurationOptions.IS_SI_CHECKBOX_CHECKED),
            isFailedScreenVisible = flag(ConfigurationOptions.SHOW_BOXPAY_FAILED_SCREEN),
            isSuccessScreenVisible = flag(ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN),
            ctaBorderRadius = uiConfiguration?.ctaBorderRadius ?: 12,
            focusedTextInputBorderColor = uiConfiguration?.focusedTextInputBorderColor ?: "",
            unfocusedTextInputBorderColor = uiConfiguration?.unfocusedTextInputBorderColor ?: "",
            fontFamily = uiConfiguration?.fontFamily,
        )
        context.startActivity(intent)
    }

    fun createElementsView(
        handler: BoxPayElementsHandler,
        paymentMethodList: List<String>,
    ): View {
        SDKPaymentResponseHandler.set(onPaymentResult)

        return BoxPayElementsView.create(
            context = context,
            handler = handler,
            token = token,
            isTestEnv = flag(ConfigurationOptions.ENABLE_SANDBOX_ENV),
            shopperToken = customerShopperToken,
            showQROnLoad = flag(ConfigurationOptions.SHOW_UPI_QR_ON_LOAD),
            isSICheckBoxEnabled = flag(ConfigurationOptions.IS_SI_CHECKBOX_ENABLED),
            isSICheckBoxChecked = flag(ConfigurationOptions.IS_SI_CHECKBOX_CHECKED),
            ctaBorderRadius = uiConfiguration?.ctaBorderRadius ?: 12,
            focusedTextInputBorderColor = uiConfiguration?.focusedTextInputBorderColor ?: "",
            unfocusedTextInputBorderColor = uiConfiguration?.unfocusedTextInputBorderColor ?: "",
            fontFamily = uiConfiguration?.fontFamily,
            paymentMethodList = paymentMethodList,
        )
    }

    private fun flag(option: ConfigurationOptions): Boolean =
        configurationOptions?.get(option) == true
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
    val unfocusedTextInputBorderColor : String,
    val fontFamily : String
)