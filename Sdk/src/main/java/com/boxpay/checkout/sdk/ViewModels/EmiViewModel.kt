package com.boxpay.checkout.sdk.ViewModels

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.composeScreens.model.Bank
import com.boxpay.checkout.sdk.composeScreens.model.CardType
import com.boxpay.checkout.sdk.composeScreens.model.ChooseEmiModel
import com.boxpay.checkout.sdk.composeScreens.model.Emi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

class EmiViewModel : ViewModel() {

    // StateFlow to hold the EMI Bank List
    private val _emiBankList = MutableStateFlow(ChooseEmiModel(emptyList()))
    val emiBankList: StateFlow<ChooseEmiModel> = _emiBankList
    val selectedCard = mutableStateOf("")
    val selectedOthersOption = mutableStateOf("")
    val selectedBank = mutableStateOf<Bank?>(null)
    val selectTenureScreen = mutableStateOf(false)
    val selectedEmi = mutableStateOf<Pair<Int, String>>(Pair(0, ""))
    val selectedPercent = mutableStateOf<Int?>(null)
    val addCardScreen = mutableStateOf(false)
    val cardNumber = mutableStateOf<TextFieldValue?>(null)
    val cardName = mutableStateOf<String?>(null)
    val expiry = mutableStateOf<TextFieldValue?>(null)
    val filterList = mutableStateOf(listOf(Pair("No Cost EMI", false)))
    val isFilterExisted = mutableStateOf(false)
    val cardIcon = mutableStateOf(R.drawable.default_card_icon)
    val cvv = mutableStateOf<String?>(null)
    val isAmexCard = mutableStateOf(false)
    val isCardValid = mutableStateOf(false)
    val contentLoaded = mutableStateOf(false)
    val firstTimeLoaded = mutableStateOf(true)
    val isCardNumberEnabled = mutableStateOf<Boolean?>(null)

    // To store the original list of banks
    private val originalEmiBankList = mutableStateOf(ChooseEmiModel(emptyList()))

    // State to hold the current search query
    val searchQuery = mutableStateOf("")

    // Function to add/update bank details
    fun addBankDetails(cardType: String, bank: Bank, emi: Emi) {
        val currentList = _emiBankList.value
        val existingCardType = currentList.cards.find { it.cardType == cardType }
        if (selectedCard.value.isEmpty()) {
            selectedCard.value = cardType
        }
        if (!isFilterExisted.value && emi.noCostApplied) {
            isFilterExisted.value = true
        }

        _emiBankList.update {
            if (existingCardType != null) {
                // Check if the bank already exists in the banks list
                val existingBank =
                    existingCardType.banks.find { it.name == bank.name && it.iconUrl == bank.iconUrl }

                if (existingBank != null) {
                    // Check if the EMI already exists in the emiList
                    val emiExists =
                        existingBank.emiList.any { it.duration == emi.duration && it.amount == emi.amount }

                    // Check if any existing EMI has noCostApplied as true
                    val noCostApplied =
                        existingBank.emiList.any { it.noCostApplied } || emi.noCostApplied

                    if (!emiExists) {
                        // Append the new EMI to the existing EMI list
                        val updatedBank = existingBank.copy(
                            emiList = existingBank.emiList + emi,
                            noCostApplied = noCostApplied // Update noCostApplied based on EMI list
                        )
                        val updatedCardType =
                            existingCardType.copy(banks = (existingCardType.banks.map {
                                if (it.name == bank.name && it.iconUrl == bank.iconUrl) updatedBank else it
                            }).sortedWith(
                                compareBy(
                                    { !it.noCostApplied },
                                    { it.name })
                            ) // Sort by noCostApplied (false first) then by name
                            )
                        it.copy(cards = it.cards.map { card ->
                            if (card.cardType == cardType) updatedCardType else card
                        })
                    } else {
                        // No changes if the EMI already exists
                        it
                    }
                } else {
                    // Add the bank with the new EMI if it doesn't exist in the list
                    val newBankWithEmi =
                        bank.copy(emiList = listOf(emi), noCostApplied = emi.noCostApplied)
                    val updatedCardType = existingCardType.copy(
                        banks = (existingCardType.banks + newBankWithEmi)
                            .sortedWith(
                                compareBy(
                                    { !it.noCostApplied },
                                    { it.name })
                            ) // Sort by noCostApplied (false first) then by name
                    )
                    it.copy(cards = it.cards.map { card ->
                        if (card.cardType == cardType) updatedCardType else card
                    })
                }
            } else {
                // Add the new card type with the bank and EMI if the card type doesn't exist
                val newBankWithEmi =
                    bank.copy(emiList = listOf(emi), noCostApplied = emi.noCostApplied)
                it.copy(
                    cards = it.cards + CardType(
                        cardType = cardType,
                        banks = listOf(newBankWithEmi)
                    )
                )
            }
        }

        originalEmiBankList.value = emiBankList.value
    }


