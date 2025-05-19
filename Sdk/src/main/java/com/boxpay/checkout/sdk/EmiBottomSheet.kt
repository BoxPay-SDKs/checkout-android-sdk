package com.boxpay.checkout.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.EmiViewModel
import com.boxpay.checkout.sdk.ViewModels.SingletonForDismissMainSheet
import com.boxpay.checkout.sdk.composeScreens.model.Bank
import com.boxpay.checkout.sdk.composeScreens.model.Emi
import com.boxpay.checkout.sdk.composeScreens.screen.AddCardDetailsScreen
import com.boxpay.checkout.sdk.composeScreens.screen.ChooseEmiScreen
import com.boxpay.checkout.sdk.composeScreens.screen.EmiShimmerScreen
import com.boxpay.checkout.sdk.composeScreens.screen.SelectTenureEmi
import com.boxpay.checkout.sdk.databinding.FragmentChooseEmiOptionBinding
import com.boxpay.checkout.sdk.enum.AnalyticsEvents
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.util.CommonFunctions
import com.boxpay.checkout.sdk.util.CommonFunctions.getEffectiveString
import com.boxpay.checkout.sdk.utils.generateRandomAlphanumericString
import com.boxpay.checkout.sdk.utils.handleException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

internal class EmiBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChooseEmiOptionBinding
    private val emiViewModel: EmiViewModel by activityViewModels()
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private lateinit var requestQueue: RequestQueue
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var Base_Session_API_URL: String
    private var token: String? = null
    private var shippingEnabled: Boolean = false
    private var transactionId: String? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }

            val window = d.window
            window?.apply {
                // Apply dim effect
                setDimAmount(0.5f) // 50% dimming
                setBackgroundDrawable(
                    ColorDrawable(
                        Color.argb(
                            128,
                            0,
                            0,
                            0
                        )
                    )
                ) // Semi-transparent black background
            }


            val screenHeight = resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.95 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    if (!binding.loadingRelativeLayout.isVisible) {
                        if (emiViewModel.addCardScreen.value) {
                            emiViewModel.onBackAddCard()
                        } else if (emiViewModel.selectTenureScreen.value) {
                            emiViewModel.onBackTenure()
                        } else {
                            dismissAndMakeButtonsOfMainBottomSheetEnabled()
                        }
                    }
                    true
                } else {
                    // Allow dialog to be dismissed if loader is not active
                    false
                }
            }

            dialog.setCancelable(!binding.boxPayLogoLottieAnimation.isVisible)

            bottomSheetBehavior?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Handle state changes
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            // Fully expanded
                        }

                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            // Collapsed
                        }

                        BottomSheetBehavior.STATE_DRAGGING -> {
                            // The BottomSheet is being dragged
//                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        BottomSheetBehavior.STATE_SETTLING -> {
                            // The BottomSheet is settling
//                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
                            //Hidden
                            dismissAndMakeButtonsOfMainBottomSheetEnabled()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        emiViewModel.clearFields()
        dismiss()
    }

    fun dismissFunction() {
        emiViewModel.clearFields()
        dismiss()
    }

    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        requestQueue = Volley.newRequestQueue(context)
        binding = FragmentChooseEmiOptionBinding.inflate(layoutInflater, container, false)


        requestQueue = Volley.newRequestQueue(context)
        showLoadingState()


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val screenHeight = requireContext().resources.displayMetrics.heightPixels
        val percentageOfScreenHeight = 0.90 // 70%
        val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

        bottomSheetBehavior?.maxHeight = desiredHeight
        binding.textView.text =
            if (emiViewModel.addCardScreen.value) "Add Card Details" else "Choose EMI Option"

        lifecycleScope.launchWhenStarted {
            emiViewModel.emiBankList.collectLatest { emiBankList ->
                try {
                    binding.composeView.setContent {
                        val showLoader = emiViewModel.showLoaderInButton.collectAsState()
                        if (emiViewModel.contentLoaded.value) {
                            if (!emiViewModel.selectTenureScreen.value && !emiViewModel.addCardScreen.value) {
                                dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                                ChooseEmiScreen(
                                    cardList = emiBankList,
                                    filterList = if (emiViewModel.isFilterExisted.value) emiViewModel.filterList.value else emptyList(),
                                    isSelectedCard = emiViewModel.selectedCard.value,
                                    onClickCard = {
                                        emiViewModel.onCardClick(it)
                                    },
                                    onClickBack = {
                                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                                    },
                                    onClickRadio = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED,
                                            "CardlessEMI",
                                            "EMI"
                                        )
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_METHOD_SELECTED,
                                            "CardlessEMI",
                                            "EMI"
                                        )
                                        emiViewModel.onClickRadio(it)
                                    },
                                    selectedRadioButton = emiViewModel.selectedOthersOption.value,
                                    sharedPreferences = sharedPreferences,
                                    searchQuery = emiViewModel.searchQuery.value,
                                    onValueChange = {
                                        emiViewModel.onValueChange(it)
                                    },
                                    onClickBank = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_METHOD_SELECTED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        emiViewModel.onClickBank(it)
                                    },
                                    onClickFilter = { card, filter ->
                                        emiViewModel.getBanksByFilter(card, filter)
                                    },
                                    onClickProceedButton = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INITIATED,
                                            "CardlessEMI",
                                            "EMI"
                                        )
                                        emiViewModel.showLoaderInButton.value = true
                                        postRequest(context!!)
                                    },
                                    showLoadingInButton = showLoader.value
                                )
                            }
                            if (emiViewModel.selectTenureScreen.value && !emiViewModel.addCardScreen.value) {
                                SelectTenureEmi(
                                    totalPrice = sharedPreferences.getString("amount", "empty")
                                        ?: "",
                                    onClickBack = { emiViewModel.onBackTenure() },
                                    selectedBank = emiViewModel.selectedBank.value!!,
                                    cardType = emiViewModel.selectedCard.value,
                                    selectedEmi = emiViewModel.selectedEmi.value,
                                    sharedPreferences = sharedPreferences,
                                    onClickRadio = { duration, amount , code->
                                        emiViewModel.onClickRadio(duration, amount, code)
                                    },
                                    onProceed = {percent, low, no, discount, netAmount ->
                                        emiViewModel.onProceedEmi(percent, low, no, discount, netAmount)
                                    },
                                    currencySymbol = sharedPreferences.getString(
                                        "currencySymbol",
                                        "₹"
                                    ) ?: ""
                                )
                            }
                            if (emiViewModel.addCardScreen.value) {
                                dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                                AddCardDetailsScreen(
                                    iconUrl = emiViewModel.selectedBank.value?.iconUrl ?: "",
                                    name = emiViewModel.selectedBank.value?.name ?: "",
                                    month = emiViewModel.selectedEmi.value.first,
                                    amount = emiViewModel.selectedEmi.value.second,
                                    percent = emiViewModel.selectedPercent.value ?: 0.0,
                                    onClickBack = {
                                        emiViewModel.onBackAddCard()
                                    },
                                    sharedPreferences = sharedPreferences,
                                    cardNumber = emiViewModel.cardNumber.value,
                                    cardName = emiViewModel.cardName.value,
                                    expiry = emiViewModel.expiry.value,
                                    cvv = emiViewModel.cvv.value,
                                    onCardNameChange = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_METHOD_SELECTED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        emiViewModel.onCardNameChange(it)
                                    },
                                    onCardExpiryChange = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_METHOD_SELECTED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        emiViewModel.onCardExpiryChange(it)
                                    },
                                    onCardNumberChange = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_METHOD_SELECTED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        emiViewModel.onCardNumberChange(it)
                                        if ((emiViewModel.cardNumber.value?.text?.length
                                                ?: 0) >= 9 && emiViewModel.cardIcon.value == R.drawable.default_card_icon
                                        ) {
                                            makeCardNetworkIdentificationCall(
                                                context!!,
                                                emiViewModel.cardNumber.value!!.text.filter { it.isDigit() })
                                        }
                                    },
                                    onCardCvvChange = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_METHOD_SELECTED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        emiViewModel.onCardCvvChange(it)
                                    },
                                    onProceedClick = {
                                        callUIAnalytics(
                                            requireContext(),
                                            AnalyticsEvents.PAYMENT_INITIATED,
                                            emiViewModel.selectedCard.value,
                                            "EMI"
                                        )
                                        emiViewModel.showLoaderInButton.value = true
                                        postRequest(context!!)
                                    },
                                    cardIcon = emiViewModel.cardIcon.value,
                                    currencySymbol = sharedPreferences.getString(
                                        "currencySymbol",
                                        "₹"
                                    ) ?: "",
                                    allDetailsValid = emiViewModel.isCardValid.value,
                                    isCardNumberEnabled = emiViewModel.isCardNumberEnabled.value,
                                    isAmexCard = emiViewModel.isAmexCard.value,
                                    showLoadingInButton = showLoader.value,
                                    isCardExpired = emiViewModel.isCardExpired.value,
                                    cardNumberErrorText = emiViewModel.cardNumberErrorText.value
                                )
                            }
                        }
                        if (emiViewModel.firstTimeLoaded.value) {
                            EmiShimmerScreen()
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }

        val baseUrl = sharedPreferences.getString("baseUrl", "null")

        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"

        if (emiViewModel.emiBankList.value.cards.isEmpty()) {
            fetchTransactionDetailsFromSharedPreferences()
            fetchEmiDetails()
        }

        return binding.root
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        token = sharedPreferences.getString("token", "empty")
    }

    private fun showLoadingState() {
        if (!emiViewModel.firstTimeLoaded.value) {
            binding.boxPayLogoLottieAnimation.apply {
                playAnimation()
                repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
            }
            binding.constraintLayout123.visibility = View.VISIBLE
            binding.loadingRelativeLayout.visibility = View.VISIBLE
            emiViewModel.updateAddCardVisibility(!binding.loadingRelativeLayout.isVisible)
        }
    }

    private fun fetchEmiDetails() {
        val url = "${Base_Session_API_URL}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {
                // Get the payment methods array
                val paymentMethodsArray =
                    response.getJSONObject("configs").getJSONArray("paymentMethods")

                // Filter payment methods based on type equal to "Wallet"
                for (i in 0 until paymentMethodsArray.length()) {
                    try {
                        val paymentMethod = paymentMethodsArray.getJSONObject(i)
                        if (paymentMethod.getString("type") == "Emi") {
                            val title = paymentMethod.getString("title")
                            val emiCardName = if (paymentMethod.getString("title")
                                    .contains("credit", true)
                            ) "Credit Card" else if (paymentMethod.getString("title")
                                    .contains("debit", true)
                            ) "Debit Card" else "Others"
                            var emiBankImage = paymentMethod.getString("logoUrl")
                            if (emiBankImage.startsWith("/assets")) {
                                emiBankImage =
                                    "https://checkout.boxpay.in" + paymentMethod.getString("logoUrl")
                            }
                            val bankName =
                                if (emiCardName.equals(
                                        "others",
                                        true
                                    )
                                ) paymentMethod.getJSONObject("emiMethod")
                                    .getString("cardlessEmiProviderTitle") else paymentMethod.getJSONObject(
                                    "emiMethod"
                                ).getString("issuerTitle")
                            val effectiveInterestRate = paymentMethod.getJSONObject("emiMethod")
                                .optDouble("effectiveInterestRate")

                            val bankInterestRate = if (emiCardName.equals("others", true)) {
                                0.0
                            } else {
                                effectiveInterestRate
                            }

                            val emiMethod = paymentMethod.getJSONObject("emiMethod")
                            var noApplicableOffer = false
                            var lowApplicableOffer = false
                            if (emiMethod.has("applicableOffer")) {
                                val applicableOffer = emiMethod.getJSONObject("applicableOffer")
                                val discount = applicableOffer.getJSONObject("discount")
                                noApplicableOffer = discount.getString("type").equals(
                                    "NoCostEmi", true
                                )
                                lowApplicableOffer = discount.getString("type").equals(
                                    "LowCostEmi", true
                                )
                            }
                            val bank = Bank(
                                iconUrl = emiBankImage,
                                name = bankName,
                                percent = if (noApplicableOffer) emiMethod.optDouble("interestRate") else bankInterestRate,
                                noCostApplied = noApplicableOffer,
                                lowCostApplied = lowApplicableOffer,
                                emiList = emptyList(),
                                cardLessEmiValue = emiMethod.optString("cardlessEmiProviderValue"),
                                issuerBrand = if (emiCardName.equals("others", true)) "" else emiMethod.optString("issuer")
                            )
                            val emi = Emi(
                                duration = emiMethod.optInt("duration"),
                                percent = if (noApplicableOffer) emiMethod.optDouble("interestRate") else bankInterestRate,
                                amount = emiMethod.optString("emiAmountLocaleFull"),
                                totalAmount = emiMethod.optString("totalAmountLocaleFull"),
                                discount = emiMethod.optString("merchantBorneInterestAmountLocaleFull"),
                                interestCharged = if (lowApplicableOffer) emiMethod.optString("interestChargedAmountLocaleFull") else emiMethod.optString(
                                    "bankChargedInterestAmountLocaleFull"
                                ),
                                noCostApplied = noApplicableOffer,
                                processingFee = if (emiMethod.optJSONObject("processingFee") == null) "0" else emiMethod.optJSONObject(
                                    "processingFee"
                                )?.getString("amountLocale") ?: "",
                                lowCostApplied = lowApplicableOffer,
                                code = emiMethod.optJSONObject("applicableOffer")?.getString("code")
                                    ?: "",
                                netAmount = emiMethod.optString("netAmountLocaleFull")
                            )
                            addBankDetails(cardType = emiCardName, bank = bank, emi = emi)
                        }
                        hideLoader()
                    } catch (e: Exception) {
                        context?.let {
                            handleException(
                                it,
                                e.message ?: "",
                                token ?: "",
                                Base_Session_API_URL,
                                "Emi Bank Details adding bank"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                context?.let {
                    handleException(
                        it,
                        e.message ?: "",
                        token ?: "",
                        Base_Session_API_URL,
                        "Emi Screen fetching the api details"
                    )
                }
            }

        }, { error ->

            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
            }
        })
        queue.add(jsonObjectAll)
    }

    private fun addBankDetails(cardType: String, bank: Bank, emi: Emi) {
        emiViewModel.addBankDetails(cardType, bank, emi)
    }

    private fun hideLoader() {
        if (emiViewModel.firstTimeLoaded.value) {
            emiViewModel.firstTimeLoaded.value = false
        }
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.constraintLayout123.visibility = View.GONE
        emiViewModel.updateAddCardVisibility(!binding.loadingRelativeLayout.isVisible)
    }

    companion object {
        fun newInstance(
            shippingEnabled: Boolean
        ): EmiBottomSheet {
            val fragment = EmiBottomSheet()
            fragment.shippingEnabled = shippingEnabled
            return fragment
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Handle the back button press here
        // Dismiss the dialog when the back button is pressed
        dismissAndMakeButtonsOfMainBottomSheetEnabled()
    }

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.O)
    fun postRequest(context: Context) {
        val requestQueue = Volley.newRequestQueue(context)

        // Constructing the request body
        val requestBody = JSONObject().apply {

            // Create the browserData JSON object
            val browserData = JSONObject().apply {
                // Get the default User-Agent string
                val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())

                // Get the screen height and width
                val displayMetrics = resources.displayMetrics
                put("screenHeight", displayMetrics.heightPixels.toString())
                put("screenWidth", displayMetrics.widthPixels.toString())
                put("acceptHeader", "application/json")
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", Locale.getDefault().toString())
                put("ipAddress", sharedPreferences.getString("ipAddress", "null"))
                put("colorDepth", 24) // Example value
                put("javaEnabled", true) // Example value
                put("timeZoneOffSet", 330) // Example value
                put("packageId", requireActivity().packageName)
            }
            put("browserData", browserData)

            val instrumentDetailsObject = JSONObject().apply {
                if (emiViewModel.selectedOthersOption.value.isEmpty()) {
                    put(
                        "type",
                        if (emiViewModel.selectedCard.value.contains(
                                "credit",
                                true
                            )
                        ) "emi/cc" else "emi/dc"
                    )
                    val cardNumber = emiViewModel.cardNumber.value!!.text.filter { it.isDigit() }
                    val expiry =
                        emiViewModel.addDashInsteadOfSlash(emiViewModel.expiry.value!!.text)
                    val cardObject = JSONObject().apply {
                        put("number", cardNumber)
                        put("expiry", expiry)
                        put("cvc", emiViewModel.cvv.value)
                        put("holderName", emiViewModel.cardName.value)

                        // Replace with the actual shopper VPA value
                    }
                    put("card", cardObject)
                    val emiObject = JSONObject().apply {
                        put("duration", emiViewModel.selectedEmi.value.first)
                    }
                    put("emi", emiObject)
                } else {
                    put(
                        "type",
                        "emi/cardless"
                    )
                    val emiObject = JSONObject().apply {
                        put("provider", emiViewModel.selectedOthersOption.value)
                        // Replace with the actual shopper VPA value
                    }
                    put("emi", emiObject)
                }
            }
            put("instrumentDetails", instrumentDetailsObject)

            if (!emiViewModel.offerSelectedCode.value.isNullOrEmpty()) {
                put("offers", JSONArray().apply {
                    put(emiViewModel.offerSelectedCode.value)
                })
            }

            val shopperObject = JSONObject().apply {
                put("email", sharedPreferences.getString("email", null))
                put("firstName", sharedPreferences.getString("firstName", null))

                put("gender", sharedPreferences.getString("gender", null))
                put("lastName", sharedPreferences.getString("lastName", null))
                put("phoneNumber", sharedPreferences.getString("phoneNumber", null))
                put("uniqueReference", sharedPreferences.getString("uniqueReference", null))
                sharedPreferences.getEffectiveString(
                    chosenKey   = "dateOfBirthChosen",
                    storedKey   = "dateOfBirth",
                    validator   = { it.isNotBlank() && it != "null" },
                    formatter   = { raw -> CommonFunctions.formatToISO8601WithCurrentTime(raw) }
                )?.let { put("dateOfBirth", it) }

                // panNumber: chosen first, otherwise stored
                sharedPreferences.getEffectiveString(
                    chosenKey = "panNumberChosen",
                    storedKey = "panNumber",
                    validator = { it.isNotBlank() && it != "null" }
                )?.let { put("panNumber", it) }

                if (shippingEnabled) {
                    val deliveryAddressObject = JSONObject().apply {

                        put("address1", sharedPreferences.getString("address1", null))
                        put("address2", sharedPreferences.getString("address2", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("countryCode", sharedPreferences.getString("countryCode", null))
                        put("postalCode", sharedPreferences.getString("postalCode", null))
                        put("state", sharedPreferences.getString("state", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("email", sharedPreferences.getString("email", null))
                        put("phoneNumber", sharedPreferences.getString("phoneNumber", null))
                        put("countryName", sharedPreferences.getString("countryName", null))

                    }
                    put("deliveryAddress", deliveryAddressObject)
                }
            }
            put("shopper", shopperObject)

            val deviceDetails = JSONObject().apply {
                put("browser", Build.BRAND)
                put("platformVersion", Build.VERSION.RELEASE)
                put("deviceType", Build.MANUFACTURER)
                put("deviceName", Build.MANUFACTURER)
                put("deviceBrandName", Build.MODEL)
            }
            put("deviceDetails", deviceDetails)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                // Handle response
                try {
                    val status = response.getJSONObject("status").getString("status")
                    val reasonCode = response.getJSONObject("status").getString("reasonCode")
                    val reason = response.getJSONObject("status").getString("reason")
                    transactionId = response.getString("transactionId").toString()
                    updateTransactionIDInSharedPreferences(transactionId!!)

                    var url = ""

                    if (status.contains("Rejected", ignoreCase = true)) {
                        var cleanedMessage = reason.substringAfter(":")
                        if (!reasonCode.startsWith("uf", true)) {
                            cleanedMessage =
                                "Please retry using other payment method or try again in sometime"
                        }
                        PaymentFailureScreen({
                        }, errorMessage = cleanedMessage).show(
                            parentFragmentManager,
                            "FailureScreen"
                        )
                        emiViewModel.showLoaderInButton.value = false
                    } else {
                        val type =
                            response.getJSONArray("actions").getJSONObject(0).getString("type")
                        if (status.contains("RequiresAction", ignoreCase = true)) {
                            editor.putString("status", "RequiresAction")
                        }
                        if (type.contains("html", true)) {
                            url = response
                                .getJSONArray("actions")
                                .getJSONObject(0)
                                .getString("htmlPageString")
                        } else {
                            url = response
                                .getJSONArray("actions")
                                .getJSONObject(0)
                                .getString("url")
                        }

                        if (status.contains("Approved", ignoreCase = true)) {
                            editor.putString("status", "Success")
                            editor.putString("transactionId", transactionId)
                            editor.apply()
                            handleSuccess()
                            emiViewModel.showLoaderInButton.value = false
                            dismissAndMakeButtonsOfMainBottomSheetEnabled()
                        } else {
                            emiViewModel.showLoaderInButton.value = false
                            showLoadingState()
                            val intent = Intent(requireContext(), OTPScreenWebView::class.java)
                            intent.putExtra("url", url)
                            intent.putExtra("type", type)
                            startFunctionCalls()
                            startActivityForResult(intent, 333)
                        }
                    }
                    editor.apply()
                } catch (e: JSONException) {
                    hideLoader()
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                hideLoader()
                emiViewModel.showLoaderInButton.value = false
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired", true) == true) {
                        val callback = SingletonClass.getInstance().getYourObject()
                        val callbackForDismissing =
                            SingletonForDismissMainSheet.getInstance().getYourObject()
                        if (callback != null) {
                            callback.onPaymentResult(
                                PaymentResultObject(
                                    "Expired",
                                    transactionId ?: "",
                                    transactionId ?: ""
                                )
                            )
                        }
                        if (callbackForDismissing != null) {
                            callbackForDismissing.dismissFunction()
                        }
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                headers["X-Client-Connector-Name"] = "Android SDK"
                headers["X-Client-Connector-Version"] = BuildConfig.SDK_VERSION
                return headers
            }
        }.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }

    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.putString("operationId", transactionIdArg)
        editor.apply()
    }

    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            // Parse the JSON string
            val jsonObject = JSONObject(response)
            // Retrieve the value associated with the "message" key
            return jsonObject.getString("message")
        } catch (e: Exception) {
            context?.let {
                handleException(
                    it,
                    e.message ?: "",
                    token ?: "",
                    Base_Session_API_URL,
                    "Emi Screen error message extract"
                )
            }
        }
        return null
    }

    fun makeCardNetworkIdentificationCall(
        context: Context, cardNumber: String
    ) {
        val queue = Volley.newRequestQueue(context)
        val url = Base_Session_API_URL + "${token}/bank-identification-numbers/${cardNumber}"
        val jsonData = JSONObject()
        val request = object : JsonObjectRequest(Method.POST, url, jsonData, { response ->
            try {
                val currBrand = response.getJSONObject("paymentMethod").getString("brand")
                val methodEnabled = response.getBoolean("methodEnabled")
                val issuerName = response.optString("issuerName")
                if (issuerName == "null") {
                    emiViewModel.cardNumberErrorText.value = "We couldn't find any EMI plans for this card. Please try using a different card number."
                    emiViewModel.isCardNumberEnabled.value = false
                }else if (!issuerName.equals(emiViewModel.issuerBrand.value) && issuerName != "null") {
                    emiViewModel.cardNumberErrorText.value = "The card is ${response.optString("issuerTitle")} ${emiViewModel.selectedCard.value}. Please enter a card number that belongs to ${emiViewModel.selectedBank.value?.name} ${emiViewModel.selectedCard.value}"
                    emiViewModel.isCardNumberEnabled.value = false
                } else {
                    if (!methodEnabled) {
                        emiViewModel.cardNumberErrorText.value = "This card is not supported for the payment"
                    }
                    emiViewModel.isCardNumberEnabled.value = methodEnabled
                }
                emiViewModel.cardIcon.value = emiViewModel.getImageDrawableForItem(currBrand)
                emiViewModel.isAmexCard.value = currBrand.equals("AmericanExpress", true)
            } catch (e: Exception) {
                context?.let {
                    handleException(
                        it,
                        e.message ?: "",
                        token ?: "",
                        Base_Session_API_URL,
                        "Emi Screen network call to identify card type"
                    )
                }
            }
        }, Response.ErrorListener { _ ->

        }) {}
        queue.add(request)
    }

    private fun fetchStatusAndReason(url: String) {
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                try {
                    val status = response.getString("status")
                    val transactionId = response.getString("transactionId").toString()

                    if (status.contains(
                            "Approved",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {

                        editor.putString("status", "Success")
                        editor.putString("amount", response.getString("amount").toString())
                        editor.putString("transactionId", transactionId)
                        editor.apply()

                        if (isAdded && isResumed && !isStateSaved) {
                            hideLoader()
                            job?.cancel()
                            handleSuccess()
                        }

                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status", "RequiresAction")
                        editor.apply()
                    } else if (status.contains("Processing", ignoreCase = true)) {
                        editor.putString("status", "Posted")
                        editor.apply()
                    } else if (status.contains("FAILED", ignoreCase = true)) {

                        editor.putString("status", "Failed")
                        editor.apply()

                        if (isAdded && isResumed && !isStateSaved) {
                            hideLoader()
                            job?.cancel()
                            PaymentFailureScreen(
                                errorMessage = "Please retry using other payment method or try again in sometime"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }

                } catch (e: JSONException) {
                    context?.let {
                        handleException(
                            it,
                            e.message ?: "",
                            token ?: "",
                            Base_Session_API_URL,
                            "Emi Screen in fetch status and reason"
                        )
                    }
                }
            },
            Response.ErrorListener {
                // no op
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                return headers
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun startFunctionCalls() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3000)
                fetchStatusAndReason("${Base_Session_API_URL}${token}/status")
            }
        }
    }

    private fun callUIAnalytics(
        context: Context,
        event: String,
        paymentSubType: String,
        paymentType: String
    ) {
        val baseUrl = sharedPreferences.getString("baseUrl", "null")

        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put(AnalyticsEvents.CALLER_TOKEN, token)
            put(AnalyticsEvents.UI_EVENT, event)

            // Create eventAttrs JSON object
            val eventAttrs = JSONObject().apply {
                put(AnalyticsEvents.PAYMENT_TYPE, paymentType)
                put(AnalyticsEvents.PAYMENT_SUB_TYPE, paymentSubType)
            }
            put("eventAttrs", eventAttrs)

            // Create browserData JSON object
            val browserData = JSONObject().apply {
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", browserLanguage)
            }
            put("browserData", browserData)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://${baseUrl}/v0/ui-analytics", requestBody,
            Response.Listener { /*no response handling */},
            Response.ErrorListener { /*no response handling */ }) {}.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }

    private fun handleSuccess() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("isSuccessScreenVisible", true)) {
            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
            bottomSheet.setIsOfferApplied(emiViewModel.isLowCostSelected.value, emiViewModel.isNoCostSelected.value, emiViewModel.netAmount.value, emiViewModel.discount.value)
            bottomSheet.show(
                parentFragmentManager,
                "PaymentStatusBottomSheetWithDetails"
            )
            dismiss()
        } else {
            val callback = SingletonClass.getInstance().getYourObject()
            if (callback != null) {
                val mainBottomSheetFragment =
                    parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
                mainBottomSheetFragment?.dismissTheSheetAfterSuccess()
                dismiss()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 333) {
            if (resultCode == Activity.RESULT_OK) {
                hideLoader()
                job?.cancel()
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")
            }
        }
    }
}