package com.boxpay.checkout.sdk

import FailureScreenSharedViewModel
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.EmiViewModel
import com.boxpay.checkout.sdk.composeScreens.model.Bank
import com.boxpay.checkout.sdk.composeScreens.model.Emi
import com.boxpay.checkout.sdk.composeScreens.screen.AddCardDetailsScreen
import com.boxpay.checkout.sdk.composeScreens.screen.ChooseEmiScreen
import com.boxpay.checkout.sdk.composeScreens.screen.SelectTenureEmi
import com.boxpay.checkout.sdk.databinding.FragmentChooseEmiOptionBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class EmiBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChooseEmiOptionBinding
    private val emiViewModel: EmiViewModel by activityViewModels()
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private lateinit var requestQueue: RequestQueue
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var Base_Session_API_URL: String
    private var successScreenFullReferencePath: String? = null
    private var token: String? = null

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


            dialog.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    // Prevent dialog from being dismissed if loader is active
                    true
                } else {
                    // Allow dialog to be dismissed if loader is not active
                    false
                }
            }

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

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        emiViewModel.clearFields()
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        requestQueue = Volley.newRequestQueue(context)
        binding = FragmentChooseEmiOptionBinding.inflate(layoutInflater, container, false)


        val failureScreenSharedViewModelCallback =
            FailureScreenSharedViewModel(::failurePaymentFunction)
        FailureScreenCallBackSingletonClass.getInstance().callBackFunctions =
            failureScreenSharedViewModelCallback

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

        lifecycleScope.launchWhenStarted {
            emiViewModel.emiBankList.collectLatest { emiBankList ->
                binding.composeView.setContent {
                    if (emiBankList.cards.isNotEmpty() && !binding.loadingRelativeLayout.isVisible && !emiViewModel.selectTenureScreen.value && !emiViewModel.addCardScreen.value) {
                        ChooseEmiScreen(
                            cardList = emiBankList,
                            filterList = emptyList(),
                            isSelectedCard = emiViewModel.selectedCard.value,
                            onClickCard = {
                                emiViewModel.onCardClick(it)
                            },
                            onClickBack = {
                                dismissAndMakeButtonsOfMainBottomSheetEnabled()
                            },
                            onClickRadio = {
                                emiViewModel.onClickRadio(it)
                            },
                            selectedRadioButton = emiViewModel.selectedOthersOption.value,
                            sharedPreferences = sharedPreferences,
                            searchQuery = emiViewModel.searchQuery.value,
                            onValueChange = {
                                emiViewModel.onValueChange(it)
                            },
                            onClickBank = {
                                emiViewModel.onClickBank(it)
                            }
                        )
                    }
                    if (emiViewModel.selectTenureScreen.value && !emiViewModel.addCardScreen.value) {
                        SelectTenureEmi(
                            totalPrice = sharedPreferences.getString(
                                "currencySymbol",
                                "₹"
                            ) + sharedPreferences.getString("amount", "empty"),
                            onClickBack = { emiViewModel.onBackTenure() },
                            selectedBank = emiViewModel.selectedBank.value!!,
                            cardType = emiViewModel.selectedCard.value,
                            selectedEmi = emiViewModel.selectedEmi.value,
                            sharedPreferences = sharedPreferences,
                            onClickRadio = { duration, amount ->
                                emiViewModel.onClickRadio(duration, amount)
                            },
                            onProceed = {
                                emiViewModel.onProceedEmi(it)
                            }
                        )
                    }

                    if (emiViewModel.addCardScreen.value) {
                        AddCardDetailsScreen(
                            iconUrl = emiViewModel.selectedBank.value?.iconUrl ?: "",
                            name = emiViewModel.selectedBank.value?.name ?: "",
                            month = emiViewModel.selectedEmi.value.first,
                            amount = emiViewModel.selectedEmi.value.second,
                            percent = emiViewModel.selectedPercent.value ?: 0,
                            onClickBack = {
                                emiViewModel.onBackAddCard()
                            },
                            sharedPreferences = sharedPreferences,
                            cardNumber = emiViewModel.cardNumber.value,
                            cardName = emiViewModel.cardName.value,
                            expiry = emiViewModel.expiry.value,
                            cvv = emiViewModel.cvv.value,
                            onCardNameChange = {
                                emiViewModel.onCardNameChange(it)
                            },
                            onCardExpiryChange = {
                                emiViewModel.onCardExpiryChange(it)
                            },
                            onCardNumberChange = {
                                emiViewModel.onCardNumberChange(it)
                            },
                            onCardCvvChange = {
                                emiViewModel.onCardCvvChange(it)
                            }
                        )
                    }
                }
            }
        }
        showLoadingState()


        val baseUrl = sharedPreferences.getString("baseUrl", "null")

        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"

        fetchTransactionDetailsFromSharedPreferences()
        fetchEmiDetails()

        return binding.root
    }

    fun failurePaymentFunction() {

        // Start a coroutine with a delay of 5 seconds
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // Delay for 1 seconds

            // Code inside this block will execute after the delay
            // Code inside this block will execute after the delay
            val bottomSheet = PaymentFailureScreen()
            bottomSheet.show(parentFragmentManager, "PaymentFailureScreen")
        }
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    private fun showLoadingState() {
        binding.boxPayLogoLottieAnimation.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.loadingRelativeLayout.visibility = View.VISIBLE
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
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    if (paymentMethod.getString("type") == "Emi") {
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
                        val bankInterestRate =
                            if (emiCardName.equals(
                                    "others",
                                    true
                                )
                            ) 0 else paymentMethod.getJSONObject("emiMethod").getInt("interestRate")
                        val bank = Bank(
                            iconUrl = emiBankImage,
                            name = bankName,
                            percent = "@$bankInterestRate%p.a",
                            noCostApplied = false,
                            emiList = emptyList()
                        )
                        val emiMethod = paymentMethod.getJSONObject("emiMethod")
                        val emi = Emi(
                            duration = emiMethod.getInt("duration"),
                            percent = emiMethod.getInt("interestRate"),
                            amount = emiMethod.getString("emiAmountLocale"),
                            totalAmount = emiMethod.getString("totalAmountLocale"),
                            discount = null,
                            interestCharged = emiMethod.getString("interestChargedAmountLocale"),
                            noCostApplied = false,
                            processingFee = emiMethod.getJSONObject("processingFee")
                                .getString("amountLocale")
                        )
                        addBankDetails(cardType = emiCardName, bank = bank, emi = emi)
                    }
                    hideLoader()
                }
            } catch (_: Exception) {

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
        binding.loadingRelativeLayout.visibility = View.GONE
    }

    companion object {
        fun newInstance(): EmiBottomSheet {
            val fragment = EmiBottomSheet()
            return fragment
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Handle the back button press here
        // Dismiss the dialog when the back button is pressed
        dismissAndMakeButtonsOfMainBottomSheetEnabled()
    }
}