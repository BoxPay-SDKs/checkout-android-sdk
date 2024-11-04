package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily

@Composable
fun FilterCard(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    selectedColor: Color
) {
    Row(
        modifier = modifier
            .wrapContentSize()
            .border(
                1.dp,
                if (isSelected) selectedColor else Color(0xFFE6E6E6),
                RoundedCornerShape(20.dp)
            )
            .background(
                if (isSelected) selectedColor.copy(0.05f) else Color.White,
                RoundedCornerShape(20.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.padding(top = 8.dp, start = 10.dp, bottom = 8.dp)
        )
        Image(
            imageVector = if (isSelected) Icons.Filled.Clear else Icons.Filled.Add,
            contentDescription = "",
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(if (isSelected) 18.dp else 20.dp),
            colorFilter = if (isSelected) ColorFilter.tint(Color.Black) else ColorFilter.tint(Color(0xFF7F7D83))
        )
    }
}