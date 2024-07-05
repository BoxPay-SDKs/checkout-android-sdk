package com.example.tray

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.tray.paymentResult.PaymentResultObject

class TestingPurpose : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testing_purpose)

            BoxPayCheckout(this, "3b9be777-2abc-491d-b2d5-6493104af4ab", ::onPaymentResultCallback,false).display()
    }
    fun onPaymentResultCallback(result : PaymentResultObject){
        if(result.status == "Success"){
            Log.d("onPaymentResultCallback","Success")
            val intent = Intent(this,SuccessScreenForTesting :: class.java)
            startActivity(intent)
        }else{
            Log.d("onPaymentResultCallback","Failure")
            val intent = Intent(this,FailureScreenForTesting :: class.java)
            startActivity(intent)
        }
    }
}