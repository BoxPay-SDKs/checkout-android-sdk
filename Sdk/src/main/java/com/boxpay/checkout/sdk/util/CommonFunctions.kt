package com.boxpay.checkout.sdk.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object CommonFunctions {

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatToISO8601WithCurrentTime(dateString: String): String {
        // Check if the dateString is already in the desired ISO 8601 format with "T00:00:00Z"
        val iso8601Pattern = Regex("\\d{4}-\\d{2}-\\d{2}T00:00:00Z")
        if (iso8601Pattern.matches(dateString)) {
            return dateString
        }

        // Parse the input date string as a LocalDate
        val date = LocalDate.parse(dateString)

        // Create a LocalDateTime with the fixed time set to "00:00:00"
        val dateTime = LocalDateTime.of(date, LocalTime.MIDNIGHT)

        // Convert LocalDateTime to ZonedDateTime in UTC
        val zonedDateTime = dateTime.atZone(ZoneOffset.UTC)

        // Format to ISO 8601 with "T00:00:00Z"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        return zonedDateTime.format(formatter)
    }

}