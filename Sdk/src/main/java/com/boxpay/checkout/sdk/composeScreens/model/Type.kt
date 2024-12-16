package com.boxpay.checkout.sdk.composeScreens.model

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.boxpay.checkout.sdk.R

val defaultFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_ttf_bold, FontWeight.Bold),
    Font(R.font.poppins_semi_bold, FontWeight.SemiBold),
    Font(R.font.poppins_extra_bold, FontWeight.ExtraBold)
)

val interFontFamily = FontFamily(
    Font(R.font.inter_regular,)
)