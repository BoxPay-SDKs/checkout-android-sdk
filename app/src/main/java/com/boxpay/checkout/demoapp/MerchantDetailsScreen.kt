package com.boxpay.checkout.demoapp

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.demoapp.databinding.ActivityMerchantDetailsScreenBinding
import com.boxpay.checkout.sdk.BoxPayCheckout
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject

class MerchantDetailsScreen : AppCompatActivity() {

    private val binding: ActivityMerchantDetailsScreenBinding by lazy {
        ActivityMerchantDetailsScreenBinding.inflate(layoutInflater)
    }
    private var selectedEnvironment: String? = null


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
                val checkout = BoxPayCheckout(this, token, ::onPaymentResult, false, customerShopperToken = shopperToken)
                checkout.testEnv = false
                checkout.display()
            } else if (selectedEnvironment == "sandbox") {
                val checkout = BoxPayCheckout(this, token, ::onPaymentResult, true, customerShopperToken = shopperToken)
                checkout.testEnv = false
                checkout.display()
            } else if (selectedEnvironment == "test") {
                val checkout = BoxPayCheckout(this, token, ::onPaymentResult, shopperToken)
                checkout.testEnv = true
                checkout.display()
            }
        }
    }
    fun onPaymentResult(result: PaymentResultObject) {
        if (result.status == "Success") {
            binding.button.setText("Payment has been Completed. please use another token")
        } else {
            binding.button.isEnabled = true
            binding.button.text = "Proceed"
        }
    }
}