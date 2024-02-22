package com.example.tray

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tray.databinding.FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date

class PaymentSuccessfulWithDetailsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
    private var token: String? = null
    private var successScreenFullReferencePath : String ?= null
    private var transactionID: String? = null
    private var transactionAmount: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPaymentSuccessfulWithDetailsBottomSheetBinding.inflate(layoutInflater,container,false)



        fetchTransactionDetailsFromSharedPreferences()
        binding.transactionAmountTextView.text = transactionAmount
        binding.transactionIDTextView.text = transactionID
        binding.transactionDateAndTimeTextView.text = getCurrentDateAndTimeInFormattedString()

        binding. proceedButton.setOnClickListener(){
            openActivity(successScreenFullReferencePath.toString(),requireContext())
//            callFunctionInActivity()
        }
        return binding.root
    }
//    private fun callFunctionInActivity() {
//        val activity = activity
//        if (activity is OTPScreenWebView) {
//            activity.killOTPWeViewActivity()
//        }
//    }

    private fun openActivity(activityPath: String, context: Context) {
        if (context is AppCompatActivity) {
            try {
                // Get the class object for the activity using reflection
                val activityClass = Class.forName(activityPath)
                // Create an instance of the activity using Kotlin reflection
                val activityInstance = activityClass.getDeclaredConstructor().newInstance() as AppCompatActivity

                // Check if the activity is a subclass of AppCompatActivity
                if (activityInstance is AppCompatActivity) {
                    // Start the activity
                    context.startActivity(Intent(context, activityClass))
                } else {
                    // Log an error or handle the case where the activity is not a subclass of AppCompatActivity
                }
            } catch (e: ClassNotFoundException) {
                // Log an error or handle the case where the activity class cannot be found
                Toast.makeText(requireContext(), "Failed to open\nmerchants success screen", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Log an error or handle the case where the context is not an AppCompatActivity
        }
    }

    private fun getCurrentDateAndTimeInFormattedString() : String{
        val currentDateTime = Date()


        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")


        return dateFormat.format(currentDateTime)
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token","empty")
        Log.d("data fetched from sharedPreferences",token.toString())
        successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")
        Log.d("success screen path fetched from sharedPreferences",successScreenFullReferencePath.toString())
        transactionID = sharedPreferences.getString("transactionId","empty")
        Log.d("transactionID fetched from sharedPreferences",transactionID.toString())
        transactionAmount = sharedPreferences.getString("transactionAmount","empty")
        Log.d("success screen path fetched from sharedPreferences",transactionID.toString())
    }

    object SharedPreferencesHelper {

        private const val SHARED_PREF_NAME = "TransactionDetails"

        fun getAllKeyValuePairs(context: Context): Map<String, *> {
            val sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.all
        }
    }

    companion object {

    }
}