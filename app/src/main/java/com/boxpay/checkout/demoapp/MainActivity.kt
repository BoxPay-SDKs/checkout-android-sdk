package com.boxpay.checkout.demoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.demoapp.databinding.ActivityMainBinding
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig


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
        val config = ClarityConfig("o4josf35jv")
        Clarity.initialize(applicationContext, config)


        binding.openByDefault.setOnClickListener {
            //        var intent: Intent? = null
            try {
                intent = Intent(
                    this,
                    Check::class.java
                )
                startActivity(intent)
            } catch (e: ClassNotFoundException) {

            }
        }

        binding.enterTokenButton.setOnClickListener {
            val intent = Intent(this, MerchantDetailsScreen::class.java)
            startActivity(intent)
        }
    }
}