package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
fun FilterCard(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier
            .wrapContentSize()
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.padding(top = 8.dp, start = 10.dp, bottom = 8.dp)
        )
        Image(
            imageVector = Icons.Filled.Add,
            contentDescription = "",
            modifier = Modifier.padding(horizontal = 8.dp).size(22.dp),
            colorFilter = ColorFilter.tint(Color(0xFF7F7D83))
        )
    }
}