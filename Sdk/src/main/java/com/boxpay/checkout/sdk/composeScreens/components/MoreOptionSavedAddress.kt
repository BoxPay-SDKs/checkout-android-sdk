package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily

@Composable
fun MoreOptionsSavedAddress(
    addressIcon: Int?,
    label: String,
    address1: String?,
    address2: String?,
    city: String?,
    state: String?,
    pinCode: String?,
    onClickEditAddress: () -> Unit,
    onClickDeleteAddress: () -> Unit,
    onClickSetDefault: () -> Unit,
    onClickBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(0.6f))
            .clickable { onClickBack() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color(0xFFF1F1F1))
                .clickable(enabled = false) {
                    // no op
                }
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = addressIcon ?: 0),
                    contentDescription = "",
                    modifier = Modifier
                        .size(16.dp)
                )
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF2D2B32),
                    modifier = Modifier.padding(start = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = buildAnnotatedString {
                    if (!address1.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$address1, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!address2.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$address2, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!city.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$city, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!state.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$state, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!pinCode.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = pinCode,
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                },
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(12.dp)
                    .clickable {
                        onClickEditAddress()
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_edit_details),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                )
                Text(
                    text = "Edit",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF2D2B32),
                    modifier = Modifier
                        .padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(id = R.drawable.ic_boxpay_chevron_right),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth()
                    .background(
                        Color.White
                    )
                    .padding(12.dp)
                    .clickable {
                        onClickSetDefault()
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_boxpay_deliver_location),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                )
                Text(
                    text = "Set as Default",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF2D2B32),
                    modifier = Modifier
                        .padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(id = R.drawable.ic_boxpay_chevron_right),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(12.dp)
                    .clickable {
                        onClickDeleteAddress()
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_boxpay_delete),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                )
                Text(
                    text = "Delete address",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF2D2B32),
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(id = R.drawable.ic_boxpay_chevron_right),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DeleteSavedAddress(
    onClickBack: () -> Unit,
    address1: String?,
    address2: String?,
    city: String?,
    state: String?,
    pinCode: String?,
    onClickDeleteAddress: () -> Unit,
    selectedTextColor: Color,
    selectedCtaColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(0.6f))
            .clickable { onClickBack() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 12.dp)
                .background(Color(0xFFF1F1F1), RoundedCornerShape(16.dp))
                .clickable(enabled = false) {
                    // no op
                }
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_boxpay_delete),
                contentDescription = "",
                modifier = Modifier
                    .size(28.dp)
            )
            Text(
                text = "Proceed to delete this address?",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildAnnotatedString {
                    if (!address1.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$address1, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!address2.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$address2, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!city.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$city, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!state.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = "$state, ",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                    if (!pinCode.isNullOrEmpty()) {
                        append(
                            AnnotatedString(
                                text = pinCode,
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF7F7D83)
                                )
                            )
                        )
                    }
                },
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    color = selectedCtaColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .padding(end = 8.dp)
                        .background(
                            Color.White,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(10.dp)
                        .clickable {
                            onClickBack()
                        },
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Yes, delete",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = selectedTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .padding(start = 8.dp)
                        .background(
                            selectedCtaColor,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(10.dp)
                        .clickable {
                            onClickDeleteAddress()
                        },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}