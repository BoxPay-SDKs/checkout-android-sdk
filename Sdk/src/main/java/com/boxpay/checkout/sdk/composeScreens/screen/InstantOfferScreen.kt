package com.boxpay.checkout.sdk.composeScreens.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxpay.checkout.sdk.composeScreens.components.OfferCard
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersResponse
import com.boxpay.checkout.sdk.utils.formatDate
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun InstantOfferScreen(
    onClickCoupon : (code : String) -> Unit,
    onClickRemoveCoupon : () -> Unit,
    couponList : List<GetInstantOffersResponse>,
    selectedColor : Color,
    selectedCouponCode : String
) {
    val offerSearchTextField = remember {
        mutableStateOf("")
    }
    val sortedCoupons = remember(couponList, selectedCouponCode) {
        couponList.sortedByDescending { it.code == selectedCouponCode }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            OutlinedTextField(
                value = offerSearchTextField.value,
                onValueChange = {
                    val capitalizeCouponCode = it.uppercase()
                    offerSearchTextField.value = capitalizeCouponCode
                },
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .height(48.dp)
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color(0xFFD9D9D9),
                        RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                placeholder = {
                    Text(
                        text = "Enter coupon code",
                        style = TextStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(400)
                        ),
                        color = Color(0xFF7F7D83)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedColor
                ),
                trailingIcon = {
                    Text(
                        text = "APPLY",
                        fontWeight = FontWeight.Bold,
                        fontFamily = defaultFontFamily,
                        color = if (offerSearchTextField.value.isNotEmpty()) selectedColor else Color(0xFF7F7D83),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 10.dp).clickable {
                            onClickCoupon(offerSearchTextField.value)
                        }
                    )
                }
            )
        }
        items(sortedCoupons) {coupon ->
            OfferCard(
                offerCode = coupon.code ?: "",
                description = coupon.description ?: "",
                terms = coupon.terms ?: "",
                selectedColor = selectedColor,
                minimumOrderAmount = if(coupon.criteria?.minMoney?.currencyCode == null) "0" else "${coupon?.criteria?.minMoney?.currencyCode} ${coupon?.criteria?.minMoney?.amount}",
                expiryDate = if(coupon.criteria?.endDate != null) formatDate(coupon.criteria?.endDate ?: "") else "",
                applicable = if(coupon.criteria?.applicableTo?.paymentMethods.isNullOrEmpty()) "" else coupon.criteria?.applicableTo?.paymentMethods?.get(0)?.type ?: "",
                modifier = Modifier.clickable {
                    if(coupon.code.equals(selectedCouponCode)) onClickRemoveCoupon() else onClickCoupon(coupon.code ?: "")
                }
                    .padding(top = 8.dp),
                selectedCouponCode = selectedCouponCode,
            )
        }
    }
}