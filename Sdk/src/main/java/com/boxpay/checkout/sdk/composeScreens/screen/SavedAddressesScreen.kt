package com.boxpay.checkout.sdk.composeScreens.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.components.DeleteSavedAddress
import com.boxpay.checkout.sdk.composeScreens.components.MoreOptionsSavedAddress
import com.boxpay.checkout.sdk.composeScreens.components.SavedAddressCard
import com.boxpay.checkout.sdk.composeScreens.components.TopBar
import com.boxpay.checkout.sdk.composeScreens.model.Address
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily

@Composable
fun SavedAddressesScreen(
    onClickAddNewAddress: () -> Unit,
    addressList: List<Address>,
    onClickEditAddress: (address: Address?) -> Unit,
    onClickBack: () -> Unit,
    showLoadingInButton: Boolean,
    onClickDeleteAddress: (uniqueRef: String) -> Unit,
    onClickSetDefault: (addresss: Address?) -> Unit,
    selectedTextColor: Color,
    selectedCtaColor: Color,
    alreadySavedAddressPostal: String
) {
    val context = LocalContext.current
    val isMoreOptionClicked = remember {
        mutableStateOf(false)
    }
    val addressIcon = remember {
        mutableStateOf<Int?>(null)
    }
    val selectedAddress = remember {
        mutableStateOf<Address?>(null)
    }
    val isDeleteClicked = remember {
        mutableStateOf(false)
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F1F1))
    ) {
        val (topBar, addAddressRow, savedAddressTitle, savedAddressList) = createRefs()
        TopBar(
            text = "Your Addresses",
            modifier = Modifier
                .constrainAs(topBar) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)

                    width = Dimension.fillToConstraints
                },
            onClickBack = {
                if (!showLoadingInButton) onClickBack()
            }
        )
        Row(
            modifier = Modifier
                .constrainAs(addAddressRow) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(topBar.bottom, 12.dp)

                    width = Dimension.fillToConstraints
                }
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .clickable {
                    onClickAddNewAddress()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_boxpay_add),
                contentDescription = "",
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(selectedCtaColor)
            )
            Text(
                text = "Add new address",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = selectedCtaColor,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(1f)
            )
            Image(
                painter = painterResource(id = R.drawable.ic_boxpay_chevron_right),
                contentDescription = "",
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "Saved Addresses",
            style = TextStyle(
                fontSize = 14.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF020815).copy(0.7f),
            modifier = Modifier.constrainAs(savedAddressTitle) {
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
                top.linkTo(addAddressRow.bottom, 16.dp)

                width = Dimension.fillToConstraints
            }
        )
        Column(
            modifier = Modifier
                .constrainAs(savedAddressList) {
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(savedAddressTitle.bottom, 8.dp)
                    bottom.linkTo(parent.bottom, 8.dp)

                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .verticalScroll(rememberScrollState())
        ) {
            if (addressList.isEmpty()) {
                Text(
                    text = "No Saved Addresses"
                )
            } else {
                addressList.map {
                    SavedAddressCard(
                        modifier = Modifier,
                        address1 = it.address1,
                        address2 = it.address2,
                        city = it.city,
                        state = it.state,
                        pinCode = it.postalCode,
                        number = it.phoneNumber,
                        selectedCtaColor = selectedCtaColor,
                        isCurrentlySelected = it.postalCode.equals(alreadySavedAddressPostal, true),
                        addressIcon = if (it.labelType.equals(
                                "home",
                                true
                            )
                        ) R.drawable.home_icon else if (it.labelType.equals(
                                "work",
                                true
                            )
                        ) R.drawable.ic_boxpay_saved_office_address else R.drawable.ic_boxpay_other_saved_address,
                        label = it.labelName ?: it.labelType ?: "",
                        onClickEditAddress = {
                            addressIcon.value = if (it.labelType.equals(
                                    "home",
                                    true
                                )
                            ) R.drawable.home_icon else if (it.labelType.equals(
                                    "work",
                                    true
                                )
                            ) R.drawable.ic_boxpay_saved_office_address else R.drawable.ic_boxpay_other_saved_address
                            selectedAddress.value = it
                            isMoreOptionClicked.value = true
                        },
                        onClickSelectAddress = {
                            onClickSetDefault(it)
                        }
                    )
                    Spacer(modifier = Modifier.padding(bottom = 20.dp))
                }
            }
        }
    }
    if (isMoreOptionClicked.value) {
        MoreOptionsSavedAddress(
            addressIcon = addressIcon.value,
            label = selectedAddress.value?.labelName ?: selectedAddress.value?.labelType ?: "",
            address1 = selectedAddress.value?.address1,
            address2 = selectedAddress.value?.address2,
            city = selectedAddress.value?.city,
            state = selectedAddress.value?.state,
            pinCode = selectedAddress.value?.postalCode,
            onClickEditAddress = {
                isMoreOptionClicked.value = false
                onClickEditAddress(selectedAddress.value)
                                 },
            onClickDeleteAddress = {
                isMoreOptionClicked.value = false
                if (addressList.size == 1) {
                    Toast.makeText(context, "Cannot delete. Only one address is saved.",Toast.LENGTH_LONG).show()
                } else {
                    isDeleteClicked.value = true
                }
            },
            onClickSetDefault = { onClickSetDefault(selectedAddress.value) },
            onClickBack = {
                isMoreOptionClicked.value = false
            }
        )
    }
    if (isDeleteClicked.value) {
        DeleteSavedAddress(
            onClickBack = { isDeleteClicked.value = false },
            address1 = selectedAddress.value?.address1,
            address2 = selectedAddress.value?.address2,
            city = selectedAddress.value?.city,
            state = selectedAddress.value?.state,
            pinCode = selectedAddress.value?.postalCode,
            onClickDeleteAddress = {
                isDeleteClicked.value = false
                onClickDeleteAddress(selectedAddress.value?.addressRef ?: "")
            },
            selectedTextColor = selectedTextColor,
            selectedCtaColor = selectedCtaColor
        )
    }
}