package com.example.tray

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.FragmentAddCardBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale


internal class AddCardBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddCardBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var bottomSheet: FrameLayout? = null
    private val Base_Session_API_URL = "https://test-apis.boxpay.tech/v0/checkout/sessions/"
    private var token: String? = null
    private var cardNumber: String? = null
    private var cardExpiryYYYY_MM: String? = null
    private var cvv: String? = null
    private var cardHolderName: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private var isCardNumberValid : Boolean = false
    private var isCardValidityValid : Boolean = false
    private var isCardCVVValid : Boolean = false
    private var isNameOnCardValid : Boolean = false
    private var successScreenFullReferencePath : String ?= null
    private var isAmericanExpressCard = MutableLiveData<Boolean>()
    private var transactionId : String ?= null
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var editor : SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Handle the back button press here
        // Dismiss the dialog when the back button is pressed
        dismissAndMakeButtonsOfMainBottomSheetEnabled()
    }
    fun makeCardNetworkIdentificationCall(context: Context,cardNumber: String){
        val queue = Volley.newRequestQueue(context)
        Log.d("makeCardNetworkIdentificationCall",cardNumber)
        val url = "https://test-apis.boxpay.tech/v0/platform/bank-identification-numbers/tokens/${cardNumber}"

        val jsonData = JSONArray()
        var token = ""

        val brands = mutableListOf<String>()

        val request = object : JsonArrayRequest(Method.POST, url, jsonData,
            { response ->
                for(i in 0 until response.length()){
                    brands.add(response.getJSONObject(i).getString("brand"))
                }
                Log.d("size of brands",brands.size.toString()+cardNumber)
                updateCardNetwork(brands)
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    Log.d("","")
                }
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =  "Bearer afcGgCv6mOVIIpnFPWBL44RRciVU8oMteV5ZhC2nwjjjuw8z0obKMjdK8ShcwLOU6uRNjQryLKl1pLAsLAXSI"
                return headers
            }
        }
        queue.add(request)
    }



    private fun getImageDrawableForItem(item: String): Int {
        return when (item) {
            "VISA" -> R.drawable.visa // Replace with your VISA image resource
            "Maestro" -> R.drawable.maestro // Replace with your MAESTRO image resource
            "Mastercard" -> R.drawable.mastercard
            "AmericanExpress" ->
                R.drawable.amex_png
            else -> R.drawable.card_02 // Default image resource
        }
    }
    private fun removeAndAddImageCardNetworks(cardNetworkName : String){
        isAmericanExpressCard.value = cardNetworkName == "AmericanExpress"

        Log.d("removeAndAddImageCardNetworksCalled",cardNetworkName)
        binding.defaultCardNetworkLinearLayout.visibility = View.GONE
        val imageView = ImageView(requireContext())
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        imageView.layoutParams = layoutParams
        val imageDrawable = getImageDrawableForItem(cardNetworkName)
        imageView.setImageResource(imageDrawable)
        binding.fetchedCardNetwork.removeAllViews()
        binding.fetchedCardNetwork.visibility = View.VISIBLE
        binding.fetchedCardNetwork.addView(imageView)
    }


    private fun updateCardNetwork(brands: MutableList<String>){
        if(brands.size == 1){
            removeAndAddImageCardNetworks(brands[0])
        }else{
            Log.d("updateCardNetworkRemoveAllViews","here")
            binding.fetchedCardNetwork.removeAllViews()
            binding.fetchedCardNetwork.visibility = View.GONE
            binding.defaultCardNetworkLinearLayout.visibility = View.VISIBLE
        }
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment = parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentAddCardBottomSheetBinding.inflate(inflater, container, false)
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()


        fetchTransactionDetailsFromSharedPreferences()

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
                if(isCardNumberValid && isCardValidityValid && isCardCVVValid && isNameOnCardValid) {
                    enableProceedButton()
                    Log.d("card enabled",""+isCardNumberValid + isCardValidityValid + isCardCVVValid)
                }
            } else {
                Log.d("card enabled false",""+isCardNumberValid + isCardValidityValid + isCardCVVValid)
                disableProceedButton()
            }
        })
        isAmericanExpressCard.observe(this, Observer { isAmericanExpressCardOrNot ->
            if(isAmericanExpressCardOrNot){
                binding.editTextCardCVV.setText("")
                binding.editTextCardCVV.filters = arrayOf(InputFilter.LengthFilter(4))
            }else{
                binding.editTextCardCVV.setText("")
                binding.editTextCardCVV.filters = arrayOf(InputFilter.LengthFilter(3))
            }
        })
        proceedButtonIsEnabled.value = false

        var checked = false
        binding.progressBar.visibility = View.INVISIBLE
        binding.ll1InvalidCardNumber.visibility = View.GONE
        binding.invalidCardValidity.visibility = View.GONE
        binding.invalidCVV.visibility = View.GONE
        binding.saveCardLinearLayout.setOnClickListener() {
            if (!checked) {
                binding.imageView3.setImageResource(R.drawable.checkbox)
                checked = true
            } else {
                binding.imageView3.setImageResource(0)
                checked = false
            }
        }


        binding.backButton.setOnClickListener() {
            dismissAndMakeButtonsOfMainBottomSheetEnabled()
        }

        binding.editTextCardNumber.filters = arrayOf(InputFilter.LengthFilter(19))


        binding.editTextCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                Log.d("textNow length",textNow.length.toString())
                if (textNow.isNotBlank()) {
                    if(textNow.length == 19){
                        isCardNumberValid = true
                        proceedButtonIsEnabled.value = true
                        binding.ll1InvalidCardNumber.visibility = View.GONE
                    }else{
                        isCardNumberValid = false
                        proceedButtonIsEnabled.value = false
                    }
                }
                if(textNow.length == 19){
                    Log.d("16 digits","crossed")
                    if(isValidCardNumberByLuhn(removeSpaces(textNow)))
                        binding.editTextCardValidity.requestFocus()
                    else{
                        isCardNumberValid = false
                        binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                        proceedButtonIsEnabled.value = false
                    }
                }

                binding.editTextCardNumber.removeTextChangedListener(this)

                val text = s.toString().replace("\\s".toRegex(), "")
                val formattedText = formatCardNumber(text)

                binding.editTextCardNumber.setText(formattedText)
                binding.editTextCardNumber.setSelection(formattedText.length)

                binding.editTextCardNumber.addTextChangedListener(this)
                val cardNumberWithOutSpaces = textNow.replace(" ", "")

                if(cardNumberWithOutSpaces.length <= 6){
                    Log.d("makeCardNetworkIdentificationCall",cardNumberWithOutSpaces)
                    makeCardNetworkIdentificationCall(requireContext(),cardNumberWithOutSpaces)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                if (textNow.isBlank()) {
                    isCardNumberValid = false
                    proceedButtonIsEnabled.value = false
                }
            }
        })


        // Set InputFilter to limit the length and add a slash after every 2 digits
        binding.editTextCardValidity.filters = arrayOf(InputFilter.LengthFilter(7))

        // Set TextWatcher to add slashes dynamically as the user types
        binding.editTextCardValidity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                var text = s.toString().replace("/", "")

                if(text.isNotEmpty() && (text[0].toString().toInt() != 0 && text[0].toString().toInt() != 1)){
                    Log.d("Invalid month in validity",text[0].toString())
                    binding.invalidCardValidity.visibility = View.GONE
                    proceedButtonIsEnabled.value = false
                }
                binding.editTextCardValidity.removeTextChangedListener(this)
                if(text.length == 1){
                    Log.d("textNow card validity",text)
                    if(text != "0" && text != "1"){
                        text = "0"+text
                    }
                }

