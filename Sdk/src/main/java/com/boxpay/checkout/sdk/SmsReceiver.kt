package com.boxpay.checkout.sdk

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status


class SmsReceiver : BroadcastReceiver() {
    private val SMS_CONSENT_REQUEST = 1010

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
//                        if (consentIntent != null) {
//                            startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
//                        }
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
