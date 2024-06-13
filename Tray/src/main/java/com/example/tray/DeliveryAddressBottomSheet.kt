package com.example.tray

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.tray.databinding.FragmentDeliveryAddressBottomSheetBinding
import com.example.tray.interfaces.UpdateMainBottomSheetInterface
import com.example.tray.paymentResult.PaymentResultObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class DeliveryAddressBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentDeliveryAddressBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var countryCodePhoneNum: String = "+91"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var callback: UpdateMainBottomSheetInterface? = null
    private var indexCountryCodePhone: Int = 0
    private var firstTime: Boolean = false
    private var countrySelected = false
    private var countrySelectedFromDropDown: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            FragmentDeliveryAddressBottomSheetBinding.inflate(layoutInflater, container, false)
        val spinnerDialCodes = binding.spinnerDialCodes

        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.dial_codes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.custom_dial_code_item)
            spinnerDialCodes.adapter = adapter

            val defaultSelectionIndex = 0
            spinnerDialCodes.setSelection(defaultSelectionIndex)
        }

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels

        // Calculate 50% of screen height
        val cardViewHeight = (screenHeight * 0.45).toInt()

        // Set the height of the CardView dynamically
        val layoutParams = binding.cardView.layoutParams
        layoutParams.height = cardViewHeight
        binding.cardView.layoutParams = layoutParams


