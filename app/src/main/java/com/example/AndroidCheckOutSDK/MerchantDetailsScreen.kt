package com.example.AndroidCheckOutSDK

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.AndroidCheckOutSDK.databinding.ActivityMerchantDetailsScreenBinding

class MerchantDetailsScreen : AppCompatActivity() {
    private val binding : ActivityMerchantDetailsScreenBinding by lazy {
        ActivityMerchantDetailsScreenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}