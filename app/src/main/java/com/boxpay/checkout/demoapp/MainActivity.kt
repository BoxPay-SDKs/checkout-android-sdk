package com.boxpay.checkout.demoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.demoapp.databinding.ActivityMainBinding
import com.boxpay.checkout.sdk.FailureScreenForTesting
import com.boxpay.checkout.sdk.SuccessScreenForTesting
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)




        binding.openByDefault.setOnClickListener {
            //        var intent: Intent? = null
            try {
                intent = Intent(
                    this,
                    Class.forName("com.example.tray.Check")
                )
                startActivity(intent)
                finish()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }

        binding.enterTokenButton.setOnClickListener {
            val intent = Intent(this, MerchantDetailsScreen::class.java)
            startActivity(intent)
        }

//        val checkout = BoxPayCheckout(this,"c472cb28-77d1-4d57-ad52-dfefc70d8015",::onPaymentResultCallback)
//        checkout.display()
    }

    fun onPaymentResultCallback(result: PaymentResultObject) {
        if (result.status == "Success") {
            Log.d("onPaymentResultCallback", "Success")
            val intent = Intent(this, SuccessScreenForTesting::class.java)
            startActivity(intent)
        } else {
            Log.d("onPaymentResultCallback", "Failure")
            val intent = Intent(this, FailureScreenForTesting::class.java)
            startActivity(intent)
        }
    }
}