//        val adapter = ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, countryNames)
//
//        // Find TextView control
//
//        // Set the number of characters the user must type before the drop down list is shown
//        binding.countryEditText.threshold = 1
//
//        // Set the adapter
//        acTextView.setAdapter(adapter)


        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, countryNames)
        binding.countryEditText.threshold = 0
        binding.countryEditText.setAdapter(adapter)

        binding.countryEditText.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            Log.d("item selected : ",selectedItem)
            
            countrySelectedFromDropDown = selectedItem
            countrySelected = true
            checkAllFieldsEntered(false)
        }

        binding.countryEditText.addTextChangedListener(object : TextWatcher {
            private var previousLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Save the length of the text before the change
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed while text is changing

            }

            override fun afterTextChanged(s: Editable?) {
                // Check if the length of the text has decreased after the change
                val currentLength = s?.length ?: 0
                if (currentLength < previousLength) {
                    // Characters are being deleted
                    // You can handle this case here
                    countrySelected = false
                    checkAllFieldsEntered(false)
                }
            }
        })


        spinnerDialCodes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Get the selected item
                val selectedDialCode = parent.getItemAtPosition(position).toString()
                // Display or use the selected item
                countryCodePhoneNum = selectedDialCode
                indexCountryCodePhone = position
                Log.d("dialCode", "Selected: $countryCodePhoneNum")
            }
            

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing, or you can handle this case if needed
            }
        }


        val allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890. "
        val filter = InputFilter { source, _, _, _, _, _ ->
            // Filter out characters not present in the allowed characters list
            source.filter { allowedCharacters.contains(it) }
        }
        binding.fullNameEditText.filters = arrayOf(filter)


        val address1 = sharedPreferences.getString("address1", null)
        val address2 = sharedPreferences.getString("address2", null)
        val city = sharedPreferences.getString("city", null)
        val state = sharedPreferences.getString("state", null)
        val postalCode = sharedPreferences.getString("postalCode", null)
        val name = sharedPreferences.getString("name", null)
        val email = sharedPreferences.getString("email", null)
        val phoneNumber = sharedPreferences.getString("address1", null)
        val indexCountryPhone = sharedPreferences.getString("indexCountryCodePhone", null)
        val countryName = sharedPreferences.getString("countryName", null)

        binding.backButton.setOnClickListener(){
            dismiss()
        }

        if (!firstTime) {
            binding.backButton.visibility = View.VISIBLE

            if (indexCountryPhone != null) {
                binding.spinnerDialCodes.setSelection(indexCountryPhone!!.toInt())
            }

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
            if (email != null) {
                binding.emailEditText.setText(email)
            }
            if (phoneNumber != null) {
                binding.mobileNumberEditText.setText(phoneNumber)
            }
            if (countryName != null) {
                binding.countryEditText.setText(countryName)
            }
        }else{
            binding.backButton.visibility = View.GONE
        }






        binding.fullNameEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })


        binding.mobileNumberEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })



        binding.emailEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
            }

            override fun afterTextChanged(s: Editable?) {
                val emailPattern = "^[A-Za-z0-9+_.-]+@(.+)\$"
                val email = s.toString()

                // Check if the email matches the pattern
                val isValidEmail = email.matches(emailPattern.toRegex())

                if (!isValidEmail) {
                    Log.d("isValidEmail", "false")
                    binding.emailEditText.error = "Invalid email address"
                    disableProceedButton()
                } else {
                    binding.emailEditText.error = null
                    Log.d("isValidEmail", "true")
                    checkAllFieldsEntered(false)
                }
            }
        })

        binding.addressEditText1.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.addressEditText2.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })




        binding.postalCodeEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.stateEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.cityEditText.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAllFieldsEntered(false)
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
            val country = binding.countryEditText.text
            val postalCode = binding.postalCodeEditText.text
            val state = binding.stateEditText.text
            val city = binding.cityEditText.text


            editor.putString("address1", address1.toString())
            editor.putString("address2", address2.toString())
            editor.putString("city", city.toString())
            editor.putString("state", state.toString())
            editor.putString("state", state.toString())
            Log.d("countryCode",countryNameToCountryCodeNameHashMap[countrySelectedFromDropDown].toString())
            editor.putString("countryCode", countryNameToCountryCodeNameHashMap[countrySelectedFromDropDown])
            editor.putString("postalCode", postalCode.toString())
            editor.putString("name", fullName.toString())
            editor.putString("email", email.toString())
            editor.putString("phoneNumber", mobileNumber.toString())
            editor.putString("countryCodePhoneNum", countryCodePhoneNum)
            editor.putString("countryName", country.toString())
            editor.putString("indexCountryCodePhone", indexCountryCodePhone.toString())
            editor.putString("countryCodeFromName", countryData[countryCodePhoneNum])


            editor.apply()



            callback?.updateBottomSheet()
            dismiss()
        }




        return binding.root
    }

    fun onPaymentResultCallback(result: PaymentResultObject) {
        if (result.status == "Success") {
            Log.d("onPaymentResultCallback", "Success")
        } else {
            Log.d("onPaymentResultCallback", "Failure")
        }
    }

    private fun checkAllFieldsEntered(calledBySubmitButton: Boolean): Boolean {
        val fullName = binding.fullNameEditText.text
        val mobileNumber = binding.mobileNumberEditText.text
        val email = binding.emailEditText.text
        val address1 = binding.addressEditText1.text
        val address2 = binding.addressEditText2.text
        val country = binding.countryEditText.text
        val postalCode = binding.postalCodeEditText.text
        val state = binding.stateEditText.text
        val city = binding.cityEditText.text

        Log.d(
            "Address Details",
            "$fullName $mobileNumber $email $country $postalCode $state $city\n $address1 $address2"
        )



        if (fullName.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.fullNameEditText.error = "This field is required"
            }
            disableProceedButton()
            return false
        }
        if (mobileNumber.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.mobileNumberEditText.error = "This field is required"
            }
            disableProceedButton()
            return false
        }
        if (email.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.emailEditText.error = "This field is required"
            }
            disableProceedButton()
            return false
        }

        if (address1.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.addressEditText1.error = "This field is required"
            }
            disableProceedButton()
        }
        if (address2.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.addressEditText2.error = "This field is required"
            }
            disableProceedButton()
        }
        if (!countrySelected) {
            if (calledBySubmitButton) {
                binding.countryEditText.error = "This field is required"
            }
            disableProceedButton()
            return false
        }
        if (postalCode.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.postalCodeEditText.error = "This field is required"
            }
            disableProceedButton()
            return false
        }
        if (state.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.stateEditText.error = "This field is required"
            }
            disableProceedButton()
            return false
        }
        if (city.isNullOrBlank()) {
            if (calledBySubmitButton) {
                binding.cityEditText.error = "This field is required"
            }
            disableProceedButton()
            return false
        }

        enableProceedButton()
        return true
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
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
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


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }

            if (bottomSheetBehavior == null)
                Log.d("bottomSheetBehavior is null", "check here")

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
            val percentageOfScreenHeight = 0.5 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

//            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//            dialog.window?.setDimAmount(0.5f)


//            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Set transparent background
//            dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(R.drawable.button_bg)


