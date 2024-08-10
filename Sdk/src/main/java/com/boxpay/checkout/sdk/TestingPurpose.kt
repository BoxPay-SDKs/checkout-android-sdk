package com.boxpay.checkout.sdk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject

class TestingPurpose : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testing_purpose)
        BoxPayCheckout(this, "3b9be777-2abc-491d-b2d5-6493104af4ab", ::onPaymentResultCallback,false).display()
    }
    fun onPaymentResultCallback(result : PaymentResultObject){
        if(result.status == "Success"){
            val intent = Intent(this,SuccessScreenForTesting :: class.java)
            startActivity(intent)
        }else{
            val intent = Intent(this,FailureScreenForTesting :: class.java)
            startActivity(intent)
        }
    }
}