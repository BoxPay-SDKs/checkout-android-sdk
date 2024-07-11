package com.boxpay.checkout.sdk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class FailureScreenForTesting : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_failure_screen_for_testing)
    }
}