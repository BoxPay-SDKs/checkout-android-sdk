package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily
import com.boxpay.checkout.sdk.composeScreens.model.interFontFamily
import com.boxpay.checkout.sdk.utils.convertAmountToIndiaLocale

@Composable
fun SurchargeBottomSheet(modifier: Modifier = Modifier, surchargeList : List<Triple<String, Int, String>>, subTotalAmount : Int, currencySymbol : String, selectedColor : Color, selectedTextColor : Color, onClickProceed : (amount : Int) -> Unit, method : String, onDismiss : () -> Unit) {
    var totalAmount : Int = 0
    if(subTotalAmount != 0) {
        totalAmount += subTotalAmount
    }
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)) // Semi-transparent black (Scrim)
                .blur(8.dp) // Applies blur to the background layer (Android 12+)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss() // Hide when clicking the background
                }
        )
        Card(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Text(
                text = "Payment Summary",
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF010102),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SubTotal",
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color(0xFF010102),
                )
                Text(
                    text = "$currencySymbol${convertAmountToIndiaLocale(subTotalAmount)}",
                    fontFamily = interFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF010102),
                )
            }
            surchargeList
                .filter {it.third.equals(method, true)}
                .map { item ->
                totalAmount += item.second
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.first,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color(0xFF010102),
                    )
                    Text(
                        text = "+ $currencySymbol${convertAmountToIndiaLocale(item.second)}",
                        fontFamily = interFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFF010102),
                    )
                }
            }
            Divider(modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF010102),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "$currencySymbol${convertAmountToIndiaLocale(totalAmount)}",
                    fontFamily = interFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF010102),
                )
            }
            Button(
                onClick = {
                    onClickProceed(totalAmount)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = selectedColor
                )
            ) {
                Text(
                    text = "Proceed to Pay",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight(600)
                    ),
                    color = selectedTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
