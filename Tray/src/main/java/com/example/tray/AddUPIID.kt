package com.example.tray

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.FragmentAddUPIIDBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone


internal class AddUPIID : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddUPIIDBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var overlayViewCurrentBottomSheet: View? = null
    private val Base_Session_API_URL = "https://test-apis.boxpay.tech/v0/checkout/sessions/"
    private var token: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private var successScreenFullReferencePath: String? = null
    private var userVPA: String? = null
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var editor : SharedPreferences.Editor
    private var transactionId : String ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddUPIIDBinding.inflate(inflater, container, false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()


        var checked = false
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.progressBar.visibility = View.INVISIBLE
        Log.d("Timezone", TimeZone.getDefault().id)
        binding.imageView3.setOnClickListener() {
            if (!checked) {
                binding.imageView3.setImageResource(R.drawable.checkbox)
                checked = true
            } else {
                binding.imageView3.setImageResource(0)
                checked = false
            }
        }





        fetchTransactionDetailsFromSharedPreferences()



        //---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        //testing purpose

        binding.textView.setOnClickListener(){
            val bottomSheet = OTPBottomSheet()
            bottomSheet.show(parentFragmentManager,"OTPBottomSheet")
        }
        //---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------










        binding.backButton.setOnClickListener() {
            dismissAndMakeButtonsOfMainBottomSheetEnabled()
        }
        binding.proceedButton.isEnabled = false

        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("beforeTextChanged", s.toString())
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                Log.d("onTextChanged", s.toString())
                if (textNow.isNotBlank()) {
                    binding.proceedButtonRelativeLayout.isEnabled = true
                    binding.proceedButton.isEnabled = true
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
                    binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
                    binding.textView6.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.white
                        )
                    )
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                Log.d("afterTextChanged", s.toString())
                if (textNow.isBlank()) {
                    binding.proceedButtonRelativeLayout.isEnabled = false
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                    binding.ll1InvalidUPI.visibility = View.GONE
                }
            }

        })
        binding.ll1InvalidUPI.visibility = View.GONE

        binding.proceedButton.setOnClickListener() {
            userVPA = binding.editTextText.text.toString()
            closeKeyboard(this)
            postRequest(requireContext(), userVPA!!)
            showLoadingInButton()
        }






        return binding.root
    }

    private fun updateTransactionIDInSharedPreferences(transactionIdArg : String) {
        editor.putString("transactionId", transactionIdArg)
        editor.apply()
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        token = sharedPreferences.getString("token", "empty")
        Log.d("data fetched from sharedPreferences", token.toString())
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
        Log.d(
            "success screen path fetched from sharedPreferences",
            successScreenFullReferencePath.toString()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment = parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
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
            dialog.setCancelable(false)



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
//        binding.editTextText.requestFocus()
    }

//    private fun fetchOTPAutomatically(){
//        val smsReceiver = otpFetcher(requireContext())
//        smsReceiver.onReceive()
//    }



    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }

    private fun showOverlayInCurrentBottomSheet() {
        // Create a semi-transparent overlay view
        overlayViewCurrentBottomSheet = View(requireContext())
        overlayViewCurrentBottomSheet?.setBackgroundColor(Color.parseColor("#80000000")) // Adjust color and transparency as needed

        // Add overlay view directly to the root view of the BottomSheet
        binding.root.addView(
            overlayViewCurrentBottomSheet,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    public fun removeOverlayFromCurrentBottomSheet() {
        overlayViewCurrentBottomSheet?.let {
            // Remove the overlay view directly from the root view
            binding.root.removeView(it)
        }
    }

    private fun postRequest(context: Context, userVPA: String) {
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
                put("ipAddress", sharedPreferences.getString("ipAddress","null"))
                put("colorDepth", 24) // Example value
                put("javaEnabled", true) // Example value
                put("timeZoneOffSet", 330) // Example value
            }
            put("browserData", browserData)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/collect")

                val upiObject = JSONObject().apply {
                    put("shopperVpa", userVPA) // Replace with the actual shopper VPA value
                }
                put("upi", upiObject)
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

            Log.d("abcdefgh","null")
            logJsonObject(shopperObject)


            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                // Handle response
                // Log.d("Response of Successful Post API call", response.toString())

                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)

                openUPITimerBottomSheet()
                hideLoadingInButton()


                logJsonObject(response)
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    binding.ll1InvalidUPI.visibility = View.VISIBLE
                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
                    Log.d("Error message", errorMessage)
                    if (errorMessage.contains(
                            "Session is no longer accepting the payment as payment is already completed",
                            ignoreCase = true
                        )
                    ) {
                        binding.textView4.text = "Payment is already done"
                    } else {
                        binding.textView4.text = "Invalid UPI"
                    }
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

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body UPI", jsonStr)
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
        val rotateAnimation =
            ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 3000 // Set the duration of the rotation in milliseconds
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE // Set to repeat indefinitely
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

    private fun openUPITimerBottomSheet() {
        val bottomSheetFragment = UPITimerBottomSheet.newInstance(userVPA)
        bottomSheetFragment.show(parentFragmentManager, "UPITimerBottomSheet")
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
    private fun closeKeyboard(fragment: Fragment) {
        val activity = fragment.activity
        val view = fragment.view
        if (activity != null && view != null) {
            val imm = ContextCompat.getSystemService(activity, InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    companion object {

    }

    private fun launchSuccessScreen(context: Context) {
        try {
            val intent = Intent().apply {
                setClassName(
                    context.packageName,
                    "com.example.AndroidCheckOutSDK.SuccessScreen"
                )
                // You may need to adjust the package name and class name accordingly
                // if SuccessScreenActivity is in a different package or has a different name
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.d("Exception in launching success screen : ", e.toString())
            // Handle the exception if the activity cannot be launched
            e.printStackTrace()
        }
    }
}