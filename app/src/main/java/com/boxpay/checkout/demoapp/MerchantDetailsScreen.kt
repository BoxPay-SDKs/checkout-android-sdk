package com.boxpay.checkout.demoapp

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.demoapp.databinding.ActivityMerchantDetailsScreenBinding
import com.boxpay.checkout.sdk.BoxPayCardComponent
import com.boxpay.checkout.sdk.BoxPayCheckout
import com.boxpay.checkout.sdk.BoxPayUpiComponent
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.ConfigurationOptions

class MerchantDetailsScreen : AppCompatActivity() {

    private val binding: ActivityMerchantDetailsScreenBinding by lazy {
        ActivityMerchantDetailsScreenBinding.inflate(layoutInflater)
    }
    private var selectedEnvironment: String? = null
    private var isUpiEnabled: Boolean = false
    private var isCardEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val sharedPrefs = getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE).edit()
        sharedPrefs.clear()
        sharedPrefs.apply()

        ArrayAdapter.createFromResource(
            this,
            R.array.environment_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.environmentSpinner.adapter = adapter
        }

        val upiRadioButton = binding.upiRadioButton
        upiRadioButton.setOnCheckedChangeListener { _, isChecked ->
            isUpiEnabled = isChecked
        }

        val cardRadioButton = binding.cardRadioButton
        cardRadioButton.setOnCheckedChangeListener { _, isChecked ->
            isCardEnabled = isChecked
        }

        binding.environmentSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    // Handle the selection
                    selectedEnvironment = parent?.getItemAtPosition(position).toString()
                    binding.button.isEnabled = true
                    binding.button.text = "Proceed"
                }


                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedEnvironment = null
                }
            }

        binding.button.setOnClickListener() {
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.editTextText.windowToken, 0)

            if (selectedEnvironment == null) {
                Toast.makeText(this, "Select the environment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val token = binding.editTextText.text.toString()
            val shopperToken = binding.shopperTokenEditText.text.toString()
            binding.button.isEnabled = false
            binding.button.text = "Please Wait"
            if (selectedEnvironment == "prod") {
                if (isUpiEnabled) {
                    val boxPayUpiComponent = BoxPayUpiComponent(token, false, ::onPaymentResult)
                    boxPayUpiComponent.setContext(this)

                    // Replace a container in your activity's layout
                    binding.mainContainer.removeAllViews()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container,boxPayUpiComponent)
                        .commit()
                } else if (isCardEnabled){
                    val boxPayCardComponent = BoxPayCardComponent(token, false,::onPaymentResult)
                    boxPayCardComponent.setContext(this)
                    binding.mainContainer.removeAllViews()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, boxPayCardComponent)
                        .commit()
                } else {
                    val checkout = BoxPayCheckout(this, token, ::onPaymentResult, customerShopperToken = shopperToken, configurationOptions = mapOf(
                        ConfigurationOptions.SHOW_UPI_QR_ON_LOAD to true,
                        ConfigurationOptions.ENABLE_SANDBOX_ENV to false,
                        ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN to true
                    ))
                    checkout.testEnv = false
                    checkout.display()
                }
            } else if (selectedEnvironment == "sandbox") {
                if (isUpiEnabled) {
                    val boxPayUpiComponent = BoxPayUpiComponent(token, true, ::onPaymentResult)
                    boxPayUpiComponent.setContext(this)

                    // Replace a container in your activity's layout
                    binding.mainContainer.removeAllViews()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container,boxPayUpiComponent)
                        .commit()
                } else if (isCardEnabled){
                    val boxPayCardComponent = BoxPayCardComponent(token, false,::onPaymentResult)
                    boxPayCardComponent.setContext(this)
                    binding.mainContainer.removeAllViews()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, boxPayCardComponent)
                        .commit()
                } else {
                    val checkout = BoxPayCheckout(this, token, ::onPaymentResult, customerShopperToken = shopperToken,
                        configurationOptions = mapOf(
                        ConfigurationOptions.SHOW_UPI_QR_ON_LOAD to true,
                        ConfigurationOptions.ENABLE_SANDBOX_ENV to true,
                        ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN to false
                    ))
                    checkout.testEnv = false
                    checkout.display()
                }
            } else if (selectedEnvironment == "test") {
                if (isUpiEnabled) {
                    val boxPayUpiComponent = BoxPayUpiComponent(token, false, ::onPaymentResult)
                    boxPayUpiComponent.setContext(this)

                    // Replace a container in your activity's layout
                    binding.mainContainer.removeAllViews()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container,boxPayUpiComponent)
                        .commit()
                } else if (isCardEnabled){
                    val boxPayCardComponent = BoxPayCardComponent(token, false,::onPaymentResult)
                    boxPayCardComponent.setContext(this)
                    binding.mainContainer.removeAllViews()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, boxPayCardComponent)
                        .commit()
                } else {
                    val checkout = BoxPayCheckout(this, token, ::onPaymentResult, shopperToken, configurationOptions = mapOf(
                        ConfigurationOptions.SHOW_UPI_QR_ON_LOAD to true,
                        ConfigurationOptions.ENABLE_SANDBOX_ENV to false,
                        ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN to true
                    ))
                    checkout.testEnv = true
                    checkout.display()
                }
            }
        }
    }
    fun onPaymentResult(result: PaymentResultObject) {
        Toast.makeText(this, result.status, Toast.LENGTH_SHORT).show()
    }
}