package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    text: String,
    onClickBack: () -> Unit
) {
    ConstraintLayout(modifier = modifier) {
        val (image, heading) = createRefs()
        Image(
            painter = painterResource(id = R.drawable.chevron_left),
            contentDescription = "",
            modifier = Modifier
                .constrainAs(image) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(parent.top, 12.dp)
                    bottom.linkTo(parent.bottom, 12.dp)
                }
                .size(28.dp)
                .clickable { onClickBack() },
            colorFilter = ColorFilter.tint(Color(0xFF7F7D83))
        )
        Text(
            text = text,
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF02040E).copy(0.7f),
            modifier = Modifier.constrainAs(heading) {
                start.linkTo(image.end)
                end.linkTo(parent.end, 24.dp)
                centerVerticallyTo(image)
            }
        )
    }
}