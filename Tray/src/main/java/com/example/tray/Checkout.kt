package com.example.tray

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class Checkout{
    fun minView(token: String, successScreenFullReferencePath: String,context: Context) {
        putTransactionDetailsInSharedPreferences(token,successScreenFullReferencePath,context)
        Log.d("Checked","Executed minView Checkout")
        if (context is Activity) {
            Log.d("Checked","inside context is activity")
            val activity = context as AppCompatActivity // or FragmentActivity, depending on your activity type
            val fragmentManager = activity.supportFragmentManager
            // Now you can use fragmentManager
            val bottomSheet = MainBottomSheet()
            bottomSheet.show(fragmentManager, "MainBottomSheet")
        }
    }
    private fun putTransactionDetailsInSharedPreferences(token: String, successScreenFullReferencePath: String,context: Context) {
        val sharedPreferences =
            context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        Log.d("token added to sharedPreferences", token)
        editor.putString("successScreenFullReferencePath", successScreenFullReferencePath)
        Log.d(
            "success Screen added to sharedPreferences",
            successScreenFullReferencePath
        )
        editor.apply()
    }
}