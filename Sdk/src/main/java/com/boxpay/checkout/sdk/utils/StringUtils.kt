package com.boxpay.checkout.sdk.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.O)
fun formatToISO8601WithCurrentTime(dateString: String): String {
    // Define a formatter to parse the input date string with time
    val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    val dateFormatter = DateTimeFormatter.ISO_DATE

    // Try to parse the input as LocalDateTime
    val date = try {
        LocalDateTime.parse(dateString, dateTimeFormatter).toLocalDate()
    } catch (e: Exception) {
        // If parsing as LocalDateTime fails, try parsing as LocalDate
        LocalDate.parse(dateString, dateFormatter)
    }

    // Create a LocalDateTime with the fixed time set to "00:00:00"
    val dateTime = LocalDateTime.of(date, LocalTime.MIDNIGHT)

    // Convert LocalDateTime to ZonedDateTime in UTC
    val zonedDateTime = dateTime.atZone(ZoneOffset.UTC)

    // Format to ISO 8601 with "T00:00:00Z"
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    return zonedDateTime.format(formatter)
}

fun generateRandomAlphanumericString(length: Int): String {
    val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

private fun getBaseUrl(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    return sharedPreferences.getString("baseUrl", null) ?: "apis.boxpay.in"
}

fun getSessionApiUrl(context: Context): String {
    return "https://${getBaseUrl(context)}/v0/checkout/sessions/"
}

fun getAnalyticsUrl(context: Context): String {
    return "https://${getBaseUrl(context)}/v0/ui-analytics"
}

fun getSessionToken(context: Context) : String {
    val sharedPreferences = context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    return sharedPreferences.getString("token", null) ?: ""
}

fun getShopperToken(context: Context) : String {
    val sharedPreferences = context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    return sharedPreferences.getString("shopperToken", null) ?: ""
}

fun getValueAtIndexByKey(jsonArray: JSONArray, key: String, index:Int) : String? {
    return jsonArray.optJSONObject(index).optString(key)
}

fun String?.clean(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && it.lowercase() != "null" }

fun getAppName(context: Context): String {
    return context.applicationInfo.loadLabel(context.packageManager).toString()
}

fun formatDate(isoDate: String): String {
    val zonedDateTime = ZonedDateTime.parse(isoDate)
    val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    return zonedDateTime.format(formatter)
}