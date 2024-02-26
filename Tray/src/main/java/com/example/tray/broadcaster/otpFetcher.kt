package com.example.tray.broadcaster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.tray.interfaces.OTPReceiveListener
import java.util.regex.Pattern

class otpFetcher(private val context: Context) : BroadcastReceiver() {

    private var otpReceiveListener: OTPReceiveListener? = null

    fun setOTPReceiveListener(listener: OTPReceiveListener) {
        otpReceiveListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("onReceive","got inside receiver")
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val bundle = intent.extras
            if (bundle != null) {
                for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    val messageBody = smsMessage.messageBody
                    Log.d("messageBody",messageBody)
                    val otp = extractOTP(messageBody)
                    if (otp != null && otpReceiveListener != null) {
                        otpReceiveListener?.fetchOTP(otp)
                    }
                }
            }
        }
    }

    private fun extractOTP(messageBody: String): String? {
        val pattern = Pattern.compile("(\\d{6})") // Assuming OTP is 6 digits long
        val matcher = pattern.matcher(messageBody)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null // If OTP is not found
        }
    }
}