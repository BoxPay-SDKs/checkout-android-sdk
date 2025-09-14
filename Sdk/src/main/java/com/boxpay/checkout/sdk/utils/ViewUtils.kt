package com.boxpay.checkout.sdk.utils

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatEditText

fun dpToPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
}

class NoPasteEditText(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {
    override fun onTextContextMenuItem(id: Int): Boolean {
        // Block copy/paste
        return when (id) {
            android.R.id.paste,
            android.R.id.pasteAsPlainText,
            android.R.id.cut,
            android.R.id.copy -> true
            else -> super.onTextContextMenuItem(id)
        }
    }
}