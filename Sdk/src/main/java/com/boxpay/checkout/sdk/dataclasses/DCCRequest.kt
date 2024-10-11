package com.boxpay.checkout.sdk.dataclasses

import com.google.gson.annotations.SerializedName

data class DCCRequest(

    @SerializedName("context") var context: Context? = Context(),
    @SerializedName("money") var money: Money? = Money(),
    @SerializedName("shopper") var shopper: Shopper? = Shopper(),
    @SerializedName("instrument") var instrument: Instrument? = Instrument()


)


data class LegalEntity(

    @SerializedName("code") var code: String? = null

)

data class Context(

    @SerializedName("countryCode") var countryCode: String? = null,
    @SerializedName("legalEntity") var legalEntity: LegalEntity? = LegalEntity(),
    @SerializedName("clientPosId") var clientPosId: String? = null,
    @SerializedName("orderId") var orderId: String? = null,
    @SerializedName("localCode") var localCode: String? = null

)


data class Money(

    @SerializedName("amount") var amount: Int? = null,
    @SerializedName("currencyCode") var currencyCode: String? = null

)

data class Shopper(

    @SerializedName("firstName") var firstName: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("uniqueReference") var uniqueReference: String? = null,
    @SerializedName("phoneNumber") var phoneNumber: String? = null

)

data class Instrument(

    @SerializedName("brand") var brand: String? = null,
    @SerializedName("accountNumber") var accountNumber: String? = null

)