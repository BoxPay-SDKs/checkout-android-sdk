package com.example.tray

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.tray.databinding.FragmentOTPBottomSheetBinding
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OTPBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentOTPBottomSheetBinding
    private val SMS_CONSENT_REQUEST = 1010
    val smsVerificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                val extras = intent.extras
                val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

                when (smsRetrieverStatus.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        // Get consent intent
                        val consentIntent =
                            extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        try {
                            // Start activity to show consent dialog to user, activity must be started in
                            // 5 minutes, otherwise you'll receive another TIMEOUT intent
                            startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
                        } catch (e: ActivityNotFoundException) {
                            // Handle the exception ...
                        }
                    }

                    CommonStatusCodes.TIMEOUT -> {
                        // Time out occurred, handle the error.
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentOTPBottomSheetBinding.inflate(layoutInflater, container, false)
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(
            requireContext(),
            smsVerificationReceiver,
            intentFilter,
            ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS
        )
        initAutoFill()
        return binding.root
    }

    private fun initAutoFill() {
        SmsRetriever.getClient(requireContext())
            .startSmsUserConsent(null)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ADD UPI ID listening", "here")
                } else {
                    Log.d("ADD UPI ID listening failed", "here")
                }
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1010) {
            // Result from SMS consent activity
            if (resultCode == Activity.RESULT_OK && data != null) {
                // User granted consent
                val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)

                binding.sampleTextView.text = message.toString()


                Log.d("message fetched Example",message.toString())
                // Handle OTP
            } else {
                // User denied consent
                // Handle denial
            }
        }
    }

    companion object {

    }
}