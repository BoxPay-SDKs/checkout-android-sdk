package com.boxpay.checkout.demoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.demoapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val sharedPrefs = getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE).edit()
        sharedPrefs.clear()
        sharedPrefs.apply()


        binding.openByDefault.setOnClickListener {
            //        var intent: Intent? = null
            try {
                intent = Intent(
                    this,
                    Class.forName("com.boxpay.checkout.sdk.Check")
                )
                startActivity(intent)
                finish()
            } catch (e: ClassNotFoundException) {

            }
        }

        binding.enterTokenButton.setOnClickListener {
            val intent = Intent(this, MerchantDetailsScreen::class.java)
            startActivity(intent)
        }

    }
}