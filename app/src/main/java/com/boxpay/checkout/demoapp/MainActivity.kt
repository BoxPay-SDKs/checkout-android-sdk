package com.boxpay.checkout.demoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.demoapp.databinding.ActivityMainBinding
import com.boxpay.checkout.sdk.FailureScreenForTesting
import com.boxpay.checkout.sdk.SuccessScreenForTesting
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)




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
                e.printStackTrace()
            }
        }

        binding.enterTokenButton.setOnClickListener {
            val intent = Intent(this, MerchantDetailsScreen::class.java)
            startActivity(intent)
        }

        binding.fetchLatestVersion.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val latestVersion = getLatestVersionFromJitPack("com.github.BoxPay-SDKs", "checkout-android-sdk")
                Toast.makeText(this@MainActivity, "Latest version is $latestVersion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onPaymentResultCallback(result: PaymentResultObject) {
        if (result.status == "Success") {
            Log.d("onPaymentResultCallback", "Success")
            val intent = Intent(this, SuccessScreenForTesting::class.java)
            startActivity(intent)
        } else {
            Log.d("onPaymentResultCallback", "Failure")
            val intent = Intent(this, FailureScreenForTesting::class.java)
            startActivity(intent)
        }
    }


    suspend fun getLatestVersionFromJitPack(groupId: String, artifactId: String): String {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                // Log your message here (you can log to Logcat)
                Log.d("HttpLoggingInterceptor", message)
            }
        }).apply {
            level = HttpLoggingInterceptor.Level.BODY  // Set logging level
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)  // Add logging interceptor
            .build()

        val request = Request.Builder()
            .url("https://jitpack.io/api/builds/$groupId/$artifactId/latestOk/")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val responseBody = response.body?.string()
                val json = JSONObject(responseBody)
                json.getString("version") // Return the version from JSON
            } catch (e: IOException) {
                e.printStackTrace()
                "Unknown"
            }
        }
    }
}