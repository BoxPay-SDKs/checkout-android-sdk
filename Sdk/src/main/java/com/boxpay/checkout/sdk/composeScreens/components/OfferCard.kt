package com.boxpay.checkout.sdk.composeScreens.components

import android.os.Build
import android.text.Html
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily
import kotlin.math.roundToInt


val DarkGrey = Color(0xFF333333)
val MediumGrey = Color(0xFF616161)
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun OfferCard(
    modifier: Modifier = Modifier,
    selectedColor : Color,
    offerCode : String,
    description : String,
    minimumOrderAmount : String,
    expiryDate : String,
    applicable : String,
    terms : String,
    selectedCouponCode : String
) {
    val showMore = remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .then(
                if (offerCode == selectedCouponCode) {
                    Modifier.border(
                        width = 2.dp,
                        color = selectedColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    // Use our custom shape as the background
                    .background(
                        color = selectedColor,
                        shape = TicketShape(circleRadius = 8f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Text needs to be rotated
                Text(
                    text = description,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = defaultFontFamily,
                    fontSize = 16.sp,
                    modifier = Modifier.rotate(-90f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Top Row: Code and Apply button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = offerCode,
                        fontWeight = FontWeight.Bold,
                        fontFamily = defaultFontFamily,
                        fontSize = 20.sp,
                        color = DarkGrey
                    )
                    Spacer(modifier = Modifier.weight(1f)) // Pushes "APPLY" to the end
                    Text(
                        text = if (offerCode == selectedCouponCode) "REMOVE" else "APPLY",
                        fontWeight = FontWeight.Bold,
                        fontFamily = defaultFontFamily,
                        color = if (offerCode == selectedCouponCode) Color(0xFFE84142) else selectedColor,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Divider
                Divider(color = Color.LightGray, thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // Terms and conditions text
                Text(
                    text = "Minimum order amount $minimumOrderAmount",
                    color = MediumGrey,
                    fontFamily = defaultFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp // Improve readability
                )
                if(expiryDate.isNotEmpty()) {
                    Text(
                        text = "Offer valid till $expiryDate",
                        color = MediumGrey,
                        fontFamily = defaultFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp // Improve readability
                    )
                }
                Text(
                    text = if(applicable.isNotEmpty()) "Applicable on all transactions made using $applicable" else "Applicable on all transactions",
                    color = MediumGrey,
                    fontFamily = defaultFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp // Improve readability
                )

                Spacer(modifier = Modifier.height(4.dp))

                // More info text
                Text(
                    text = if(showMore.value) "- LESS" else "+ MORE",
                    color = MediumGrey,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable{
                        showMore.value = !showMore.value
                    }
                )

                if (showMore.value) {
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                text = Html.fromHtml(terms, Html.FROM_HTML_MODE_COMPACT)
                                typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
                            }
                        },
                        update = { it.text = Html.fromHtml(terms, Html.FROM_HTML_MODE_COMPACT) }
                    )
                }
            }
        }
    }
}

class TicketShape(private val circleRadius: Float) : Shape {
    // ... (paste the rest of the TicketShape class code here) ...
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                reset()
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                val numberOfCircles = (size.height / (2 * circleRadius)).roundToInt()
                val actualCircleRadius = size.height / (2 * numberOfCircles)
                for (i in 1..numberOfCircles) {
                    val y = (2 * i - 1) * actualCircleRadius
                    arcTo(
                        rect = Rect(
                            left = size.width - actualCircleRadius,
                            top = y - actualCircleRadius,
                            right = size.width + actualCircleRadius,
                            bottom = y + actualCircleRadius
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                lineTo(0f, 0f)
                close()
            }
        )
    }
}