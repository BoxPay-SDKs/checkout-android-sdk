package com.boxpay.checkout.sdk.dataclasses

import com.google.gson.annotations.SerializedName

data class DCCResponse(

    @SerializedName("dccQuotationId") var dccQuotationId: String? = null,
    @SerializedName("merchantId") var merchantId: String? = null,
    @SerializedName("context") var context: ContextResponse? = ContextResponse(),
    @SerializedName("baseMoney") var baseMoney: BaseMoney? = BaseMoney(),
    @SerializedName("dccQuotationDetails") var dccQuotationDetails: DccQuotationDetails? = DccQuotationDetails(),
    @SerializedName("brand") var brand: String? = null,
    @SerializedName("supportedPsps") var supportedPsps: ArrayList<String> = arrayListOf(),
    @SerializedName("addedOn") var addedOn: String? = null,
    @SerializedName("updatedOn") var updatedOn: String? = null,
    @SerializedName("addedOnLocale") var addedOnLocale: String? = null,
    @SerializedName("updatedOnLocale") var updatedOnLocale: String? = null

)

data class LegalEntityResponse(

    @SerializedName("code") var code: String? = null

)

data class ContextResponse(

    @SerializedName("legalEntity") var legalEntity: LegalEntityResponse? = LegalEntityResponse(),
    @SerializedName("countryCode") var countryCode: String? = null,
    @SerializedName("localeCode") var localeCode: String? = null,
    @SerializedName("clientPosId") var clientPosId: String? = null,
    @SerializedName("orderId") var orderId: String? = null,
    @SerializedName("clientOrgIP") var clientOrgIP: String? = null

)

data class BaseMoney(

    @SerializedName("amount") var amount: Int? = null,
    @SerializedName("currencyCode") var currencyCode: String? = null,
    @SerializedName("amountLocale") var amountLocale: String? = null,
    @SerializedName("amountLocaleFull") var amountLocaleFull: String? = null,
    @SerializedName("currencySymbol") var currencySymbol: String? = null

)

data class DccMoney(

    @SerializedName("amount") var amount: Double? = null,
    @SerializedName("currencyCode") var currencyCode: String? = null,
    @SerializedName("amountLocale") var amountLocale: String? = null,
    @SerializedName("amountLocaleFull") var amountLocaleFull: String? = null,
    @SerializedName("currencySymbol") var currencySymbol: String? = null

)

data class DccQuotationDetails(

    @SerializedName("dccMoney") var dccMoney: DccMoney? = DccMoney(),
    @SerializedName("fxRate") var fxRate: Double? = null,
    @SerializedName("marginPercent") var marginPercent: Double? = null,
    @SerializedName("commissionPercent") var commissionPercent: Int? = null,
    @SerializedName("source") var source: String? = null,
    @SerializedName("dspCode") var dspCode: String? = null,
    @SerializedName("dspQuotationReference") var dspQuotationReference: String? = null

)