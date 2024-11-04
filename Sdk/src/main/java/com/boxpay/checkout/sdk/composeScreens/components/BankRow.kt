package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily
import com.boxpay.checkout.sdk.composeScreens.model.interFontFamily

@Composable
fun BankRow(
    iconUrl: String,
    bankName: String,
    percentText: String,
    isNoCostApplied: Boolean,
    modifier: Modifier = Modifier
) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()
    ConstraintLayout(modifier) {
        val (icon, name, noCostTag, percent, arrowIcon) = createRefs()

        Image(
            painter = rememberAsyncImagePainter(
                iconUrl,
                imageLoader = imageLoader,
                error = painterResource(id = R.drawable.netbanking_logo)
            ),
            contentDescription = "",
            modifier = Modifier
                .constrainAs(icon) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(parent.top, 16.dp)
                    bottom.linkTo(parent.bottom, 16.dp)
                }
                .size(34.dp)
                .border(1.dp, Color(0xFFDCDEE3), RoundedCornerShape(4.dp))
                .padding(4.dp)
        )
        Text(
            text = bankName,
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF4F4D55),
            modifier = Modifier.constrainAs(name) {
                start.linkTo(icon.end, 8.dp)
                if (isNoCostApplied) {
                    top.linkTo(parent.top, 16.dp)
                } else {
                    centerVerticallyTo(icon)
                }
            }
        )
        FilterTag(
            text = "NO COST EMI",
            modifier = Modifier
                .constrainAs(noCostTag) {
                    start.linkTo(icon.end, 8.dp)
                    top.linkTo(name.bottom, 4.dp)

                    visibility = if (isNoCostApplied) Visibility.Visible else Visibility.Gone
                }
        )
        Image(
            painter = painterResource(id = R.drawable.ic_keyboard_left_arrow),
            contentDescription = "",
            modifier = Modifier
                .constrainAs(arrowIcon) {
                    end.linkTo(parent.end, 16.dp)

                    centerVerticallyTo(icon)
                }
        )
        Text(
            text = percentText,
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF4F4D55),
            modifier = Modifier.constrainAs(percent) {
                end.linkTo(arrowIcon.start, 4.dp)

                centerVerticallyTo(arrowIcon)
            }
        )
    }
}

@Composable
fun OthersEmiRow(
    modifier: Modifier = Modifier,
    iconUrl: String,
    isSelected: Boolean,
    otherName: String,
    onClickRadio: () -> Unit,
    selectedColor: Color
) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()
    ConstraintLayout(modifier) {
        val (icon, name, radioButton) = createRefs()
        Image(
            painter = rememberAsyncImagePainter(
                iconUrl,
                imageLoader = imageLoader,
                error = painterResource(id = R.drawable.ic_em_default_others)
            ),
            contentDescription = "",
            modifier = Modifier
                .constrainAs(icon) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(parent.top, 16.dp)
                    bottom.linkTo(parent.bottom, 16.dp)
                }
                .size(34.dp)
                .border(1.dp, Color(0xFFDCDEE3), RoundedCornerShape(4.dp))
                .padding(4.dp)
        )
        Text(
            text = otherName,
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF4F4D55),
            modifier = Modifier.constrainAs(name) {
                start.linkTo(icon.end, 8.dp)
                end.linkTo(radioButton.start, 16.dp)

                width = Dimension.fillToConstraints
                centerVerticallyTo(icon)
            }
        )
        RadioButton(
            selected = isSelected,
            onClick = { onClickRadio() },
            modifier = Modifier.constrainAs(radioButton) {
                end.linkTo(parent.end, 16.dp)

                centerVerticallyTo(icon)
            },
            colors = RadioButtonDefaults.colors(
                selectedColor = selectedColor
            )
        )
    }
}