    // Function to filter banks based on the search query
    fun filterBanks() {
        if (searchQuery.value.isEmpty()) {
            // When the search query is empty, reset to the original bank list
            _emiBankList.value = originalEmiBankList.value
        } else {
            // Filter the list of banks based on the search query
            val filteredList = originalEmiBankList.value.copy(
                cards = originalEmiBankList.value.cards.map { cardType ->
                    cardType.copy(
                        banks = cardType.banks.filter {
                            it.name.startsWith(searchQuery.value, ignoreCase = true)
                        }
                    )
                }
            )
            _emiBankList.value = filteredList
        }
    }

    fun clearFields() {
        _emiBankList.value = ChooseEmiModel(emptyList())
        originalEmiBankList.value = ChooseEmiModel(emptyList()) // Reset the original list too
        selectedCard.value = ""
        selectedOthersOption.value = ""
        searchQuery.value = ""
        selectTenureScreen.value = false
        selectedEmi.value = Pair(0, "")
        addCardScreen.value = false
        contentLoaded.value = false
        firstTimeLoaded.value = true
        filterList.value = listOf(Pair("No Cost EMI", false))
    }

    fun onCardClick(cardName: String) {
        selectedCard.value = cardName
        selectedOthersOption.value = ""
        searchQuery.value = ""

        // Find the selected card and check if any bank has `noCostApplied`
        val selectedCardType = _emiBankList.value.cards.find { it.cardType == cardName }
        val hasNoCostApplied = selectedCardType?.banks?.any { it.noCostApplied } ?: false

        // You can now use `hasNoCostApplied` for further logic or UI updates
        isFilterExisted.value = hasNoCostApplied
        _emiBankList.value = originalEmiBankList.value
    }


    fun onClickRadio(name: String) {
        selectedOthersOption.value = name
    }

    fun onValueChange(text: String) {
        searchQuery.value = text
        filterBanks()
    }

    fun onClickBank(bank: Bank) {
        val sortedEmiList = bank.emiList.sortedBy { it.duration }

        val sortedBank = bank.copy(emiList = sortedEmiList)

        selectedBank.value = sortedBank
        selectedOthersOption.value = ""
        selectTenureScreen.value = true
    }


    fun onClickRadio(duration: Int, amount: String) {
        selectedEmi.value = Pair(duration, amount)
    }

    fun onBackTenure() {
        selectTenureScreen.value = false
        selectedBank.value = null
        selectedEmi.value = Pair(0, "")
    }

    fun onProceedEmi(percent: Int) {
        selectedPercent.value = percent
        addCardScreen.value = true
    }

    fun onBackAddCard() {
        cardName.value = null
        cardIcon.value = (R.drawable.default_card_icon)
        cardNumber.value = null
        expiry.value = null
        cvv.value = null
        addCardScreen.value = false
        selectedPercent.value = null
    }

    fun onCardNumberChange(text: TextFieldValue) {
        if (text.text.length < 9) {
            cardIcon.value = (R.drawable.default_card_icon)
        }
        // Sanitize the input and get the new formatted number
        val digitsOnly = text.text.filter { it.isDigit() }

        // Limit the digits to a maximum of 16
        val limitedDigits = digitsOnly.take(16)

        // Format the card number with spaces every 4 digits
        val newFormattedNumber = formatCardNumber(limitedDigits)

        // Check if the length exceeds the maximum allowed length (19 including spaces)
        if (newFormattedNumber.length > 19) {
            return // Ignore input if it exceeds the max length
        }

        // Calculate the new cursor position
        val cursorPosition = text.selection.start
        val newCursorPosition =
            newFormattedNumber.length.coerceAtMost(cursorPosition + (newFormattedNumber.length - text.text.length))

        // Update the card number and text field value
        cardNumber.value = TextFieldValue(newFormattedNumber, TextRange(newCursorPosition))
        isCardValid.value = isCardValid()
    }

    fun onCardNameChange(text: String) {
        cardName.value = text
        isCardValid.value = isCardValid()
    }

    fun onCardExpiryChange(text: TextFieldValue) {
        // Extract only digits from the input
        var digitsOnly = text.text.filter { it.isDigit() }

        if (digitsOnly.length == 1 && digitsOnly.toIntOrNull() in 3..9) {
            digitsOnly = "0$digitsOnly"
        }

        // Limit the digits to a maximum of 4 (MMYY)
        val limitedDigits = digitsOnly.take(4)

        // Format the expiry date with a "/" after the first two digits
        val newFormattedExpiry = when {
            limitedDigits.length >= 2 -> {
                val month = limitedDigits.take(2)
                val year = limitedDigits.drop(2)
                "$month/$year"
            }

            else -> limitedDigits
        }

        // Calculate the cursor position after formatting
        val previousText = expiry.value?.text
        val isAddingCharacter = newFormattedExpiry.length > (previousText?.length ?: 0)
        val newCursorPosition = when {
            isAddingCharacter -> {
                // If a character was added, adjust the position accordingly
                val positionAdjustment = if (newFormattedExpiry.length == 3) 2 else 0
                text.selection.start + positionAdjustment
            }

            else -> {
                // If a character was removed, keep the cursor in place
                text.selection.start
            }
        }.coerceIn(0, newFormattedExpiry.length)

        // Update the expiry value with the new TextFieldValue and cursor position
        expiry.value = TextFieldValue(
            text = newFormattedExpiry,
            selection = TextRange(newCursorPosition)
        )

        // Optionally, re-validate the card
        isCardValid.value = isCardValid()
    }


