package com.example.tray

import SingletonClass
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.CallBackFunctions
import com.example.tray.paymentResult.PaymentResultObject
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

class BoxPayCheckout(private val context: Context, private val token: String, val onPaymentResult: (PaymentResultObject) -> Unit, private val sandboxEnabled: Boolean = false){
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()
    private var environment : String = ""
    init {
        if(sandboxEnabled){
            editor.putString("environment", "sandbox-")
            this.environment = "sandbox-"
        }else{
            editor.putString("environment","")
            this.environment = ""
        }
        editor.apply()
    }
    fun display() {
        if (context is Activity) {
            Log.d("Checked","inside context if condition")
            val activity = context as AppCompatActivity // or FragmentActivity, depending on your activity type
            val fragmentManager = activity.supportFragmentManager
            // Now you can use fragmentManager
//            val bottomSheet = BottomSheetLoadingSheet()
//            bottomSheet.show(fragmentManager, "BottomSheetLoadingSheet")
            openBottomSheet()
        }



        Log.d("Checking Time issue","Called display")
        Log.d("environment variable",sharedPreferences.getString("environment","null").toString())
        putTransactionDetailsInSharedPreferences()
        Log.d("Checked","Executed minView Checkout")
        Log.d("Checking Time issue","Before fetching shopper details")
//        fetchShopperDetailsAndUpdateInSharedPreferences()
    }

    private fun openBottomSheet(){
        initializingCallBackFunctions()

        if (context is Activity) {
            Log.d("Checked","inside context if condition")
            val activity = context as AppCompatActivity // or FragmentActivity, depending on your activity type
            val fragmentManager = activity.supportFragmentManager
            // Now you can use fragmentManager
            val bottomSheet = MainBottomSheet()
            bottomSheet.show(fragmentManager, "MainBottomSheet")
        }

    }
    fun initializingCallBackFunctions(){
        Log.d("result for callback","checkingPurpose")
        val callBackFunctions = CallBackFunctions(onPaymentResult)
        SingletonClass.getInstance().callBackFunctions = callBackFunctions
    }




    private fun putTransactionDetailsInSharedPreferences() {
        editor.putString("token", token)
        Log.d("token added to sharedPreferences", token)
        editor.apply()
    }

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Checkout", jsonStr)
    }
}