package com.example.tray

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.FragmentAddCardBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


class AddCardBottomSheet : BottomSheetDialogFragment() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentAddCardBottomSheetBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.progressBar.visibility = View.INVISIBLE
        proceedButtonIsEnabled.observe(this, Observer { enableProceedButton ->
            if (enableProceedButton) {
                enableProceedButton()
            } else {
                disableProceedButton()
            }
        })
        proceedButtonIsEnabled.value = false

        var checked = false
        binding.progressBar.visibility = View.INVISIBLE
        binding.ll1InvalidUPI.visibility = View.GONE
        binding.invalidCardNumber.visibility = View.GONE
        binding.invalidCVV.visibility = View.GONE
        binding.imageView3.setOnClickListener() {
            if (!checked) {
                binding.imageView3.setImageResource(R.drawable.checkbox)
                checked = true
            } else {
                binding.imageView3.setImageResource(0)
                checked = false
            }
        }

        binding.proceedButton.setOnClickListener() {


            binding.textView6.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
            val rotateAnimation = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
            rotateAnimation.duration = 3000 // Set the duration of the rotation in milliseconds
            rotateAnimation.repeatCount = ObjectAnimator.INFINITE // Set to repeat indefinitely
            binding.proceedButton.isEnabled = false

            rotateAnimation.start()

            Handler(Looper.getMainLooper()).postDelayed({
                binding.progressBar.visibility = View.INVISIBLE
                binding.ll1InvalidUPI.visibility = View.VISIBLE
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
            }, 3000)
        }


        ////JUST FOR CHECKING PURPOSE....................................................................................................................................................................................................................................................................................................................................................................................................................................

        var enabled = false
        binding.textView2.setOnClickListener() {
            if (!enabled)
                enableProceedButton()
            else
                disableProceedButton()

            enabled = !enabled
        }
        var errorsEnabled = false
        binding.imageView3.setOnClickListener() {

            if (!errorsEnabled) {
                giveErrors()
                //Useful
                binding.imageView3.setImageResource(R.drawable.checkbox)
            } else {
                removeErrors()
                binding.imageView3.setImageResource(0)
            }

            errorsEnabled = !errorsEnabled
        }


        //....................................................................................................................................................................................................................................................................................................................................................................................................................................


        binding.imageView2.setOnClickListener() {
            dismiss()
        }

        binding.proceedButton.isEnabled = false
        binding.editTextText.filters = arrayOf(InputFilter.LengthFilter(19))


        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                if (textNow.isNotBlank()) {
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }

                binding.editTextText.removeTextChangedListener(this)

                val text = s.toString().replace("\\s".toRegex(), "")
                val formattedText = formatCardNumber(text)

                binding.editTextText.setText(formattedText)
                binding.editTextText.setSelection(formattedText.length)

                binding.editTextText.addTextChangedListener(this)


            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()

                if (textNow.isBlank()) {
                    binding.proceedButtonRelativeLayout.isEnabled = false
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                    binding.ll1InvalidUPI.visibility = View.GONE
                }
            }

        })


        // Set InputFilter to limit the length and add a slash after every 2 digits
        binding.editTextCardValidity.filters = arrayOf(InputFilter.LengthFilter(7))

        // Set TextWatcher to add slashes dynamically as the user types
        binding.editTextCardValidity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTextCardValidity.removeTextChangedListener(this)

                val text = s.toString().replace("/", "")
                val formattedText = formatMMYY(text)

                binding.editTextCardValidity.setText(formattedText)
                binding.editTextCardValidity.setSelection(formattedText.length)

                binding.editTextCardValidity.addTextChangedListener(this)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editTextNameOnCard.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }

            override fun afterTextChanged(s: Editable?) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }

        })

        binding.editTextNameOnCard.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("beforeTextChanged",s.toString())
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                Log.d("onTextChanged",s.toString())
                if(textNow.isNotBlank()){
                    enableProceedButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                Log.d("afterTextChanged",s.toString())
                if(textNow.isBlank()){
                    binding.proceedButtonRelativeLayout.isEnabled = false
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                    binding.ll1InvalidUPI.visibility = View.GONE
                }
            }

        })


        binding.editTextCardCVV.setTransformationMethod(AsteriskPasswordTransformationMethod())



        val originalCVV = StringBuilder()

        binding.proceedButton.setOnClickListener() {
            cardNumber = deformatCardNumber(binding.editTextText.text.toString())
            Log.d("card number", cardNumber!!)
            cardExpiryYYYY_MM = addDashInsteadOfSlash(binding.editTextCardValidity.text.toString())
            Log.d("card expiryr", cardExpiryYYYY_MM!!)
            cvv = binding.editTextCardCVV.text.toString()
            Log.d("card cvv", cvv!!)
            cardHolderName = binding.editTextNameOnCard.text.toString()
            Log.d("card holder name", cardHolderName!!)

            postRequest(requireContext())
            showLoadingInButton()
        }

        return binding.root
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
            bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
//                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }
        }
        return dialog
    }

    private fun removeErrors() {
        binding.ll1InvalidUPI.visibility = View.GONE
        binding.invalidCardNumber.visibility = View.GONE
        binding.invalidCVV.visibility = View.GONE
    }

    private fun giveErrors() {
        binding.ll1InvalidUPI.visibility = View.VISIBLE
        binding.invalidCardNumber.visibility = View.VISIBLE
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
                    put("address1", "delivery address for the delivery")
                    put("address2", "delivery")
                    put("address3", JSONObject.NULL)
                    put("city", "Saharanpur")
                    put("countryCode", "IN")
                    put("countryName", "India")
                    put("postalCode", "247554")
                    put("state", "Uttar Pradesh")
                }
                put("deliveryAddress", deliveryAddressObject)
                put("email", "test123@gmail.com")
                put("firstName", "test")
                put("gender", JSONObject.NULL)
                put("lastName", "last")
                put("phoneNumber", "919656262256")
                put("uniqueReference", "x123y")
            }
            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                // Handle response
                logJsonObject(response)
                hideLoadingInButton()

                try {
                    // Parse the JSON response
                    val jsonObject = response

                    // Retrieve the "actions" array
                    val actionsArray = jsonObject.getJSONArray("actions")
                    var url = ""
                    // Loop through the actions array to find the URL
                    for (i in 0 until actionsArray.length()) {
                        val actionObject = actionsArray.getJSONObject(i)
                        url = actionObject.getString("url")
                        // Do something with the URL
                        Log.d("URL", url)
                    }


                    val intent = Intent(requireContext(),OTPScreenWebView :: class.java)
                    intent.putExtra("url", url)
                    startActivity(intent)

                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    binding.ll1InvalidUPI.visibility = View.VISIBLE
                    binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
                    hideLoadingInButton()
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
        val mm = date.substring(0, 2)
        val yyyy = "20"+date.substring(3, 5)

        return yyyy+ "-" + mm
    }

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body", jsonStr)
    }

    companion object {
        fun newInstance(data: String?): AddCardBottomSheet {
            val fragment = AddCardBottomSheet()
            val args = Bundle()
            args.putString("token", data)
            fragment.arguments = args
            return fragment
        }
    }
}