package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily

@Composable
fun SavedAddressCard(
    modifier: Modifier,
    address1: String?,
    address2: String?,
    city: String?,
    state: String?,
    pinCode: String?,
    number: String?,
    isCurrentlySelected: Boolean,
    addressIcon: Int?,
    label: String,
    onClickEditAddress:()-> Unit,
    onClickSelectAddress:()-> Unit,
    selectedCtaColor: Color
) {
    ConstraintLayout(modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .background(Color.White, RoundedCornerShape(12.dp))
        .clickable { onClickSelectAddress() }.border(1.dp, if (isCurrentlySelected) selectedCtaColor else Color.White, RoundedCornerShape(12.dp))) {
        val (icon, addressLabel, currentlySelectedAddress, editIcon, address, phoneText, filter) = createRefs()
        Image(
            painter = painterResource(id = addressIcon ?: 0),
            contentDescription = "",
            modifier = Modifier
                .constrainAs(icon) {
                    start.linkTo(parent.start, 12.dp)
                    top.linkTo(parent.top, 14.dp)
                }
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
            modifier = Modifier.constrainAs(addressLabel) {
                start.linkTo(icon.end, 2.dp)
                centerVerticallyTo(icon)

                width = Dimension.fillToConstraints
            }
        )
        FilterTag(
            text = "CURRENTLY SELECTED",
            modifier = Modifier.constrainAs(currentlySelectedAddress) {
                start.linkTo(addressLabel.end, 2.dp)

                centerVerticallyTo(icon)

                visibility = if (isCurrentlySelected) Visibility.Visible else Visibility.Gone
            }
        )
        Image(
            painter = painterResource(id = R.drawable.ic_boxpay_edit_saved_address),
            contentDescription = "",
            modifier = Modifier
                .constrainAs(editIcon) {
                    end.linkTo(parent.end, 12.dp)
                    centerVerticallyTo(icon)
                }
                .size(16.dp)
                .clickable {
                    onClickEditAddress()
                }
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
            modifier = Modifier.constrainAs(address) {
                start.linkTo(parent.start,12.dp)
                end.linkTo(parent.end, 14.dp)
                top.linkTo(icon.bottom, 8.dp)

                width = Dimension.fillToConstraints
            }
        )
        Text(
            text = "Mobile: $number",
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.Normal
            ),
            color = Color(0xFF7F7D83),
            modifier = Modifier
                .constrainAs(phoneText) {
                    start.linkTo(parent.start, 12.dp)
                    end.linkTo(parent.end, 12.dp)
                    top.linkTo(address.bottom, 2.dp)

                    width = Dimension.fillToConstraints
                }
                .padding(bottom = 12.dp)
        )
    }
}