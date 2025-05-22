package com.boxpay.checkout.sdk.utils

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
fun getDOBAndPanEffectiveEntry(sharedPreferences: SharedPreferences): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()

    sharedPreferences.getEffectiveString(
        chosenKey = "dateOfBirthChosen",
        storedKey = "dateOfBirth",
        validator = { it.isNotBlank() && it != "null" },
        formatter = { raw -> formatToISO8601WithCurrentTime(raw) }
    )?.let { result.add("dateOfBirth" to it) }

    sharedPreferences.getEffectiveString(
        chosenKey = "panNumberChosen",
        storedKey = "panNumber",
        validator = { it.isNotBlank() && it != "null" }
    )?.let { result.add("panNumber" to it) }

    return result
}


fun SharedPreferences.getEffectiveString(
    chosenKey: String,
    storedKey: String,
    validator: (String) -> Boolean = { it.isNotBlank() && it != "null" },
    formatter: (String) -> String = { it }
): String? {
    return listOf(chosenKey, storedKey)
        .mapNotNull { getString(it, null) }
        .firstOrNull { validator(it) }
        ?.let { formatter(it) }
}