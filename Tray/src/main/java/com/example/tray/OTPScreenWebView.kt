package com.example.tray

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.tray.databinding.ActivityOtpscreenWebViewBinding

class OTPScreenWebView : AppCompatActivity() {
    private val binding by lazy{
        ActivityOtpscreenWebViewBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val receivedUrl= intent.getStringExtra("url")
        Log.d("url",receivedUrl.toString())

        binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
    }
}