package com.boxpay.checkout.sdk.ViewModels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.boxpay.checkout.sdk.composeScreens.model.Bank
import com.boxpay.checkout.sdk.composeScreens.model.CardType
import com.boxpay.checkout.sdk.composeScreens.model.ChooseEmiModel
import com.boxpay.checkout.sdk.composeScreens.model.Emi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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
    val cardNumber = mutableStateOf("")
    val cardName = mutableStateOf("")
    val expiry = mutableStateOf("")
    val cvv = mutableStateOf("")

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

        _emiBankList.update {
            if (existingCardType != null) {
                // Check if the bank already exists in the banks list
                val existingBank =
                    existingCardType.banks.find { it.name == bank.name && it.iconUrl == bank.iconUrl }

                if (existingBank != null) {
                    // Check if the EMI already exists in the emiList
                    val emiExists =
                        existingBank.emiList.any { it.duration == emi.duration && it.amount == emi.amount }

                    if (!emiExists) {
                        // Append the new EMI to the existing EMI list
                        val updatedBank = existingBank.copy(emiList = existingBank.emiList + emi)
                        val updatedCardType =
                            existingCardType.copy(banks = existingCardType.banks.map {
                                if (it.name == bank.name && it.iconUrl == bank.iconUrl) updatedBank else it
                            })
                        it.copy(cards = it.cards.map { card ->
                            if (card.cardType == cardType) updatedCardType else card
                        })
                    } else {
                        // No changes if the EMI already exists
                        it
                    }
                } else {
                    // Add the bank with the new EMI if it doesn't exist in the list
                    val newBankWithEmi = bank.copy(emiList = listOf(emi))
                    val updatedCardType =
                        existingCardType.copy(banks = existingCardType.banks + newBankWithEmi)
                    it.copy(cards = it.cards.map { card ->
                        if (card.cardType == cardType) updatedCardType else card
                    })
                }
            } else {
                // Add the new card type with the bank and EMI if the card type doesn't exist
                val newBankWithEmi = bank.copy(emiList = listOf(emi))
                it.copy(
                    cards = it.cards + CardType(
                        cardType = cardType,
                        banks = listOf(newBankWithEmi)
                    )
                )
            }
        }

        originalEmiBankList.value = emiBankList.value
        println("=====original ${originalEmiBankList.value}")
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
                }.filter { it.banks.isNotEmpty() } // Only keep card types with matching banks
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
    }

    fun onCardClick(cardName: String) {
        selectedCard.value = cardName
        selectedOthersOption.value = ""
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
        addCardScreen.value = false
        selectedPercent.value = null
    }

    fun onCardNumberChange(text: String) {
        cardNumber.value = text
    }

    fun onCardNameChange(text: String) {
        cardName.value = text
    }

    fun onCardExpiryChange(text: String) {
        expiry.value = text
    }

    fun onCardCvvChange(text: String) {
        cvv.value = text
    }
}
