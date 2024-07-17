package com.boxpay.checkout.sdk

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import dalvik.system.DexClassLoader
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SdkUpdater(private val context: Context) {
    companion object {
        private const val CURRENT_VERSION = BuildConfig.SDK_VERSION
        private const val JITPACK_URL =
            "https://jitpack.io/api/builds/com.github.BoxPay-SDKs/checkout-android-sdk/"
    }

    private val client = OkHttpClient()

    fun isUpdateAvailable(callback: (Boolean, String) -> Unit) {
        val request = Request.Builder().url(JITPACK_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody)
                    val versionsObject = json.getJSONObject("com.github.BoxPay-SDKs")
                        .getJSONObject("checkout-android-sdk")
                    val versionsMap = versionsObject.toMap()
                    val lastVersionWithoutVAndOkStatus = versionsMap.filter { (version, status) ->
                        !version.contains("v") && !version.contains("beta") && status == "ok"
                    }.keys.maxWithOrNull { v1, v2 -> compareVersions(v1, v2) }
                    println("===new version $lastVersionWithoutVAndOkStatus")
                    callback(
                        CURRENT_VERSION != lastVersionWithoutVAndOkStatus,
                        lastVersionWithoutVAndOkStatus ?: ""
                    )
                } else {
                    callback(false, "")
                }
            }
        })
    }

    fun downloadUpdate(latestVersion: String, callback: (File?) -> Unit) {
        val downloadUrl =
            "https://jitpack.io/com/github/BoxPay-SDKs/checkout-android-sdk/$latestVersion/checkout-android-sdk-$latestVersion.aar"
        val request = Request.Builder().url(downloadUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val newSdkFile = File(
                        context.filesDir,
                        "boxpay_sdk_new/checkout-android-sdk-$latestVersion.aar"
                    )
                    newSdkFile.parentFile?.mkdirs()

                    FileOutputStream(newSdkFile).use { outputStream ->
                        outputStream.write(response.body?.bytes())
                    }

                    val sharedPreferences =
                        context.getSharedPreferences("sdk_prefs", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putBoolean("newSdkAvailable", true)
                        apply()
                    }

                    println("=====file $newSdkFile")

                    callback(newSdkFile)
                } else {
                    callback(null)
                }
            }
        })
    }

    fun installUpdate(sdkFile: File, callback: (DexClassLoader) -> Unit) {
        val dexClassLoader = DexClassLoader(
            sdkFile.absolutePath,
            context.cacheDir.absolutePath,
            null,
            context.classLoader
        )

        println("=====install $dexClassLoader")
        callback(dexClassLoader)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reinitializeSdk(dexClassLoader: DexClassLoader, token: String) {
        try {
            val sdkClass = dexClassLoader.loadClass("com.boxpay.checkout.sdk.BoxPayCheckout")

            // Use reflection to find the correct Function1 type
            val functionType = Class.forName("kotlin.jvm.functions.Function1")

            // Print available constructors for debugging
            sdkClass.constructors.forEach { ctor ->
                println("Constructor: ${ctor.parameterTypes.joinToString()}")
            }

            // Get the constructor with the correct parameter types
            val constructor = sdkClass.constructors.firstOrNull { ctor ->
                val paramTypes = ctor.parameterTypes
                paramTypes.size == 4 &&
                        paramTypes[0] == Context::class.java &&
                        paramTypes[1] == String::class.java &&
                        functionType.isAssignableFrom(paramTypes[2]) &&
                        paramTypes[3] == java.lang.Boolean::class.java
            } ?: throw NoSuchMethodException("No matching constructor found")

            // Create a lambda for the payment result callback
            val paymentResultCallback: (PaymentResultObject) -> Unit = { paymentResult ->
                // Handle payment result here
            }

            // Pass 'false' explicitly for the Boolean parameter
            val sdkInstance = constructor.newInstance(
                context,
                token,
                paymentResultCallback,
                false // Ensure this is a non-null Boolean
            )

            println("====method ${sdkClass.methods}")
            val openBottomSheetMethod = sdkClass.getDeclaredMethod("openBottomSheet")
            val sharedPreferences =
                context.getSharedPreferences("sdk_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean("newSdkAvailable", false)
                apply()
            }
            println("======reinitialize $openBottomSheetMethod")
            openBottomSheetMethod.invoke(sdkInstance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun JSONObject.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = getString(key)
        }
        return map
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split('.')
        val parts2 = version2.split('.')
        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val part1 = parts1.getOrElse(i) { "0" }.toInt()
            val part2 = parts2.getOrElse(i) { "0" }.toInt()
            if (part1 != part2) {
                return part1 - part2
            }
        }
        return 0
    }
}