//        // Adjust the height of the bottom sheet content view
//        val layoutParams = bottomSheetContent.layoutParams
//        layoutParams.height = desiredHeight
//        bottomSheetContent.layoutParams = layoutParams
//            bottomSheetBehavior?.maxHeight = desiredHeight
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            if(firstTime)
                dialog.setCancelable(false)
            else
                dialog.setCancelable(true)





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


                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    val countryNameToCountryCodeNameHashMap = hashMapOf(
        "Afghanistan" to "AF",
        "Aland Islands" to "AX",
        "Albania" to "AL",
        "Algeria" to "DZ",
        "AmericanSamoa" to "AS",
        "Andorra" to "AD",
        "Angola" to "AO",
        "Anguilla" to "AI",
        "Antarctica" to "AQ",
        "Antigua and Barbuda" to "AG",
        "Argentina" to "AR",
        "Armenia" to "AM",
        "Aruba" to "AW",
        "Australia" to "AU",
        "Austria" to "AT",
        "Azerbaijan" to "AZ",
        "Bahamas" to "BS",
        "Bahrain" to "BH",
        "Bangladesh" to "BD",
        "Barbados" to "BB",
        "Belarus" to "BY",
        "Belgium" to "BE",
        "Belize" to "BZ",
        "Benin" to "BJ",
        "Bermuda" to "BM",
        "Bhutan" to "BT",
        "Bolivia, Plurinational State of" to "BO",
        "Bosnia and Herzegovina" to "BA",
        "Botswana" to "BW",
        "Brazil" to "BR",
        "British Indian Ocean Territory" to "IO",
        "Brunei Darussalam" to "BN",
        "Bulgaria" to "BG",
        "Burkina Faso" to "BF",
        "Burundi" to "BI",
        "Cambodia" to "KH",
        "Cameroon" to "CM",
        "Canada" to "CA",
        "Cape Verde" to "CV",
        "Cayman Islands" to "KY",
        "Central African Republic" to "CF",
        "Chad" to "TD",
        "Chile" to "CL",
        "China" to "CN",
        "Christmas Island" to "CX",
        "Cocos (Keeling) Islands" to "CC",
        "Colombia" to "CO",
        "Comoros" to "KM",
        "Congo" to "CG",
        "Congo, The Democratic Republic of the Congo" to "CD",
        "Cook Islands" to "CK",
        "Costa Rica" to "CR",
        "Cote d'Ivoire" to "CI",
        "Croatia" to "HR",
        "Cuba" to "CU",
        "Cyprus" to "CY",
        "Czech Republic" to "CZ",
        "Denmark" to "DK",
        "Djibouti" to "DJ",
        "Dominica" to "DM",
        "Dominican Republic" to "DO",
        "Ecuador" to "EC",
        "Egypt" to "EG",
        "El Salvador" to "SV",
        "Equatorial Guinea" to "GQ",
        "Eritrea" to "ER",
        "Estonia" to "EE",
        "Ethiopia" to "ET",
        "Falkland Islands (Malvinas)" to "FK",
        "Faroe Islands" to "FO",
        "Fiji" to "FJ",
        "Finland" to "FI",
        "France" to "FR",
        "French Guiana" to "GF",
        "French Polynesia" to "PF",
        "Gabon" to "GA",
        "Gambia" to "GM",
        "Georgia" to "GE",
        "Germany" to "DE",
        "Ghana" to "GH",
        "Gibraltar" to "GI",
        "Greece" to "GR",
        "Greenland" to "GL",
        "Grenada" to "GD",
        "Guadeloupe" to "GP",
        "Guam" to "GU",
        "Guatemala" to "GT",
        "Guernsey" to "GG",
        "Guinea" to "GN",
        "Guinea-Bissau" to "GW",
        "Guyana" to "GY",
        "Haiti" to "HT",
        "Holy See (Vatican City State)" to "VA",
        "Honduras" to "HN",
        "Hong Kong" to "HK",
        "Hungary" to "HU",
        "Iceland" to "IS",
        "India" to "IN",
        "Indonesia" to "ID",
        "Iran, Islamic Republic of Persian Gulf" to "IR",
        "Iraq" to "IQ",
        "Ireland" to "IE",
        "Isle of Man" to "IM",
        "Israel" to "IL",
        "Italy" to "IT",
        "Jamaica" to "JM",
        "Japan" to "JP",
        "Jersey" to "JE",
        "Jordan" to "JO",
        "Kazakhstan" to "KZ",
        "Kenya" to "KE",
        "Kiribati" to "KI",
        "Korea, Democratic People's Republic of Korea" to "KP",
        "Korea, Republic of South Korea" to "KR",
        "Kuwait" to "KW",
        "Kyrgyzstan" to "KG",
        "Laos" to "LA",
        "Latvia" to "LV",
        "Lebanon" to "LB",
        "Lesotho" to "LS",
        "Liberia" to "LR",
        "Libyan Arab Jamahiriya" to "LY",
        "Liechtenstein" to "LI",
        "Lithuania" to "LT",
        "Luxembourg" to "LU",
        "Macao" to "MO",
        "Macedonia" to "MK",
        "Madagascar" to "MG",
        "Malawi" to "MW",
        "Malaysia" to "MY",
        "Maldives" to "MV",
        "Mali" to "ML",
        "Malta" to "MT",
        "Marshall Islands" to "MH",
        "Martinique" to "MQ",
        "Mauritania" to "MR",
        "Mauritius" to "MU",
        "Mayotte" to "YT",
        "Mexico" to "MX",
        "Micronesia, Federated States of Micronesia" to "FM",
        "Moldova" to "MD",
        "Monaco" to "MC",
        "Mongolia" to "MN",
        "Montenegro" to "ME",
        "Montserrat" to "MS",
        "Morocco" to "MA",
        "Mozambique" to "MZ",
        "Myanmar" to "MM",
        "Namibia" to "NA",
        "Nauru" to "NR",
        "Nepal" to "NP",
        "Netherlands" to "NL",
        "Netherlands Antilles" to "AN",
        "New Caledonia" to "NC",
        "New Zealand" to "NZ",
        "Nicaragua" to "NI",
        "Niger" to "NE",
        "Nigeria" to "NG",
        "Niue" to "NU",
        "Norfolk Island" to "NF",
        "Northern Mariana Islands" to "MP",
        "Norway" to "NO",
        "Oman" to "OM",
        "Pakistan" to "PK",
        "Palau" to "PW",
        "Palestinian Territory, Occupied" to "PS",
        "Panama" to "PA",
        "Papua New Guinea" to "PG",
        "Paraguay" to "PY",
        "Peru" to "PE",
        "Philippines" to "PH",
        "Pitcairn" to "PN",
        "Poland" to "PL",
        "Portugal" to "PT",
        "Puerto Rico" to "PR",
        "Qatar" to "QA",
        "Romania" to "RO",
        "Russia" to "RU",
        "Rwanda" to "RW",
        "Reunion" to "RE",
        "Saint Barthelemy" to "BL",
        "Saint Helena, Ascension and Tristan Da Cunha" to "SH",
        "Saint Kitts and Nevis" to "KN",
        "Saint Lucia" to "LC",
        "Saint Martin" to "MF",
        "Saint Pierre and Miquelon" to "PM",
        "Saint Vincent and the Grenadines" to "VC",
        "Samoa" to "WS",
        "San Marino" to "SM",
        "Sao Tome and Principe" to "ST",
        "Saudi Arabia" to "SA",
        "Senegal" to "SN",
        "Serbia" to "RS",
        "Seychelles" to "SC",
        "Sierra Leone" to "SL",
        "Singapore" to "SG",
        "Slovakia" to "SK",
        "Slovenia" to "SI",
        "Solomon Islands" to "SB",
        "Somalia" to "SO",
        "South Africa" to "ZA",
        "South Sudan" to "SS",
        "South Georgia and the South Sandwich Islands" to "GS",
        "Spain" to "ES",
        "Sri Lanka" to "LK",
        "Sudan" to "SD",
        "Suriname" to "SR",
        "Svalbard and Jan Mayen" to "SJ",
        "Swaziland" to "SZ",
        "Sweden" to "SE",
        "Switzerland" to "CH",
        "Syrian Arab Republic" to "SY",
        "Taiwan" to "TW",
        "Tajikistan" to "TJ",
        "Tanzania, United Republic of Tanzania" to "TZ",
        "Thailand" to "TH",
        "Timor-Leste" to "TL",
        "Togo" to "TG",
        "Tokelau" to "TK",
        "Tonga" to "TO",
        "Trinidad and Tobago" to "TT",
        "Tunisia" to "TN",
        "Turkey" to "TR",
        "Turkmenistan" to "TM",
        "Turks and Caicos Islands" to "TC",
        "Tuvalu" to "TV",
        "Uganda" to "UG",
        "Ukraine" to "UA",
        "United Arab Emirates" to "AE",
        "United Kingdom" to "GB",
        "United States" to "US",
        "Uruguay" to "UY",
        "Uzbekistan" to "UZ",
        "Vanuatu" to "VU",
        "Venezuela, Bolivarian Republic of Venezuela" to "VE",
        "Vietnam" to "VN",
        "Virgin Islands, British" to "VG",
        "Virgin Islands, U.S." to "VI",
        "Wallis and Futuna" to "WF",
        "Yemen" to "YE",
        "Zambia" to "ZM",
        "Zimbabwe" to "ZW")

    val countryData = hashMapOf(
        "+91" to "IN",
        "+93" to "AF",
        "+358" to "AX",
        "+355" to "AL",
        "+213" to "DZ",
        "+1684" to "AS",
        "+376" to "AD",
        "+244" to "AO",
        "+1264" to "AI",
        "+672" to "AQ",
        "+1268" to "AG",
        "+54" to "AR",
        "+374" to "AM",
        "+297" to "AW",
        "+61" to "AU",
        "+43" to "AT",
        "+994" to "AZ",
        "+1242" to "BS",
        "+973" to "BH",
        "+880" to "BD",
        "+1246" to "BB",
        "+375" to "BY",
        "+32" to "BE",
        "+501" to "BZ",
        "+229" to "BJ",
        "+1441" to "BM",
        "+975" to "BT",
        "+591" to "BO",
        "+387" to "BA",
        "+267" to "BW",
        "+55" to "BR",
        "+246" to "IO",
        "+673" to "BN",
        "+359" to "BG",
        "+226" to "BF",
        "+257" to "BI",
        "+855" to "KH",
        "+237" to "CM",
        "+1" to "CA",
        "+238" to "CV",
        "+345" to "KY",
        "+236" to "CF",
        "+235" to "TD",
        "+56" to "CL",
        "+86" to "CN",
        "+61" to "CX",
        "+61" to "CC",
        "+57" to "CO",
        "+269" to "KM",
        "+242" to "CG",
        "+243" to "CD",
        "+682" to "CK",
        "+506" to "CR",
        "+225" to "CI",
        "+385" to "HR",
        "+53" to "CU",
        "+357" to "CY",
        "+420" to "CZ",
        "+45" to "DK",
        "+253" to "DJ",
        "+1767" to "DM",
        "+1849" to "DO",
        "+593" to "EC",
        "+20" to "EG",
        "+503" to "SV",
        "+240" to "GQ",
        "+291" to "ER",
        "+372" to "EE",
        "+251" to "ET",
        "+500" to "FK",
        "+298" to "FO",
        "+679" to "FJ",
        "+358" to "FI",
        "+33" to "FR",
        "+594" to "GF",
        "+689" to "PF",
        "+241" to "GA",
        "+220" to "GM",
        "+995" to "GE",
        "+49" to "DE",
        "+233" to "GH",
        "+350" to "GI",
        "+30" to "GR",
        "+299" to "GL",
        "+1473" to "GD",
        "+590" to "GP",
        "+1671" to "GU",
        "+502" to "GT",
        "+44" to "GG",
        "+224" to "GN",
        "+245" to "GW",
        "+595" to "GY",
        "+509" to "HT",
        "+379" to "VA",
        "+504" to "HN",
        "+852" to "HK",
        "+36" to "HU",
        "+354" to "IS",
        "+62" to "ID",
        "+98" to "IR",
        "+964" to "IQ",
        "+353" to "IE",
        "+44" to "IM",
        "+972" to "IL",
        "+39" to "IT",
        "+1876" to "JM",
        "+81" to "JP",
        "+44" to "JE",
        "+962" to "JO",
        "+77" to "KZ",
        "+254" to "KE",
        "+686" to "KI",
        "+850" to "KP",
        "+82" to "KR",
        "+965" to "KW",
        "+996" to "KG",
        "+856" to "LA",
        "+371" to "LV",
        "+961" to "LB",
        "+266" to "LS",
        "+231" to "LR",
        "+218" to "LY",
        "+423" to "LI",
        "+370" to "LT",
        "+352" to "LU",
        "+853" to "MO",
        "+389" to "MK",
        "+261" to "MG",
        "+265" to "MW",
        "+60" to "MY",
        "+960" to "MV",
        "+223" to "ML",
        "+356" to "MT",
        "+692" to "MH",
        "+596" to "MQ",
        "+222" to "MR",
        "+230" to "MU",
        "+262" to "YT",
        "+52" to "MX",
        "+691" to "FM",
        "+373" to "MD",
        "+377" to "MC",
        "+976" to "MN",
        "+382" to "ME",
        "+1664" to "MS",
        "+212" to "MA",
        "+258" to "MZ",
        "+95" to "MM",
        "+264" to "NA",
        "+674" to "NR",
        "+977" to "NP",
        "+31" to "NL",
        "+599" to "AN",
        "+687" to "NC",
        "+64" to "NZ",
        "+505" to "NI",
        "+227" to "NE",
        "+234" to "NG",
        "+683" to "NU",
        "+672" to "NF",
        "+1670" to "MP",
        "+47" to "NO",
        "+968" to "OM",
        "+92" to "PK",
        "+680" to "PW",
        "+970" to "PS",
        "+507" to "PA",
        "+675" to "PG",
        "+595" to "PY",
        "+51" to "PE",
        "+63" to "PH",
        "+872" to "PN",
        "+48" to "PL",
        "+351" to "PT",
        "+1939" to "PR",
        "+974" to "QA",
        "+40" to "RO",
        "+7" to "RU",
        "+250" to "RW",
        "+262" to "RE",
        "+590" to "BL",
        "+290" to "SH",
        "+1869" to "KN",
        "+1758" to "LC",
        "+590" to "MF",
        "+508" to "PM",
        "+1784" to "VC",
        "+685" to "WS",
        "+378" to "SM",
        "+239" to "ST",
        "+966" to "SA",
        "+221" to "SN",
        "+381" to "RS",
        "+248" to "SC",
        "+232" to "SL",
        "+65" to "SG",
        "+421" to "SK",
        "+386" to "SI",
        "+677" to "SB",
        "+252" to "SO",
        "+27" to "ZA",
        "+211" to "SS",
        "+500" to "GS",
        "+34" to "ES",
        "+94" to "LK",
        "+249" to "SD",
        "+597" to "SR",
        "+47" to "SJ",
        "+268" to "SZ",
        "+46" to "SE",
        "+41" to "CH",
        "+963" to "SY",
        "+886" to "TW",
        "+992" to "TJ",
        "+255" to "TZ",
        "+66" to "TH",
        "+670" to "TL",
        "+228" to "TG",
        "+690" to "TK",
        "+676" to "TO",
        "+1868" to "TT",
        "+216" to "TN",
        "+90" to "TR",
        "+993" to "TM",
        "+1649" to "TC",
        "+688" to "TV",
        "+256" to "UG",
        "+380" to "UA",
        "+971" to "AE",
        "+44" to "GB",
        "+598" to "UY",
        "+998" to "UZ",
        "+678" to "VU",
        "+58" to "VE",
        "+84" to "VN",
        "+1284" to "VG",
        "+1340" to "VI",
        "+681" to "WF",
        "+967" to "YE",
        "+260" to "ZM",
        "+263" to "ZW"
    )
    val countryNames = arrayOf(
        "India",
        "Afghanistan",
        "Aland Islands",
        "Albania",
        "Algeria",
        "AmericanSamoa",
        "Andorra",
        "Angola",
        "Anguilla",
        "Antarctica",
        "Antigua and Barbuda",
        "Argentina",
        "Armenia",
        "Aruba",
        "Australia",
        "Austria",
        "Azerbaijan",
        "Bahamas",
        "Bahrain",
        "Bangladesh",
        "Barbados",
        "Belarus",
        "Belgium",
        "Belize",
        "Benin",
        "Bermuda",
        "Bhutan",
        "Bolivia, Plurinational State of",
        "Bosnia and Herzegovina",
        "Botswana",
        "Brazil",
        "British Indian Ocean Territory",
        "Brunei Darussalam",
        "Bulgaria",
        "Burkina Faso",
        "Burundi",
        "Cambodia",
        "Cameroon",
        "Canada",
        "Cape Verde",
        "Cayman Islands",
        "Central African Republic",
        "Chad",
        "Chile",
        "China",
        "Christmas Island",
        "Cocos (Keeling) Islands",
        "Colombia",
        "Comoros",
        "Congo",
        "Congo, The Democratic Republic of the Congo",
        "Cook Islands",
        "Costa Rica",
        "Cote d'Ivoire",
        "Croatia",
        "Cuba",
        "Cyprus",
        "Czech Republic",
        "Denmark",
        "Djibouti",
        "Dominica",
        "Dominican Republic",
        "Ecuador",
        "Egypt",
        "El Salvador",
        "Equatorial Guinea",
        "Eritrea",
        "Estonia",
        "Ethiopia",
        "Falkland Islands (Malvinas)",
        "Faroe Islands",
        "Fiji",
        "Finland",
        "France",
        "French Guiana",
        "French Polynesia",
        "Gabon",
        "Gambia",
        "Georgia",
        "Germany",
        "Ghana",
        "Gibraltar",
        "Greece",
        "Greenland",
        "Grenada",
        "Guadeloupe",
        "Guam",
        "Guatemala",
        "Guernsey",
        "Guinea",
        "Guinea-Bissau",
        "Guyana",
        "Haiti",
        "Holy See (Vatican City State)",
        "Honduras",
        "Hong Kong",
        "Hungary",
        "Iceland",
        "Indonesia",
        "Iran, Islamic Republic of Persian Gulf",
        "Iraq",
        "Ireland",
        "Isle of Man",
        "Israel",
        "Italy",
        "Jamaica",
        "Japan",
        "Jersey",
        "Jordan",
        "Kazakhstan",
        "Kenya",
        "Kiribati",
        "Korea, Democratic People's Republic of Korea",
        "Korea, Republic of South Korea",
        "Kuwait",
        "Kyrgyzstan",
        "Laos",
        "Latvia",
        "Lebanon",
        "Lesotho",
        "Liberia",
        "Libyan Arab Jamahiriya",
        "Liechtenstein",
        "Lithuania",
        "Luxembourg",
        "Macao",
        "Macedonia",
        "Madagascar",
        "Malawi",
        "Malaysia",
        "Maldives",
        "Mali",
        "Malta",
        "Marshall Islands",
        "Martinique",
        "Mauritania",
        "Mauritius",
        "Mayotte",
        "Mexico",
        "Micronesia, Federated States of Micronesia",
        "Moldova",
        "Monaco",
        "Mongolia",
        "Montenegro",
        "Montserrat",
        "Morocco",
        "Mozambique",
        "Myanmar",
        "Namibia",
        "Nauru",
        "Nepal",
        "Netherlands",
        "Netherlands Antilles",
        "New Caledonia",
        "New Zealand",
        "Nicaragua",
        "Niger",
        "Nigeria",
        "Niue",
        "Norfolk Island",
        "Northern Mariana Islands",
        "Norway",
        "Oman",
        "Pakistan",
        "Palau",
        "Palestinian Territory, Occupied",
        "Panama",
        "Papua New Guinea",
        "Paraguay",
        "Peru",
        "Philippines",
        "Pitcairn",
        "Poland",
        "Portugal",
        "Puerto Rico",
        "Qatar",
        "Romania",
        "Russia",
        "Rwanda",
        "Reunion",
        "Saint Barthelemy",
        "Saint Helena, Ascension and Tristan Da Cunha",
        "Saint Kitts and Nevis",
        "Saint Lucia",
        "Saint Martin",
        "Saint Pierre and Miquelon",
        "Saint Vincent and the Grenadines",
        "Samoa",
        "San Marino",
        "Sao Tome and Principe",
        "Saudi Arabia",
        "Senegal",
        "Serbia",
        "Seychelles",
        "Sierra Leone",
        "Singapore",
        "Slovakia",
        "Slovenia",
        "Solomon Islands",
        "Somalia",
        "South Africa",
        "South Sudan",
        "South Georgia and the South Sandwich Islands",
        "Spain",
        "Sri Lanka",
        "Sudan",
        "Suriname",
        "Svalbard and Jan Mayen",
        "Swaziland",
        "Sweden",
        "Switzerland",
        "Syrian Arab Republic",
        "Taiwan",
        "Tajikistan",
        "Tanzania, United Republic of Tanzania",
        "Thailand",
        "Timor-Leste",
        "Togo",
        "Tokelau",
        "Tonga",
        "Trinidad and Tobago",
        "Tunisia"
    )



    companion object {
        fun newInstance(
            callback: UpdateMainBottomSheetInterface,
            firstTime: Boolean
        ): DeliveryAddressBottomSheet {
            val fragment = DeliveryAddressBottomSheet()
            fragment.callback = callback

            Log.d("Instance first time : ",firstTime.toString())
            fragment.firstTime = firstTime
            return fragment
        }
    }
}