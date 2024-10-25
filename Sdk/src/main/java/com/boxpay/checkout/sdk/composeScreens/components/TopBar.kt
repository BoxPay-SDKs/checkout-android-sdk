package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(modifier: Modifier = Modifier, text:String, onClickBack:()-> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "",
            modifier = Modifier
                .padding(top = 20.dp, start = 16.dp, bottom = 20.dp, end = 8.dp)
                .size(28.dp)
                .clickable { onClickBack() },
            colorFilter = ColorFilter.tint(Color(0xFF7F7D83))
        )
        Text(
            text = text,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight(800)
            ),
            color = Color(0xFF02040E).copy(0.7f)
        )
    }
}