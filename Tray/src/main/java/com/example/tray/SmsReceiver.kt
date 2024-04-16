package com.example.tray

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.util.regex.Pattern



class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Broadcast Receiver","inside onReceive")
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val iterator = messages?.iterator()

            if (iterator != null) {
                while (iterator.hasNext()) {
                    val sms = iterator.next()
                    val sender = sms.displayOriginatingAddress
                    val messageBody = sms.messageBody
                    handleIncomingSms(context, sender, messageBody)
                }
            }
        }
    }

    private fun handleIncomingSms(context: Context, sender: String, messageBody: String) {
        // Implement your logic to handle the incoming SMS
        Log.d("Broadcast Receiver",messageBody)
        val otp = extractOTP(messageBody)
        if (otp != null) {
            // OTP extracted, trigger appropriate action (e.g., show dialog, process OTP)
            // You can use LocalBroadcastManager or other means to communicate this event
            // to your activity/fragment
        }
    }

    private fun extractOTP(message: String): String? {
        val pattern = Pattern.compile("\\b\\d{6}\\b") // Match 6-digit number
        val matcher = pattern.matcher(message)
        return if (matcher.find()) {
            matcher.group(0) // Extract the OTP
        } else {
            null // OTP not found
        }
    }
}
