package com.boxpay.checkout.sdk

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.airbnb.lottie.LottieDrawable
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.FragmentDeliveryAddressBottomSheetBinding
import com.boxpay.checkout.sdk.constants.AnalyticsEvents
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.boxpay.checkout.sdk.utils.callUIAnalytics
import com.boxpay.checkout.sdk.utils.getSessionApiUrl
import com.boxpay.checkout.sdk.utils.getSessionToken
import com.boxpay.checkout.sdk.utils.getShopperToken
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hbb20.CountryCodePicker
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import com.boxpay.checkout.sdk.dataclasses.DeliveryAddressErrorHandlingData
import com.boxpay.checkout.sdk.utils.dpToPx


class DeliveryAddressBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentDeliveryAddressBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var countryCodePhoneNum: String = "+91"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var callback: UpdateMainBottomSheetInterface? = null
    private var firstTime: Boolean = false
    private var token: String? = null
    private var uniqueRef: String? = null
    private var customerShopperToken: String? = null
    private var selectedCountryNameCode = "IN"
    private lateinit var Base_Session_API_URL: String
    private var isShippingEnabled = false
    private var isNameEnabled = false
    private var labelType: String? = null
    private var labelName: String? = null
    private var isPhoneEnabled = false
    private var isEmailEnabled = false
    private var isDOBEnabled = false
    private var isPANEnabled = false
    private var isNameEditable = true
    private var isPhoneEditable = true
    private var isEmailEditable = true
    private var isPANEditable = true
    private var isDOBEditable = true
    private var isDobSelected = false
    private var isPANFilled = false
    private var isHomeAddressSaved: Boolean = false
    private var isOfficeAddressSaved: Boolean = false
    private lateinit var inputMethodManager: InputMethodManager
    private val panRegex = "^[A-Z]{5}[0-9]{4}[A-Z]$".toRegex()

    private var convertedDate: String? = null
    val emailRegex =
        "^(?!.*\\.\\.)(?!.*\\.\\@)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".toRegex()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            FragmentDeliveryAddressBottomSheetBinding.inflate(layoutInflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        token = getSessionToken(requireContext())
        uniqueRef = sharedPreferences.getString("uniqueReference", null)
        customerShopperToken = getShopperToken(requireContext())
        Base_Session_API_URL = getSessionApiUrl(requireContext())
        binding.ccpPhoneNumber.registerCarrierNumberEditText(binding.mobileNumberEditText)

        if (customerShopperToken != null && customerShopperToken != "" && isShippingEnabled) {
            binding.saveAddressLayout.visibility = View.VISIBLE
        } else {
            binding.saveAddressLayout.visibility = View.GONE
        }

        val focusedDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f // Adjust the corner radius
            setStroke(
                4, Color.parseColor(
                    sharedPreferences.getString(
                        "primaryButtonColor",
                        "#000000"
                    )
                )
            ) // Set border thickness and color
        }
        binding.homeSavedAddress.setOnClickListener {
            if (!isHomeAddressSaved) {
                binding.homeSavedAddress.background = focusedDrawable
                binding.officeSavedAddress.background =
                    AppCompatResources.getDrawable(context!!, R.drawable.saved_address_background)
                binding.otherSavedAddress.background =
                    AppCompatResources.getDrawable(context!!, R.drawable.saved_address_background)
                binding.otherSaveAddressTextField.text = null
                binding.otherSaveAddressTextField.visibility = View.GONE
                labelType = "Home"
                isSavedAddressLabelValid()
            } else {
                Toast.makeText(context, "Address already saved with Home", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.ccpPhoneNumber.setDialogEventsListener(object : CountryCodePicker.DialogEventsListener {
            override fun onCcpDialogOpen(dialog: Dialog?) {
                // Set custom height here
                dialog?.window?.apply {
                    setLayout(dpToPx(300), dpToPx(400))
                    setGravity(Gravity.CENTER)
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }

            override fun onCcpDialogDismiss(dialogInterface: DialogInterface?) {
                // no changes required
            }

            override fun onCcpDialogCancel(dialogInterface: DialogInterface?) {
                // no changes required
            }
        })



        binding.officeSavedAddress.setOnClickListener {
            if (!isOfficeAddressSaved) {
                binding.officeSavedAddress.background = focusedDrawable
                binding.homeSavedAddress.background =
                    AppCompatResources.getDrawable(context!!, R.drawable.saved_address_background)
                binding.otherSavedAddress.background =
                    AppCompatResources.getDrawable(context!!, R.drawable.saved_address_background)
                binding.otherSaveAddressTextField.text = null
                binding.otherSaveAddressTextField.visibility = View.GONE
                labelType = "Work"
                isSavedAddressLabelValid()
            } else {
                Toast.makeText(context, "Address already saved with Office", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.otherSavedAddress.setOnClickListener {
            binding.otherSavedAddress.background = focusedDrawable
            binding.homeSavedAddress.background =
                AppCompatResources.getDrawable(context!!, R.drawable.saved_address_background)
            binding.officeSavedAddress.background =
                AppCompatResources.getDrawable(context!!, R.drawable.saved_address_background)
            binding.otherSaveAddressTextField.visibility = View.VISIBLE
            labelType = "Other"
            isSavedAddressLabelValid()
        }

        binding.ccpCountry.setDialogEventsListener(object : CountryCodePicker.DialogEventsListener {
            override fun onCcpDialogOpen(dialog: Dialog?) {
                dialog?.window?.apply {
                    setLayout(dpToPx(300), dpToPx(400))
                    setGravity(Gravity.CENTER)
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }

            override fun onCcpDialogDismiss(dialogInterface: DialogInterface?) {
                // no changes required
            }

            override fun onCcpDialogCancel(dialogInterface: DialogInterface?) {
                // no changes required
            }
        })

        binding.otherSaveAddressTextField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                labelName = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })


        binding.panEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No need to implement
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isPanValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // No need to implement
            }
        })

        binding.dobEditText.setOnClickListener() {
            showDatePickerDialog(binding.dobEditText)
        }

        binding.ccpPhoneNumber.setOnCountryChangeListener {
            countryCodePhoneNum = binding.ccpPhoneNumber.selectedCountryCode
            selectedCountryNameCode = binding.ccpPhoneNumber.selectedCountryNameCode
            binding.ccpCountry.setCountryForNameCode(binding.ccpPhoneNumber.selectedCountryNameCode)
        }

        binding.ccpCountry.setOnCountryChangeListener {
            selectedCountryNameCode = binding.ccpPhoneNumber.selectedCountryNameCode
        }

        val allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890. "
        val filter = InputFilter { source, _, _, _, _, _ ->
            // Filter out characters not present in the allowed characters list
            source.filter { allowedCharacters.contains(it) }
        }
        binding.fullNameEditText.filters = arrayOf(filter)

        val numberAllowedCharacters = "1234567890"
        val numberFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { numberAllowedCharacters.contains(it) }
        }
        binding.mobileNumberEditText.filters = arrayOf(numberFilter)

        binding.backButton.setOnClickListener {
            dismiss()
        }

        binding.backButton.visibility = View.VISIBLE

        if (!firstTime) {
            val address1 = sharedPreferences.getString("address1", "")
            val address2 = sharedPreferences.getString("address2", "")
            val city = sharedPreferences.getString("city", "")
            val state = sharedPreferences.getString("state", "")
            val postalCode = sharedPreferences.getString("postalCode", "")
            val panNumber = sharedPreferences.getString("panNumber", "")
            val dateOfBirth = sharedPreferences.getString("dateOfBirth", "")
            val name = if (sharedPreferences.getString("firstName", "").isNullOrEmpty()) {
                ""
            } else {
                sharedPreferences.getString(
                    "firstName",
                    ""
                ) + " " + sharedPreferences.getString("lastName", "")
            }
            val email = sharedPreferences.getString("email", "")
            val phoneNumber = sharedPreferences.getString("phoneNumber", "")
            val countryName = sharedPreferences.getString("countryName", "India")
            labelName = sharedPreferences.getString("labelName", "")
            labelType = sharedPreferences.getString("labelType", "")

            binding.otherSaveAddressTextField.apply {
                setText(labelName)
                isEnabled = false
                isFocusable = false
                background = ContextCompat.getDrawable(context, R.drawable.edittext_disabled_bg)
                backgroundTintList = null  // 🔴 This line disables unexpected tints
                setTextColor("#7F7F7F".toColorInt()) // Optional: grey text
            }
            val addressTypeBlockedListener = View.OnClickListener {
                Toast.makeText(
                    context,
                    "You cannot change the address type for this entry.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.homeSavedAddress.setOnClickListener(addressTypeBlockedListener)
            binding.officeSavedAddress.setOnClickListener(addressTypeBlockedListener)
            binding.otherSavedAddress.setOnClickListener(addressTypeBlockedListener)

            binding.addressEditText1.setText(address1)
            binding.addressEditText2.setText(address2)
            binding.cityEditText.setText(city)
            binding.stateEditText.setText(state)
            binding.postalCodeEditText.setText(postalCode)
            binding.fullNameEditText.setText(name)
            binding.emailEditText.setText(email)

            binding.ccpPhoneNumber.fullNumber = phoneNumber
            binding.ccpCountry.setCountryForNameCode(countryName)

            if (labelType.equals("other", true)) {
                binding.otherSavedAddress.background = focusedDrawable
                binding.otherSaveAddressTextField.visibility = View.VISIBLE
            } else if (labelType.equals("home", true)) {
                binding.homeSavedAddress.background = focusedDrawable
            } else if (labelType.equals("work", true)) {
                binding.officeSavedAddress.background = focusedDrawable
            }

            if (panNumber!!.isNotEmpty()) {
                binding.panEditText.setText(panNumber)
                isPANFilled = true
            }
            if (isDOBEnabled && dateOfBirth!!.isNotEmpty()) {
                binding.dobEditText.setText(convertToMMDDYYYY(extractDateFromTimestamp(dateOfBirth)))
                isDobSelected = true
            }
        }
        binding.fullNameEditText.isEnabled = isNameEditable
        binding.mobileNumberEditText.isEnabled = isPhoneEditable
        binding.emailEditText.isEnabled = isEmailEditable
        enableProceedButton()

        if (!isNameEnabled && !isShippingEnabled) {
            binding.fullNameLayout.visibility = View.GONE
        }

        if (!isPhoneEnabled && !isShippingEnabled) {
            binding.mobileNumberLayout.visibility = View.GONE
        }

        if (!isEmailEnabled && !isShippingEnabled) {
            binding.emailLayout.visibility = View.GONE
        }


        if (!isPANEnabled) {
            binding.panLayout.visibility = View.GONE
            isPANFilled = true
        }

        if (!isDOBEnabled) {
            binding.dobLayout.visibility = View.GONE
            isDobSelected = true
        }

        if (!isPANEditable) {
            binding.panEditText.isEnabled = false
        }

        if (!isDOBEditable) {
            binding.dobEditText.isEnabled = false
        }


        if (!isShippingEnabled) {
            binding.addressLayout.visibility = View.GONE
        }

        toCheckAllFieldsAreFilled { isAllValid ->
            if (isAllValid) {
                binding.textView.text =
                    if (isShippingEnabled) "Edit Address" else "Edit Personal Details"
            } else {
                binding.textView.text =
                    if (isShippingEnabled) "Add New Address" else "Add Personal Details"            }
        }
        binding.fullNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no changes required
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isFullNameValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.mobileNumberEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no changes required
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isMobileNumberValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no changes required
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isEmailValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.addressEditText1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no changes required
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isPrimaryAddressValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.addressEditText2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no changes required
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.postalCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (countryCodePhoneNum.equals("+91", true)) {
                    binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
                } else {
                    binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_TEXT
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isPostalValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.stateEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no changes required
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isStateValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.cityEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no changes required
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                logAddressUpdatedEvent()
                isCityValid()
            }

            override fun afterTextChanged(s: Editable?) {
                // no changes required
            }
        })
        binding.panTextView.setOnClickListener() {
            showBubbleMessage(
                binding.panTextView,
                "Enter Permanent Account Number issued \nby the Indian Government, Example: AAAPZ1234C"
            )
        }

        binding.dobTextView.setOnClickListener() {
            showBubbleMessage(
                binding.dobTextView,
                "For business transactions, please enter the \norganization's date of incorporation. For \npersonal transactions, enter the individual's \ndate of birth"
            )
        }
        binding.proceedButton.setOnClickListener() {
            toCheckAllFieldsAreFilled { isAllValid ->
                if (isAllValid) {
                    logAddressUpdatedEvent()
                    val fullName = binding.fullNameEditText.text
                    val mobileNumber = binding.ccpPhoneNumber.fullNumberWithPlus
                    val email = binding.emailEditText.text
                    val address1 = binding.addressEditText1.text
                    val address2 = binding.addressEditText2.text
                    val postalCode = binding.postalCodeEditText.text
                    val state = binding.stateEditText.text
                    var PAN: String? = null
                    var DOB: String? = null
                    if (binding.panEditText.text.toString().isNotEmpty()) {
                        PAN = binding.panEditText.text.toString()
                    }
                    if (binding.dobEditText.text.toString().isNotEmpty()) {
                        DOB = if (convertedDate != null) {
                            convertedDate!!
                        } else {
                            convertDateFormat(extractDateFromTimestamp(binding.dobEditText.text.toString()))!!
                        }
                    }
                    val city = binding.cityEditText.text
                    val nameParts = fullName.split(" ")

                    val firstName = if (nameParts.size > 1) {
                        nameParts.dropLast(1).joinToString(" ")
                    } else {
                        nameParts[0]
                    }

                    val lastName = if (nameParts.size > 1) {
                        nameParts.last()
                    } else {
                        ""
                    }
                    editor.putString("address1", address1.toString())
                    editor.putString("address2", address2.toString())
                    editor.putString("city", city.toString())
                    editor.putString("state", state.toString())
                    editor.putString("countryCode", selectedCountryNameCode)
                    editor.putString("postalCode", postalCode.toString())
                    editor.putString("firstName", firstName)
                    editor.putString("lastName", lastName)
                    editor.putString("email", email.toString())
                    editor.putString("phoneNumber", mobileNumber)
                    editor.putString("phoneCode", countryCodePhoneNum)
                    editor.putString("panNumber", PAN)
                    editor.putString("dateOfBirth", DOB)
                    editor.putString("labelType", labelType)
                    editor.putString("labelName", labelName)

                    editor.apply()

                    if (customerShopperToken != null && customerShopperToken != "" && isShippingEnabled && firstTime) {
                        postSavedAddress()
                    } else if (customerShopperToken != null && customerShopperToken != "" && isShippingEnabled && !firstTime) {
                        updateSavedAddress()
                    } else {
                        callback?.updateBottomSheet()
                        dismiss()
                    }
                } else {
                    isPanValid()
                    isEmailValid()
                    isPrimaryAddressValid()
                    isPostalValid()
                    isCityValid()
                    isStateValid()
                    isFullNameValid()
                    isOthersTextFieldValid()
                    isSavedAddressLabelValid()
                    isMobileNumberValid()
                    isDOBValid()
                }
            }
        }

        return binding.root
    }


    private fun enableProceedButton() {
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
            Color.parseColor(
                sharedPreferences.getString(
                    "buttonTextColor",
                    "#ffffff"
                )
            )
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun extractDateFromTimestamp(timestamp: String): String {
        return try {
            // Parse the timestamp into a LocalDateTime object
            val dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)

            // Extract and format only the date part (yyyy-MM-dd)
            dateTime.toLocalDate().toString()
        } catch (e: DateTimeParseException) {
            // If parsing fails, return the original string
            timestamp
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog(editText: EditText) {
        // Get the current date to initialize the DatePickerDialog
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Show the DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            requireActivity(), R.style.CustomDatePickerTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the selected date as mm-dd-yyyy
                val formattedDate = formatDate(selectedYear, selectedMonth, selectedDay)
                editText.setText(formattedDate)
                isDobSelected = true
                isDOBValid()
                convertedDate = convertDateFormat(formattedDate)
                editor.putString("dateOfBirthChosen", convertToISO8601(formattedDate, 0, 0, 0))
                editor.apply()
            }, year, month, day
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showBubbleMessage(anchorView: View, message: String) {
        val inflater = LayoutInflater.from(anchorView.context)
        val bubbleView = inflater.inflate(R.layout.tooltip_bubble, null)

        val tooltipText = bubbleView.findViewById<TextView>(R.id.tooltipText)
        tooltipText.text = message

        val popupWindow = PopupWindow(
            bubbleView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Show the popup below the anchor view (EditText)
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }

    private fun formatDate(year: Int, month: Int, day: Int): String {
        // Calendar months are 0-based, so add 1 to the month
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)

        val formatter = SimpleDateFormat("MM-dd-yyyy", Locale.US)
        return formatter.format(calendar.time)
    }

    fun convertDateFormat(dateString: String): String? {
        return try {
            val inputFormat = SimpleDateFormat("MM-dd-yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            outputFormat.format(date!!)
        } catch (e: Exception) {

            null // Return null in case of a parsing error
        }
    }

    fun convertToMMDDYYYY(dateString: String): String? {
        return try {
            // Parse the input date string (yyyy-MM-dd)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormat.parse(dateString)

            // Convert to the desired output format (MM-dd-yyyy)
            val outputFormat = SimpleDateFormat("MM-dd-yyyy", Locale.US)
            outputFormat.format(date!!)
        } catch (e: Exception) {

            null // Return null if parsing fails
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertToISO8601(date: String, hour: Int, minute: Int, second: Int): String {
        // Input date formatter (MM-dd-yyyy)
        val inputFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")
        val localDate = LocalDate.parse(date, inputFormatter)
        val localDateTime = localDate.atTime(hour, minute, second)

        // ISO 8601 formatter (yyyy-MM-dd'T'HH:mm:ss'Z')
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        // Format the LocalDateTime to ISO 8601 string
        return isoFormatter.format(localDateTime)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

                val window = d.window
                window?.apply {
                    // Apply dim effect
                    setDimAmount(0.5f) // 50% dimming
                    setBackgroundDrawable(
                        Color.argb(128, 0, 0, 0).toDrawable()
                    )
                }

                val displayMetrics = context?.resources?.displayMetrics
                val screenHeight = displayMetrics?.heightPixels ?: 0
                val desiredHeight: Int
                if (isShippingEnabled) {
                    desiredHeight = (screenHeight * 0.6).toInt()
                } else {
                    desiredHeight = (screenHeight * 0.5).toInt()
                }

                val layoutParams = bottomSheet.layoutParams
                if (layoutParams is CoordinatorLayout.LayoutParams) {
                    layoutParams.height = desiredHeight
                    bottomSheet.layoutParams = layoutParams
                }
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                dialog.setCanceledOnTouchOutside(false)

                bottomSheetBehavior?.isDraggable = false
                bottomSheetBehavior?.isHideable = false
                bottomSheetBehavior?.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_SETTLING -> {
                                // The BottomSheet is settling
                                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                            }
                            else -> {
                                // no changes required
                            }
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {

                    }
                })
            }
        }
        return dialog
    }

    companion object {
        fun newInstance(
            callback: UpdateMainBottomSheetInterface?,
            firstTime: Boolean = false,
            isNameEnabled: Boolean,
            isPhoneEnabled: Boolean,
            isEmailEnabled: Boolean,
            isPANEnabled: Boolean,
            isDOBEnabled: Boolean,
            isShippingEnabled: Boolean,
            isNameEditable: Boolean,
            isPhoneEditable: Boolean,
            isEmailEditable: Boolean,
            isPANEditable: Boolean,
            isDOBEditable: Boolean,
        ): DeliveryAddressBottomSheet {
            val fragment = DeliveryAddressBottomSheet()
            fragment.callback = callback
            fragment.firstTime = firstTime
            fragment.isNameEnabled = isNameEnabled
            fragment.isPhoneEnabled = isPhoneEnabled
            fragment.isEmailEnabled = isEmailEnabled
            fragment.isDOBEnabled = isDOBEnabled
            fragment.isPANEnabled = isPANEnabled
            fragment.isShippingEnabled = isShippingEnabled
            fragment.isNameEditable = isNameEditable
            fragment.isPhoneEditable = isPhoneEditable
            fragment.isEmailEditable = isEmailEditable
            fragment.isPANEditable = isPANEditable
            fragment.isDOBEditable = isDOBEditable
            return fragment
        }
    }

    private fun toCheckAllFieldsAreFilled(onResult : (Boolean) -> Unit) {
        val isFullNameFilled = !binding.fullNameEditText.text.isNullOrBlank()
        val isMobileNumberFilled = !binding.mobileNumberEditText.text.isNullOrBlank()
        val isEmailFilled = !binding.emailEditText.text.isNullOrBlank()
        val isAddressFilled = !binding.addressEditText1.text.isNullOrBlank()
        val isPostalCodeFilled = !binding.postalCodeEditText.text.isNullOrBlank()
        val isStateFilled = !binding.stateEditText.text.isNullOrBlank()
        val isCityFilled = !binding.cityEditText.text.isNullOrBlank()
        val isEmailValid = binding.emailEditText.text.matches(emailRegex)
        val isCountryValid = selectedCountryNameCode.isNotBlank()
        val isLabelValid = !labelType.isNullOrBlank() &&
                (!labelType.equals("other", true) || !labelName.isNullOrEmpty())

        val basicChecks = listOf(
            isFullNameFilled,
            isMobileNumberFilled,
            isEmailFilled,
            isEmailValid,
            isPANFilled,
            isDobSelected
        )

        val shippingChecks = listOf(
            isAddressFilled,
            isPostalCodeFilled,
            isStateFilled,
            isCityFilled,
            isCountryValid
        )

        val labelCheck = if (!customerShopperToken.isNullOrEmpty()) listOf(isLabelValid) else emptyList()

        val allLocalChecks = when {
            isShippingEnabled && !customerShopperToken.isNullOrEmpty() -> basicChecks + shippingChecks + labelCheck
            isShippingEnabled -> basicChecks + shippingChecks
            else -> basicChecks
        }

        val areLocalChecksValid = allLocalChecks.all { it }

        if (!areLocalChecksValid) {
            onResult(false)
            return
        }

        toCheckValidityThroughAPI { isValid ->
            onResult(isValid)
        }
    }



    private fun toCheckValidityThroughAPI(callback: (isValid: Boolean) -> Unit) {
        val fieldMetaMap = mapOf(
            "email" to DeliveryAddressErrorHandlingData(binding.emailErrorText, binding.emailEditText, "Invalid Email"),
            "phoneNumber" to DeliveryAddressErrorHandlingData(binding.mobileErrorText, binding.mobileNumberEditText, "Invalid phone number"),
            "deliveryAddress.address1" to DeliveryAddressErrorHandlingData(binding.address1ErrorText, binding.addressEditText1, "Address required"),
            "deliveryAddress.city" to DeliveryAddressErrorHandlingData(binding.cityErrortext, binding.cityEditText, "City required"),
            "deliveryAddress.state" to DeliveryAddressErrorHandlingData(binding.stateErrorText, binding.stateEditText, "State required"),
            "deliveryAddress.postalCode" to DeliveryAddressErrorHandlingData(binding.postalCodeErrorText, binding.postalCodeEditText, "Postal Code required")
        )
        var errorViewInFocus : EditText? = null
        val url = "${Base_Session_API_URL}shoppers/validations"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())

        val requestBody = JSONObject().apply {
            put("phoneNumber", binding.ccpPhoneNumber.fullNumberWithPlus)
            put("email", binding.emailEditText.text)
            put("uniqueReference", uniqueRef)
            val deliveryAddressJsonObject = JSONObject().apply {
                put("address1", binding.addressEditText1.text)
                put("address2", binding.addressEditText2.text)
                put("city", binding.cityEditText.text)
                put("state", binding.stateEditText.text)
                put("postalCode", binding.postalCodeEditText.text)
                put("countryCode", selectedCountryNameCode)
            }
            put("deliveryAddress", deliveryAddressJsonObject)
        }

        val jsonObjectAll = object : JsonObjectRequest(Method.POST, url, requestBody, { response ->
            // ✅ No errors → valid
            callback(true)
        }, { error ->
            val responseData = error.networkResponse?.data
            val responseString = responseData?.let { String(it) }

            if(!responseString.isNullOrEmpty()) {
                responseString.let {
                    try {
                        val json = JSONObject(it)
                        val fieldErrors = json.optJSONArray("fieldErrorItems")

                        if (fieldErrors != null) {
                            for (i in 0 until fieldErrors.length()) {
                                val errorObj = fieldErrors.getJSONObject(i)
                                val message = errorObj.optString("message", "")
                                val fieldName = message.substringBefore(":").trim()
                                val errorMessage = message.substringAfter(":").trim()

                                fieldMetaMap[fieldName]?.let { meta ->
                                    meta.errorTextView.text = if(fieldName.contains("phoneNumber", true)) errorMessage else meta.defaultMessage
                                    meta.errorTextView.visibility = View.VISIBLE
                                    meta.editTextField.background = ContextCompat.getDrawable(requireContext(), R.drawable.error_red_border)
                                    if (errorViewInFocus == null) errorViewInFocus = meta.editTextField
                                }
                            }
                        }
                        errorViewInFocus?.let { errorField ->
                            binding.scrollView.post {
                                binding.scrollView.smoothScrollTo(0, errorField.top)
                                errorField.requestFocus()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callback(false)
            } else {
                callback(true)
            }
        }) {}
        queue.add(jsonObjectAll)
    }

    fun isSavedAddressLabelValid() {
        binding.savedAddressLabelErrorText.isVisible = labelType == null
    }

    fun isEmailValid() {
        val email = binding.emailEditText.text
        if (!email.matches(emailRegex)) {
            binding.emailErrorText.text = if (email.isEmpty()) {
                "Required"
            } else {
                "Invalid Email"
            }
            binding.emailErrorText.visibility = View.VISIBLE
            binding.emailEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        } else  {
            binding.emailErrorText.visibility = View.INVISIBLE
            binding.emailEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    fun isPostalValid() {
        val postalCode = binding.postalCodeEditText.text
        if (!countryCodePhoneNum.equals("+91", true) && postalCode.isEmpty()) {
            binding.postalCodeErrorText.text = if (postalCode.isEmpty()) {
                "Required"
            } else {
                ""
            }
            binding.postalCodeErrorText.visibility = View.VISIBLE
            binding.postalCodeEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        }
        else if (countryCodePhoneNum.equals("+91", true) && postalCode.length != 6) {
            binding.postalCodeErrorText.text = if (postalCode.isEmpty()) {
                "Required"
            } else {
                "Zip/Postal code must be 6 digits"
            }
            binding.postalCodeErrorText.visibility = View.VISIBLE
            binding.postalCodeEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        }
        else if (countryCodePhoneNum.equals("+91", true) && postalCode.length == 6) {
            getPostalCodeDetails()
            binding.postalCodeErrorText.visibility = View.INVISIBLE
            binding.postalCodeEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    fun isPanValid() {
        val panNumber = binding.panEditText.text?.toString() ?: ""
        if (panNumber.isNotEmpty() && panNumber.length == 10 &&  panRegex.matches(panNumber)) {
            binding.panErrorText.visibility = View.INVISIBLE
            binding.panEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
            isPANFilled = true
            editor.putString("panNumberChosen", panNumber.toString())
            editor.apply()
        } else {
            isPANFilled = !isPANFilled
            binding.panErrorText.text = if (panNumber.length == 10) "Invalid PAN Number" else if(panNumber.length != 10) "PAN must be 10 characters" else "Required"
            binding.panErrorText.visibility = View.VISIBLE
            binding.panEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        }
    }

    fun isDOBValid() {
        if(isDobSelected) {
            binding.dobErrorText.visibility = View.INVISIBLE
            binding.dobEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        } else {
            binding.dobErrorText.visibility = View.VISIBLE
            binding.dobEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        }
    }

    fun isPrimaryAddressValid() {
        val primaryAddress = binding.addressEditText1.text
        if (primaryAddress.isEmpty()) {
            binding.address1ErrorText.visibility = View.VISIBLE
            binding.addressEditText1.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        } else {
            binding.address1ErrorText.visibility = View.INVISIBLE
            binding.addressEditText1.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    fun isStateValid() {
        val primaryAddress = binding.stateEditText.text
        if (primaryAddress.isEmpty()) {
            binding.stateErrorText.visibility = View.VISIBLE
            binding.stateEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        } else {
            binding.stateErrorText.visibility = View.INVISIBLE
            binding.stateEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    fun isMobileNumberValid() {
        val mobileNumber = binding.mobileNumberEditText.text
        if (mobileNumber.isEmpty()) {
            binding.mobileErrorText.text = "Required"
            binding.mobileErrorText.visibility = View.VISIBLE
            binding.mobileNumberEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        } else {
            binding.mobileErrorText.visibility = View.INVISIBLE
            binding.mobileNumberEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    fun isFullNameValid() {
        val fullName = binding.fullNameEditText.text
        if (fullName.isEmpty()) {
            binding.fullNameErrorTex.visibility = View.VISIBLE
            binding.fullNameEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        } else {
            binding.fullNameErrorTex.visibility = View.INVISIBLE
            binding.fullNameEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    fun isOthersTextFieldValid() {
        val otherTextField = binding.otherSaveAddressTextField.text
        if (otherTextField.isEmpty() && labelType == "Other") {
            binding.otherSavedAddressErrorText.visibility = View.VISIBLE
            binding.otherSaveAddressTextField.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        } else {
            binding.otherSavedAddressErrorText.visibility = View.INVISIBLE
            binding.otherSaveAddressTextField.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    fun isCityValid() {
        val primaryAddress = binding.cityEditText.text
        if (primaryAddress.isEmpty()) {
            binding.cityErrortext.visibility = View.VISIBLE
            binding.cityEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
        } else {
            binding.cityErrortext.visibility = View.INVISIBLE
            binding.cityEditText.background =
                ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
        }
    }

    private fun getPostalCodeDetails() {
        val postalCodeValue = binding.postalCodeEditText.text.toString()
        val url = "${Base_Session_API_URL}${token}/postal-codes?postalCode=${postalCodeValue}&countryCode=IN"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = object : JsonObjectRequest(Method.GET, url, null, { response ->

            try {
                binding.stateEditText.setText(response.getString("state"))
                binding.cityEditText.setText(response.getString("city"))
                binding.stateEditText.isEnabled = false
            } catch (e: Exception) {
                // no op
            }

        }, Response.ErrorListener { }){}
        queue.add(jsonObjectAll)
    }

    fun dismissCurrentBottomSheet() {
        dismiss()
    }

    private fun postSavedAddress() {
        showLoadingState()
        inputMethodManager.hideSoftInputFromWindow(binding.addressEditText2.windowToken, 0)
        val url = "${Base_Session_API_URL}${token}/shoppers/${uniqueRef}/addresses"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val requestBody = JSONObject().apply {
            val fullName = binding.fullNameEditText.text
            put("name", fullName)
            put("phoneNumber", binding.ccpPhoneNumber.fullNumberWithPlus)
            put("email", binding.emailEditText.text)
            if (binding.dobEditText.text.toString().isNotEmpty() || convertedDate != null) {
                put(
                    "dateOfBirth", if (convertedDate != null) {
                        convertedDate!!
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            convertDateFormat(extractDateFromTimestamp(binding.dobEditText.text.toString()))!!
                        } else {
                            null
                        }
                    }
                )
            }
            put("panNumber", binding.panEditText.text)
            put("address1", binding.addressEditText1.text)
            put("address2", binding.addressEditText2.text)
            put("city", binding.cityEditText.text)
            put("state", binding.stateEditText.text)
            put("countryCode", selectedCountryNameCode)
            put("postalCode", binding.postalCodeEditText.text)
            put(
                "addressRef",
                if (labelType.equals("other", true)) "$labelType-$labelName" else "$labelType"
            )
            put("labelType", labelType)
            put("labelName", labelName)
        }

        val jsonObjectAll =
            object : JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                Response.Listener {
                    hideLoader()
                    callback?.updateBottomSheet()
                    dismiss()
                },
                Response.ErrorListener { error ->
                    if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                        val errorResponse = String(error.networkResponse.data)
                        Toast.makeText(context, errorResponse, Toast.LENGTH_LONG).show()
                        hideLoader()
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Session $customerShopperToken"
                    return headers
                }
            }
        queue.add(jsonObjectAll)
    }

    private fun updateSavedAddress() {
        showLoadingState()
        val label = if (labelType.equals("other", true)) "$labelType-$labelName" else labelType
        inputMethodManager.hideSoftInputFromWindow(binding.addressEditText2.windowToken, 0)
        val url = "${Base_Session_API_URL}${token}/shoppers/${uniqueRef}/addresses/$label"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val requestBody = JSONObject().apply {
            val nameParts = binding.fullNameEditText.text.split(" ")

            val firstName = if (nameParts.size > 1) {
                nameParts.dropLast(1).joinToString(" ")
            } else {
                nameParts[0]
            }

            val lastName = if (nameParts.size > 1) {
                nameParts.last()
            } else {
                ""
            }
            put("firstName", firstName)
            put("lastName", lastName)
            put("phoneNumber", "$countryCodePhoneNum${binding.mobileNumberEditText.text}")
            put("email", binding.emailEditText.text)
            if (binding.dobEditText.text.toString().isNotEmpty() || convertedDate != null) {
                put(
                    "dateOfBirth", if (convertedDate != null) {
                        convertedDate!!
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            convertDateFormat(extractDateFromTimestamp(binding.dobEditText.text.toString()))!!
                        } else {
                            null
                        }
                    }
                )
            }
            put("panNumber", binding.panEditText.text)
            put("address1", binding.addressEditText1.text)
            put("address2", binding.addressEditText2.text)
            put("city", binding.cityEditText.text)
            put("state", binding.stateEditText.text)
            put("countryCode", selectedCountryNameCode)
            put("postalCode", binding.postalCodeEditText.text)
            put(
                "addressRef",
                if (labelType.equals("other", true)) "$labelType-$labelName" else "$labelType"
            )
            put("labelType", labelType)
            put("labelName", labelName)
        }

        val jsonObjectAll =
            object : JsonObjectRequest(
                Method.PUT,
                url,
                requestBody,
                Response.Listener {
                    hideLoader()
                    callback?.updateBottomSheet()
                    dismiss()
                },
                Response.ErrorListener { error ->
                    if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                        val errorResponse = String(error.networkResponse.data)
                        Toast.makeText(context, errorResponse, Toast.LENGTH_LONG).show()
                        hideLoader()
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Session $customerShopperToken"
                    return headers
                }
            }
        queue.add(jsonObjectAll)
    }

    private fun hideLoader() {
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.cardView.visibility = View.VISIBLE
        binding.proceedButton.visibility = View.VISIBLE
    }

    private fun showLoadingState() {
        binding.boxPayLogoLottieAnimation.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.loadingRelativeLayout.visibility = View.VISIBLE
        binding.cardView.visibility = View.GONE
        binding.proceedButton.visibility = View.GONE
    }

    fun setClickOfHomeAndWork(isHomeSaved: Boolean, isWorkSaved: Boolean) {
        this.isHomeAddressSaved = isHomeSaved
        this.isOfficeAddressSaved = isWorkSaved
    }

    private fun logAddressUpdatedEvent() {
        callUIAnalytics(
            context = requireContext(),
            message = "",
            screenName = "DeliveryAddressBottomSheet",
            uiEvent = AnalyticsEvents.ADDRESS_UPDATED
        )
    }
}
