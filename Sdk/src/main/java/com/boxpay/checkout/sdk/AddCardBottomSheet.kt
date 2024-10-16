package com.boxpay.checkout.sdk

import DismissViewModel
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import coil.decode.SvgDecoder
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.airbnb.lottie.LottieDrawable
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.DCCViewModel
import com.boxpay.checkout.sdk.ViewModels.SessionViewModel
import com.boxpay.checkout.sdk.ViewModels.SingletonForDismissMainSheet
import com.boxpay.checkout.sdk.databinding.FragmentAddCardBottomSheetBinding
import com.boxpay.checkout.sdk.dataclasses.CurrencyData
import com.boxpay.checkout.sdk.dataclasses.DCCRequest
import com.boxpay.checkout.sdk.dataclasses.DCCResponse
import com.boxpay.checkout.sdk.dataclasses.Instrument
import com.boxpay.checkout.sdk.dataclasses.LegalEntity
import com.boxpay.checkout.sdk.dataclasses.Money
import com.boxpay.checkout.sdk.dataclasses.SessionResponse
import com.boxpay.checkout.sdk.dataclasses.Shopper
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random


internal class AddCardBottomSheet : BottomSheetDialogFragment() {

    private var callback: UpdateMainBottomSheetInterface? = null
    private lateinit var binding: FragmentAddCardBottomSheetBinding
    private lateinit var viewModel: DismissViewModel
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var bottomSheet: FrameLayout? = null
    private lateinit var Base_Session_API_URL: String
    private lateinit var requestQueue: RequestQueue
    private var token: String? = null
    private var cardNumber: String? = null
    private var cardExpiryYYYY_MM: String? = null
    private var job: Job? = null
    private var cvv: String? = null
    private var cardHolderName: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private var isCardNumberValid: Boolean = false
    private var isCardValidityValid: Boolean = false
    private var isCardCVVValid: Boolean = false
    private var isNameOnCardValid: Boolean = false
    private var successScreenFullReferencePath: String? = null
    private var isAmericanExpressCard = MutableLiveData<Boolean>()
    private var transactionId: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var cardNetworkFound = false
    private var cardNetworkName: String = ""
    private var shippingEnabled: Boolean = false
    private val dccViewModel: DCCViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by viewModels()
    private var sessionData: SessionResponse? = null
    private var isCurrencySelected = false
    private var dccRequest: DCCRequest? = null
    private var isDCCFetched = false
    private var isDCCEnabled = false
    private var quotationID: String? = ""
    private var isQuotationRequired = false
    private var dccResponseUniversal : DCCResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Handle the back button press here
        // Dismiss the dialog when the back button is pressed
        dismissAndMakeButtonsOfMainBottomSheetEnabled()
    }

    fun makeCardNetworkIdentificationCall(
        context: Context, cardNumber: String, completeCardNumber: String
    ) {
        val queue = Volley.newRequestQueue(context)
        val url = Base_Session_API_URL + "${token}/bank-identification-numbers/${cardNumber}"
        val jsonData = JSONObject()
        val brands = mutableListOf<String>()
        val request = object : JsonObjectRequest(Method.POST, url, jsonData, { response ->
            logJsonObject(response)
            try {
                val currBrand = response.getJSONObject("paymentMethod").getString("brand")
                brands.add(currBrand)
                cardNetworkName = currBrand
                val methodEnabled = response.getBoolean("methodEnabled")

                if (!methodEnabled) {
                    isCardNumberValid = false
                    binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                    binding.textView4.text = "This card is not supported for the payment"
                    proceedButtonIsEnabled.value = false
                }

                updateCardNetwork(brands)
                //we get the card type from the API and call the DCC API
                dccRequest!!.instrument!!.brand = cardNetworkName
                dccRequest!!.instrument!!.accountNumber = completeCardNumber
                if (!isDCCFetched && isCardNumberValid && completeCardNumber.length >= 10) {
                    dccViewModel.getDCC(dccRequest!!, token!!).distinctUntilChanged()
                        .observe(this, Observer { dccResponse ->
                            if (dccResponse != null) {
                                //successful
                                callAndSetDCCData(dccResponse)
                                dccResponseUniversal = dccResponse
                            }else{
                                binding.flLoaderAndDcc.visibility = View.GONE
                                PaymentFailureScreen(
                                    errorMessage = "Please retry using other payment method or try again in sometime"
                                ).show(parentFragmentManager, "FailureScreen")
                            }
                        })
                }

            } catch (e: Exception) {
                Log.e("TAG", "makeCardNetworkIdentificationCall: ", e)
                binding.flLoaderAndDcc.visibility = View.GONE
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")
            }
        }, Response.ErrorListener { _ ->

        }) {}
        queue.add(request)
    }


    private fun getImageDrawableForItem(item: String): Int {

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

    private fun removeAndAddImageCardNetworks(cardNetworkName: String) {
        isAmericanExpressCard.value = cardNetworkName == "AmericanExpress"

        binding.defaultCardNetworkLinearLayout.visibility = View.GONE
        val imageView = ImageView(requireContext())
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        imageView.layoutParams = layoutParams
        val imageDrawable = getImageDrawableForItem(cardNetworkName)
        imageView.setImageResource(imageDrawable)



        binding.fetchedCardNetwork.removeAllViews()
        binding.fetchedCardNetwork.visibility = View.VISIBLE
        binding.fetchedCardNetwork.addView(imageView)
    }


    private fun updateCardNetwork(brands: MutableList<String>) {
        if (brands.size == 1) {

            removeAndAddImageCardNetworks(brands[0])
        } else {

            binding.fetchedCardNetwork.removeAllViews()
            binding.fetchedCardNetwork.visibility = View.GONE
            binding.defaultCardNetworkLinearLayout.visibility = View.VISIBLE
        }
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {

        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()

        viewModel.onChildDismissed()
        dismiss()
    }

    private fun dismissMainBottomSheet() {

    }


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddCardBottomSheetBinding.inflate(inflater, container, false)
        requestQueue = Volley.newRequestQueue(context)
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        clearAllDCCData(requireActivity())


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }



        viewModel = ViewModelProvider(this).get(DismissViewModel::class.java)

        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"


        fetchTransactionDetailsFromSharedPreferences()
        sessionViewModel.createCheckoutSession(token!!).observe(this, Observer { response ->
            if (response != null) {
                sessionData = response
                dccRequest = DCCRequest(
                    context = com.boxpay.checkout.sdk.dataclasses.Context(
                        countryCode = sessionData!!.paymentDetails!!.context!!.countryCode,
                        legalEntity = LegalEntity(
                            code = sessionData!!.paymentDetails!!.context!!.legalEntity!!.code
                        ),
                        clientPosId = null,
                        orderId = null,
                        localCode = sessionData!!.paymentDetails!!.context!!.localCode
                    ), money = Money(
                        amount = sessionData!!.paymentDetails!!.money!!.amount,
                        currencyCode = sessionData!!.paymentDetails!!.money!!.currencyCode
                    ), shopper = Shopper(
                        firstName = sessionData!!.paymentDetails!!.shopper!!.firstName,
                        email = sessionData!!.paymentDetails!!.shopper!!.email,
                        uniqueReference = sessionData!!.paymentDetails!!.shopper!!.uniqueReference,
                        phoneNumber = sessionData!!.paymentDetails!!.shopper!!.phoneNumber
                    ), instrument = Instrument(
                        brand = "", accountNumber = ""
                    )
                )
            }else{
            binding.flLoaderAndDcc.visibility = View.GONE
        }
        })


        binding.radioButton1.buttonTintList = ColorStateList.valueOf(
            Color.parseColor(
                sharedPreferences.getString("primaryButtonColor", "#000000")
            )
        )
        binding.radioButton2.buttonTintList = ColorStateList.valueOf(
            Color.parseColor(
                sharedPreferences.getString("primaryButtonColor", "#000000")
            )
        )
        binding.radioButton1.setOnClickListener() {
            if (binding.radioButton1.isChecked) {
                binding.radioButton2.isChecked = false
                isCurrencySelected = true
                proceedButtonIsEnabled.value = true
                enableProceedButton()
                isQuotationRequired = true
            }
        }

        binding.radioButton2.setOnClickListener() {
            if (binding.radioButton2.isChecked) {
                binding.radioButton1.isChecked = false
                isCurrencySelected = true
                proceedButtonIsEnabled.value = true
                enableProceedButton()
                isQuotationRequired = false
            }
        }


        val allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890. "
        val filter = InputFilter { source, _, _, _, _, _ ->
            // Filter out characters not present in the allowed characters list
            source.filter { allowedCharacters.contains(it) }
        }
        binding.editTextNameOnCard.filters = arrayOf(filter)



        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.progressBar.visibility = View.INVISIBLE
        proceedButtonIsEnabled.observe(this, Observer { enableProceedButton ->
            if (enableProceedButton) {
                if (isCardNumberValid && isCardValidityValid && isCardCVVValid && isNameOnCardValid && isCurrencySelected) {
                    enableProceedButton()
                }
            } else {

                disableProceedButton()
            }
        })
        proceedButtonIsEnabled.value = false

        var checked = false
        binding.progressBar.visibility = View.INVISIBLE
        binding.ll1InvalidCardNumber.visibility = View.INVISIBLE
        binding.invalidCardValidity.visibility = View.INVISIBLE
        binding.invalidCVV.visibility = View.INVISIBLE
        binding.saveCardLinearLayout.setOnClickListener() {
            if (!binding.progressBar.isVisible) {
                if (!checked) {
                    binding.imageView3.setImageResource(R.drawable.checkbox)
                    checked = true
                } else {
                    binding.imageView3.setImageResource(0)
                    checked = false
                }
            }
        }


        binding.backButton.setOnClickListener() {
            if (!binding.progressBar.isVisible && !binding.loadingLayout.isVisible) {
                dismissAndMakeButtonsOfMainBottomSheetEnabled()
            }
        }

        binding.editTextCardNumber.filters = arrayOf(InputFilter.LengthFilter(19))


        binding.editTextCardNumber.addTextChangedListener(object : TextWatcher {

            var isFormatting = false // Flag to prevent reformatting when deleting spaces
            var userDeletingChars = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                userDeletingChars = count > after

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {


                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                if (s.toString().isNullOrBlank()) {
                    isCardNumberValid = false
                    proceedButtonIsEnabled.value = false
                } else {
                    isCardNumberValid = true
                    proceedButtonIsEnabled.value = true
                }
                s.let {
                    if (s?.length == 19) {
                        val text = s.toString().replace("\\s".toRegex(), "")
                        if (isValidCardNumberByLuhn(removeSpaces(text))) {
                            binding.editTextCardValidity.requestFocus()
                            isCardNumberValid = true
                            binding.ll1InvalidCardNumber.visibility = View.INVISIBLE
                            proceedButtonIsEnabled.value = true
                        } else {
                            isCardNumberValid = false
                            proceedButtonIsEnabled.value = false
                        }
                    }
                    enableProceedButton()
                }

                callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "", "Card")
            }

            override fun afterTextChanged(s: Editable?) {


                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                if (!isFormatting) {
                    s?.let { editable ->
                        val textNow = editable.toString()
                        val text = textNow.replace("\\s".toRegex(), "")
                        val formattedText = formatCardNumber(text)
                        if (editable.toString() != formattedText && !userDeletingChars) {
                            isFormatting = true // Set flag to prevent reformatting
                            binding.editTextCardNumber.setText(formattedText)
                            binding.editTextCardNumber.setSelection(formattedText.length)
                        } else if (editable.toString().length > 1 && editable.toString()[editable.toString().length - 1] == ' ') {
                            editable.delete(editable.length - 1, editable.length)
                        }

                        isFormatting = false // Reset the flag

                        if (text.isBlank()) {
                            isCardNumberValid = false
                            proceedButtonIsEnabled.value = false
                        } else if (text.length == 19) {
                            if (isValidCardNumberByLuhn(removeSpaces(text))) {
                                binding.editTextCardValidity.requestFocus()
                                isCardNumberValid = true
                                binding.ll1InvalidCardNumber.visibility = View.INVISIBLE
                                proceedButtonIsEnabled.value = true
                            } else {
                                isCardNumberValid = false
                                binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                                proceedButtonIsEnabled.value = false
                            }
                        }

                        if (text.length >= 9) {
                            makeCardNetworkIdentificationCall(
                                requireContext(), text.substring(0, 9), text
                            )
                            isCardNumberValid = true

                        } else {
                            binding.ll1InvalidCardNumber.visibility = View.INVISIBLE
                            isCardNumberValid = false
                        }

                        if (text.length >= 10) {
                            makeCardNetworkIdentificationCall(
                                requireContext(), text.substring(0, 9), text
                            )
                        } else {
                            if (isDCCFetched){
                                hideViewWithAnimation(binding.ll1InvalidCardNumber,View.INVISIBLE)
                                hideViewWithAnimation(binding.llLoaderDccAndInfo,View.GONE)
                                hideViewWithAnimation(binding.llDccOptions,View.GONE)
                                hideViewWithAnimation(binding.tvInfoDcc,View.INVISIBLE)
                                binding.tvInfoDcc.text = ""
                                isDCCFetched = false
                                isCardNumberValid = false
                            }
                        }
                    }
                }
            }
        })


        // Set InputFilter to limit the length and add a slash after every 2 digits
        binding.editTextCardValidity.filters = arrayOf(InputFilter.LengthFilter(5))

        // Set TextWatcher to add slashes dynamically as the user types
        binding.editTextCardValidity.addTextChangedListener(object : TextWatcher {
            var isFormatting = false
            var userDeletingChars = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                userDeletingChars = count > after
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                if (s.toString().isNullOrBlank()) {
                    isCardValidityValid = true
                    proceedButtonIsEnabled.value = true
                } else {
                    isCardValidityValid = false
                    proceedButtonIsEnabled.value = false
                }

                callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "", "Card")
                enableProceedButton()
            }

            override fun afterTextChanged(s: Editable?) {

                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                if (!isFormatting) {
                    val textNow = s.toString()
                    var text = s.toString().replace("/", "")

                    if (text.isNotEmpty() && (text[0].toString().toInt() != 0 && text[0].toString()
                            .toInt() != 1)
                    ) {
                        binding.invalidCardValidity.visibility = View.INVISIBLE
                        proceedButtonIsEnabled.value = false
                    }

                    if (text.length == 1) {
                        if (text != "0" && text != "1") {
                            text = "0" + text
                        }
                    }

                    val formattedText = formatMMYY(text)

                    if (text.length == 4) {
                        binding.editTextCardCVV.requestFocus()
                    }

                    // Set isFormatting to true to prevent infinite loop
                    if (!userDeletingChars) {
                        isFormatting = true
                        binding.editTextCardValidity.setText(formattedText)
                        binding.editTextCardValidity.setSelection(formattedText.length)
                    } else if (s.toString().length > 1 && s.toString()[s.toString().length - 1] == '/') {
                        s?.delete(s.toString().length - 1, s.toString().length)
                    }

                    // Set isFormatting back to false after modifying the text
                    isFormatting = false
                    if (textNow.isNotBlank()) {
                        if (textNow.length == 5) {
                            val cardValidity = binding.editTextCardValidity.text.toString()
                            if (!(isValidExpirationDate(
                                    cardValidity.substring(0, 2), cardValidity.substring(3, 5)
                                ))
                            ) {
                                isCardValidityValid = false
                                proceedButtonIsEnabled.value = false
                                binding.invalidCardValidity.visibility = View.VISIBLE
                                binding.textView7.text = "Invalid card validity"
                            } else {
                                isCardValidityValid = true
                                proceedButtonIsEnabled.value = true
                                binding.invalidCardValidity.visibility = View.INVISIBLE
                            }
                        } else {
                            isCardValidityValid = false
                            proceedButtonIsEnabled.value = false
                        }
                    }

                    if (textNow.isBlank()) {
                        isCardValidityValid = false
                        proceedButtonIsEnabled.value = false
                    }
                }
            }
        })

        binding.editTextCardCVV.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                if (isAmericanExpressCard.value == true) {
                    binding.editTextCardCVV.filters = arrayOf(InputFilter.LengthFilter(4))
                } else {
                    binding.editTextCardCVV.filters = arrayOf(InputFilter.LengthFilter(3))
                }
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                val textNow = s.toString()
                if (textNow.isNotBlank()) {
                    if (isAmericanExpressCard.value == true) {
                        if (textNow.length == 4) {
                            isCardCVVValid = true
                            proceedButtonIsEnabled.value = true
                            binding.invalidCVV.visibility = View.INVISIBLE
                        } else {
                            isCardCVVValid = false
                            proceedButtonIsEnabled.value = false
                        }
                    } else {
                        if (textNow.length == 3) {
                            isCardCVVValid = true
                            proceedButtonIsEnabled.value = true
                            binding.invalidCVV.visibility = View.INVISIBLE
                        } else {
                            isCardCVVValid = false
                            proceedButtonIsEnabled.value = false
                        }
                    }
                    callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "", "Card")
                } else {
                    isCardCVVValid = false
                    proceedButtonIsEnabled.value = false
                }

                if (isAmericanExpressCard.value == true) {
                    if (textNow.length == 4) {
                        binding.editTextNameOnCard.requestFocus()
                        binding.editTextNameOnCard.requestFocus()
                    }
                } else {
                    if (textNow.length == 3) {
                        binding.editTextNameOnCard.requestFocus()
                        binding.editTextNameOnCard.requestFocus()
                    }
                }
                enableProceedButton()
            }

            override fun afterTextChanged(s: Editable?) {

                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                val textNow = s.toString()
                if (textNow.isBlank()) {
                    isCardCVVValid = false
                    proceedButtonIsEnabled.value = false
                } else {
                    isCardCVVValid = true
                    proceedButtonIsEnabled.value = true
                }
            }
        })

        binding.editTextNameOnCard.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val textNow = s.toString()
                if (textNow.isBlank()) {
                    isNameOnCardValid = false
                    binding.nameOnCardErrorLayout.visibility = View.INVISIBLE
                    proceedButtonIsEnabled.value = false
                }
                callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "", "Card")
            }

            override fun afterTextChanged(s: Editable?) {

                val textNow = s.toString()
                if (textNow.isBlank()) {
                    isNameOnCardValid = false
                    binding.nameOnCardErrorLayout.visibility = View.VISIBLE
                    proceedButtonIsEnabled.value = false
                } else {
                    isNameOnCardValid = true
                    binding.nameOnCardErrorLayout.visibility = View.INVISIBLE
                }
                enableProceedButton()
            }
        })

        binding.editTextCardCVV.setTransformationMethod(AsteriskPasswordTransformationMethod())

        binding.proceedButton.setOnClickListener() {
            callUIAnalytics(requireContext(), "PAYMENT_INITIATED", cardNetworkName, "Card")
            removeErrors()
            cardNumber = deformatCardNumber(binding.editTextCardNumber.text.toString())
            cardExpiryYYYY_MM = addDashInsteadOfSlash(binding.editTextCardValidity.text.toString())
            if (cardExpiryYYYY_MM.isNullOrEmpty()) {
                return@setOnClickListener
            }
            cvv = binding.editTextCardCVV.text.toString()
            cardHolderName = binding.editTextNameOnCard.text.toString()
            var anyFieldEmpty = false

            if (cardNumber.isNullOrEmpty()) {
                binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                binding.textView4.text = "Enter card number"
                anyFieldEmpty = true
            }

            if (cardExpiryYYYY_MM.isNullOrEmpty()) {
                binding.invalidCardValidity.visibility = View.VISIBLE
                binding.textView7.text = "Enter card validity"
                anyFieldEmpty = true
            }

            if (cardHolderName.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Enter name on card", Toast.LENGTH_SHORT).show()
                anyFieldEmpty = true
            }

            if (cvv.isNullOrEmpty()) {
                binding.invalidCVV.visibility = View.VISIBLE
                binding.textView8.text = "Enter CVV"
                anyFieldEmpty = true
            }

            if (anyFieldEmpty) {
                return@setOnClickListener
            }

            postRequest(requireContext())
            showLoadingInButton()
        }


        binding.editTextCardNumber.setOnFocusChangeListener(OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {

            } else {
                val cardNumber = removeSpaces(binding.editTextCardNumber.text.toString())
                if (!(isValidCardNumberByLuhn(cardNumber) && isValidCardNumberLength(cardNumber))) {
                    binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                    if (binding.editTextCardNumber.text.isNullOrEmpty()) {
                        binding.textView4.text = "Enter Card Number"
                    } else {
                        binding.textView4.text = "Invalid card number"
                    }
                } else {
                    binding.ll1InvalidCardNumber.visibility = View.INVISIBLE
                }
//                Toast.makeText(requireContext(), "Lost the focus", Toast.LENGTH_LONG).show()
            }
        })

        binding.editTextCardValidity.setOnFocusChangeListener(OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {

            } else {
                val cardValidity = binding.editTextCardValidity.text.toString()
                try {
                    if (!(isValidExpirationDate(
                            cardValidity.substring(0, 2),
                            cardValidity.substring(3, 5)
                        ))
                    ) {
                        binding.invalidCardValidity.visibility = View.VISIBLE
                        if (binding.editTextCardValidity.text.isNullOrEmpty()) {
                            binding.textView7.text = "Enter Card Validity"
                        } else {
                            binding.textView7.text = "Invalid card Validity"
                        }
                    } else {
                        binding.invalidCardValidity.visibility = View.INVISIBLE
                    }
                } catch (e: Exception) {
                    binding.invalidCardValidity.visibility = View.VISIBLE
                    if (binding.editTextCardValidity.text.isNullOrEmpty()) {
                        binding.textView7.text = "Enter Card Validity"
                    } else {
                        binding.textView7.text = "Invalid card Validity"
                    }
                }
            }
        })
        binding.editTextCardCVV.setOnFocusChangeListener(OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {

            } else {
                try {
                    val cardCVV = binding.editTextCardCVV.text.toString()
                    if (!isValidCVC(cardCVV.toInt())) {
                        binding.invalidCVV.visibility = View.VISIBLE
                        if (binding.editTextCardCVV.text.isNullOrEmpty()) {
                            binding.textView8.text = "Enter CVV"
                        } else {
                            binding.textView8.text = "Invalid CVV"
                        }
                    } else {
                        binding.invalidCVV.visibility = View.INVISIBLE
                    }
                } catch (e: Exception) {
                    binding.invalidCVV.visibility = View.VISIBLE
                    if (binding.editTextCardCVV.text.isNullOrEmpty()) {
                        binding.textView8.text = "Enter CVV"
                    } else {
                        binding.textView8.text = "Invalid CVV"
                    }
                }
            }
        })
        binding.editTextNameOnCard.setOnFocusChangeListener(OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {

            } else {
                if (binding.editTextNameOnCard.text.isNullOrEmpty()) {
                    isNameOnCardValid = false
                    binding.nameOnCardErrorLayout.visibility = View.VISIBLE
                } else {
                    isNameOnCardValid = true
                    binding.nameOnCardErrorLayout.visibility = View.INVISIBLE
                }
            }
        })
        return binding.root
    }

    private fun getCurrencyData(context: Context): List<CurrencyData>? {
        val json: String?
        try {
            json = context.assets.open("currency_data.json").bufferedReader().use { it.readText() }
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        val gson = Gson()
        val type = object : TypeToken<List<CurrencyData>>() {}.type
        return gson.fromJson(json, type)
    }

    // Function to get the flag for a specific currency code
    fun getFlagForCurrencyCode(context: Context, currencyCode: String): String? {
        val currencyDataList = getCurrencyData(context)
        val currency = currencyDataList?.find { it.currencyCode == currencyCode }
        return currency?.flag
    }

    private fun readJsonFromAssets(context: Context, fileName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = inputStream.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    fun hideViewWithAnimation(view: View,type:Int) {
        view.animate()
            .alpha(0f)
            .setDuration(350)
            .withEndAction {
                if (type == View.GONE){
                    view.visibility = View.GONE
                }else if (type == View.INVISIBLE){
                    view.visibility = View.INVISIBLE
                }
            }
            .start()
    }

    fun showViewWithAnimation(view: View) {
        view.animate()
            .alpha(1.0f)
            .setDuration(350)
            .withEndAction {
                view.visibility = View.VISIBLE
            }.start()
    }

    fun formatToINR(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        return format.format(amount)
    }

    @SuppressLint("SetTextI18n")
    private fun callAndSetDCCData(dccResponse: DCCResponse) {
        //check if the user typed some other card before if YES we don't update anything
        if (dccResponse.baseMoney!!.amount != null){
            if (quotationID != dccResponse.dccQuotationId) {
                if (!isDCCFetched) {
                    showViewWithAnimation(binding.llLoaderDccAndInfo)
                    showViewWithAnimation(binding.llLoader)
                }

                Log.e(
                    "DCCRESPONSE",
                    "" + GsonBuilder().setPrettyPrinting().create().toJson(dccResponse)
                )
                binding.countryCode1.text = dccResponse.dccQuotationDetails!!.dccMoney!!.currencyCode
                binding.countryCode2.text = dccResponse.baseMoney!!.currencyCode
                binding.detailsText2.text =
                    "Exchange rate will be determined by the card issuer.\n" + dccResponse.baseMoney!!.currencyCode + " " + formatToINR((dccResponse.baseMoney!!.amount)!!.toDouble())
                quotationID = dccResponse.dccQuotationId


                //two types of card VISA and MASTERCARD
                if (dccResponse.brand.equals("visa", true)) {
                    binding.tvInfoDcc.text = ""

                    showViewWithAnimation(binding.llDccOptions)

                    hideViewWithAnimation(binding.tvInfoDcc,View.GONE)
                    hideViewWithAnimation(binding.llLoader,View.INVISIBLE)
                    binding.detailsText1.text =
                        "1 " + dccResponse.baseMoney!!.currencyCode + " = " + dccResponse.dccQuotationDetails!!.fxRate + " " + dccResponse.dccQuotationDetails!!.dccMoney!!.currencyCode + "\n" + "Includes Margin: " + dccResponse.dccQuotationDetails!!.marginPercent + "%\n" + dccResponse.dccQuotationDetails!!.dccMoney!!.currencyCode + " " + formatToINR((dccResponse.dccQuotationDetails!!.dccMoney!!.amount)!!.toDouble())
                } else {
                    binding.tvInfoDcc.text =
                        "Make sure you understand the costs of currency conversion as they may be different depending on whether you select your home currency or the transaction currency."
                    showViewWithAnimation(binding.llDccOptions)
                    showViewWithAnimation(binding.tvInfoDcc)
                    hideViewWithAnimation(binding.llLoader,View.INVISIBLE)
                    binding.detailsText1.text =
                        "1 " + dccResponse.baseMoney!!.currencyCode + " = " + dccResponse.dccQuotationDetails!!.fxRate + " " + dccResponse.dccQuotationDetails!!.dccMoney!!.currencyCode + "\n" + dccResponse.dccQuotationDetails!!.dccMoney!!.currencyCode + " " + formatToINR((dccResponse.dccQuotationDetails!!.dccMoney!!.amount)!!.toDouble())
                }

                binding.countryFlag1.load(getFlagForCurrencyCode(requireActivity(),dccResponse.dccQuotationDetails!!.dccMoney!!.currencyCode!!)) {
                    decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }
                    scale(Scale.FIT)
                }

                binding.countryFlag2.load(getFlagForCurrencyCode(requireActivity(), dccResponse.baseMoney!!.currencyCode!!)) {
                    decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }
                    scale(Scale.FIT) // Ensures the image fits within the ImageView
                }
                isDCCFetched = true
            }

        }else{
            binding.tvInfoDcc.text = ""
            isDCCFetched = false
            isDCCEnabled = true
        }
    }


    fun isValidExpirationDate(inputExpMonth: String, inputExpYear: String): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        val isValidYearValue = (inputExpYear.toInt() > 0)
        val isValidYearLength = (inputExpYear.length == 2)


        val isMonthValid = (inputExpMonth.toInt() in 1..12)

        val isFutureYear = (("20" + inputExpYear).toInt() >= currentYear)

        val isValidMonthRange =
            ((inputExpMonth.toInt() >= currentMonth) || isFutureYear)

        val isSameYear_FutureOrCurrentMonth =
            ((inputExpYear.toInt() == currentYear) && (inputExpMonth.toInt() >= currentMonth))

        val result = ((isValidMonthRange && isValidYearLength && isValidYearValue) &&
                (isFutureYear || isSameYear_FutureOrCurrentMonth) && isMonthValid)
        if (!result)
            proceedButtonIsEnabled.value = false

        return result
    }

    fun isValidCVC(inputCVC: Int): Boolean {
        val stringInputCVC = inputCVC.toString()
        val result: Boolean = ((stringInputCVC.length >= 3) &&
                (stringInputCVC.length <= 4))

        if (!result)
            proceedButtonIsEnabled.value = false

        return result
    }

    private fun isValidCardNumberByLuhn(stringInputCardNumber: String): Boolean {
        // Define the minimum length for a valid card number
        val minCardLength = 13

        // Check if the card number meets the minimum length requirement
        if (stringInputCardNumber.length < minCardLength) {
            proceedButtonIsEnabled.value = false
            return false
        }

        var sum = 0
        var isSecondDigit = false

        for (i in stringInputCardNumber.length - 1 downTo 0) {
            var d = stringInputCardNumber[i] - '0'

            if (isSecondDigit) {
                d *= 2
            }

            sum += d / 10
            sum += d % 10

            isSecondDigit = !isSecondDigit
        }

        val result: Boolean = (sum % 10 == 0)

        if (!result) {
            proceedButtonIsEnabled.value = false
        }

        return result
    }

    private fun isValidCardNumberLength(inputCardNumber: String): Boolean {
        val result: Boolean =
            ((inputCardNumber.length >= 15) &&
                    (inputCardNumber.length <= 16))
        return result
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()

        viewModel.onChildDismissed()
        super.onDismiss(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }

            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

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

            dialog.setCancelable(!binding.progressBar.isVisible && !binding.loadingLayout.isVisible)

            dialog.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.progressBar.isVisible) {
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

    private fun removeErrors() {
        binding.ll1InvalidCardNumber.visibility = View.INVISIBLE
        binding.invalidCardValidity.visibility = View.INVISIBLE
        binding.invalidCVV.visibility = View.INVISIBLE
    }

    private fun giveErrors() {
        binding.ll1InvalidCardNumber.visibility = View.VISIBLE
        binding.invalidCardValidity.visibility = View.VISIBLE
        binding.invalidCVV.visibility = View.VISIBLE
    }

    private fun formatCardNumber(cardNumber: String): String {
        val formatted = StringBuilder()
        for (i in cardNumber.indices) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ") // Add space after every 4 digits
            }
            formatted.append(cardNumber[i])
        }
        return formatted.toString()
    }


    private fun deformatCardNumber(cardNumber: String): String {
        return cardNumber.replace(" ", "") // Remove all spaces
    }

    private fun formatMMYY(date: String): String {
        val formatted = StringBuilder()
        for (i in date.indices) {
            if (i > 0 && i % 2 == 0 && i < 4) {
                formatted.append("/") // Add slash after every 2 digits, but not after the year
            }
            if (formatted.length >= 5) {
                break // Stop appending if length exceeds 5 characters
            }
            formatted.append(date[i])
        }
        return formatted.toString()
    }


    class AsteriskPasswordTransformationMethod : PasswordTransformationMethod() {
        override fun getTransformation(source: CharSequence, view: View): CharSequence {
            return PasswordCharSequence(source)
        }

        private inner class PasswordCharSequence(private val source: CharSequence) : CharSequence {
            override fun get(index: Int): Char {
                return '*' // This is the important part
            }

            override val length: Int
                get() = source.length // Return default

            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
                return source.subSequence(startIndex, endIndex) // Return default
            }
        }
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.putString("operationId", transactionIdArg)
        editor.apply()
    }

    private fun callUIAnalytics(
        context: Context,
        event: String,
        paymentSubType: String,
        paymentType: String
    ) {
        val baseUrl = sharedPreferences.getString("baseUrl", "null")

        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("callerToken", token)
            put("uiEvent", event)

            // Create eventAttrs JSON object
            val eventAttrs = JSONObject().apply {
                put("paymentType", paymentType)
                put("paymentSubType", paymentSubType)
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
            Response.Listener { /*no response handling */ },
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

    fun postRequest(context: Context) {
        job?.cancel()
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {

            // Create the browserData JSON object
            val browserData = JSONObject().apply {

                val webView = WebView(requireContext())

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
                put("type", "card/plain")

                val cardObject = JSONObject().apply {
                    put("number", cardNumber)
                    put("expiry", cardExpiryYYYY_MM)
                    put("cvc", cvv)
                    put("holderName", cardHolderName)

                    // Replace with the actual shopper VPA value
                }
                put("card", cardObject)
            }

            // Instrument Details
            put("instrumentDetails", instrumentDetailsObject)


            val shopperObject = JSONObject().apply {
                put("email", sharedPreferences.getString("email", null))
                put("firstName", sharedPreferences.getString("firstName", null))

                put("gender", sharedPreferences.getString("gender", null))
                put("lastName", sharedPreferences.getString("lastName", null))
                put("phoneNumber", sharedPreferences.getString("phoneNumber", null))
                put("uniqueReference", sharedPreferences.getString("uniqueReference", null))
                if (sharedPreferences.getString("dateOfBirthChosen", null) != null){
                    put("dateOfBirth", sharedPreferences.getString("dateOfBirthChosen", null))
                }else{
                    put("dateOfBirth", sharedPreferences.getString("dateOfBirth", null))
                }

                if (sharedPreferences.getString("panNumberChosen", null) != null){
                    put("panNumber", sharedPreferences.getString("panNumberChosen", null))
                }else{
                    put("panNumber", sharedPreferences.getString("panNumber", null))
                }

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
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                // Handle response
                hideLoadingInButton()
                try {
                    logJsonObject(response)

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
                        job?.cancel()
                        PaymentFailureScreen(errorMessage = cleanedMessage).show(
                            parentFragmentManager,
                            "FailureScreen"
                        )
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
                            if (isDCCFetched && isQuotationRequired){

                                val sharedPreferences: SharedPreferences =
                                    requireActivity().getSharedPreferences("DCC_PREF", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()

                                // Convert DCCResponse object to JSON string
                                val gson = Gson()
                                val json = gson.toJson(dccResponseUniversal!!)

                                // Save JSON string in SharedPreferences
                                editor.putString("DCC_RESPONSE_KEY", json)
                                editor.putString("CARD_HOLDER_NAME", binding.editTextNameOnCard.text.toString())
                                editor.putString("MERCHANT_NAME_SESSION", sessionData!!.merchantDetails!!.merchantName)
                                editor.putString("MERCHANT_NAME", dccResponseUniversal!!.dccQuotationDetails!!.dspCode)
                                editor.apply()  // Apply changes asynchronously
                            }else if (isDCCFetched && !isQuotationRequired){
                                val sharedPreferences: SharedPreferences =
                                    requireActivity().getSharedPreferences("NON_DCC_PREF", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("CURRENCY_TYPE",dccResponseUniversal!!.baseMoney!!.currencyCode)
                                editor.putString("AMOUNT",
                                    dccResponseUniversal!!.baseMoney!!.amount.toString()
                                )
                                editor.apply()
                            }else{
                                val sharedPreferences: SharedPreferences =
                                    requireActivity().getSharedPreferences("NON_DCC_PREF", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("CURRENCY_TYPE",sessionData!!.paymentDetails!!.money!!.currencyCode)
                                editor.putString("AMOUNT",
                                    sessionData!!.paymentDetails!!.money!!.amount.toString()
                                )
                                editor.apply()
                            }
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            dismissAndMakeButtonsOfMainBottomSheetEnabled()
                        } else {
                            showLoadingState()
                            val intent = Intent(requireContext(), OTPScreenWebView::class.java)
                            intent.putExtra("url", url)
                            intent.putExtra("type", type)
                            startFunctionCalls()
                            startActivity(intent)
                        }

                    }
                    editor.apply()
                } catch (e: JSONException) {

                }

            },
            Response.ErrorListener { error ->
                // Handle error
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired",true) == true) {
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
                        job?.cancel()
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

    fun clearAllDCCData(context: Context) {
        val sharedPreferences = context.getSharedPreferences("DCC_PREF", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Clear all data from the SharedPreferences
        editor.clear()
        editor.apply() // Apply changes asynchronously
    }

    private fun removeSpaces(stringWithSpaces: String): String {
        return stringWithSpaces.replace(" ", "")
    }

    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            // Parse the JSON string
            val jsonObject = JSONObject(response)
            // Retrieve the value associated with the "message" key
            return jsonObject.getString("message")
        } catch (e: Exception) {
            // Handle JSON parsing exception
        }
        return null
    }

    fun dismissCurrentBottomSheet() {
        dismiss()
    }

    fun hideLoadingInButton() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "primaryButtonColor",
                    "#000000"
                )
            )
        )
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.isEnabled = true
    }

    fun showLoadingInButton() {
        binding.textView6.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        val rotateAnimation = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 3000
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE
        binding.proceedButton.isEnabled = false
        rotateAnimation.start()
    }

    private fun enableProceedButton() {
        if (allFieldsAreValid()) {
            binding.proceedButton.isEnabled = true
            binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
            binding.proceedButtonRelativeLayout.setBackgroundColor(
                Color.parseColor(
                    sharedPreferences.getString(
                        "primaryButtonColor",
                        "#000000"
                    )
                )
            )
            binding.textView6.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.white
                )
            )
        }
    }

    private fun disableProceedButton() {
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButton.isEnabled = false
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }

    private fun addDashInsteadOfSlash(date: String): String {
        try {
            val mm = date.substring(0, 2)
            val yyyy = "20" + date.substring(3, 5)
            return yyyy + "-" + mm
        } catch (e: Exception) {
            binding.textView7.text = "Invalid Validity"
            return ""
        }
    }


    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
    }

    fun getMessageForFieldErrorItems(errorString: String) {

        // Parse JSON response
        val jsonObject = JSONObject(errorString)


        // Extract field error items array
        val fieldErrorItems = jsonObject.getJSONArray("fieldErrorItems")

        // Iterate over field error items
        for (i in 0 until fieldErrorItems.length()) {
            // Extract message from each item
            val errorMessage = fieldErrorItems.getJSONObject(i).getString("message")
            if (errorMessage.contains("Invalid instrumentDetails.card.expiry", ignoreCase = true)) {
                binding.invalidCardValidity.visibility = View.VISIBLE
                binding.textView7.text = "Invalid Validity"
            }

            if (errorMessage.contains(
                    "instrumentDetails.card.number is invalid",
                    ignoreCase = true
                )
            ) {
                binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                binding.textView4.text = "Invalid Card Number"
            }

            if (errorMessage.contains(
                    "instrumentDetails.card.number is invalid",
                    ignoreCase = true
                )
            ) {
                binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                binding.textView4.text = "Invalid Card Number"
            }
        }
    }


    fun getStatusReasonFromResponse(response: String) {
        val jsonObject = JSONObject(response)


        // Extract status object
        val statusObject = jsonObject.getJSONObject("status")

        // Extract status reason
        val statusReason = statusObject.getString("reason")


        binding.invalidCardValidity.visibility = View.VISIBLE
        binding.textView7.text = statusReason
        if (statusReason.contains("Invalid Card Expiry", ignoreCase = true)) {
            binding.invalidCardValidity.visibility = View.VISIBLE
            binding.textView7.text = "Invalid Validity"
        } else if (statusReason.contains("Invalid CVV", ignoreCase = true)) {
            binding.invalidCVV.visibility = View.VISIBLE
            binding.textView8.text = "Invalid CVV"
        }
    }

    companion object {
        fun newInstance(
            shippingEnabled: Boolean
        ): AddCardBottomSheet {
            val fragment = AddCardBottomSheet()
            fragment.shippingEnabled = shippingEnabled
            return fragment
        }
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun allFieldsAreValid(): Boolean {
        return (binding.nameOnCardErrorLayout.visibility == View.INVISIBLE || binding.nameOnCardErrorLayout.visibility == View.GONE) && (binding.ll1InvalidCardNumber.visibility == View.INVISIBLE || binding.ll1InvalidCardNumber.visibility == View.GONE) && (binding.invalidCardValidity.visibility == View.INVISIBLE || binding.invalidCardValidity.visibility == View.GONE) && (binding.invalidCVV.visibility == View.INVISIBLE || binding.invalidCVV.visibility == View.GONE) && binding.editTextCardCVV.text.isNotEmpty() && binding.editTextCardValidity.text.isNotEmpty() && binding.editTextNameOnCard.text.isNotEmpty() && binding.editTextCardNumber.text.isNotEmpty() && isValidCardNumberByLuhn(
            binding.editTextCardNumber.text.toString().replace("\\s".toRegex(), "")
        ) && isValidCVC(
            binding.editTextCardCVV.text.toString().toInt()
        ) && binding.editTextCardValidity.text.length == 5 && isValidExpirationDate(
            binding.editTextCardValidity.text.toString().substring(0, 2),
            binding.editTextCardValidity.text.toString().substring(3, 5)
        ) && isNameOnCardValid && (isCurrencySelected || isDCCEnabled)
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
                            removeLoadingState()
                            val callback = SingletonClass.getInstance().getYourObject()
                            val callbackForDismissing =
                                SingletonForDismissMainSheet.getInstance().getYourObject()
                            job?.cancel()
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            if (callback != null) {
                                callback.onPaymentResult(
                                    PaymentResultObject(
                                        "Success",
                                        transactionId,
                                        transactionId
                                    )
                                )
                            }
                            if (callbackForDismissing != null) {
                                callbackForDismissing.dismissFunction()
                            }
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
                            removeLoadingState()
                            job?.cancel()
                            PaymentFailureScreen(
                                errorMessage = "Please retry using other payment method or try again in sometime"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }

                } catch (e: JSONException) {

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

    private fun showLoadingState() {
        binding.boxpayLogoLottie.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.loadingLayout.visibility = View.VISIBLE
        binding.cardDetails.visibility = View.INVISIBLE
        disableProceedButton()
    }

    private fun removeLoadingState() {
        binding.loadingLayout.visibility = View.GONE
        binding.boxpayLogoLottie.cancelAnimation()
        binding.cardDetails.visibility = View.VISIBLE
        enableProceedButton()
    }
}