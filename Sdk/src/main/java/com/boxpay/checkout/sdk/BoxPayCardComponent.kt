package com.boxpay.checkout.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.LottieDrawable
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.FragmentCardComponentAloneBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.formatToISO8601WithCurrentTime
import com.boxpay.checkout.sdk.utils.generateRandomAlphanumericString
import com.boxpay.checkout.sdk.utils.handleException
import com.simform.customcomponent.SSCustomEdittextOutlinedBorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BoxPayCardComponent(
    val token: String?,
    val onPaymentResult: ((PaymentResultObject) -> Unit)
) : Fragment() {

    private lateinit var binding: FragmentCardComponentAloneBinding
    private var BASE_URL = ""
    private var sessionTimer: CountDownTimer? = null
    private var selectedColor = ""
    private var selectedTextColor = ""
    private var showProceedButton = true
    private var isAmericanExpressCard: Boolean = false
    private var email: String? = null
    private var firstName: String? = null
    private var lastName: String? = null
    private var handleCardValidity: ((Boolean) -> Unit)? = null
    private var handleCardVisibility :(() -> Unit)? = null
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
    private var isCardExpired: Boolean? = null
    private var isCardNumberEnabled: Boolean? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCardComponentAloneBinding.inflate(inflater, container, false)
        setupCardNumberFormatting(binding.edtCardNumber, binding.edtExpiry)
        setupCardExpiryFormatting(binding.edtExpiry, binding.edtCVV)
        setUpCardCvvFormatting(binding.edtCVV, binding.edtcardName)
        inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        setUpCardNameFormatting(binding.edtcardName)
        makeSessionDataCall()

        binding.proceedButton.setOnClickListener {
           onClickProceed()
        }

        proceedButtonIsEnabled.observe(this) { enableProceedButton ->
            if (enableProceedButton) {
                if (isCardValid()) {
                    enableProceedButton()
                }
            } else {
                disableProceedButton()
            }
        }

        binding.topView.setOnClickListener {
            hideCardComponent()
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
                        proceedButtonIsEnabled.value = isValidCardNumberByLuhn(removeSpaces(text))
                    } else if (s?.length == 17 && isAmericanExpressCard) {
                        val text = s.toString().replace("\\s".toRegex(), "")
                        proceedButtonIsEnabled.value = isValidCardNumberByLuhn(removeSpaces(text))
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
                                requireContext(), text.substring(0, 9)
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
                handleCardValidity?.let { it(isCardValid()) }
            }
        }

        editText.addTextChangedListener(textWatcher)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (binding.textView4.isVisible) {
                    val text = editText.text.replace("\\s".toRegex(), "")
                    if (editText.length() >= 9) {
                        makeCardNetworkIdentificationCall(
                            requireContext(), text.substring(0, 9)
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
                binding.textView4.visibility = View.INVISIBLE
            } else {
                val cardNumber = removeSpaces(editText.text.toString())
                if (!(isValidCardNumberByLuhn(cardNumber) && isValidCardNumberLength(cardNumber))) {
                    binding.textView4.visibility = View.VISIBLE
                    if (editText.text.isNullOrEmpty()) {
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
                    if (binding.textView4.isVisible) {
                        val text = editText.text.replace("\\s".toRegex(), "")
                        if (editText.length() >= 9) {
                            makeCardNetworkIdentificationCall(
                                requireContext(), text.substring(0, 9)
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
                    binding.textView4.visibility = View.INVISIBLE
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
//        customEditText.background = defaultDrawable
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
                handleCardValidity?.let { it(isCardValid()) }
            }
        }

        editText.addTextChangedListener(textWatcher)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.expiryErrorText.visibility = View.INVISIBLE
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    null, // Start drawable
                    null, // Top drawable
                    null, // End drawable
                    null // Bottom drawable
                )
            } else {
                if (editText.text.isNullOrEmpty()) {
                    binding.expiryErrorText.visibility = View.VISIBLE
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
                    binding.expiryErrorText.visibility = View.INVISIBLE
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
        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        val nextEditText = nextCustomEditText.findViewById<EditText>(R.id.editText)
        val textWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed in this callback for cursor handling
                if (isAmericanExpressCard) {
                    editText.filters = arrayOf(InputFilter.LengthFilter(4))
                } else {
                    editText.filters = arrayOf(InputFilter.LengthFilter(3))
                }
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
                handleCardValidity?.let { it(isCardValid()) }
            }
        }
        editText.addTextChangedListener(textWatcher)
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.cvvErrorText.visibility = View.INVISIBLE
            } else {
                if (editText.text.isNullOrEmpty()) {
                    binding.cvvErrorText.visibility = View.VISIBLE
                    binding.cvvErrorText.text = "Required"
                } else if (editText.length() != 3 && !isAmericanExpressCard) {
                    binding.cvvErrorText.visibility = View.VISIBLE
                    binding.cvvErrorText.text = "CVV is invalid"
                } else if (editText.length() != 4 && isAmericanExpressCard) {
                    binding.cvvErrorText.visibility = View.VISIBLE
                    binding.cvvErrorText.text = "CVV is invalid"
                } else {
                    binding.cvvErrorText.visibility = View.INVISIBLE
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
                    proceedButtonIsEnabled.value = textNow.isNotEmpty()
                }
                handleCardValidity?.let { it(isCardValid()) }
            }
        }
        editText.addTextChangedListener(textWatcher)

        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (editText.text.isNullOrEmpty()) {
                    binding.textView5.visibility = View.VISIBLE
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
            } else {
                binding.textView5.visibility = View.INVISIBLE
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    null, // Start drawable
                    null, // Top drawable
                    null, // End drawable
                    null // Bottom drawable
                )
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

    private fun isValidExpirationDate(inputExpMonth: String, inputExpYear: String): Boolean {
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
                        formatToISO8601WithCurrentTime(shopperObject.optString("dateOfBirth"))
                    } else {
                        null
                    }

                val money = paymentDetailsObject.getJSONObject("money").getString("amountLocaleFull")
                val currencySymbol =
                    paymentDetailsObject.getJSONObject("money").getString("currencySymbol")
                binding.textView6.text = "Pay $currencySymbol$money"
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
                Toast.makeText(
                    context,
                    errorResponse,
                    Toast.LENGTH_LONG
                ).show()
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
            binding.proceedButton.visibility = if (showProceedButton) View.VISIBLE else View.GONE
            binding.cardDetailsLinearLayout.visibility = if (handleCardVisibility != null) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        binding.boxpayLogoLottie.cancelAnimation()
    }

    private fun startCountdown(endTime: String) {
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

    private fun makeCardNetworkIdentificationCall(
        context: Context,
        cardNumber: String
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

    private fun postRequest() {
        val cardExpiryYyyyMm = addDashInsteadOfSlash(binding.edtExpiry.getTextValue)
        showLoadingState()
        handleCardValidity?.let { it(false) }
        val requestQueue = Volley.newRequestQueue(context)
        val requestBody = JSONObject().apply {
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())

                // Get the screen height and width
                val displayMetrics = resources.displayMetrics
                put("screenHeight", displayMetrics.heightPixels.toString())
                put("screenWidth", displayMetrics.widthPixels.toString())
                put("acceptHeader", "application/json")
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", Locale.getDefault().toString())
                put("javaEnabled", true) // Example value
                put("packageId", requireActivity().packageName)
            }
            put("browserData", browserData)

            val instrumentDetailsObject = JSONObject().apply {
                put("type", "card/plain")

                val cardObject = JSONObject().apply {
                    put("number", binding.edtCardNumber.getTextValue)
                    put("expiry", cardExpiryYyyyMm)
                    put("cvc", binding.edtCVV.getTextValue)
                    put("holderName", binding.edtcardName.getTextValue)
                }
                put("card", cardObject)
            }
            put("instrumentDetails", instrumentDetailsObject)

            val shopperObject = JSONObject().apply {
                put("email", email)
                put("firstName", firstName)
                put("gender", gender)
                put("lastName", lastName)
                put("phoneNumber", phoneNumber)
                put("uniqueReference", uniqueReference)
                put("dateOfBirth", dob)
                put("panNumber", panNumber)

                val deliveryAddressObject = JSONObject().apply {

                    put("address1", address1)
                    put("address2", address2)
                    put("city", city)
                    put("countryCode", countryCode)
                    put("postalCode", postalCode)
                    put("state", state)
                    put("city", city)
                    put("countryName", countryName)

                }
                put("deliveryAddress", deliveryAddressObject)
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
            Method.POST, BASE_URL + token, requestBody,
            Response.Listener { response ->

                val status = response.getJSONObject("status").getString("status")
                val transactionId = response.getString("transactionId").toString()

                if (status.contains("Rejected", ignoreCase = true)) {
                    handleCardValidity?.let { it(true) }
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                    removeLoadingState()
                } else {
                    val type =
                        response.getJSONArray("actions").getJSONObject(0).getString("type")
                    if (status.contains("RequiresAction", ignoreCase = true)) {
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = status,
                                    transactionIdFetched = "",
                                    operationIdFetched = ""
                                )
                            )
                        }
                        val url = if (type.contains("html", true)) {
                            response
                                .getJSONArray("actions")
                                .getJSONObject(0)
                                .getString("htmlPageString")
                        } else {
                            response
                                .getJSONArray("actions")
                                .getJSONObject(0)
                                .getString("url")
                        }
                        showLoadingState()
                        val intent = Intent(requireContext(), OTPScreenWebView::class.java)
                        intent.putExtra("url", url)
                        intent.putExtra("type", type)
                        startFunctionCalls()
                        startActivityForResult(intent, 333)
                    } else if (status.contains("Approved", ignoreCase = true)) {
                        handleCardValidity?.let { it(true) }
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Success",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                        removeLoadingState()
                    }
                }
            },
            Response.ErrorListener { error ->
                // Handle error
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    handleCardValidity?.let { it(true) }
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                }
                removeLoadingState()
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

    private fun addDashInsteadOfSlash(date: String): String {
        try {
            val mm = date.substring(0, 2)
            val yyyy = "20" + date.substring(3, 5)
            return "$yyyy-$mm"
        } catch (e: Exception) {
            binding.expiryErrorText.text = "Invalid Validity"
            return ""
        }
    }

    private fun startFunctionCalls() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3000)
                fetchStatusAndReason("${BASE_URL}${token}/status")
                // Delay for 4 seconds
            }
        }
    }

    private fun fetchStatusAndReason(url: String) {
        val requestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                try {
                    val status = response.getString("status")
                    val transactionId = response.getString("transactionId").toString()

                    if (status.equals("Rejected", ignoreCase = true) || status.equals(
                            "failed",
                            true
                        )
                    ) {
                        job?.cancel()
                        handleCardValidity?.let { it(true) }
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Failed",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                        removeLoadingState()
                    } else {
                        if (status.equals("RequiresAction", ignoreCase = true)) {
                            job?.cancel()
                            onPaymentResult?.let {
                                it(
                                    PaymentResultObject(
                                        resultFetched = status,
                                        transactionIdFetched = transactionId,
                                        operationIdFetched = transactionId
                                    )
                                )
                            }
                            removeLoadingState()
                        } else if (status.equals(
                                "Approved",
                                ignoreCase = true
                            ) || status.equals("paid", true)
                        ) {
                            job?.cancel()
                            handleCardValidity?.let { it(true) }
                            onPaymentResult?.let {
                                it(
                                    PaymentResultObject(
                                        resultFetched = "Success",
                                        transactionIdFetched = transactionId,
                                        operationIdFetched = transactionId
                                    )
                                )
                            }
                            removeLoadingState()
                        }
                    }
                } catch (_: JSONException) {

                }
            },
            Response.ErrorListener { error ->
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    handleCardValidity?.let { it(true) }
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                }
                job?.cancel()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Trace-Id"] = generateRandomAlphanumericString(10)
                return headers
            }
        }
        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 333) {
            if (resultCode == Activity.RESULT_OK) {
                removeLoadingState()
                job?.cancel()
                onPaymentResult?.let {
                    it(
                        PaymentResultObject(
                            resultFetched = "Failed",
                            transactionIdFetched = "",
                            operationIdFetched = ""
                        )
                    )
                }
            }
        }
    }

    fun setProceedButtonVisibility(visible: Boolean, handleCardValidityCallback:((Boolean)-> Unit)?) {
        showProceedButton = visible
        handleCardValidity = handleCardValidityCallback
    }

    fun onClickProceed() {
        if (isCardValid()) {
            postRequest()
        } else {
            Toast.makeText(context, "Something is wrong", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionTimer?.cancel()
        job?.cancel()
    }

    fun displayCardComponent(sessionUrl: String, layout: Int) {
        this.BASE_URL = "https://${sessionUrl}/v0/checkout/sessions/"

        val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
        transaction.replace(layout, this)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun hideCardComponent() {
        if (binding.cardDetailsLinearLayout.isVisible) {
            binding.cardDetailsLinearLayout.visibility = View.GONE
        } else {
            binding.cardDetailsLinearLayout.visibility = View.VISIBLE
            handleCardValidity?.let { it(isCardValid()) }
        }
        handleCardVisibility?.invoke()
    }

    fun setVisibilityFunction(handleVisibility: ()-> Unit) {
        this.handleCardVisibility = handleVisibility
    }

    fun onClickUpiComponent() {
        binding.cardDetailsLinearLayout.visibility = View.GONE
        inputMethodManager.hideSoftInputFromWindow(binding.edtCVV.windowToken, 0)
    }
}