    fun onCardCvvChange(text: String) {
        // Get the maximum length based on the card type
        val maxLength = if (isAmexCard.value) 4 else 3

        // Sanitize the input to ensure it's a numeric string and does not exceed max length
        val sanitizedText = text.filter { it.isDigit() }.take(maxLength)

        // Update the CVV state with the sanitized value
        cvv.value = sanitizedText
        isCardValid.value = isCardValid()
    }

    fun getImageDrawableForItem(item: String): Int {

        return when (item) {
            "VISA" -> R.drawable.visa
            "Mastercard" -> R.drawable.mastercard
            "Maestro" -> R.drawable.maestro
            "Cirrus" -> R.drawable.cirrus
            "AmericanExpress" -> R.drawable.american_express
            "Diners" -> R.drawable.diners
            "Discover" -> R.drawable.discover
            "Electron" -> R.drawable.electron
            "JCB" -> R.drawable.jcb
            "RUPAY" -> R.drawable.rupay
            "BancontactCard" -> R.drawable.bancontact
            "CARNET" -> R.drawable.carnet
            "CartesBancaires" -> R.drawable.cartesbancaires
            "ChinaUnionPay" -> R.drawable.chinaunionpay
            "Elo" -> R.drawable.elo
            "Hipercard" -> R.drawable.hipercard
            "Troy" -> R.drawable.troy
            "AllStar" -> R.drawable.allstar
            "LaSer" -> R.drawable.laser
            "Sears" -> R.drawable.sears
            "Overdrive" -> R.drawable.overdrive
            "Keyfuels" -> R.drawable.keyfuels
            "Supercharge" -> R.drawable.charge
            "UATP" -> R.drawable.uatp
            "Aura" -> R.drawable.auraaxis
            "Mada" -> R.drawable.mada
            "Bankcard" -> R.drawable.bankcard
            "Eftpos" -> R.drawable.eftpos
            "Bajaj" -> R.drawable.bajaj
            else -> R.drawable.card_02 // Default image resource
        }
    }

    fun getNoCostBanks(cardType: String) {
        filterList.value = filterList.value.map { filter ->
            Pair(filter.first, !filter.second)
        }
        if (filterList.value[0].second) {
            _emiBankList.update { currentList ->
                val updatedCards = currentList.cards.map { card ->
                    if (card.cardType == cardType) {
                        // Filter and sort banks for the specified card type
                        card.copy(
                            banks = card.banks
                                .filter { it.noCostApplied }
                                .sortedBy { it.name } // Sort by bank name if needed
                        )
                    } else {
                        card
                    }
                }
                currentList.copy(cards = updatedCards)
            }
        } else {
            _emiBankList.update {
                originalEmiBankList.value
            }
        }
    }

    private fun formatCardNumber(cardNumber: String): String {
        return cardNumber.filter { it.isDigit() } // Keep only digits
            .chunked(4) // Chunk the string into groups of 4
            .joinToString(" ")
    }

    fun addDashInsteadOfSlash(date: String): String {
        try {
            val mm = date.substring(0, 2)
            val yyyy = "20" + date.substring(3, 5)
            return yyyy + "-" + mm
        } catch (e: Exception) {
            return ""
        }
    }

    fun isCardValid(): Boolean {
        val cardNumber = cardNumber.value?.text?.filter { it.isDigit() }
        val expiry = expiry.value?.text?.filter { it.isDigit() }
        val cvv = cvv.value

        // 1. Validate Card Number (only digits, correct length, Luhn check)
        if (cardNumber?.isEmpty() == true || (cardNumber?.length != 16 && cardNumber?.length != 15)) return false

        // 2. Validate Expiry Date (MMYY format, not expired)
        if (expiry?.length != 4) return false // Expecting 4 digits (MMYY)
        val month = expiry.substring(0, 2).toIntOrNull() ?: return false
        val year = expiry.substring(2, 4).toIntOrNull() ?: return false
        if (month !in 1..12) return false // Invalid month

        val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        if (year < currentYear || (year == currentYear && month < currentMonth)) return false // Expired

        // 3. Validate CVV (3 digits for most cards, 4 for Amex)
        val isAmex = cardNumber.length == 15
        if ((isAmex && cvv?.length != 4) || (!isAmex && cvv?.length != 3)) return false
        if (isCardNumberEnabled.value == false) return false
        if (cardName.value.isNullOrEmpty()) return false

        // All validations passed
        return true
    }

    fun updateAddCardVisibility(visible: Boolean) {
        contentLoaded.value = visible
    }

}
