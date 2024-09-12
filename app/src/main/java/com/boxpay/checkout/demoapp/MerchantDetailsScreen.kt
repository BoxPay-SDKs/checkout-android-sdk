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
import org.json.JSONObject

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
        val orderJson = JSONObject(
            """
                {
    "action": "paymentPage",
    "merchantId": "lGfqzNSKKA_razorpay",
    "clientId": "railyatripp",
    "orderId": "1725133659jpp1988719986",
    "amount": "2180",
    "toolbarSecondLine": "NZM - BDTS | 2024-08-21",
    "customerId": "cth_udBM2B34PMgSZXjX",
    "customerEmail": "avinash.singh@railyatri.in",
    "customerMobile": "9555681381",
    "orderDetails": "{\"metadata.webhook_url\":\"https://payment-test.railyatri.in/api/juspay/update-payment\",\"udf1\":\"30313407\",\"udf2\":\"4\",\"return_url\":\"https://payment-test.railyatri.in/payment/juspay/process\",\"order_id\":\"1725133659jpp1988719986\",\"merchant_id\":\"lGfqzNSKKA_razorpay\",\"amount\":\"2180.0\",\"timestamp\":\"1725133662\",\"customer_id\":\"cth_udBM2B34PMgSZXjX\",\"currency\":\"INR\",\"customer_email\":\"avinash.singh@railyatri.in\",\"customer_phone\":\"9555681381\",\"metadata.merchant_container_list\":\"[{\\\"payment_method\\\":\\\"RY_CASH\\\",\\\"payment_method_type\\\":\\\"MERCHANT_CONTAINER\\\",\\\"display_name\\\":\\\"RY CASH\\\",\\\"eligible_amount\\\":10,\\\"balance_amount\\\":400,\\\"walletIconURL\\\":\\\"https://cdn-icons-png.flaticon.com/512/216/216490.png\\\"},{\\\"payment_method\\\":\\\"RY_CASH_PLUS\\\",\\\"payment_method_type\\\":\\\"MERCHANT_CONTAINER\\\",\\\"display_name\\\":\\\"RY CASH PLUS\\\",\\\"eligible_amount\\\":400,\\\"balance_amount\\\":400,\\\"walletIconURL\\\":\\\"https://cdn-icons-png.flaticon.com/512/216/216490.png\\\"}]\"}",
    "product_summary": "[[{\"type\":\"text\",\"text\":\"Updated Status:\",\"textSize\":14,\"color\":\"#000000\"},{\"type\":\"text\",\"text\":\"50 WAITLIST\",\"textSize\":16,\"fontType\":\"Bold\",\"color\":\"#00B829\"},{\"type\":\"linegap\",\"gap\":0}],[{\"type\":\"linegap\",\"gap\":4},{\"type\":\"text\",\"text\":\"High confirmation chance tickets are likely to get confirmed!\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#F0F7FD\"}],[{\"type\":\"text\",\"text\":\"Review Ticket\",\"textSize\":15,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\"Booking ID: 30313407\",\"textSize\":11,\"fontType\":\"SemiBold\",\"color\":\"#333333\"}],[{\"type\":\"divider\",\"thickness\":2,\"color\":\"#ffffff\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#F0F7FD\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":0},{\"type\":\"text\",\"text\":\"12910 - Garib Rath Express\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":4},{\"type\":\"text\",\"text\":\"2 Travellers| Class- 3A| Quota- GN\",\"textSize\":9,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":4},{\"type\":\"text\",\"text\":\"Saturday, 30 Sep\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"Saturday, 30 Sep\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":0},{\"type\":\"text\",\"text\":\"NZM , 16:30\",\"textSize\":18,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"image\",\"url\":\"https://images.railyatri.in/ry_images_prod/train-icon-1715841210.png\",\"size\":30},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\"09:15, BDTS \",\"textSize\":18,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":0},{\"type\":\"text\",\"text\":\"DELHI HAZRAT NIZAMUDDIN\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\"---- 16:45 h ----\",\"textSize\":10,\"color\":\"#888888\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\" MUMBAI BANDRA TERMINUS\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#F0F7FD\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"accordion\",\"limit\":1,\"content\":[[{\"type\":\"text\",\"text\":\"Passenger Details & Fare Breakup\",\"textSize\":15,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"toggleImage\",\"openIcon\":\"https://assets.juspay.in/hyper/images/internalPP/ic_arrow_down.png\",\"closeIcon\":\"https://assets.juspay.in/hyper/images/internalPP/ic_arrow_up.png\",\"size\":20}],[{\"type\":\"text\",\"text\":\"1. Test (Male)\",\"textSize\":14,\"color\":\"#333333\"},{\"type\":\"text\",\"text\":\"25 \",\"textSize\":12,\"color\":\"#888888\"},{\"type\":\"text\",\"text\":\"| Lower Berth | \",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"text\",\"text\":\"2. Test (Male)\",\"textSize\":14,\"color\":\"#333333\"},{\"type\":\"text\",\"text\":\"30 \",\"textSize\":12,\"color\":\"#888888\"},{\"type\":\"text\",\"text\":\"| Lower Berth | \",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#E5E5E5\"}],[{\"type\":\"text\",\"text\":\"Fare Breakup\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"space\",\"width\":10,\"weight\":1}],[{\"type\":\"text\",\"text\":\"Ticket Base Fare\",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹2180.0\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"text\",\"text\":\"Agent Service Charge\",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹40.0\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"text\",\"text\":\"IRCTC Conv. Fee\",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹35.4\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"text\",\"text\":\"Net Amount payable\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹2304\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"space\",\"width\":1,\"weight\":2}]]}]]",
    "signature": "aSUaLK6cvlPgMptvU7QqF+Asdg4p5+ZuYekFCriW98ijgsXNrYwWG19aQEwOJ3lhZFGmGdcUR9jV27fVdGktzH2N0eAO88yxMp/tmopbEbObJOypXdvPtlRUW6FNS0YAfz6RYWZJ7DB0ZNciuQ83Zei8s7d4UzPB41kKy4lwi9DqtQHAQOQUl9eSEqhY80VAcAd40X0/Tuf2p8X4/vhi6r0oaie1w2acJWCnxOrErLk3Z2W1vuw2B28aurBEQPS9qDOhn9Q49DSwIwg7nAP1XkE2szh1ybvcrUpRzDgdNU7ozLxSAyVpEyH15C9rNYsIuxEyiZbHFk9dr0gLbPqlPw==",
    "merchantKeyId": "13031",
    "language": "English"
}
               """
        )

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