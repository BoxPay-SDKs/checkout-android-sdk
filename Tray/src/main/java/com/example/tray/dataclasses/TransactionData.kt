package com.example.tray.dataclasses

data class TransactionData(
    val token: String,
    val successReferenceScreenFullPath: String,
    val transactionId: String,
    val transactionAmount: String
)
