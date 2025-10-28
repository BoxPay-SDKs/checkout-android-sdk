package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily
import com.boxpay.checkout.sdk.R

@Composable
fun ApplyCouponCard(
    modifier: Modifier,
    selectedColor: Color,
    code : String,
    description : String ,
    onClickApply : (code : String) -> Unit,
    onClickViewAll : () -> Unit
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_offer_tag),
                contentDescription = "",
                colorFilter = ColorFilter.tint(selectedColor),
                modifier = Modifier.border(1.dp, Color(0xFFE6E6E6),RoundedCornerShape(6.dp)).padding(6.dp)
            )
            Column(modifier = Modifier.padding(start = 6.dp, end = 18.dp).weight(1f)) {
                Text(
                    text = code,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1D20),
                    fontSize = 18.sp
                )
                Text(
                    text = description,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1C1D20),
                    fontSize = 14.sp
                )
            }
            Text(
                text = "Apply",
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = selectedColor,
                fontSize = 18.sp,
                modifier = Modifier.clickable {
                    onClickApply(code)
                }
            )
        }
        Divider()
        Text(
            text = "View All",
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = selectedColor,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable {
                onClickViewAll()
            },
            textAlign = TextAlign.Center
        )
    }
}
