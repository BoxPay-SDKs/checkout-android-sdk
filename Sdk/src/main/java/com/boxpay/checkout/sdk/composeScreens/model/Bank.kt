package com.boxpay.checkout.sdk.composeScreens.model

data class CardType(
    val cardType: String, // Can be "credit", "debit", or "others"
    val banks: List<Bank>
)

data class Bank(
    val iconUrl: String,
    val name: String,
    val percent: String,
    val noCostApplied: Boolean,
    val emiList: List<Emi>,
    val cardLessEmiValue: String
)

data class ChooseEmiModel(
    val cards: List<CardType>
)

data class Emi(
    val duration: Int,
    val percent: Int,
    val amount: String,
    val totalAmount: String,
    val discount: String?,
    val interestCharged: String?,
    val noCostApplied: Boolean,
    val processingFee: String
)
