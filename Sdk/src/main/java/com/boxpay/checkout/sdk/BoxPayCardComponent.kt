package com.boxpay.checkout.sdk

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.LottieDrawable
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.FragmentCardComponentAloneBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.util.CommonFunctions
import com.boxpay.checkout.sdk.utils.handleException
import com.simform.customcomponent.SSCustomEdittextOutlinedBorder
import kotlinx.coroutines.Job
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BoxPayCardComponent(
    val token: String?,
    val sandboxEnabled: Boolean?,
    val onPaymentResult: ((PaymentResultObject) -> Unit)
) : Fragment() {

    private lateinit var binding: FragmentCardComponentAloneBinding
    private var BASE_URL = ""
    private var testEnvironment: Boolean = false
    private var sessionTimer: CountDownTimer? = null
    private var selectedColor = ""
    private var selectedTextColor = ""
    private var totalAmount = ""
    private var isAmericanExpressCard: Boolean = false
    private var email: String? = null
    private var firstName: String? = null
    private var lastName: String? = null
    private var cardVisible = false
    private var gender: String? = null
    private var phoneNumber: String? = null
    private var uniqueReference: String? = null
    private var dob: String? = null
    private var panNumber: String? = null
    private var address1: String? = null
    private var address2: String? = null
    private var city: String? = null
    private var state: String? = null
    private var countryCode: String? = null
    private var postalCode: String? = null
    private var countryName: String? = null
    private var context: Context? = null
    private var job: Job? = null
    private var focusedDrawable: GradientDrawable? = null
    private var unfocusedDrawable: GradientDrawable? = null
    private var errorDrawable: GradientDrawable? = null
    private var isCardExpired: Boolean? = null
    private var isCardNumberEnabled: Boolean? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCardComponentAloneBinding.inflate(inflater, container, false)
        var sessionUrl = ""
        sessionUrl = if (sandboxEnabled == true) {
            "sandbox-apis.boxpay.tech"
        } else if (testEnvironment) {
            "test-apis.boxpay.tech"
        } else {
           "apis.boxpay.in"
        }
        this.BASE_URL = "https://${sessionUrl}/v0/checkout/sessions/"
        setupCardNumberFormatting(binding.edtCardNumber, binding.edtExpiry)
        setupCardExpiryFormatting(binding.edtExpiry, binding.edtCVV)
        setUpCardCvvFormatting(binding.edtCVV, binding.edtcardName)
        setUpCardNameFormatting(binding.edtcardName)
        makeSessionDataCall()

        proceedButtonIsEnabled.observe(this) { enableProceedButton ->
            if (enableProceedButton) {
                val isCardValid = isCardValid()
                if (isCardValid) {
                    enableProceedButton()
                }
            } else {
                disableProceedButton()
            }
        }


        return binding.root
    }

    private fun setupCardNumberFormatting(
        customEditText: SSCustomEdittextOutlinedBorder,
        nextCustomEditText: SSCustomEdittextOutlinedBorder
    ) {
        // Use a TextWatcher to react to external text updates
        val editText = customEditText.findViewById<EditText>(R.id.editText)
        val textWatcher = object : TextWatcher {

            var isFormatting = false // Flag to prevent reformatting when deleting spaces
            var userDeletingChars = false
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                userDeletingChars = count > after
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (s.toString().isBlank()) {
                    proceedButtonIsEnabled.value = false
                }
                s.let {
                    if (s?.length == 18 && !isAmericanExpressCard) {
                        val text = s.toString().replace("\\s".toRegex(), "")
                        if (isValidCardNumberByLuhn(removeSpaces(text))) {
                            proceedButtonIsEnabled.value = true
                        } else {
                            proceedButtonIsEnabled.value = false
                        }
                    } else if (s?.length == 17 && isAmericanExpressCard) {
                        val text = s.toString().replace("\\s".toRegex(), "")
                        if (isValidCardNumberByLuhn(removeSpaces(text))) {
                            proceedButtonIsEnabled.value = true
                        } else {
                            proceedButtonIsEnabled.value = false
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isFormatting) {
                    s?.let { editable ->
                        val textNow = editable.toString()
                        val text = textNow.replace("\\s".toRegex(), "")
                        val formattedText = formatCardNumber(text)
                        if (editable.toString() != formattedText && !userDeletingChars) {
                            isFormatting = true // Set flag to prevent reformatting
                            val editText = customEditText.findViewById<EditText>(R.id.editText)
                            editText.setText(formattedText)

                            // Move the cursor to the end of the text
                            proceedButtonIsEnabled.value = false
                            editText.setSelection(formattedText.length)
                        } else if (editable.toString().length > 1 && editable.toString()[editable.toString().length - 1] == ' ') {
                            editable.delete(editable.length - 1, editable.length)
                        }

                        isFormatting = false // Reset the flag

                        if (text.isBlank()) {
                            proceedButtonIsEnabled.value = false
                            // no op
                        } else if (text.length == 16 && !isAmericanExpressCard) {
                            if (isValidCardNumberByLuhn(text)) {
                                proceedButtonIsEnabled.value = true
                                val nextEditText =
                                    nextCustomEditText.findViewById<EditText>(R.id.editText)
                                nextEditText.requestFocus()
                            } else {
                                proceedButtonIsEnabled.value = false
                                binding.textView4.text = "This card number is invalid"
                                editText.background = errorDrawable
                                binding.textView4.visibility = View.VISIBLE
                                editText.setCompoundDrawablesWithIntrinsicBounds(
                                    null, // Start drawable
                                    null, // Top drawable
                                    ContextCompat.getDrawable(
                                        editText.context,
                                        R.drawable.ic_error
                                    ), // End drawable
                                    null // Bottom drawable
                                )
                            }
                        } else if (text.length == 15 && isAmericanExpressCard) {
                            if (isValidCardNumberByLuhn(text)) {
                                proceedButtonIsEnabled.value = true
                                val nextEditText =
                                    nextCustomEditText.findViewById<EditText>(R.id.editText)
                                nextEditText.requestFocus()
                            } else {
                                proceedButtonIsEnabled.value = false
                                binding.textView4.text = "This card number is invalid"
                                editText.background = errorDrawable
                                editText.setCompoundDrawablesWithIntrinsicBounds(
                                    null, // Start drawable
                                    null, // Top drawable
                                    ContextCompat.getDrawable(
                                        editText.context,
                                        R.drawable.ic_error
                                    ), // End drawable
                                    null // Bottom drawable
                                )
                                binding.textView4.visibility = View.VISIBLE
                            }
                        }

                        if (text.length >= 9) {
                            makeCardNetworkIdentificationCall(
                                requireContext(), text.substring(0, 9), text
                            )
                        } else {
                            editText.setCompoundDrawablesWithIntrinsicBounds(
                                null, // Start drawable
                                null, // Top drawable
                                ContextCompat.getDrawable(
                                    editText.context,
                                    R.drawable.default_card_icon
                                ), // End drawable
                                null // Bottom drawable
                            )
                        }
                    }
                }
            }
        }

        editText.addTextChangedListener(textWatcher)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editText.background = focusedDrawable
                binding.textView4.visibility = View.GONE
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    null, // Start drawable
                    null, // Top drawable
                    ContextCompat.getDrawable(
                        editText.context,
                        R.drawable.default_card_icon
                    ), // End drawable
                    null // Bottom drawable
                )
            } else {
                editText.background = unfocusedDrawable
                val cardNumber = removeSpaces(editText.text.toString())
                if (!(isValidCardNumberByLuhn(cardNumber) && isValidCardNumberLength(cardNumber))) {
                    binding.textView4.visibility = View.VISIBLE
                    if (editText.text.isNullOrEmpty()) {
                        editText.background = errorDrawable
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            null, // Start drawable
                            null, // Top drawable
                            ContextCompat.getDrawable(
                                editText.context,
                                R.drawable.ic_error
                            ), // End drawable
                            null // Bottom drawable
                        )
                        binding.textView4.text = "Required"
                    } else {
                        binding.textView4.text = "This card number is invalid"
                        editText.background = errorDrawable
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            null, // Start drawable
                            null, // Top drawable
                            ContextCompat.getDrawable(
                                editText.context,
                                R.drawable.ic_error
                            ), // End drawable
                            null // Bottom drawable
                        )
                    }
                } else {
                    binding.textView4.visibility = View.GONE
                    editText.setCompoundDrawablesWithIntrinsicBounds(
                        null, // Start drawable
                        null, // Top drawable
                        ContextCompat.getDrawable(
                            editText.context,
                            R.drawable.default_card_icon
                        ), // End drawable
                        null // Bottom drawable
                    )
                }
            }
        }
        editText.setCompoundDrawablesWithIntrinsicBounds(
            null, // Start drawable
            null, // Top drawable
            ContextCompat.getDrawable(
                editText.context,
                R.drawable.default_card_icon
            ), // End drawable
            null // Bottom drawable
        )
    }

    private fun isValidCardNumberLength(inputCardNumber: String): Boolean {
        val result: Boolean =
            ((inputCardNumber.length >= 15) &&
                    (inputCardNumber.length <= 16))
        return result
    }

    private fun setupCardExpiryFormatting(
        customEditText: SSCustomEdittextOutlinedBorder,
        nextCustomEditText: SSCustomEdittextOutlinedBorder
    ) {
        val editText = customEditText.findViewById<EditText>(R.id.editText)
        val nextEditText = nextCustomEditText.findViewById<EditText>(R.id.editText)
        val textWatcher = object : TextWatcher {
            var isFormatting = false
            var userDeletingChars = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                userDeletingChars = count > after
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed in this callback for cursor handling
                if (s.toString().isBlank()) {
                    proceedButtonIsEnabled.value = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isFormatting) {
                    s?.let { editable ->
                        val textNow = editable.toString()
                        val cursorPosition =
                            customEditText.findViewById<EditText>(R.id.editText).selectionStart
                        var text = textNow.replace("/", "")

                        // Logic to format MM/YY
                        if (text.isNotEmpty() && text.length >= 2) {
                            if (text[0].toString().toInt() !in 0..1) {
                                text = "0$text"
                            }
                        }

                        val formattedText = formatMMYY(text)

                        // Set isFormatting to true to prevent infinite loop
                        isFormatting = true
                        editText.setText(formattedText)

                        // Calculate new cursor position
                        val newCursorPosition = calculateNewCursorPosition(
                            formattedText,
                            cursorPosition,
                            userDeletingChars
                        )

                        // Restore the cursor position
                        editText.setSelection(newCursorPosition)
                        isFormatting = false
                        if (text.length == 4 && isValidExpirationDate(
                                editText.text.substring(0, 2), editText.text.substring(3, 5)
                            )
                        ) {
                            proceedButtonIsEnabled.value = true
                            nextEditText.requestFocus()
                        } else {
                            proceedButtonIsEnabled.value = false
                        }
                    }
                }
            }
        }

        editText.addTextChangedListener(textWatcher)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editText.background = focusedDrawable
                binding.expiryErrorText.visibility = View.GONE
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    null, // Start drawable
                    null, // Top drawable
                    null, // End drawable
                    null // Bottom drawable
                )
            } else {
                editText.background = unfocusedDrawable
                if (editText.text.isNullOrEmpty()) {
                    binding.expiryErrorText.visibility = View.VISIBLE
                    editText.background = errorDrawable
                    binding.expiryErrorText.text = "Required"
                    editText.setCompoundDrawablesWithIntrinsicBounds(
                        null, // Start drawable
                        null, // Top drawable
                        ContextCompat.getDrawable(
                            editText.context,
                            R.drawable.ic_error
                        ), // End drawable
                        null // Bottom drawable
                    )
                } else if (editText.length() != 5) {
                    binding.expiryErrorText.visibility = View.VISIBLE
                    binding.expiryErrorText.text = "Expiry is invalid"
                    editText.background = errorDrawable
                    editText.setCompoundDrawablesWithIntrinsicBounds(
                        null, // Start drawable
                        null, // Top drawable
                        ContextCompat.getDrawable(
                            editText.context,
                            R.drawable.ic_error
                        ), // End drawable
                        null // Bottom drawable
                    )
                } else if (editText.length() == 5 && !isValidExpirationDate(
                        editText.text.substring(
                            0,
                            2
                        ), editText.text.substring(3, 5)
                    )) {
                    binding.expiryErrorText.visibility = View.VISIBLE
                    binding.expiryErrorText.text = "Expiry is invalid"
                    editText.background = errorDrawable
                    editText.setCompoundDrawablesWithIntrinsicBounds(
                        null, // Start drawable
                        null, // Top drawable
                        ContextCompat.getDrawable(
                            editText.context,
                            R.drawable.ic_error
                        ), // End drawable
                        null // Bottom drawable
                    )
                }  else {
                    binding.expiryErrorText.visibility = View.GONE
                    editText.setCompoundDrawablesWithIntrinsicBounds(
                        null, // Start drawable
                        null, // Top drawable
                        null, // End drawable
                        null // Bottom drawable
                    )
                }
            }
        }
    }

    private fun setUpCardCvvFormatting(
        customEditText: SSCustomEdittextOutlinedBorder,
        nextCustomEditText: SSCustomEdittextOutlinedBorder
    ) {
        val editText = customEditText.findViewById<EditText>(R.id.editText)
        val nextEditText = nextCustomEditText.findViewById<EditText>(R.id.editText)
        val textWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed in this callback for cursor handling
                if (s.toString().isBlank()) {
                    proceedButtonIsEnabled.value = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                s?.let { editable ->
                    val textNow = editable.toString()
                    val text = textNow.replace("/", "")
                    if (text.length == 3 && !isAmericanExpressCard) {
                        proceedButtonIsEnabled.value = true
                        nextEditText.requestFocus()
                    } else if (text.length == 4 && isAmericanExpressCard) {
                        proceedButtonIsEnabled.value = true
                        nextEditText.requestFocus()
                    } else {
                        proceedButtonIsEnabled.value = false
                    }
                }
            }
        }
        editText.addTextChangedListener(textWatcher)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editText.background = focusedDrawable
                binding.cvvErrorText.visibility = View.GONE
            } else {
                editText.background = unfocusedDrawable
                if (editText.text.isNullOrEmpty()) {
                    binding.cvvErrorText.visibility = View.VISIBLE
                    editText.background = errorDrawable
                    binding.cvvErrorText.text = "Required"
                } else if (editText.length() != 3 && !isAmericanExpressCard) {
                    binding.cvvErrorText.visibility = View.VISIBLE
                    binding.cvvErrorText.text = "CVV is invalid"
                    editText.background = errorDrawable
                } else if (editText.length() != 4 && isAmericanExpressCard) {
                    binding.cvvErrorText.visibility = View.VISIBLE
                    binding.cvvErrorText.text = "CVV is invalid"
                    editText.background = errorDrawable
                } else {
                    binding.cvvErrorText.visibility = View.GONE
                }
            }
        }
        editText.setCompoundDrawablesWithIntrinsicBounds(
            null, // Start drawable
            null, // Top drawable
            ContextCompat.getDrawable(
                editText.context,
                R.drawable.ic_question_mark
            ), // End drawable
            null // Bottom drawable
        )

        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2] // End drawable (right side)
                if (drawableEnd != null) {
                    val drawableBounds = drawableEnd.bounds
                    val drawableStart =
                        editText.width - editText.paddingEnd - drawableBounds.width()
                    if (event.x >= drawableStart) {
                        // Handle icon click
                        showCvvInfoDialog(editText.context)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun showCvvInfoDialog(context: Context) {
        val bottomSheet = CvvBottomSheetDialogFragment(
            selectedColorBottomSheet = androidx.compose.ui.graphics.Color(
                Color.parseColor(
                    selectedColor
                )
            ),
            selectedTextColor = androidx.compose.ui.graphics.Color(
                Color.parseColor(
                    selectedTextColor
                )
            )
        )
        parentFragmentManager.beginTransaction()
            .add(bottomSheet, "CVVInfoBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun setUpCardNameFormatting(customEditText: SSCustomEdittextOutlinedBorder) {
        val editText = customEditText.findViewById<EditText>(R.id.editText)

        val textWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed in this callback for cursor handling
            }

            override fun afterTextChanged(s: Editable?) {
                s?.let { editable ->
                    val textNow = editable.toString()
                    if (textNow.isNullOrEmpty()) {
                        proceedButtonIsEnabled.value = false
                    } else {
                        proceedButtonIsEnabled.value = true
                    }
                }
            }
        }
        editText.addTextChangedListener(textWatcher)

        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editText.background = focusedDrawable
                binding.textView5.visibility = View.GONE
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    null, // Start drawable
                    null, // Top drawable
                    null, // End drawable
                    null // Bottom drawable
                )
            } else {
                editText.background = unfocusedDrawable
                if (editText.text.isNullOrEmpty()) {
                    binding.textView5.visibility = View.VISIBLE
                    editText.background = errorDrawable
                    binding.textView5.text = "Required"
                    editText.setCompoundDrawablesWithIntrinsicBounds(
                        null, // Start drawable
                        null, // Top drawable
                        ContextCompat.getDrawable(
                            editText.context,
                            R.drawable.ic_error
                        ), // End drawable
                        null // Bottom drawable
                    )
                }
            }
        }
    }

    private fun isValidCardNumberByLuhn(stringInputCardNumber: String): Boolean {
        val minCardLength = 13
        if (stringInputCardNumber.length < minCardLength) {
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

        return result
    }

    private fun removeSpaces(stringWithSpaces: String): String {
        return stringWithSpaces.replace(" ", "")
    }

    private fun calculateNewCursorPosition(
        formattedText: String,
        previousCursorPosition: Int,
        userDeletingChars: Boolean
    ): Int {
        return when {
            userDeletingChars -> maxOf(previousCursorPosition - 1, 0) // Move left on delete
            else -> minOf(
                previousCursorPosition + 1,
                formattedText.length
            ) // Move right on addition
        }
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

    fun isValidExpirationDate(inputExpMonth: String, inputExpYear: String): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        val isValidYearValue = (inputExpYear.toInt() > 0)
        val isValidYearLength = (inputExpYear.length == 2)


        val isMonthValid = (inputExpMonth.toInt() in 1..12)

        val isFutureYear = (("20$inputExpYear").toInt() >= currentYear)

        val isValidMonthRange =
            ((inputExpMonth.toInt() >= currentMonth) || isFutureYear)

        val isSameYearFutureOrCurrentMonth =
            ((inputExpYear.toInt() == currentYear) && (inputExpMonth.toInt() >= currentMonth))

        val result = ((isValidMonthRange && isValidYearLength && isValidYearValue) &&
                (isFutureYear || isSameYearFutureOrCurrentMonth) && isMonthValid)

        return result
    }

    private fun makeSessionDataCall() {
        showLoadingState()

        val url = "${BASE_URL}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val jsonObjectAll = @RequiresApi(Build.VERSION_CODES.O)
        object : JsonObjectRequest(Method.GET, url, null, { response ->

            try {
                val status = response.getString("status")
                val expireTiming = response.getString("sessionExpiryTimestamp")
                startCountdown(expireTiming)
                if (status.equals(
                        "Approved",
                        ignoreCase = true
                    ) || status.equals("paid", true)
                ) {
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Success",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                }
                if (status.equals(
                        "expired",
                        ignoreCase = true
                    )
                ) {
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Expired",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                }
                val jsonString = readJsonFromAssets(context!!, "countryCodes.json")
                val countryCodeJson = JSONObject(jsonString)
                val merchantDetailsObject = response.getJSONObject("merchantDetails")
                val checkoutThemeObject = merchantDetailsObject.getJSONObject("checkoutTheme")
                selectedColor = checkoutThemeObject.getString("headerColor")
                selectedTextColor = checkoutThemeObject.getString("buttonTextColor")
                val paymentDetailsObject = response.getJSONObject("paymentDetails")
                val shopperObject = paymentDetailsObject.getJSONObject("shopper")
                email = shopperObject.optString("email")
                firstName = shopperObject.optString("firstName")
                lastName = shopperObject.optString("lastName")
                gender = shopperObject.optString("gender")
                phoneNumber = shopperObject.optString("phoneNumber")
                uniqueReference = shopperObject.optString("uniqueReference")
                val deliveryAddress = shopperObject.optJSONObject("deliveryAddress")
                if (deliveryAddress != null) {
                    address1 = deliveryAddress.optString("address1")
                    address2 = deliveryAddress.optString("address2")
                    city = deliveryAddress.optString("city")
                    state = deliveryAddress.optString("state")
                    countryCode = deliveryAddress.optString("countryCode")
                    countryName = getCountryName(countryCodeJson, countryCode ?: "IN")
                    postalCode = deliveryAddress.optString("postalCode")
                }
                panNumber = shopperObject.optString("panNumber")

                dob =
                    if (shopperObject.getString("dateOfBirth") != null && shopperObject.getString("dateOfBirth") != "null") {
                        CommonFunctions.formatToISO8601WithCurrentTime(shopperObject.optString("dateOfBirth"))
                    } else {
                        null
                    }

                val money = paymentDetailsObject.getJSONObject("money").getString("amount")
                val amount = money.toDouble()
                var currencySymbol =
                    paymentDetailsObject.getJSONObject("money").getString("currencySymbol")
                totalAmount =
                    "$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
                val paymentMethodsArray =
                    response.getJSONObject("configs").getJSONArray("paymentMethods")
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    val paymentMethodName = paymentMethod.getString("type")
                    if (paymentMethodName == "Card") {
                        cardVisible = true
                    }
                }

                if (!cardVisible) {
                    binding.cardDetailsLinearLayout.visibility = View.GONE
                    binding.errorlayout.visibility = View.VISIBLE
                }
                focusedDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f // Adjust the corner radius
                    setStroke(
                        4, Color.parseColor(
                            selectedColor
                        )
                    ) // Set border thickness and color
                }
                unfocusedDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f // Adjust the corner radius
                    setStroke(
                        4, Color.parseColor(
                            "#E6E6E6"
                        )
                    ) // Set border thickness and color
                }

                errorDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f
                    setStroke(
                        4, Color.parseColor("#E12121")
                    )
                }

                disableProceedButton()

                removeLoadingState()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Invalid token/selected environment.\nPlease press back button and try again",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, Response.ErrorListener { error ->
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)

            }
        }) {
            // no op
        }
        queue.add(jsonObjectAll)
    }

    private fun showLoadingState() {
        binding.boxpayLogoLottie.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.cardDetailsLinearLayout.visibility = View.GONE
        binding.boxpayLoader.visibility = View.VISIBLE
    }

    private fun removeLoadingState() {
        binding.boxpayLoader.visibility = View.GONE
        if (cardVisible) {
            binding.cardDetailsLinearLayout.visibility = View.VISIBLE
        }
        binding.boxpayLogoLottie.cancelAnimation()
    }

    fun startCountdown(endTime: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        try {
            val endDate = dateFormat.parse(endTime) ?: return
            val currentTime = Date().time
            val timeDifference = endDate.time - currentTime
            if (timeDifference > 0) {
                sessionTimer = object : CountDownTimer(timeDifference, 1000) {

                    override fun onTick(millisUntilFinished: Long) {

                    }

                    override fun onFinish() {
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Expired",
                                    transactionIdFetched = "",
                                    operationIdFetched = ""
                                )
                            )
                        }
                    }
                }
                sessionTimer?.start()
            }
        } catch (_: Exception) {
            // no op
        }
    }

    private fun getCountryName(
        countryCodeJson: JSONObject,
        countryCode: String
    ): String? {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            if (countryCode.equals(key, true)) {
                return countryDetails.getString("fullName")
            }
        }
        return null
    }

    private fun readJsonFromAssets(context: Context, fileName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = inputStream.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    fun setTestEnv(testEnv: Boolean) {
        this.testEnvironment = testEnv
    }

    fun setContext(context: Context) {
        this.context = context
    }

    private fun enableProceedButton() {
        binding.proceedButtonRelativeLayout.isEnabled = true
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                selectedColor
            )
        )
        binding.textView6.setTextColor(
            Color.parseColor(
                selectedTextColor
            )
        )
    }

    private fun disableProceedButton() {
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButton.isEnabled = false
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }

    fun makeCardNetworkIdentificationCall(
        context: Context, cardNumber: String, completeCardNumber: String
    ) {
        val queue = Volley.newRequestQueue(context)
        val url = BASE_URL + "${token}/bank-identification-numbers/${cardNumber}"
        val jsonData = JSONObject()
        val brands = mutableListOf<String>()
        val request = object : JsonObjectRequest(Method.POST, url, jsonData, { response ->
            try {
                val currBrand = response.getJSONObject("paymentMethod").getString("brand")
                brands.add(currBrand)
                val methodEnabled = response.getBoolean("methodEnabled")

                if (!methodEnabled) {
                    isCardNumberEnabled = false
                    binding.textView4.visibility = View.VISIBLE
                    binding.textView4.text = "This card is not supported for the payment"
                    val editText = binding.edtCardNumber.findViewById<EditText>(R.id.editText)
                    editText.background = errorDrawable
                    proceedButtonIsEnabled.value = false
                }

                updateCardNetwork(brands)

            } catch (e: Exception) {
                handleException(
                    context,
                    e.message ?: "",
                    token ?: "",
                    BASE_URL,
                    "BoxPayCardComponent"
                )
            }
        }, Response.ErrorListener { _ ->

        }) {}
        queue.add(request)
    }

    private fun getImageDrawableForItem(item: String): Int {

        return when (item) {
            "VISA" -> R.drawable.ic_boxpay_visa
            "Mastercard" -> R.drawable.ic_boxpay_mastercard
            "Maestro" -> R.drawable.maestro
            "Cirrus" -> R.drawable.cirrus
            "AmericanExpress" -> R.drawable.ic_boxpay_american_express
            "Diners" -> R.drawable.diners
            "Discover" -> R.drawable.ic_boxpay_discover
            "Electron" -> R.drawable.electron
            "JCB" -> R.drawable.jcb
            "RUPAY" -> R.drawable.ic_boxpay_rupay
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

    private fun updateCardNetwork(brands: MutableList<String>) {
        if (brands.size == 1) {
            isAmericanExpressCard = brands[0] == "AmericanExpress"
            val image = getImageDrawableForItem(brands[0])
            val editText = binding.edtCardNumber.findViewById<EditText>(R.id.editText)
            editText.setCompoundDrawablesWithIntrinsicBounds(
                null, // Start drawable
                null, // Top drawable
                ContextCompat.getDrawable(editText.context, image), // End drawable
                null // Bottom drawable
            )
        }
    }

    private fun isCardValid(): Boolean {
        val cardNumber =
            binding.edtCardNumber.findViewById<EditText>(R.id.editText).text?.filter { it.isDigit() }
        val expiry =
            binding.edtExpiry.findViewById<EditText>(R.id.editText).text?.filter { it.isDigit() }
        val cvv = binding.edtCVV.findViewById<EditText>(R.id.editText)
        val cardName = binding.edtcardName.findViewById<EditText>(R.id.editText)

        if (expiry?.length != 4) return false // Expecting 4 digits (MMYY)
        val month = expiry.substring(0, 2).toIntOrNull() ?: return false
        val year = expiry.substring(2, 4).toIntOrNull() ?: return false
        if (month !in 1..12) return false // Invalid month

        val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        isCardExpired =
            (year < currentYear || (year == currentYear && month < currentMonth)) // Expired
        if (isCardExpired == true) return false

        // 1. Validate Card Number (only digits, correct length, Luhn check)
        if (cardNumber?.isEmpty() == true || (cardNumber?.length != 16 && cardNumber?.length != 15)) return false


        // 3. Validate CVV (3 digits for most cards, 4 for Amex)
        val isAmex = cardNumber.length == 15
        if ((isAmex && cvv?.length() != 4) || (!isAmex && cvv?.length() != 3)) return false
        if (isCardNumberEnabled == false) return false
        if (cardName.text.isNullOrEmpty()) return false

        // All validations passed
        return true
    }
}