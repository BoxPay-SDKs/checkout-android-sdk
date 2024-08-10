package com.boxpay.checkout.sdk.dataclasses

data class WalletDataClass(
    val walletName: String,
    val walletImage: String,
    val walletBrand : String,
    val instrumentTypeValue : String
)


data class BnplDataClass(
    val bnplName: String,
    val bnplImagee: String,
    val bnplBrand: String,
    val instrumentTypeValue: String
)