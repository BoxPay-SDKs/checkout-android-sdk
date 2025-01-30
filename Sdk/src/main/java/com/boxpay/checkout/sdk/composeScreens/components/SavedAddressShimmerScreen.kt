package com.boxpay.checkout.sdk.composeScreens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily

@Composable
fun SavedAddressShimmerScreen() {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFFF1F1F1))
    ) {
        val (topBar, filterBackground, cardRow, filterRow, allBanksText, list) = createRefs()
        TopBar(
            text = "Your Addresses",
            modifier = Modifier
                .constrainAs(topBar) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)

                    width = Dimension.fillToConstraints
                },
            onClickBack = {}
        )
        Box(
            modifier = Modifier
                .constrainAs(filterBackground) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(topBar.bottom, 2.dp)
                    bottom.linkTo(cardRow.bottom)

                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .background(Color.White, RoundedCornerShape(12.dp)),
        )
        Row(
            modifier = Modifier
                .constrainAs(cardRow) {
                    start.linkTo(parent.start, 20.dp)
                    end.linkTo(parent.end, 20.dp)
                    top.linkTo(filterBackground.top)

                    width = Dimension.fillToConstraints
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth()
            )
        }
        Text(
            text = "Saved Addresses",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF020815).copy(0.71f),
            modifier = Modifier.constrainAs(allBanksText) {
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
                top.linkTo(cardRow.bottom, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        LazyColumn(
            modifier = Modifier
                .constrainAs(list) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(allBanksText.bottom, 8.dp)
                    bottom.linkTo(parent.bottom, 30.dp)

                    width = Dimension.fillToConstraints
                }
                .heightIn(
                    min = 300.dp,
                    max = 400.dp
                ) // This ensures the LazyColumn takes only as much height as needed by the items
                .background(Color.White, RoundedCornerShape(12.dp)),
        ) {
            items(4) {
                ShimmerEffect(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(16.dp)
                        .height(30.dp)
                )
            }
        }
    }
}