package com.boxpay.checkout.sdk.utils

import android.content.Intent
import androidx.fragment.app.Fragment
import com.boxpay.checkout.sdk.OTPScreenWebView
import com.boxpay.checkout.sdk.UPITimerBottomSheet
import org.json.JSONObject

fun openWebView(fragment: Fragment, response: JSONObject) {
    val type = response.getJSONArray("actions").getJSONObject(0).getString("type")
    val transactionRequest = response.getJSONArray("actions").getJSONObject(0).optJSONObject("data")?.optString("txnreq")
    val url = if (type.contains("html", true)) {
        response.getJSONArray("actions").getJSONObject(0).getString("htmlPageString")
    } else {
        response.getJSONArray("actions").getJSONObject(0).getString("url")
    }

    val intent = Intent(fragment.context, OTPScreenWebView::class.java)
    intent.putExtra("url", url)
    intent.putExtra("type", type)
    intent.putExtra("transactionRequest", transactionRequest)
    fragment.startActivityForResult(intent, 333)
}

fun showWebOrTimerScreen(fragment: Fragment,response: JSONObject, displayUserId : String , startFetchStatusCall : ()-> Unit) {
    val actionObject = response.optJSONArray("actions")
        ?.takeIf { it.length() > 0 }
        ?.getJSONObject(0)

    val actionType = actionObject?.optString("type", null)

    when (actionType) {
        "html", "url" -> {
            openWebView(fragment,response)
            startFetchStatusCall()
        }
        "timer" -> {
            val expirySec = actionObject?.optInt("expirySec", DEFAULT_UPI_TIMER_IN_SEC) ?: DEFAULT_UPI_TIMER_IN_SEC
            openUPITimerBottomSheet(displayUserId, expirySec, fragment)
        }
        else -> {
            openUPITimerBottomSheet(displayUserId, DEFAULT_UPI_TIMER_IN_SEC, fragment) // fallback
        }
    }
}

private fun openUPITimerBottomSheet(displayName : String,timerInSec: Int,fragment: Fragment) {
    val bottomSheetFragment = UPITimerBottomSheet.newInstance(displayName, timerInSec)
    fragment.childFragmentManager.beginTransaction()
        .add(bottomSheetFragment, "UPITimerBottomSheet")
        .commitAllowingStateLoss()
}

const val DEFAULT_UPI_TIMER_IN_SEC = 300

