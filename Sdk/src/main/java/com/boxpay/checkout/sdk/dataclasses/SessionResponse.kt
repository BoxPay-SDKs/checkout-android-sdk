package com.boxpay.checkout.sdk.dataclasses

import com.google.gson.annotations.SerializedName

data class SessionResponse(

    @SerializedName("token") var token: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("productName") var productName: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("callerType") var callerType: String? = null,
    @SerializedName("merchantId") var merchantId: String? = null,
    @SerializedName("paymentDetails") var paymentDetails: PaymentDetails? = PaymentDetails(),
    @SerializedName("merchantDetails") var merchantDetails: MerchantDetails? = MerchantDetails(),
    @SerializedName("configs") var configs: Configs? = Configs(),
    @SerializedName("sessionExpiryTimestamp") var sessionExpiryTimestamp: String? = null,
    @SerializedName("lastPaidAtTimestamp") var lastPaidAtTimestamp: String? = null,
    @SerializedName("lastTransactionId") var lastTransactionId: String? = null,
    @SerializedName("status") var status: String? = null,
    @SerializedName("anyTransactionSuccessful") var anyTransactionSuccessful: Boolean? = null,
    @SerializedName("multipleTransactionSupported") var multipleTransactionSupported: Boolean? = null,
    @SerializedName("lastTransactionDetails") var lastTransactionDetails: String? = null,
    @SerializedName("sessionExpiryTimestampLocale") var sessionExpiryTimestampLocale: String? = null

)

data class ContextSession(

    @SerializedName("legalEntity") var legalEntity: LegalEntitySession? = LegalEntitySession(),
    @SerializedName("countryCode") var countryCode: String? = null,
    @SerializedName("localeCode") var localeCode: String? = null,
    @SerializedName("clientPosId") var clientPosId: String? = null,
    @SerializedName("orderId") var orderId: String? = null,
    @SerializedName("clientOrgIP") var clientOrgIP: String? = null

)

data class LegalEntitySession(

    @SerializedName("code") var code: String? = null

)

data class MoneySession(

    @SerializedName("amount") var amount: Int? = null,
    @SerializedName("currencyCode") var currencyCode: String? = null,
    @SerializedName("amountLocale") var amountLocale: String? = null,
    @SerializedName("amountLocaleFull") var amountLocaleFull: String? = null,
    @SerializedName("currencySymbol") var currencySymbol: String? = null

)

data class DeliveryAddress(

    @SerializedName("address1") var address1: String? = null,
    @SerializedName("address2") var address2: String? = null,
    @SerializedName("address3") var address3: String? = null,
    @SerializedName("city") var city: String? = null,
    @SerializedName("state") var state: String? = null,
    @SerializedName("countryCode") var countryCode: String? = null,
    @SerializedName("postalCode") var postalCode: String? = null,
    @SerializedName("shopperRef") var shopperRef: String? = null,
    @SerializedName("addressRef") var addressRef: String? = null,
    @SerializedName("labelType") var labelType: String? = null,
    @SerializedName("labelName") var labelName: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("phoneNumber") var phoneNumber: String? = null

)

data class ShopperSession(

    @SerializedName("firstName") var firstName: String? = null,
    @SerializedName("lastName") var lastName: String? = null,
    @SerializedName("gender") var gender: String? = null,
    @SerializedName("phoneNumber") var phoneNumber: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("uniqueReference") var uniqueReference: String? = null,
    @SerializedName("deliveryAddress") var deliveryAddress: DeliveryAddress? = DeliveryAddress(),
    @SerializedName("dateOfBirth") var dateOfBirth: String? = null,
    @SerializedName("panNumber") var panNumber: String? = null

)

data class Items(

    @SerializedName("id") var id: String? = null,
    @SerializedName("itemName") var itemName: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("quantity") var quantity: Int? = null,
    @SerializedName("manufacturer") var manufacturer: String? = null,
    @SerializedName("brand") var brand: String? = null,
    @SerializedName("color") var color: String? = null,
    @SerializedName("productUrl") var productUrl: String? = null,
    @SerializedName("imageUrl") var imageUrl: String? = null,
    @SerializedName("categories") var categories: String? = null,
    @SerializedName("amountWithoutTax") var amountWithoutTax: Double? = null,
    @SerializedName("taxAmount") var taxAmount: Double? = null,
    @SerializedName("taxPercentage") var taxPercentage: String? = null,
    @SerializedName("discountedAmount") var discountedAmount: String? = null,
    @SerializedName("timestamp") var timestamp: String? = null,
    @SerializedName("gender") var gender: String? = null,
    @SerializedName("size") var size: String? = null,
    @SerializedName("amountWithoutTaxLocale") var amountWithoutTaxLocale: String? = null,
    @SerializedName("amountWithoutTaxLocaleFull") var amountWithoutTaxLocaleFull: String? = null,
    @SerializedName("taxAmountLocale") var taxAmountLocale: String? = null,
    @SerializedName("taxAmountLocaleFull") var taxAmountLocaleFull: String? = null

)