@Composable
fun EmiAmountDetails(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onClickRadio: () -> Unit,
    selectedColor: Color,
    month: Int,
    amount: String,
    percent: Int,
    total: String,
    interest: String,
    discount: String?,
    processingFee: String,
    bankName: String,
    onProceed: () -> Unit,
    isNoCostApplied: Boolean,
    currencySymbol: String
) {
    ConstraintLayout(
        modifier
            .background(
                if (isSelected) Color(0xFFEFF3FA) else Color.White
            )
            .padding(bottom = 16.dp)
    ) {
        val (radioButton, heading, table, noteDesc, cashBack, gst, cta, noCost) = createRefs()
        RadioButton(
            selected = isSelected,
            onClick = { onClickRadio() },
            modifier = Modifier.constrainAs(radioButton) {
                top.linkTo(parent.top, 20.dp)
            },
            colors = RadioButtonDefaults.colors(
                selectedColor = selectedColor
            )
        )
        Text(
            text = buildAnnotatedString {
                append(
                    AnnotatedString(
                        text = "$month months x ",
                        spanStyle = SpanStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(600)
                        )
                    )
                )
                append(
                    AnnotatedString(
                        text = currencySymbol,
                        spanStyle = SpanStyle(
                            fontFamily = interFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(200)
                        )
                    )
                )
                append(
                    AnnotatedString(
                        text = amount,
                        spanStyle = SpanStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(600)
                        )
                    )
                )
            },
            color = Color(0xFF4F4D55),
            modifier = Modifier.constrainAs(heading) {
                start.linkTo(radioButton.end, 8.dp)
                centerVerticallyTo(radioButton)
            }
        )
        if (isNoCostApplied) {
            FilterTag(text = "NO COST EMI", modifier = Modifier.constrainAs(noCost) {
                start.linkTo(heading.end, 8.dp)

                centerVerticallyTo(radioButton)
            })
        }
        if (isSelected) {
            TableDetails(
                modifier = Modifier.constrainAs(table) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(heading.bottom, 16.dp)

                    width = Dimension.fillToConstraints
                },
                amount = amount,
                total = total,
                interest = interest,
                discount = discount,
                interestRate = "$percent",
                isNoCostApplied = isNoCostApplied,
                currencySymbol = currencySymbol
            )
            if (isNoCostApplied) {
                Text(
                    text = buildAnnotatedString {
                        append(
                            AnnotatedString(
                                text = "Note:",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontWeight = FontWeight(600),
                                    fontSize = 12.sp
                                )
                            )
                        )
                        append(
                            AnnotatedString(
                                text = " The bank will continue to charge to charge interest on No Cost EMI plans as per existing rates. However, the interest to be charged by bank will be passed on to you as an upfront discount.",
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontWeight = FontWeight(400),
                                    fontSize = 12.sp
                                )
                            )
                        )
                    },
                    color = Color(0xFF2D2B32),
                    modifier = Modifier.constrainAs(noteDesc) {
                        start.linkTo(parent.start, 16.dp)
                        end.linkTo(parent.end, 16.dp)
                        top.linkTo(table.bottom, 12.dp)

                        width = Dimension.fillToConstraints
                    }
                )
            }
            Text(
                text = if (processingFee.equals("0", true)) buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = "No Processing Fee will be charged by $bankName",
                            spanStyle = SpanStyle(
                                fontFamily = defaultFontFamily,
                                fontWeight = FontWeight(600),
                                fontSize = 12.sp
                            )
                        )
                    )
                } else buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = interFontFamily,
                                fontWeight = FontWeight(200),
                                fontSize = 12.sp
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = "$processingFee+GST",
                            spanStyle = SpanStyle(
                                fontFamily = defaultFontFamily,
                                fontWeight = FontWeight(600),
                                fontSize = 12.sp
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = " will be charged by $bankName as one-time processing fee.",
                            spanStyle = SpanStyle(
                                fontWeight = FontWeight(600),
                                fontSize = 12.sp
                            )
                        )
                    )
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier.constrainAs(gst) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    if (!isNoCostApplied) {
                        top.linkTo(table.bottom, 12.dp)
                    } else {
                        top.linkTo(noteDesc.bottom, 12.dp)
                    }

                    width = Dimension.fillToConstraints
                }
            )
            Button(
                onClick = onProceed,
                modifier = Modifier
                    .constrainAs(cta) {
                        start.linkTo(parent.start, 16.dp)
                        end.linkTo(parent.end, 16.dp)
                        top.linkTo(gst.bottom, 12.dp)

                        width = Dimension.fillToConstraints
                    },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = selectedColor
                )
            ) {
                Text(
                    text = "Enter Card Details",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight(600)
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TableDetails(
    modifier: Modifier = Modifier,
    amount: String,
    interest: String,
    discount: String?,
    total: String,
    interestRate: String,
    isNoCostApplied: Boolean,
    currencySymbol: String
) {
    Column(modifier.border(1.dp, Color(0xFFF1F1F1), RoundedCornerShape(12.dp))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFF1F1F1),
                    RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Monthly EMI",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.2f)
                    .padding(8.dp)
            )
            Text(
                text = "Interest @$interestRate% p.a.",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.2f)
                    .padding(8.dp)
            )
            if (isNoCostApplied) {
                Text(
                    text = "Discount",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight(600)
                    ),
                    color = Color(0xFF2D2B32),
                    modifier = Modifier
                        .weight(0.2f)
                        .padding(8.dp)
                )
            }
            Text(
                text = "Total Cost",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.2f)
                    .padding(8.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White,
                    RoundedCornerShape(bottomEnd = 12.dp, bottomStart = 12.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = interFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(200)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = amount,
                            spanStyle = SpanStyle(
                                fontFamily = defaultFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    )
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.2f)
                    .padding(12.dp)
            )
            Text(
                text = buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = interFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(200)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = interest,
                            spanStyle = SpanStyle(
                                fontFamily = defaultFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    )
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.2f)
                    .padding(12.dp)
            )
            if (isNoCostApplied) {
                Text(
                    text = buildAnnotatedString {
                        append(
                            AnnotatedString(
                                text = "-$currencySymbol",
                                spanStyle = SpanStyle(
                                    fontFamily = interFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight(200)
                                )
                            )
                        )
                        append(
                            AnnotatedString(
                                text = interest,
                                spanStyle = SpanStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight(200)
                                )
                            )
                        )
                    },
                    color = Color(0xFF1CA672),
                    modifier = Modifier
                        .weight(0.2f)
                        .padding(12.dp)
                )
            }
            Text(
                text = buildAnnotatedString {
                    append(
                        AnnotatedString(
                            text = currencySymbol,
                            spanStyle = SpanStyle(
                                fontFamily = interFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(200)
                            )
                        )
                    )
                    append(
                        AnnotatedString(
                            text = total,
                            spanStyle = SpanStyle(
                                fontFamily = defaultFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    )
                },
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .weight(0.2f)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun CvvBottomSheet(
    modifier: Modifier = Modifier,
    selectedColor: Color,
    onClickBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04000B).copy(0.68f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp))
        ) {
            Text(
                text = "Where to find CVV?",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .padding(start = 16.dp, top = 20.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.where_to_find_cvv),
                contentDescription = "",
                modifier = Modifier.padding(top = 28.dp, start = 16.dp)
            )
            Text(
                text = "Generic position for CVV",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
            )
            Text(
                text = "3 digit numeric code on the back side of card",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400)
                ),
                color = Color(0xFF4F4D55),
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp)
            )
            DashedDivider(
                color = Color(0xFFADACB0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 28.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.where_to_find_cvv_for_american_express_card),
                contentDescription = "",
                modifier = Modifier.padding(start = 16.dp)
            )
            Text(
                text = "CVV for American Express Card",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color(0xFF2D2B32),
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
            )
            Text(
                text = "4 digit numeric code on the front side of the card, just above the card number",
                style = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400)
                ),
                color = Color(0xFF4F4D55),
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp)
            )
            Button(
                onClick = { onClickBack() },
                modifier = Modifier
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = selectedColor
                )
            ) {
                Text(
                    text = "Got it",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight(600)
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CardAcceptanceRow(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "WE ACCEPT",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight(800)
            ),
            color = Color(0xFF7F7D83)
        )
        Image(
            painter = painterResource(id = R.drawable.visa),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 8.dp)
                .size(20.dp)
        )
        Image(
            painter = painterResource(id = R.drawable.mastercard),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 4.dp)
                .size(20.dp)
        )
        Image(
            painter = painterResource(id = R.drawable.discover),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 4.dp)
                .size(20.dp)
        )
    }
}

@Composable
fun CardSecureRow(modifier: Modifier = Modifier) {
    Row(
        modifier.background(Color(0xFFF1F1F1), RoundedCornerShape(4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_lock),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                .size(14.dp)
        )
        Text(
            text = "100% SECURE",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight(800)
            ),
            color = Color(0xFF7F7D83),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun FilterTag(modifier: Modifier = Modifier, text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = defaultFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight(500)
        ),
        color = Color(0xFFEB2F96),
        modifier = modifier
            .border(1.dp, Color(0xFFFFADD2), RoundedCornerShape(4.dp))
            .background(Color(0xFFFFF0F6), RoundedCornerShape(4.dp))
            .padding(vertical = 2.dp, horizontal = 4.dp)
    )
}