package com.boxpay.checkout.sdk

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.FragmentDeliveryAddressBottomSheetBinding
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale


class DeliveryAddressBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentDeliveryAddressBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var countryCodePhoneNum: String = "+91"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var callback: UpdateMainBottomSheetInterface? = null
    private var indexCountryCodePhone: String = ""
    private var firstTime: Boolean = false
    private var token: String? = null
    private var selectedCountryName = "IN"
    private var countrySelected = false
    private var phoneCodeSelected = false
    private lateinit var Base_Session_API_URL: String
    private var isShippingEnabled = false
    private var isNameEnabled = false
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
    private var minPhoneLength = 10
    private var convertedDate : String? = null
    val emailRegex =
        "^(?!.*\\.\\.)(?!.*\\.\\@)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".toRegex()
    val numberRegex = "^[0-9]+$".toRegex()
    private var maxPhoneLength = 10
    private var countrySelectedFromDropDown: String? = null
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
        val jsonString = readJsonFromAssets(requireContext(), "countryCodes.json")
        val countryCodeJson = JSONObject(jsonString)
        val countryCodesArray = loadCountryCodes(countryCodeJson)
        val countryList = loadCountryName(countryCodeJson)
        var phoneLength = getMinMaxLength(countryCodeJson, countryCodePhoneNum)
        minPhoneLength = phoneLength.first
        maxPhoneLength = phoneLength.second
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val baseUrlFetched = sharedPreferences.getString("baseUrl", "null")
        token = sharedPreferences.getString("token", "empty")
        Base_Session_API_URL = "https://${baseUrlFetched}/v0/checkout/sessions/"

        val indexCountryPhone = sharedPreferences.getString("phoneCode", "+91")


        binding.countryEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    binding.countryErrorText.visibility = View.VISIBLE
                } else {
                    binding.countryErrorText.visibility = View.INVISIBLE
                }
                if (isValidCountryName(countryCodeJson)) {
                    if (toCheckAllFieldsAreFilled()) {
                        enableProceedButton()
                    } else {
                        disableProceedButton()
                    }
                } else {
                    disableProceedButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.spinnerDialCodes.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    binding.mobileErrorText.text = "Required"
                    binding.mobileErrorText.visibility = View.VISIBLE
                } else {
                    binding.mobileErrorText.visibility = View.INVISIBLE
                }
                if (inValidPhoneCode(countryCodeJson)) {
                    if (toCheckAllFieldsAreFilled()) {
                        enableProceedButton()
                    } else {
                        disableProceedButton()
                    }
                } else {
                    disableProceedButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.panEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No need to implement
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.isNotEmpty()){
                        if (it.length == 10) {
                            if (isValidPAN(it.toString())) {
                                binding.panErrorText.text = ""
                                binding.panErrorText.visibility = View.INVISIBLE
                                isPANFilled = true
                                editor.putString("panNumberChosen", it.toString())
                                editor.apply()
                                if (toCheckAllFieldsAreFilled()) {
                                    enableProceedButton()
                                } else {
                                    disableProceedButton()
                                }
                            } else {
                                binding.panErrorText.text = "Invalid PAN Number"
                                binding.panErrorText.visibility = View.VISIBLE
                                disableProceedButton()
                            }
                        } else {
                            binding.panErrorText.text = "PAN must be 10 characters"
                            binding.panErrorText.visibility = View.VISIBLE
                            disableProceedButton()
                        }
                    }else{
                        binding.panErrorText.text = "Required"
                        binding.panErrorText.visibility = View.VISIBLE
                        disableProceedButton()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // No need to implement
            }
        })

        binding.dobEditText.setOnClickListener(){
            showDatePickerDialog(binding.dobEditText)
        }

        binding.countryEditText.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()

            countrySelectedFromDropDown = selectedItem
            countrySelected = true
            countryCodePhoneNum = setPhoneCodeUsingCountryName(
                countryCodeJson,
                countrySelectedFromDropDown.toString()
            )
            selectedCountryName =
                findCountryCodeByIsdCode(countryCodeJson, selectedItem) ?: "IN"
            binding.spinnerDialCodes.setText(countryCodePhoneNum)
            phoneLength = getMinMaxLength(countryCodeJson, countryCodePhoneNum)
            minPhoneLength = phoneLength.first
            maxPhoneLength = phoneLength.second
            if (binding.mobileNumberEditText.text.isNotEmpty()) {
                if(isMobileNumberValid()) {
                    enableProceedButton()
                }
            }
            if (countryCodePhoneNum.equals("+91", true)) {
                binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
            } else {
                binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_TEXT
            }

            if (binding.postalCodeEditText.text.isNotEmpty()) {
                isPostalValid()
            }
        }

        binding.spinnerDialCodes.setOnItemClickListener { parent, view, position, id ->
            val selectedDialCode = parent.getItemAtPosition(position).toString()
            // Display or use the selected item
            if (!selectedDialCode.contains("no",true)) {
                countryCodePhoneNum = selectedDialCode
                indexCountryCodePhone = selectedDialCode
                phoneCodeSelected = true
                countrySelectedFromDropDown =
                    setCountryNameUsingPhoneCode(countryCodeJson, countryCodePhoneNum)
                binding.countryEditText.setText(countrySelectedFromDropDown)
                binding.stateEditText.isEnabled = true
                phoneLength = getMinMaxLength(countryCodeJson, selectedDialCode)
                minPhoneLength = phoneLength.first
                maxPhoneLength = phoneLength.second
                if (countryCodePhoneNum.equals("+91", true)) {
                    binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
                } else {
                    binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_TEXT
                }
                if (binding.postalCodeEditText.text.isNotEmpty()) {
                    isPostalValid()
                }
                if (isMobileNumberValid()) {
                    enableProceedButton()
                }
            }
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

        binding.backButton.setOnClickListener() {
            if (binding.proceedButton.isEnabled) {
                editor.putString("phoneNumber", "$countryCodePhoneNum${binding.mobileNumberEditText.text}")
                editor.apply()
            }
            dismiss()
        }

        binding.backButton.visibility = View.VISIBLE

        binding.spinnerDialCodes.setText(indexCountryPhone)
        phoneLength = getMinMaxLength(countryCodeJson, indexCountryPhone ?: countryCodePhoneNum)
        minPhoneLength = phoneLength.first
        maxPhoneLength = phoneLength.second

        if (address1 != null) {
            binding.addressEditText1.setText(address1)
        }
        if (address2 != null) {
            binding.addressEditText2.setText(address2)
        }

        if (city != null) {
            binding.cityEditText.setText(city)
        }

        if (state != null) {
            binding.stateEditText.setText(state)
        }

        if (postalCode != null) {
            binding.postalCodeEditText.setText(postalCode)
        }

        if (name != null) {
            binding.fullNameEditText.setText(name)
        }
        if (panNumber!!.isNotEmpty()) {
            binding.panEditText.setText(panNumber)
            isPANFilled = true
        }else{
            binding.panErrorText.text = "Required"
            binding.panErrorText.visibility = View.VISIBLE
            disableProceedButton()
        }
        if (isDOBEnabled && dateOfBirth!!.isNotEmpty()) {
            binding.dobEditText.setText(convertToMMDDYYYY(extractDateFromTimestamp(dateOfBirth)))
            isDobSelected = true
        }else{
            binding.dobErrorText.text = "Required"
            binding.dobErrorText.visibility = View.VISIBLE
            disableProceedButton()
        }
        if (email != null) {
            binding.emailEditText.setText(email)
        }
        if (phoneNumber != null) {
            binding.mobileNumberEditText.setText(phoneNumber)
        }
        binding.fullNameEditText.isEnabled = isNameEditable
        binding.mobileNumberEditText.isEnabled = isPhoneEditable
        binding.emailEditText.isEnabled = isEmailEditable
        binding.countryEditText.setText(countryName)
        countrySelectedFromDropDown = countryName
        countryCodePhoneNum = indexCountryPhone ?: "+91"
        countrySelected = true
        disableProceedButton()

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

        binding.spinnerDialCodes.apply {
            // Set up the adapter
            val adapter = CustomArrayAdapter(
                requireContext(),
                countryCodesArray,
                true
            )
            setAdapter(adapter)

            binding.spinnerDialCodes.threshold = 2

            // Control dropdown behavior on focus changes
            onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                } else {
                    dismissDropDown()
                    val selectedDialCode = binding.spinnerDialCodes.text.toString()
                    if (inValidPhoneCode(countryCodeJson)) {
                        if (countryCodePhoneNum != selectedDialCode && binding.mobileNumberEditText.text.isNotEmpty() && selectedDialCode != indexCountryPhone) {
                            binding.mobileNumberEditText.setText("")
                        }
                        countryCodePhoneNum = selectedDialCode
                        indexCountryCodePhone = selectedDialCode
                        phoneCodeSelected = true
                        countrySelectedFromDropDown =
                            setCountryNameUsingPhoneCode(countryCodeJson, countryCodePhoneNum)
                        binding.countryEditText.setText(countrySelectedFromDropDown)
                        binding.stateEditText.isEnabled = true
                        phoneLength = getMinMaxLength(countryCodeJson, selectedDialCode)
                        minPhoneLength = phoneLength.first
                        maxPhoneLength = phoneLength.second
                        if (countryCodePhoneNum.equals("+91", true)) {
                            binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
                        } else {
                            binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_TEXT
                        }
                        if (binding.postalCodeEditText.text.isNotEmpty()) {
                            isPostalValid()
                        }
                        binding.spinnerDialCodes.dismissDropDown()
                        toCheckAllFieldsAreFilled()
                    } else {
                        binding.spinnerDialCodes.setText(countryCodePhoneNum)
                        binding.spinnerDialCodes.dismissDropDown()
                    }
                    if (isMobileNumberValid()) {
                        enableProceedButton()
                    }
                }
            }
        }

        binding.countryEditText.apply {
            // Set up the adapter
            val countryNameListAdapter =
                CustomArrayAdapter(requireContext(), countryList, false)
            setAdapter(countryNameListAdapter)

            binding.countryEditText.threshold = 1

            // Control dropdown behavior on focus changes
            onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    showDropDown()
                } else {
                    dismissDropDown()
                    val countryAvailable =
                        countryCodesArray.find {
                            it.equals(
                                binding.countryEditText.text.toString(),
                                true
                            )
                        }
                    if (countryAvailable != null) {
                        val selectedItem = binding.countryEditText.text.toString()

                        countrySelectedFromDropDown = selectedItem
                        countrySelected = true
                        countryCodePhoneNum = setPhoneCodeUsingCountryName(
                            countryCodeJson,
                            countrySelectedFromDropDown.toString()
                        )
                        selectedCountryName =
                            findCountryCodeByIsdCode(countryCodeJson, selectedItem) ?: "IN"
                        binding.spinnerDialCodes.setText(countryCodePhoneNum)
                        phoneLength = getMinMaxLength(countryCodeJson, countryCodePhoneNum)
                        minPhoneLength = phoneLength.first
                        maxPhoneLength = phoneLength.second
                        if (binding.mobileNumberEditText.text.isNotEmpty()) {
                            if(isMobileNumberValid()) {
                                enableProceedButton()
                            }
                        }
                        if (countryCodePhoneNum.equals("+91", true)) {
                            binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
                        } else {
                            binding.postalCodeEditText.inputType = InputType.TYPE_CLASS_TEXT
                        }

                        if (binding.postalCodeEditText.text.isNotEmpty()) {
                            isPostalValid()
                        }
                        toCheckAllFieldsAreFilled()
                    } else {
                        binding.countryEditText.setText(countrySelectedFromDropDown)
                        binding.stateEditText.isEnabled = true
                        binding.countryEditText.dismissDropDown()
                    }

                }
            }
        }

        binding.fullNameEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    binding.fullNameErrorTex.visibility = View.VISIBLE
                    binding.fullNameEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
                } else {
                    binding.fullNameErrorTex.visibility = View.INVISIBLE
                    binding.fullNameEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
                }
                if (toCheckAllFieldsAreFilled()) {
                    enableProceedButton()
                } else {
                    disableProceedButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })


        binding.mobileNumberEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    isMobileNumberValid()
                    binding.mobileNumberEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
                } else {
                    binding.mobileNumberEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
                    if (isMobileNumberValid()) {
                        if (toCheckAllFieldsAreFilled()) {
                            enableProceedButton()
                        }
                    }
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }
        })



        binding.emailEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    isEmailValid()
                    binding.emailEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
                } else {
                    binding.emailEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
                    if (isEmailValid()) {
                        if (toCheckAllFieldsAreFilled()) {
                            enableProceedButton()
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.addressEditText1.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    isPrimaryAddressValid()
                    binding.addressEditText1.background =
                        ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
                } else {
                    binding.addressEditText1.background =
                        ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
                    if (isPrimaryAddressValid()) {
                        if (toCheckAllFieldsAreFilled()) {
                            enableProceedButton()
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.addressEditText2.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                toCheckAllFieldsAreFilled()
            }

            override fun afterTextChanged(s: Editable?) {

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
                if (s?.isEmpty() == true) {
                    isPostalValid()
                    binding.postalCodeEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
                } else {
                    binding.postalCodeEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
                    if (isPostalValid()) {
                        if (toCheckAllFieldsAreFilled()) {
                            enableProceedButton()
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.stateEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    isStateValid()
                    binding.stateEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
                } else {
                    binding.stateEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
                    if (isStateValid()) {
                        if (toCheckAllFieldsAreFilled()) {
                            enableProceedButton()
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.cityEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isEmpty() == true) {
                    isCityValid()
                    binding.cityEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.error_red_border)
                } else {
                    binding.cityEditText.background =
                        ContextCompat.getDrawable(context!!, R.drawable.edittext_bg)
                    if (isCityValid()) {
                        if (toCheckAllFieldsAreFilled()) {
                            enableProceedButton()
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })



        binding.proceedButton.setOnClickListener() {
            val fullName = binding.fullNameEditText.text
            val mobileNumber = binding.mobileNumberEditText.text
            val email = binding.emailEditText.text
            val address1 = binding.addressEditText1.text
            val address2 = binding.addressEditText2.text
            val country = countrySelectedFromDropDown
            val postalCode = binding.postalCodeEditText.text
            val state = binding.stateEditText.text
            var PAN = ""
            var DOB = ""
            if (binding.panEditText.text.toString().isNotEmpty()){
               PAN = binding.panEditText.text.toString()
            }
            if (binding.dobEditText.text.toString().isNotEmpty()){
                if(convertedDate != null){
                    DOB = convertedDate!!
                }else{
                    DOB = convertDateFormat(extractDateFromTimestamp(binding.dobEditText.text.toString()))!!
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
            editor.putString("countryCode", selectedCountryName)
            editor.putString("postalCode", postalCode.toString())
            editor.putString("firstName", firstName)
            editor.putString("lastName", lastName)
            editor.putString("email", email.toString())
            editor.putString("phoneNumber", "$countryCodePhoneNum$mobileNumber")
            editor.putString("phoneCode", countryCodePhoneNum)
            editor.putString("countryName", country.toString())
            editor.putString("indexCountryCodePhone", indexCountryCodePhone)
            editor.putString("panNumber", PAN)
            editor.putString("dateOfBirth", DOB)


            editor.apply()

            callback?.updateBottomSheet()
            dismiss()
        }

        if (toCheckAllFieldsAreFilled()) {
            binding.textView.text =
                if (isShippingEnabled) "Edit Address" else "Edit Personal Details"
            enableProceedButton()
        } else {
            binding.textView.text =
                if (isShippingEnabled) "Add New Address" else "Add Personal Details"
            disableProceedButton()
        }

        return binding.root
    }


    private fun enableProceedButton() {
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "primaryButtonColor",
                    "#000000"
                )
            )
        )
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
    }

    fun isValidPAN(pan: String): Boolean {
        val panRegex = "^[A-Z]{5}[0-9]{4}[A-Z]$".toRegex()
        return panRegex.matches(pan)
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
        val datePickerDialog = DatePickerDialog(requireActivity(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the selected date as mm-dd-yyyy
                val formattedDate = formatDate(selectedYear, selectedMonth, selectedDay)
                editText.setText(formattedDate)
                isDobSelected = true
                if (toCheckAllFieldsAreFilled()) {
                    enableProceedButton()
                } else {
                    disableProceedButton()
                }
                binding.dobErrorText.visibility = View.INVISIBLE
                convertedDate = convertDateFormat(formattedDate)
                val (hour, minute, seconds) = getCurrentTime()
                editor.putString("dateOfBirthChosen", convertToISO8601(formattedDate,hour, minute, seconds))
                editor.apply()
            }, year, month, day)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
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
            e.printStackTrace()
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
            e.printStackTrace()
            null // Return null if parsing fails
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTime(): Triple<Int, Int, Int> {
        val now = LocalDateTime.now()
        val hour = now.hour
        val minute = now.minute
        val second = now.second

        return Triple(hour, minute, second) // Return as a triple of hour, minute, second
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun convertToISO8601(date: String, hour: Int, minute: Int, second: Int): String {
        // Input date formatter (MM-dd-yyyy)
        val inputFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")
        val localDate = LocalDate.parse(date, inputFormatter)

        // Create a LocalDateTime with the provided time
        val localDateTime = localDate.atTime(hour, minute, second)

        // ISO 8601 formatter (yyyy-MM-dd'T'HH:mm:ss'Z')
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

        // Format the LocalDateTime to ISO 8601 string
        return isoFormatter.format(localDateTime)
    }


    private fun disableProceedButton() {
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButton.isEnabled = false
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
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

                val displayMetrics = context?.resources?.displayMetrics
                val screenHeight = displayMetrics?.heightPixels ?: 0
                var desiredHeight: Int
                if (isShippingEnabled) {
                    desiredHeight = (screenHeight * 0.6).toInt()
                } else {
                    desiredHeight = (screenHeight * 0.5).toInt()
                }
                // 50% of screen height

                val layoutParams = bottomSheet.layoutParams
                if (layoutParams is CoordinatorLayout.LayoutParams) {
                    layoutParams.height = desiredHeight
                    bottomSheet.layoutParams = layoutParams
                }


                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                bottomSheetBehavior?.isDraggable = false
                bottomSheetBehavior?.isHideable = false
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
                                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                            }

                            BottomSheetBehavior.STATE_HIDDEN -> {
                                //Hidden


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
            callback: UpdateMainBottomSheetInterface,
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

    fun toCheckAllFieldsAreFilled(): Boolean {
        if (isShippingEnabled) {
            return !binding.fullNameEditText.text.isNullOrBlank() &&
                    !binding.mobileNumberEditText.text.isNullOrBlank() &&
                    !binding.emailEditText.text.isNullOrBlank() &&
                    !binding.addressEditText1.text.isNullOrBlank() &&
                    countrySelected &&
                    !binding.postalCodeEditText.text.isNullOrBlank() && !binding.stateEditText.text.isNullOrBlank() &&
                    !binding.cityEditText.text.isNullOrBlank() &&
                    binding.mobileNumberEditText.text.length in minPhoneLength..maxPhoneLength &&
                    binding.emailEditText.text.matches(emailRegex) &&
                    binding.countryEditText.text.isNotEmpty() &&
                    binding.spinnerDialCodes.text.isNotEmpty() &&
                    !binding.spinnerDialCodes.text.toString().equals("+", true) && isPANFilled && isDobSelected
        } else {
            return !binding.fullNameEditText.text.isNullOrBlank() &&
                    !binding.mobileNumberEditText.text.isNullOrBlank() &&
                    !binding.emailEditText.text.isNullOrBlank() &&
                    binding.mobileNumberEditText.text.length in minPhoneLength..maxPhoneLength &&
                    binding.emailEditText.text.matches(emailRegex) &&
                    binding.spinnerDialCodes.text.isNotEmpty() &&
                    !binding.spinnerDialCodes.text.toString().equals("+", true)  && isPANFilled && isDobSelected
        }
    }

    fun readJsonFromAssets(context: Context, fileName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = inputStream.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    fun loadCountryCodes(countryCodeJson: JSONObject): Array<String> {
        val isdCodes = mutableSetOf<String>()

        // Iterate through each country and extract the isdCode
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val isdCode = countryDetails.getString("isdCode")
            isdCodes.add(isdCode)
        }

        // Sort ISD codes in ascending order
        val sorted = isdCodes.sorted()

        return sorted.toTypedArray()
    }

    fun loadCountryName(countryCodeJson: JSONObject): Array<String> {
        val isdCodes = mutableSetOf<String>()

        // Iterate through each country and extract the isdCode
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val isdCode = countryDetails.getString("fullName")
            isdCodes.add(isdCode)
        }

        // Sort ISD codes in ascending order
        val sorted = isdCodes.sorted()

        return sorted.toTypedArray()
    }


    fun findCountryCodeByIsdCode(countryCodeJson: JSONObject, isdCode: String): String? {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val code = countryDetails.getString("fullName")
            if (code == isdCode) {
                return key
            }
        }

        // Return null if no matching ISD code is found
        return null
    }


    fun setPhoneCodeUsingCountryName(countryCodeJson: JSONObject, countryName: String): String {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val code = countryDetails.getString("fullName")
            if (code.equals(countryName)) {
                return countryDetails.getString("isdCode")
            }
        }
        return ""
    }

    fun setCountryNameUsingPhoneCode(countryCodeJson: JSONObject, isdCode: String): String {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val code = countryDetails.getString("isdCode")
            if (code.equals(isdCode)) {
                return countryDetails.getString("fullName")
            }
        }
        return ""
    }

    fun getMinMaxLength(countryCodeJson: JSONObject, isdCode: String): Pair<Int, Int> {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val isdCodes = countryDetails.getString("isdCode")
            if (isdCodes.equals(isdCode)) {
                val phoneLengthArray = countryDetails.getJSONArray("phoneNumberLength")
                return Pair(
                    phoneLengthArray.getInt(0),
                    phoneLengthArray.getInt(phoneLengthArray.length() - 1)
                )
            }
        }

        // Return null if no matching ISD code is found
        return Pair(10, 10)
    }

    fun isMobileNumberValid(): Boolean {
        val mobileNumber = binding.mobileNumberEditText.text
        if (mobileNumber.length !in minPhoneLength..maxPhoneLength || !mobileNumber.matches(
                numberRegex
            )
        ) {
            disableProceedButton()
            binding.mobileErrorText.text = if (mobileNumber.isEmpty()) {
                "Required"
            } else {
                "Mobile number must be $maxPhoneLength digits"
            }
            binding.mobileErrorText.visibility = View.VISIBLE
            return false
        }
        binding.mobileErrorText.visibility = View.INVISIBLE
        return true
    }

    fun isEmailValid(): Boolean {
        val email = binding.emailEditText.text
        if (!email.matches(emailRegex)) {
            binding.emailErrorText.text = if (email.isEmpty()) {
                "Required"
            } else {
                "Invalid Email"
            }
            binding.emailErrorText.visibility = View.VISIBLE
            disableProceedButton()
            return false
        }
        binding.emailErrorText.visibility = View.INVISIBLE
        return true
    }

    fun isPostalValid(): Boolean {
        val postalCode = binding.postalCodeEditText.text
        if (!countryCodePhoneNum.equals("+91", true) && postalCode.isEmpty()) {
            binding.postalCodeErrorText.text = if (postalCode.isEmpty()) {
                "Required"
            } else {
                ""
            }
            binding.postalCodeErrorText.visibility = View.VISIBLE
            disableProceedButton()
            return false
        }
        if (countryCodePhoneNum.equals("+91", true) && postalCode.length != 6) {
            binding.postalCodeErrorText.text = if (postalCode.isEmpty()) {
                "Required"
            } else {
                "Zip/Postal code must be 6 digits"
            }
            binding.postalCodeErrorText.visibility = View.VISIBLE
            disableProceedButton()
            return false
        }
        if (countryCodePhoneNum.equals("+91",true) && postalCode.length == 6) {
            getPostalCodeDetails()
        }
        binding.postalCodeErrorText.visibility = View.INVISIBLE
        return true
    }

    fun isPrimaryAddressValid(): Boolean {
        val primaryAddress = binding.addressEditText1.text
        if (primaryAddress.isEmpty()) {
            disableProceedButton()
            binding.address1ErrorText.visibility = View.VISIBLE
            return false
        }
        binding.address1ErrorText.visibility = View.INVISIBLE
        return true
    }

    fun isStateValid(): Boolean {
        val primaryAddress = binding.stateEditText.text
        if (primaryAddress.isEmpty()) {
            disableProceedButton()
            binding.stateErrorText.visibility = View.VISIBLE
            return false
        }
        binding.stateErrorText.visibility = View.INVISIBLE
        return true
    }

    fun isCityValid(): Boolean {
        val primaryAddress = binding.cityEditText.text
        if (primaryAddress.isEmpty()) {
            disableProceedButton()
            binding.cityErrortext.visibility = View.VISIBLE
            return false
        }
        binding.cityErrortext.visibility = View.INVISIBLE
        return true
    }

    fun inValidPhoneCode(countryCodeJson: JSONObject): Boolean {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val code = countryDetails.getString("isdCode")
            if (code.equals(binding.spinnerDialCodes.text)) {
                return true
            }
        }
        return false
    }

    fun isValidCountryName(countryCodeJson: JSONObject): Boolean {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val code = countryDetails.getString("isdCode")
            if (code.equals(binding.countryEditText.text.toString())) {
                return true
            }
        }
        return false
    }

    private fun getPostalCodeDetails() {
        val url = "${Base_Session_API_URL}${token}/postal-codes"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = object  : JsonObjectRequest(Method.GET, url, null, { response ->

            try{
                binding.stateEditText.setText(response.getString("state"))
                binding.cityEditText.setText(response.getString("city"))
                binding.stateEditText.isEnabled = false
            } catch (e: Exception) {
                // no op
            }

        }, Response.ErrorListener { /*no response handling */ }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["postalCode"] = binding.postalCodeEditText.text.toString()
                params["countryCode"] = "IN"
                return params
            }
        }
        queue.add(jsonObjectAll)
    }
}

class CustomArrayAdapter(
    context: Context,
    private val originalArray: Array<String>,
    private val isPhoneCodeCheck: Boolean
) : ArrayAdapter<String>(context, R.layout.spinner_dial_codes, originalArray) {

    private var filteredArray: Array<String> = originalArray

    override fun getCount(): Int {
        // Ensure there's at least one item ("No results found") if filteredArray is empty
        return if (filteredArray.isEmpty()) 1 else filteredArray.size
    }

    override fun getItem(position: Int): String? {
        // Return "No results found" if the filtered list is empty
        return if (filteredArray.isEmpty()) "No results found" else filteredArray[position]
    }

    override fun isEnabled(position: Int): Boolean {
        // Disable clicks if showing "No results found"
        return getItem(position) != "No results found"
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        // Display the "No results found" message and style it if the list is empty
        if (filteredArray.isEmpty()) {
            textView.text = "No results found"
            textView.setTextColor(Color.GRAY)  // Optional: Gray color for the "No results" message
        } else {
            textView.setTextColor(Color.BLACK)  // Regular color for normal items
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                if (!constraint.isNullOrEmpty()) {
                    // Filter the list based on the constraint
                    val filteredResults = originalArray.filter {
                        if (isPhoneCodeCheck) {
                            it.contains(constraint, ignoreCase = true)
                        } else {
                            it.startsWith(constraint, ignoreCase = true)
                        }
                    }.sorted().toTypedArray()

                    // Check if the filtered results are empty
                    results.values =
                        if (filteredResults.isEmpty()) arrayOf("No results found") else filteredResults
                    results.count = filteredResults.size
                } else {
                    // If no constraint, return the original array
                    results.values = originalArray
                    results.count = originalArray.size
                }

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.values is Array<*>) {
                    val filteredValues = results.values as Array<String>

                    // Check if the filtered values contain the "No results found" message
                    filteredArray = if (filteredValues.contains("No results found")) {
                        arrayOf("No results found")  // Set filteredArray to show the message
                    } else {
                        filteredValues
                    }
                } else {
                    filteredArray = originalArray
                }

                // Notify that the data set has changed
                notifyDataSetChanged()
            }
        }
    }

}
