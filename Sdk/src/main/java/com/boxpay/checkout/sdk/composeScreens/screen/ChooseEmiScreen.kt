package com.boxpay.checkout.sdk.composeScreens.screen

import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.components.BankRow
import com.boxpay.checkout.sdk.composeScreens.components.CardAcceptanceRow
import com.boxpay.checkout.sdk.composeScreens.components.CardSecureRow
import com.boxpay.checkout.sdk.composeScreens.components.CvvBottomSheet
import com.boxpay.checkout.sdk.composeScreens.components.EmiAmountDetails
import com.boxpay.checkout.sdk.composeScreens.components.FilterCard
import com.boxpay.checkout.sdk.composeScreens.components.OthersEmiRow
import com.boxpay.checkout.sdk.composeScreens.components.TopBar
import com.boxpay.checkout.sdk.composeScreens.model.Bank
import com.boxpay.checkout.sdk.composeScreens.model.ChooseEmiModel

@Composable
fun ChooseEmiScreen(
    cardList: ChooseEmiModel,
    filterList: List<Pair<String, Boolean>>,
    isSelectedCard: String,
    onClickCard: (String) -> Unit,
    onClickBack: () -> Unit,
    onClickRadio: (String) -> Unit,
    selectedRadioButton: String,
    sharedPreferences: SharedPreferences,
    searchQuery: String,
    onValueChange: (String) -> Unit,
    onClickBank: (Bank) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFFF5F6FB))
    ) {
        val (topBar, filterBackground, cardRow, searchField, filterRow, allBanksText, list, footerRow, divider, cta) = createRefs()
        TopBar(
            text = "Choose EMI Option",
            modifier = Modifier
                .constrainAs(topBar) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)

                    width = Dimension.fillToConstraints
                }
                .background(Color.White),
            onClickBack = onClickBack
        )
        Box(
            modifier = Modifier
                .constrainAs(filterBackground) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(topBar.bottom, 4.dp)
                    bottom.linkTo(filterRow.bottom)

                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .background(Color.White)
        )
        Row(
            modifier = Modifier.constrainAs(cardRow) {
                start.linkTo(parent.start, 20.dp)
                end.linkTo(parent.end, 20.dp)
                top.linkTo(filterBackground.top, 14.dp)

                width = Dimension.fillToConstraints
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            cardList.cards.map {
                Column(modifier = Modifier
                    .clickable { onClickCard(it.cardType) }
                    .padding(end = 24.dp)) {
                    Text(
                        text = it.cardType,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight(800)
                        ),
                        textAlign = TextAlign.Center,
                        color = if (it.cardType.equals(
                                isSelectedCard,
                                true
                            )
                        ) Color(
                            android.graphics.Color.parseColor(
                                sharedPreferences.getString(
                                    "primaryButtonColor",
                                    "#000000"
                                )
                            )
                        ) else Color(0xFF010102).copy(0.45f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 6.dp)
                    )
                    if (it.cardType.equals(isSelectedCard, true)) {
                        Divider(
                            color = Color(
                                android.graphics.Color.parseColor(
                                    sharedPreferences.getString(
                                        "primaryButtonColor",
                                        "#000000"
                                    )
                                )
                            ),
                            modifier = Modifier
                                .width(90.dp)
                                .height(2.dp)
                        )
                    }
                }
            }
        }
        Divider(
            modifier = Modifier.constrainAs(divider) {
                start.linkTo(filterBackground.start)
                end.linkTo(filterBackground.end)
                top.linkTo(cardRow.bottom)
            }
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                onValueChange(it)
            },
            modifier = Modifier
                .constrainAs(searchField) {
                    start.linkTo(filterBackground.start, 16.dp)
                    end.linkTo(filterBackground.end, 16.dp)
                    top.linkTo(divider.bottom, 32.dp)

                    width = Dimension.fillToConstraints
                }
                .height(48.dp)
                .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.searchicon),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(Color(0xFF7F7D83)),
                    modifier = Modifier.size(20.dp)
                )
            },
            placeholder = {
                Text(
                    text = "Search for bank",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(500)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            )
        )
        Row(modifier = Modifier
            .constrainAs(filterRow) {
                start.linkTo(filterBackground.start, 16.dp)
                end.linkTo(filterBackground.end, 16.dp)
                top.linkTo(searchField.bottom)

                width = Dimension.fillToConstraints
            }
            .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterList.map {
                FilterCard(text = it.first, modifier = Modifier.padding(end = 8.dp))
            }
        }
        Text(
            text = if (isSelectedCard.equals("others", true)) "Others" else "All Banks",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight(800)
            ),
            color = Color(0xFF020815).copy(0.71f),
            modifier = Modifier.constrainAs(allBanksText) {
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
                top.linkTo(filterBackground.bottom, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        LazyColumn(
            modifier = Modifier
                .constrainAs(list) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(allBanksText.bottom, 8.dp)
                    if (selectedRadioButton.isNotEmpty()) {
                        bottom.linkTo(cta.top, 30.dp)
                    } else {
                        bottom.linkTo(footerRow.top, 30.dp)
                    }

                    width = Dimension.fillToConstraints
                }
                .heightIn(
                    min = 300.dp,
                    max = 400.dp
                ) // This ensures the LazyColumn takes only as much height as needed by the items
                .background(Color.White, RoundedCornerShape(12.dp))
        ) {
            items(cardList.cards) {
                if (it.cardType.equals(isSelectedCard, true) && !isSelectedCard.equals(
                        "others",
                        true
                    )
                ) {
                    it.banks.map { bank ->
                        BankRow(
                            iconUrl = bank.iconUrl,
                            bankName = bank.name,
                            percentText = bank.percent,
                            isNoCostApplied = bank.noCostApplied,
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .clickable { onClickBank(bank) }
                        )
                    }
                }
                if (isSelectedCard.equals("others", true) && it.cardType.equals(
                        isSelectedCard,
                        true
                    )
                ) {
                    it.banks.map { bank ->
                        OthersEmiRow(
                            modifier = Modifier.fillParentMaxWidth(),
                            iconUrl = bank.iconUrl,
                            isSelected = selectedRadioButton.equals(bank.name, true),
                            otherName = bank.name,
                            onClickRadio = {
                                onClickRadio(bank.name)
                            },
                            selectedColor = Color(
                                android.graphics.Color.parseColor(
                                    sharedPreferences.getString(
                                        "primaryButtonColor",
                                        "#000000"
                                    )
                                )
                            )
                        )
                    }
                }
            }
        }
        if (selectedRadioButton.isNotEmpty()) {
            Button(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .constrainAs(cta) {
                        start.linkTo(parent.start, 16.dp)
                        end.linkTo(parent.end, 16.dp)
                        bottom.linkTo(footerRow.top, 10.dp)

                        width = Dimension.fillToConstraints
                    },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(
                        android.graphics.Color.parseColor(
                            sharedPreferences.getString(
                                "primaryButtonColor",
                                "#000000"
                            )
                        )
                    )
                )
            ) {
                Text(
                    text = "Proceed",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight(800)
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier.constrainAs(footerRow) {
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
                bottom.linkTo(parent.bottom, 16.dp)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Secured by",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight(500)
                ),
                color = Color(0xFF888888),
                modifier = Modifier
            )
            Image(
                painter = painterResource(id = R.drawable.boxpay_copyright),
                contentDescription = "",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun SelectTenureEmi(
    totalPrice: String,
    onClickBack: () -> Unit,
    selectedBank: Bank,
    cardType: String,
    selectedEmi: Pair<Int, String>,
    sharedPreferences: SharedPreferences,
    onClickRadio: (duration: Int, amount: String) -> Unit,
    onProceed: (Int) -> Unit
) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFFF5F6FB))
    ) {
        val (topBar, itemsPrice, list, footerRow) = createRefs()
        TopBar(
            text = "Select Tenure",
            modifier = Modifier
                .constrainAs(topBar) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)

                    width = Dimension.fillToConstraints
                }
                .background(Color.White),
            onClickBack = onClickBack
        )
        Text(
            text = "Item(s) price: $totalPrice",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight(800)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(itemsPrice) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(topBar.bottom, 10.dp)
            }
        )
        LazyColumn(
            modifier = Modifier
                .constrainAs(list) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(itemsPrice.bottom, 12.dp)
                    bottom.linkTo(footerRow.top, 20.dp)

                    width = Dimension.fillToConstraints
                }
                .heightIn(min = 300.dp, max = 400.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(12.dp))
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            selectedBank.iconUrl,
                            imageLoader = imageLoader,
                            error = painterResource(id = R.drawable.netbanking_logo)
                        ),
                        contentDescription = "",
                        modifier = Modifier
                            .size(30.dp)
                    )
                    Text(
                        text = "${selectedBank.name} | $cardType",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight(800)
                        ),
                        color = Color(0xFF2D2B32),
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                    )
                }
                Divider(
                    modifier = Modifier.fillParentMaxWidth()
                )
            }
            items(selectedBank.emiList) {
                EmiAmountDetails(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .clickable { onClickRadio(it.duration, it.amount) },
                    isSelected = selectedEmi.first == it.duration && selectedEmi.second == it.amount,
                    onClickRadio = { onClickRadio(it.duration, it.amount) },
                    selectedColor = Color(
                        android.graphics.Color.parseColor(
                            sharedPreferences.getString(
                                "primaryButtonColor",
                                "#000000"
                            )
                        )
                    ),
                    month = it.duration,
                    amount = it.amount,
                    percent = it.percent,
                    discount = it.discount,
                    interest = it.interestCharged ?: "",
                    total = it.totalAmount,
                    processingFee = it.processingFee,
                    bankName = selectedBank.name,
                    onProceed = {
                        onProceed(it.percent)
                    }
                )
                if (it.duration != selectedBank.emiList.last().duration && it.amount != selectedBank.emiList.last().amount) {
                    Divider(modifier = Modifier.fillParentMaxWidth())
                }
            }
        }
        Row(
            modifier = Modifier.constrainAs(footerRow) {
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
                bottom.linkTo(parent.bottom, 16.dp)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Secured by",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight(500)
                ),
                color = Color(0xFF888888),
                modifier = Modifier
            )
            Image(
                painter = painterResource(id = R.drawable.boxpay_copyright),
                contentDescription = "",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun AddCardDetailsScreen(
    iconUrl: String,
    name: String,
    month: Int,
    amount: String,
    percent: Int,
    onClickBack: () -> Unit,
    sharedPreferences: SharedPreferences,
    cardNumber: String,
    cardName: String,
    expiry: String,
    cvv: String,
    onCardNumberChange:(String)-> Unit,
    onCardNameChange:(String) -> Unit,
    onCardExpiryChange:(String)-> Unit,
    onCardCvvChange:(String)-> Unit
) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()
    val showCvvDetails = remember {
        mutableStateOf(false)
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White)
            .imePadding()
    ) {
        val (topBar, bankBorder, bankIcon, bankName, divider, emiDetails, cardNumberTitle, cardNumberInput, cardNameTitle, cardNameInput, expiryTitle, expiryInput, cvvTitle, cvvInput, footerStart, footerEnd) = createRefs()
        val (cvvBottomSheet, interestRate, topDivider, cta) = createRefs()
        TopBar(
            text = "Add Card Details",
            modifier = Modifier
                .constrainAs(topBar) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)

                    width = Dimension.fillToConstraints
                }
                .background(Color.White),
            onClickBack = onClickBack
        )
        Divider(
            modifier = Modifier.constrainAs(topDivider) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(topBar.bottom)
            }
        )
        Box(
            modifier = Modifier
                .constrainAs(bankBorder) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(topBar.bottom, 10.dp)

                    width = Dimension.fillToConstraints
                }
                .height(60.dp)
                .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
        )
        Image(
            painter = rememberAsyncImagePainter(
                iconUrl,
                imageLoader = imageLoader,
                error = painterResource(id = R.drawable.netbanking_logo)
            ),
            contentDescription = "",
            modifier = Modifier
                .constrainAs(bankIcon) {
                    start.linkTo(bankBorder.start, 12.dp)
                    top.linkTo(bankBorder.top, 14.dp)
                }
                .size(32.dp)
        )
        Text(
            text = name,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(bankName) {
                start.linkTo(bankIcon.end, 8.dp)
                end.linkTo(divider.start, 8.dp)

                width = Dimension.fillToConstraints

                centerVerticallyTo(bankIcon)
            }
        )
        Text(
            text = "$month months x ₹$amount",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier
                .constrainAs(emiDetails) {
                    end.linkTo(bankBorder.end, 12.dp)
                    top.linkTo(bankBorder.top, 12.dp)
                }
                .padding(start = 12.dp)
        )
        Text(
            text = "@$percent% p.a.",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(interestRate) {
                start.linkTo(divider.end, 12.dp)
                top.linkTo(emiDetails.bottom, 2.dp)
            }
        )
        Box(
            modifier = Modifier
                .constrainAs(divider) {
                    start.linkTo(emiDetails.start)
                    top.linkTo(emiDetails.top)
                    bottom.linkTo(interestRate.bottom)

                    height = Dimension.fillToConstraints
                }
                .width(2.dp)
                .background(Color(0xFFE6E6E6))
        )
        Text(
            text = "Card Number",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(cardNumberTitle) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(bankBorder.bottom, 12.dp)
                end.linkTo(parent.end, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        OutlinedTextField(
            value = cardNumber,
            onValueChange = {
                onCardNumberChange(it)
            },
            modifier = Modifier
                .constrainAs(cardNumberInput) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(cardNumberTitle.bottom, 4.dp)

                    width = Dimension.fillToConstraints
                }
                .height(48.dp)
                .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "XXXX XXXX XXXX XXXX",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(500)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.NumberPassword
            )
        )
        Text(
            text = "Name on card",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(cardNameTitle) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(cardNumberInput.bottom, 16.dp)
                end.linkTo(parent.end, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        OutlinedTextField(
            value = cardName,
            onValueChange = {
                onCardNameChange(it)
            },
            modifier = Modifier
                .constrainAs(cardNameInput) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(cardNameTitle.bottom, 4.dp)

                    width = Dimension.fillToConstraints
                }
                .height(48.dp)
                .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "Please enter name on your card",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(500)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )
        Text(
            text = "Expiry",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(expiryTitle) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(cardNameInput.bottom, 16.dp)
                end.linkTo(cvvTitle.start, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        OutlinedTextField(
            value = expiry,
            onValueChange = {
                onCardExpiryChange(it)
            },
            modifier = Modifier
                .constrainAs(expiryInput) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(cvvInput.start, 16.dp)
                    top.linkTo(expiryTitle.bottom, 4.dp)

                    width = Dimension.fillToConstraints
                }
                .height(48.dp)
                .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "MM/YY",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(500)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )
        Text(
            text = "CVV",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(cvvTitle) {
                start.linkTo(cvvInput.start)
                centerVerticallyTo(expiryTitle)

                width = Dimension.fillToConstraints
            }
        )
        OutlinedTextField(
            value = cvv,
            onValueChange = {
                onCardCvvChange(it)
            },
            modifier = Modifier
                .constrainAs(cvvInput) {
                    start.linkTo(expiryInput.end, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    centerVerticallyTo(expiryInput)

                    width = Dimension.fillToConstraints
                }
                .height(48.dp)
                .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "Enter CVV",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(500)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_question_mark),
                    contentDescription = "",
                    modifier = Modifier.size(20.dp).clickable {
                        showCvvDetails.value = true
                    }
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            )
        )
        CardAcceptanceRow(
            modifier = Modifier.constrainAs(footerStart) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(expiryInput.bottom, 12.dp)
            }
        )
        CardSecureRow(
            modifier = Modifier.constrainAs(footerEnd) {
                end.linkTo(parent.end, 16.dp)
                centerVerticallyTo(footerStart)
            }
        )
        Button(
            onClick = {},
            modifier = Modifier
                .constrainAs(cta) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(footerEnd.bottom, 40.dp)

                    width = Dimension.fillToConstraints
                }
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(
                    android.graphics.Color.parseColor(
                        sharedPreferences.getString(
                            "primaryButtonColor",
                            "#000000"
                        )
                    )
                )
            )
        ) {
            Text(
                text = "Pay Now",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight(800)
                ),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
        if (showCvvDetails.value) {
            CvvBottomSheet(
                selectedColor = Color(
                    android.graphics.Color.parseColor(
                        sharedPreferences.getString(
                            "primaryButtonColor",
                            "#000000"
                        )
                    )
                ),
                onClickBack = {
                    showCvvDetails.value = false
                }
            )
        }
    }
}