//                val text = s.toString().replace("/", "")
                val formattedText = formatMMYY(text)

                if(text.length == 4){
                    binding.editTextCardCVV.requestFocus()
                }
                binding.editTextCardValidity.setText(formattedText)
                binding.editTextCardValidity.setSelection(formattedText.length)

                binding.editTextCardValidity.addTextChangedListener(this)
                val textNow = s.toString()
                Log.d("onTextChanged", s.toString())
                if (textNow.isNotBlank()) {

                    if(textNow.length == 5) {
                        Log.d("card validity is made true",textNow.length.toString())
                        val cardValidity = binding.editTextCardValidity.text.toString()
                        if (!(isValidExpirationDate(
                                cardValidity.substring(0, 2),
                                cardValidity.substring(3, 5)
                            ))
                        ) {
                            isCardValidityValid = false
                            proceedButtonIsEnabled.value = false
                            binding.invalidCardValidity.visibility = View.VISIBLE
                            binding.textView7.text = "Invalid card validity"
                        }else{
                            isCardValidityValid = true
                            proceedButtonIsEnabled.value = true
                            binding.invalidCardValidity.visibility = View.GONE
                        }
                    }
                    else {
                        isCardValidityValid = false
                        proceedButtonIsEnabled.value = false
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                Log.d("afterTextChanged", s.toString())
                if (textNow.isBlank()) {
                    isCardValidityValid = false
                    proceedButtonIsEnabled.value = false
                }
            }
        })

        binding.editTextCardCVV.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("beforeTextChanged", s.toString())
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                Log.d("onTextChanged", s.toString())
                if (textNow.isNotBlank()) {
                    if(textNow.length >= 3) {
                        isCardCVVValid = true
                        proceedButtonIsEnabled.value = true
                        binding.invalidCVV.visibility = View.GONE
                    }
                    else {
                        isCardCVVValid = false
                        proceedButtonIsEnabled.value = false
                    }
                }

                if(textNow.length == 4){
                    binding.editTextNameOnCard.requestFocus()

                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                Log.d("afterTextChanged", s.toString())
                if (textNow.isBlank()) {
                    isCardCVVValid = false
                    proceedButtonIsEnabled.value = false
                }
            }
        })

        binding.editTextNameOnCard.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("beforeTextChanged", s.toString())
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                if(textNow.isBlank()){
                    Log.d("inside true of text changed",s.toString())
                    isNameOnCardValid = false
                    binding.nameOnCardErrorLayout.visibility = View.GONE
                    proceedButtonIsEnabled.value = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                Log.d("afterTextChanged", s.toString())
                if (textNow.isBlank()) {
                    isNameOnCardValid = false
                    binding.nameOnCardErrorLayout.visibility = View.VISIBLE
                    proceedButtonIsEnabled.value = false
                }else{
                    isNameOnCardValid = true
                    binding.nameOnCardErrorLayout.visibility = View.GONE
                    proceedButtonIsEnabled.value = true
                }
            }

        })


        binding.editTextCardCVV.setTransformationMethod(AsteriskPasswordTransformationMethod())

        binding.proceedButton.setOnClickListener() {
            removeErrors()
            cardNumber = deformatCardNumber(binding.editTextCardNumber.text.toString())
            Log.d("card number", cardNumber!!)
            cardExpiryYYYY_MM = addDashInsteadOfSlash(binding.editTextCardValidity.text.toString())
            if (cardExpiryYYYY_MM.isNullOrEmpty()) {
                return@setOnClickListener
            }
            Log.d("card expiry", cardExpiryYYYY_MM!!)
            cvv = binding.editTextCardCVV.text.toString()
            Log.d("card cvv", cvv!!)
            cardHolderName = binding.editTextNameOnCard.text.toString()
            Log.d("card holder name", cardHolderName!!)
            var anyFieldEmpty = false

            if(cardNumber.isNullOrEmpty()){
                binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                binding.textView4.text = "Enter card number"
                anyFieldEmpty = true
            }

            if(cardExpiryYYYY_MM.isNullOrEmpty()){
                binding.invalidCardValidity.visibility = View.VISIBLE
                binding.textView7.text = "Enter card validity"
                anyFieldEmpty = true
            }
            if(cardHolderName.isNullOrEmpty()){
                Toast.makeText(requireContext(), "Enter name on card", Toast.LENGTH_SHORT).show()
                anyFieldEmpty = true
            }
            if(cvv.isNullOrEmpty()){
                binding.invalidCVV.visibility = View.VISIBLE
                binding.textView8.text = "Enter CVV"
                anyFieldEmpty = true
            }

            if(anyFieldEmpty){
                return@setOnClickListener
            }

            postRequest(requireContext())
            showLoadingInButton()
        }


        binding.editTextCardNumber.setOnFocusChangeListener(OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                val cardNumber = removeSpaces(binding.editTextCardNumber.text.toString())
                if(!(isValidCardNumberByLuhn(cardNumber) && isValidCardNumberLength(cardNumber))){
                    binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                    binding.textView4.text = "Invalid card number. Please check"
                }else{
                    binding.ll1InvalidCardNumber.visibility = View.GONE
                }
//                Toast.makeText(requireContext(), "Lost the focus", Toast.LENGTH_LONG).show()
            }
        })

        binding.editTextCardValidity.setOnFocusChangeListener(OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                val cardValidity = binding.editTextCardValidity.text.toString()
                try {
                    if (!(isValidExpirationDate(
                            cardValidity.substring(0, 2),
                            cardValidity.substring(3, 5)
                        ))
                    ) {
                        binding.invalidCardValidity.visibility = View.VISIBLE
                        binding.textView7.text = "Invalid card validity"
                    }else{
                        binding.invalidCardValidity.visibility = View.GONE
                    }
                }catch (e : Exception){
                    binding.invalidCardValidity.visibility = View.VISIBLE
                    binding.textView7.text = "Invalid card validity"
                }
//                Toast.makeText(requireContext(), "Lost the focus", Toast.LENGTH_LONG).show()
            }
        })
        binding.editTextCardCVV.setOnFocusChangeListener(OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                try {
                    val cardCVV = binding.editTextCardCVV.text.toString()
                    if (!isValidCVC(cardCVV.toInt())) {
                        binding.invalidCVV.visibility = View.VISIBLE
                        binding.textView8.text = "Invalid CVV"
                    }else{
                        binding.invalidCVV.visibility = View.GONE
                    }
                } catch (e : Exception){
                    binding.invalidCVV.visibility = View.VISIBLE
                    binding.textView8.text = "Invalid CVV"
                }
            }
        })
        binding.editTextNameOnCard.setOnFocusChangeListener(OnFocusChangeListener{view,hasFocus ->
            if(hasFocus){
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }else{
                if(binding.editTextNameOnCard.text.isNullOrEmpty()){
                    isNameOnCardValid = false
                    binding.nameOnCardErrorLayout.visibility = View.VISIBLE
                }else{
                    isNameOnCardValid = true
                    binding.nameOnCardErrorLayout.visibility = View.GONE
                }
            }
        })
        return binding.root
    }

    fun isValidExpirationDate(inputExpMonth : String, inputExpYear: String) : Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        Log.d("date details","Current Month = $currentMonth, Current year =  $currentYear, inputExpMonth = $inputExpMonth, inputExpYear = $inputExpYear")
        val isValidMonthRange =
            ((inputExpMonth.toInt() >= currentMonth))
        val isValidYearValue = (inputExpYear.toInt() > 0)
        val isValidYearLength = (inputExpYear.length == 2)

        val isMonthValid = (inputExpMonth.toInt() in 1..12)

        val isFutureYear = (("20"+inputExpYear).toInt() >= currentYear)
        val isSameYear_FutureOrCurrentMonth =
            ((inputExpYear.toInt() == currentYear) && (inputExpMonth.toInt() >= currentMonth))

        val result = ((isValidMonthRange && isValidYearLength && isValidYearValue) &&
                (isFutureYear || isSameYear_FutureOrCurrentMonth) && isMonthValid)
        if(!result)
            proceedButtonIsEnabled.value = false

        return result
    }
    fun isValidCVC(inputCVC : Int) : Boolean {
        val stringInputCVC = inputCVC.toString()
        val result : Boolean = ((stringInputCVC.length >= 3) &&
                (stringInputCVC.length <= 4))

        if(!result)
            proceedButtonIsEnabled.value = false

        return result
    }
    private fun isValidCardNumberByLuhn(stringInputCardNumber: String): Boolean {
        var sum = 0
        var isSecondDigit = false

        for (i in stringInputCardNumber.length - 1 downTo 0) {
            var d = stringInputCardNumber[i] - '0'

            if (isSecondDigit) {
                d = d * 2
            }

            sum += d / 10
            sum += d % 10

            isSecondDigit = !isSecondDigit
        }

        val result : Boolean = ((sum % 10) == 0)

        if(!result)
            Log.d("Invalid by luhn","here")

        if(!result)
            proceedButtonIsEnabled.value = false



        return result
    }

    private fun isValidCardNumberLength(inputCardNumber: String) : Boolean {
        val result : Boolean =
            ((inputCardNumber.length>= 15) &&
                    (inputCardNumber.length<= 16))
        Log.d("isValidCardNumberLength",result.toString()+" "+inputCardNumber.length)
        return result
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
//                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }

            if (bottomSheetBehavior == null)
                Log.d("bottomSheetBehavior is null", "check here")


            val screenHeight = resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.7 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

//        // Adjust the height of the bottom sheet content view
//        val layoutParams = bottomSheetContent.layoutParams
//        layoutParams.height = desiredHeight
//        bottomSheetContent.layoutParams = layoutParams
            bottomSheetBehavior?.maxHeight = desiredHeight
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
        binding.ll1InvalidCardNumber.visibility = View.GONE
        binding.invalidCardValidity.visibility = View.GONE
        binding.invalidCVV.visibility = View.GONE
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
        token = sharedPreferences.getString("token","empty")
        Log.d("data fetched from sharedPreferences",token.toString())
        successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")
        Log.d("success screen path fetched from sharedPreferences",successScreenFullReferencePath.toString())
    }
    private fun updateTransactionIDInSharedPreferences(transactionIdArg : String) {
        editor.putString("transactionId", transactionIdArg)
        editor.apply()
    }


    fun postRequest(context: Context) {
        Log.d("postRequestCalled", System.currentTimeMillis().toString())
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
            // Billing Address
            val billingAddressObject = JSONObject().apply {
                put("address1", "delivery address for the delivery")
                put("address2", "delivery")
                put("address3", JSONObject.NULL)
                put("city", "Saharanpur")
                put("countryCode", "IN")
                put("countryName", "India")
                put("postalCode", "247554")
                put("state", "Uttar Pradesh")
            }
            put("billingAddress", billingAddressObject)

            // Browser Data

            // Get the IP address

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
                put("ipAddress", "121.12.23.44")
                put("colorDepth", 24) // Example value
                put("javaEnabled", true) // Example value
                put("timeZoneOffSet", 330) // Example value
            }
            put("browserData", browserData)

            // Instrument Details
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
            put("instrumentDetails", instrumentDetailsObject)
            // Shopper
            val shopperObject = JSONObject().apply {
                val deliveryAddressObject = JSONObject().apply {

                    put("address1", sharedPreferences.getString("address1","null"))
                    put("address2", sharedPreferences.getString("address2","null"))
                    put("address3", sharedPreferences.getString("address3","null"))
                    put("city", sharedPreferences.getString("city","null"))
                    put("countryCode", sharedPreferences.getString("countryCode","null"))
                    put("countryName", sharedPreferences.getString("countryName","null"))
                    put("postalCode", sharedPreferences.getString("postalCode","null"))
                    put("state", sharedPreferences.getString("state","null"))

                }


                put("deliveryAddress", deliveryAddressObject)
                put("email", sharedPreferences.getString("email","null"))
                put("firstName", sharedPreferences.getString("firstName","null"))
                if(sharedPreferences.getString("gender","null") == "null")
                    put("gender", JSONObject.NULL)
                else
                    put("gender",sharedPreferences.getString("gender","null"))
                put("lastName", sharedPreferences.getString("lastName","null"))
                put("phoneNumber", sharedPreferences.getString("phoneNumber","null"))
                put("uniqueReference", sharedPreferences.getString("uniqueReference","null"))
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
                    val reason = response.getJSONObject("status").getString("reason")
                    transactionId = response.getString("transactionId").toString()
                    updateTransactionIDInSharedPreferences(transactionId!!)

                    Log.d("status and reason",status+" due to "+reason)
                    var url = ""

                    if (status.contains("Rejected", ignoreCase = true)) {
                        Log.d("reason check","here")
                        getStatusReasonFromResponse(response.toString())

                    }else{
                        val url = response
                            .getJSONArray("actions")
                            .getJSONObject(0)
                            .getString("url")
                        Log.d("url is fetched",url)

                        if (status.contains("Approved", ignoreCase = true)) {
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(parentFragmentManager, "PaymentStatusBottomSheetWithDetails")
                            dismissAndMakeButtonsOfMainBottomSheetEnabled()
                        } else {
//                            val intent = Intent(requireContext(), OTPScreenWebView::class.java)
//                            intent.putExtra("url", url)
//                            intent.putExtra("token", token)
//                            startActivity(intent)

                            val bottomSheet = ForceTestPaymentBottomSheet()
                            bottomSheet.show(parentFragmentManager,"ForcedTestPaymentFromCard")
                        }
                    // Assuming there's only one action, change index if needed
                    }
                } catch (e: JSONException) {
                    Log.d("status check error",e.toString())
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    binding.ll1InvalidCardNumber.visibility = View.VISIBLE
                    binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
                    getMessageForFieldErrorItems(errorResponse)
                    hideLoadingInButton()
                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
                    Log.d("Error message", errorMessage)
                    if (errorMessage.contains("Session is no longer accepting the payment as payment is already completed",ignoreCase = true)){
                        binding.textView4.text = "Payment is already done"
                    }
                }

            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = token.toString()
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
    private fun removeSpaces(stringWithSpaces : String) : String{
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
            e.printStackTrace()
        }
        return null
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
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
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
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
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
        Log.d("Request Body", jsonStr)
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
            Log.d("errorMessage", errorMessage)

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
        }
    }


    fun getStatusReasonFromResponse(response: String) {
        val jsonObject = JSONObject(response)
        Log.d("Reason 123","statusReasonCheck")

        // Extract status object
        val statusObject = jsonObject.getJSONObject("status")

        // Extract status reason
        val statusReason = statusObject.getString("reason")
        Log.d("Reason xyz",statusReason)

        if (statusReason.contains("Invalid Card Expiry", ignoreCase = true)) {
            binding.invalidCardValidity.visibility = View.VISIBLE
            binding.textView7.text = "Invalid Validity"
        } else if (statusReason.contains("Invalid CVV", ignoreCase = true)) {
            binding.invalidCVV.visibility = View.VISIBLE
            binding.textView8.text = "Invalid CVV"
        }
    }
    companion object {

    }
}