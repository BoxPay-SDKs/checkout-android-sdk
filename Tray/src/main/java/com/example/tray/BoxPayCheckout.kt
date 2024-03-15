package com.example.tray

import SingletonClass
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.CallBackFunctions
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

class BoxPayCheckout( private val context : Context, private val token: String,val onPaymentResult: (String) -> Unit){
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var editor : SharedPreferences.Editor

    fun display() {
        sharedPreferences = context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        putTransactionDetailsInSharedPreferences()
        Log.d("Checked","Executed minView Checkout")
        fetchShopperDetailsAndUpdateInSharedPreferences()
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

    private fun fetchShopperDetailsAndUpdateInSharedPreferences(){
        val url = "https://test-apis.boxpay.tech/v0/checkout/sessions/${token}"
        val queue: RequestQueue = Volley.newRequestQueue(context)
        Log.d("fetchSHopperDetailsAndUpdate","Checkout")
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            try {

                val paymentDetailsObject = response.getJSONObject("paymentDetails")

                val shopperJSONObject = paymentDetailsObject.getJSONObject("shopper")
                Log.d("firstname",shopperJSONObject.getString("firstName"))
                editor.putString("firstName",shopperJSONObject.getString("firstName"))
                editor.putString("lastName",shopperJSONObject.getString("lastName"))
                editor.putString("gender",shopperJSONObject.getString("gender"))
                editor.putString("phoneNumber",shopperJSONObject.getString("phoneNumber"))
                editor.putString("email",shopperJSONObject.getString("email"))
                editor.putString("uniqueReference",shopperJSONObject.getString("uniqueReference"))


                val deliveryAddressObject = shopperJSONObject.getJSONObject("deliveryAddress")
                logJsonObject(deliveryAddressObject)
                editor.putString("address1",deliveryAddressObject.getString("address1"))
                editor.putString("address2",deliveryAddressObject.getString("address2"))
                editor.putString("address3",deliveryAddressObject.getString("address3"))
                editor.putString("city",deliveryAddressObject.getString("city"))
                editor.putString("state",deliveryAddressObject.getString("state"))
                editor.putString("countryCode",deliveryAddressObject.getString("countryCode"))
                editor.putString("postalCode",deliveryAddressObject.getString("postalCode"))
                Log.d("postalCode",deliveryAddressObject.getString("postalCode"))

                val orderObject = paymentDetailsObject.getJSONObject("order")
                editor.putString("originalAmount",orderObject.getString("originalAmount"))


                val moneyObject = paymentDetailsObject.getJSONObject("money")
                editor.putString("currencySymbol",moneyObject.getString("currencySymbol"))

                val ipAddress = convertIPv6ToIPv4(getLocalIpAddress())
                Log.d("ipAddress",ipAddress.toString())
                editor.putString("ipAddress",ipAddress)

                editor.apply()


                openBottomSheet()


            } catch (e: Exception) {
                Log.d("Error Occurred", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", " fetching Checkout error response: $errorResponse")
            }
        })
        queue.add(jsonObjectAll)

    }


    private fun putTransactionDetailsInSharedPreferences() {
        editor.putString("token", token)
        Log.d("token added to sharedPreferences", token)
        editor.apply()

    }


    private fun getLocalIpAddress(): String {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in Collections.list(networkInterfaces)) {
                val inetAddresses = Collections.list(networkInterface.inetAddresses)
                for (inetAddress in inetAddresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
    private fun convertIPv6ToIPv4(ipv6Address: String): String? {
        try {
            val inet6Address = InetAddress.getByName(ipv6Address)
            if (inet6Address is Inet6Address) {
                if (inet6Address.isLinkLocalAddress) {
                    // Handle link-local address case
                    return "127.0.0.1"
                }
                val inetAddress = InetAddress.getByAddress(null, inet6Address.hostAddress.toByteArray())
                return inetAddress.hostAddress
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Checkout", jsonStr)
    }
}