data class Order(

    @SerializedName("voucherCode") var voucherCode: String? = null,
    @SerializedName("shippingAmount") var shippingAmount: Int? = null,
    @SerializedName("taxAmount") var taxAmount: Double? = null,
    @SerializedName("originalAmount") var originalAmount: Double? = null,
    @SerializedName("totalDiscountedAmount") var totalDiscountedAmount: String? = null,
    @SerializedName("items") var items: ArrayList<Items> = arrayListOf(),
    @SerializedName("shippingAmountLocale") var shippingAmountLocale: String? = null,
    @SerializedName("shippingAmountLocaleFull") var shippingAmountLocaleFull: String? = null,
    @SerializedName("taxAmountLocale") var taxAmountLocale: String? = null,
    @SerializedName("taxAmountLocaleFull") var taxAmountLocaleFull: String? = null,
    @SerializedName("originalAmountLocale") var originalAmountLocale: String? = null,
    @SerializedName("originalAmountLocaleFull") var originalAmountLocaleFull: String? = null

)

data class PaymentDetails(

    @SerializedName("context") var context: Context? = Context(),
    @SerializedName("money") var money: Money? = Money(),
    @SerializedName("onDemandAmount") var onDemandAmount: Boolean? = null,
    @SerializedName("frontendReturnUrl") var frontendReturnUrl: String? = null,
    @SerializedName("frontendBackUrl") var frontendBackUrl: String? = null,
    @SerializedName("billingAddress") var billingAddress: String? = null,
    @SerializedName("shopper") var shopper: Shopper? = Shopper(),
    @SerializedName("order") var order: Order? = Order(),
    @SerializedName("product") var product: String? = null,
    @SerializedName("subscriptionDetails") var subscriptionDetails: String? = null

)

data class CheckoutTheme(

    @SerializedName("headerColor") var headerColor: String? = null,
    @SerializedName("primaryButtonColor") var primaryButtonColor: String? = null,
    @SerializedName("secondaryButtonColor") var secondaryButtonColor: String? = null,
    @SerializedName("headerTextColor") var headerTextColor: String? = null,
    @SerializedName("buttonTextColor") var buttonTextColor: String? = null,
    @SerializedName("buttonShape") var buttonShape: String? = null,
    @SerializedName("buttonContent") var buttonContent: String? = null,
    @SerializedName("font") var font: String? = null

)

data class MerchantDetails(

    @SerializedName("merchantName") var merchantName: String? = null,
    @SerializedName("logoUrl") var logoUrl: String? = null,
    @SerializedName("checkoutTheme") var checkoutTheme: CheckoutTheme? = CheckoutTheme(),
    @SerializedName("timeZone") var timeZone: String? = null,
    @SerializedName("locale") var locale: String? = null,
    @SerializedName("template") var template: String? = null,
    @SerializedName("customFields") var customFields: String? = null

)

data class PaymentMethods(

    @SerializedName("id") var id: String? = null,
    @SerializedName("type") var type: String? = null,
    @SerializedName("brand") var brand: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("typeTitle") var typeTitle: String? = null,
    @SerializedName("logoUrl") var logoUrl: String? = null,
    @SerializedName("instrumentTypeValue") var instrumentTypeValue: String? = null,
    @SerializedName("applicableOffers") var applicableOffers: ArrayList<String> = arrayListOf()

)

data class Configs(

    @SerializedName("paymentMethods") var paymentMethods: ArrayList<PaymentMethods>? = arrayListOf(),
    @SerializedName("additionalFieldSets") var additionalFieldSets: ArrayList<String>? = arrayListOf(),
    @SerializedName("enabledFields") var enabledFields: ArrayList<EnabledFields>? = arrayListOf(),
    @SerializedName("referrers") var referrers: ArrayList<String>? = arrayListOf()

)


data class EnabledFields(

    @SerializedName("field") var field: String? = null,
    @SerializedName("editable") var editable: Boolean? = null,
    @SerializedName("mandatory") var mandatory: Boolean? = null

)