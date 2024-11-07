package com.boxpay.checkout.sdk.composeScreens.screen

import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
import com.boxpay.checkout.sdk.composeScreens.components.CardSecureRow
import com.boxpay.checkout.sdk.composeScreens.components.CvvBottomSheet
import com.boxpay.checkout.sdk.composeScreens.components.EmiAmountDetails
import com.boxpay.checkout.sdk.composeScreens.components.ErrorRow
import com.boxpay.checkout.sdk.composeScreens.components.FilterCard
import com.boxpay.checkout.sdk.composeScreens.components.OthersEmiRow
import com.boxpay.checkout.sdk.composeScreens.components.ShimmerEffect
import com.boxpay.checkout.sdk.composeScreens.components.TopBar
import com.boxpay.checkout.sdk.composeScreens.model.Bank
import com.boxpay.checkout.sdk.composeScreens.model.ChooseEmiModel
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily
import com.boxpay.checkout.sdk.composeScreens.model.interFontFamily

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
    onClickBank: (Bank) -> Unit,
    onClickFilter: (text: String) -> Unit
) {
    val focusRequester = FocusRequester()
    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()
    val isFocused = remember {
        mutableStateOf(false)
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFFF1F1F1))
    ) {
        val (topBar, filterBackground, cardRow, searchField, filterRow, allBanksText, list, divider, cta) = createRefs()
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
                    top.linkTo(topBar.bottom, 2.dp)
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
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(600)
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
                .height(54.dp)
                .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp))
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused.value = focusState.isFocused
                },
            shape = RoundedCornerShape(8.dp),
            leadingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.searchicon),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(Color(0xFF7F7D83)),
                    modifier = Modifier
                        .size(20.dp) // Use offset instead of negative padding
                )
            },
            placeholder = {
                Text(
                    text = "Search for bank",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight(400)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(
                    android.graphics.Color.parseColor(
                        sharedPreferences.getString(
                            "primaryButtonColor",
                            "#000000"
                        )
                    )
                )
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
                FilterCard(
                    text = it.first,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { onClickFilter(isSelectedCard) },
                    isSelected = it.second,
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
        Text(
            text = if (isSelectedCard.equals("others", true)) "Others" else "All Banks",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
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
            state = lazyListState,
            modifier = Modifier
                .constrainAs(list) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(allBanksText.bottom, 8.dp)
                    if (selectedRadioButton.isNotEmpty()) {
                        bottom.linkTo(cta.top, 30.dp)
                    } else {
                        bottom.linkTo(parent.bottom, 30.dp)
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
                    if (it.banks.isNotEmpty()) {
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
                            if (bank.name != it.banks.last().name) {
                                Divider(modifier = Modifier.fillParentMaxWidth())
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(top = 20.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Results Found for $searchQuery",
                                style = TextStyle(
                                    fontFamily = defaultFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight(600)
                                ),
                                color = Color(0xFF7F7D83),
                                modifier = Modifier
                            )
                        }
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
                        bottom.linkTo(parent.bottom, 30.dp)

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

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (isFocused.value) {
            // Handle the scrolling state here, e.g., hide keyboard or clear focus
            focusManager.clearFocus()
            isFocused.value = false
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
    onProceed: (Int) -> Unit,
    currencySymbol: String
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
            .background(Color(0xFFF1F1F1))
    ) {
        val (topBar, itemsPrice, list) = createRefs()
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
            text = buildAnnotatedString {
                append(
                    AnnotatedString(
                        text = "Item(s) price: ",
                        spanStyle = SpanStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(400)
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
                        text = totalPrice,
                        spanStyle = SpanStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(600)
                        )
                    )
                )
            },
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
                    bottom.linkTo(parent.bottom, 30.dp)

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
                            .size(34.dp)
                    )
                    Text(
                        text = "${selectedBank.name} | $cardType",
                        style = TextStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight(600)
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
                    currencySymbol = currencySymbol,
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
                    },
                    isNoCostApplied = it.noCostApplied
                )
                if (it.amount != selectedBank.emiList.last().amount) {
                    Divider(modifier = Modifier.fillParentMaxWidth())
                }
            }
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
    cardNumber: TextFieldValue?,
    cardName: String?,
    expiry: TextFieldValue?,
    cvv: String?,
    onCardNumberChange: (TextFieldValue) -> Unit,
    onCardNameChange: (String) -> Unit,
    onCardExpiryChange: (TextFieldValue) -> Unit,
    onCardCvvChange: (String) -> Unit,
    onProceedClick: () -> Unit,
    cardIcon: Int,
    currencySymbol: String,
    allDetailsValid: Boolean,
    isCardNumberEnabled: Boolean?,
    isAmexCard: Boolean
) {
    val cardNumberFocusRequester = FocusRequester()
    val cardNameFocusRequester = FocusRequester()
    val cardCvvFocusRequester = FocusRequester()
    val cardExpiryFocusRequester = FocusRequester()

    val isCardNumberFocused = remember {
        mutableStateOf(false)
    }
    val isCardCvvFocused = remember {
        mutableStateOf(false)
    }
    val isCardExpiryFocused = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        cardNumberFocusRequester.requestFocus()
    }

    val selectedColor = Color(
        android.graphics.Color.parseColor(
            sharedPreferences.getString(
                "primaryButtonColor",
                "#000000"
            )
        )
    )
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()
    val showCvvDetails = remember {
        mutableStateOf(false)
    }

    val AsteriskVisualTransformation = VisualTransformation { text ->
        val transformedText = buildString {
            for (i in text.text.indices) {
                append('*')  // Replace each character with '*'
            }
        }

        // Create an OffsetMapping to map between the transformed text and the original text
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset // Each character maps directly to the same position
            }

            override fun transformedToOriginal(offset: Int): Int {
                return offset // Each character maps directly to the same position
            }
        }

        // Return TransformedText with both the transformed string and the OffsetMapping
        TransformedText(AnnotatedString(transformedText), offsetMapping)
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White)
            .imePadding()
    ) {
        val (topBar, bankBorder, bankIcon, bankName, divider, emiDetails, cardNumberTitle, cardNumberInput, cardNameTitle, cardNameInput, expiryTitle, expiryInput, cvvTitle, cvvInput, footerEnd,cardNumberInvalidError) = createRefs()
        val (interestRate, topDivider, cta, cardNumberError, cardNameError, cardExpiryError, cardCvvError,cardCvvInvalidError, cardExpiryInvalidError) = createRefs()
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
                fontFamily = defaultFontFamily,
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
            text = buildAnnotatedString {
                append(
                    AnnotatedString(
                        text = "$month months x ",
                        spanStyle = SpanStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600)
                        )
                    )
                )
                append(
                    AnnotatedString(
                        text = currencySymbol,
                        spanStyle = SpanStyle(
                            fontFamily = interFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight(200)
                        )
                    )
                )
                append(
                    AnnotatedString(
                        text = amount,
                        spanStyle = SpanStyle(
                            fontFamily = defaultFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600)
                        )
                    )
                )
            },
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
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
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
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
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
            value = cardNumber ?: TextFieldValue(""),
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
                .focusRequester(cardNumberFocusRequester)
                .onFocusChanged { focusState ->
                    isCardNumberFocused.value = focusState.isFocused
                }
                .height(48.dp)
                .border(
                    1.dp,
                    if (cardNumber?.text?.isEmpty() == true || isCardNumberEnabled == false) Color(
                        0xFFB9232F
                    ) else Color(0xFFD9D9D9),
                    RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "Enter card number",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.NumberPassword
            ),
            trailingIcon = {
                Image(
                    painter = painterResource(id = cardIcon),
                    contentDescription = "",
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = selectedColor
            ),
            enabled = !showCvvDetails.value
        )
        if (cardNumber?.text?.isEmpty() == true || isCardNumberEnabled == false) {
            ErrorRow(
                modifier = Modifier.constrainAs(cardNumberError) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(cardNumberInput.bottom, 2.dp)
                    end.linkTo(parent.end, 16.dp)

                    width = Dimension.fillToConstraints
                },
                errorText = if (isCardNumberEnabled == false) "This card is not supported for the payment" else "Required"
            )
        } else if (!isCardNumberFocused.value && cardNumber != null && (cardNumber.text.length != 19 || isAmexCard)) {
            ErrorRow(
                modifier = Modifier.constrainAs(cardNumberInvalidError) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(cardNumberInput.bottom, 2.dp)
                    end.linkTo(parent.end, 16.dp)

                    width = Dimension.fillToConstraints
                },
                errorText = "Invalid card number"
            )
        }
        Text(
            text = "Name on card",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(cardNameTitle) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(expiryInput.bottom, 16.dp)
                end.linkTo(parent.end, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        OutlinedTextField(
            value = cardName ?: "",
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
                .focusRequester(cardNameFocusRequester)
                .height(48.dp)
                .border(
                    1.dp,
                    if (name.isEmpty()) Color(0xFFB9232F) else Color(0xFFD9D9D9),
                    RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "Enter name on the card",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = selectedColor
            ),
            enabled = !showCvvDetails.value
        )
        if (cardName?.isEmpty() == true) {
            ErrorRow(
                modifier = Modifier.constrainAs(cardNameError) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(cardNameInput.bottom, 2.dp)
                    end.linkTo(parent.end, 16.dp)

                    width = Dimension.fillToConstraints
                },
                errorText = "Required"
            )
        }
        Text(
            text = "Expiry",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight(400)
            ),
            color = Color(0xFF2D2B32),
            modifier = Modifier.constrainAs(expiryTitle) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(cardNumberInput.bottom, 16.dp)
                end.linkTo(cvvTitle.start, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        OutlinedTextField(
            value = expiry ?: TextFieldValue(""),
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
                .focusRequester(cardExpiryFocusRequester)
                .onFocusChanged { focusState ->
                    isCardExpiryFocused.value = focusState.isFocused
                }
                .height(48.dp)
                .border(
                    1.dp,
                    if (expiry?.text?.isEmpty() == true) Color(0xFFB9232F) else Color(0xFFD9D9D9),
                    RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "MM/YY",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.NumberPassword
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = selectedColor
            ),
            enabled = !showCvvDetails.value
        )
        if (expiry?.text?.isEmpty() == true) {
            ErrorRow(
                modifier = Modifier.constrainAs(cardExpiryError) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(expiryInput.bottom, 2.dp)
                    end.linkTo(parent.end, 16.dp)

                    width = Dimension.fillToConstraints
                },
                errorText = "Required"
            )
        } else if (!isCardExpiryFocused.value && expiry != null && expiry.text.length != 5) {
            ErrorRow(
                modifier = Modifier.constrainAs(cardExpiryInvalidError) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(expiryInput.bottom, 2.dp)
                    end.linkTo(parent.end, 16.dp)

                    width = Dimension.fillToConstraints
                },
                errorText = "Invalid expiry"
            )
        }
        Text(
            text = "CVV",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 12.sp,
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
            value = cvv ?: "",
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
                .focusRequester(cardCvvFocusRequester)
                .onFocusChanged { focusState ->
                    isCardCvvFocused.value = focusState.isFocused
                }
                .height(48.dp)
                .border(
                    1.dp,
                    if (cvv?.isEmpty() == true) Color(0xFFB9232F) else Color(0xFFD9D9D9),
                    RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    text = "Enter CVV",
                    style = TextStyle(
                        fontFamily = defaultFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400)
                    ),
                    color = Color(0xFF7F7D83)
                )
            },
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_question_mark),
                    contentDescription = "",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            showCvvDetails.value = true
                        }
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.NumberPassword
            ),
            visualTransformation = AsteriskVisualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = selectedColor
            ),
            enabled = !showCvvDetails.value
        )
        if (cvv?.isEmpty() == true) {
            ErrorRow(
                modifier = Modifier.constrainAs(cardCvvError) {
                    top.linkTo(cvvInput.bottom, 2.dp)
                    end.linkTo(parent.end, 16.dp)
                    start.linkTo(cvvInput.start)

                    width = Dimension.fillToConstraints
                },
                errorText = "Required"
            )
        } else if (!isCardCvvFocused.value && cvv != null && (cvv.length != 3 || isAmexCard)) {
            ErrorRow(
                modifier = Modifier.constrainAs(cardCvvInvalidError) {
                    top.linkTo(cvvInput.bottom, 2.dp)
                    end.linkTo(parent.end, 16.dp)
                    start.linkTo(cvvInput.start)

                    width = Dimension.fillToConstraints
                },
                errorText = "Invalid CVV"
            )
        }
        CardSecureRow(
            modifier = Modifier.constrainAs(footerEnd) {
                end.linkTo(parent.end, 16.dp)
                top.linkTo(cardNameInput.bottom, 14.dp)
            }
        )
        Button(
            enabled = allDetailsValid,
            onClick = onProceedClick,
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
                    fontFamily = defaultFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight(600)
                ),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
    LaunchedEffect(cardNumber) {
        if (cardNumber?.text?.length == 19) {
            cardExpiryFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(expiry) {
        if (expiry?.text?.length == 5) {
            cardCvvFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(cvv) {
        if (isAmexCard) {
            if (cvv?.length == 4) {
                cardNameFocusRequester.requestFocus()
            }
        } else {
            if (cvv?.length == 3) {
                cardNameFocusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun EmiShimmerScreen() {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFFF1F1F1))
    ) {
        val (topBar, filterBackground, cardRow, searchField, filterRow, allBanksText, list, divider, cta) = createRefs()
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
            onClickBack = {}
        )
        Box(
            modifier = Modifier
                .constrainAs(filterBackground) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(topBar.bottom, 2.dp)
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth()
            )
        }
        Row(modifier = Modifier
            .constrainAs(filterRow) {
                start.linkTo(filterBackground.start, 20.dp)
                end.linkTo(filterBackground.end, 20.dp)
                top.linkTo(cardRow.bottom)

                width = Dimension.fillToConstraints
            }
            .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth()
            )
        }
        Text(
            text = "All Banks",
            style = TextStyle(
                fontFamily = defaultFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight(600)
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

