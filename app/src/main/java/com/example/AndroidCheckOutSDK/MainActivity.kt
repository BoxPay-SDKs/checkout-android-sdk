package com.example.AndroidCheckOutSDK

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.AndroidCheckOutSDK.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.openByDefault.setOnClickListener() {
            intent = Intent(
                this,
                Class.forName("com.example.tray.Check")
            )
            startActivity(intent)
            finish()
        }

        binding.enterTokenButton.setOnClickListener() {
            val intent = Intent(this, MerchantDetailsScreen::class.java)
            startActivity(intent)
        }
    }
}