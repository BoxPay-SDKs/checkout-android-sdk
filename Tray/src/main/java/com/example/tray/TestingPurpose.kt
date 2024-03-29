package com.example.tray

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.example.tray.paymentResult.PaymentResultObject

class TestingPurpose : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testing_purpose)


        Log.d("called testing purpose","here")

            BoxPayCheckout(this, "3b9be777-2abc-491d-b2d5-6493104af4ab", ::onPaymentResultCallback).display()
         // 5000 milliseconds = 5 seconds
    }
    fun onPaymentResultCallback(result : PaymentResultObject){
        if(result.result == "Success"){
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