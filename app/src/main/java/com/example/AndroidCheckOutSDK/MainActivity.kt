package com.example.AndroidCheckOutSDK

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.tray.BoxPayCheckout
import com.example.tray.FailureScreenForTesting
import com.example.tray.SuccessScreenForTesting
import com.example.tray.paymentResult.PaymentResultObject


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var intent: Intent? = null
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
//       val intent = Intent(this, MerchantDetailsScreen ::class.java)
//        startActivity(intent)

//        val checkout = BoxPayCheckout(this,"c472cb28-77d1-4d57-ad52-dfefc70d8015",::onPaymentResultCallback)
//        checkout.display()
    }
    fun onPaymentResultCallback(result : PaymentResultObject){
        if(result.result == "Success"){
            Log.d("onPaymentResultCallback","Success")
            val intent = Intent(this, SuccessScreenForTesting :: class.java)
            startActivity(intent)
        }else{
            Log.d("onPaymentResultCallback","Failure")
            val intent = Intent(this, FailureScreenForTesting :: class.java)
            startActivity(intent)
        }
    }
}