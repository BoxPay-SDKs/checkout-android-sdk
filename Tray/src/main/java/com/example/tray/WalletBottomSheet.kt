package com.example.tray

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SearchView
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.adapters.WalletAdapter
import com.example.tray.databinding.FragmentWalletBottomSheetBinding
import com.example.tray.dataclasses.WalletDataClass
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


class WalletBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentWalletBottomSheetBinding
    private lateinit var allWalletAdapter: WalletAdapter
    private var walletDetailsOriginal: ArrayList<WalletDataClass> = ArrayList()
    private var walletDetailsFiltered: ArrayList<WalletDataClass> = ArrayList()
    private var overlayViewCurrentBottomSheet: View? = null
    private var token: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private val Base_Session_API_URL = "https://test-apis.boxpay.tech/v0/checkout/sessions/"
    private var checkedPosition : Int ?= null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var bottomSheet: FrameLayout? = null
    private var successScreenFullReferencePath: String? = null
    var liveDataPopularWalletSelectedOrNot: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        value = false
    }
    private var popularWalletsSelected: Boolean = false
    private var popularWalletsSelectedIndex: Int = -1
    private lateinit var colorAnimation: ValueAnimator
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
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
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
    private fun unselectItemsInPopularLayout(){
        if(popularWalletsSelectedIndex != -1) {
            fetchConstraintLayout(popularWalletsSelectedIndex).setBackgroundResource(0)
        }

        popularWalletsSelected = false
    }
    private fun fetchAndUpdateApiInPopularWallets() {
        binding.apply {
            for(index in 0 until 4){
                val walletDetail = walletDetailsOriginal[index]
                val constraintLayout = fetchConstraintLayout(index)
                val imageView = when (index) {
                    0 -> popularWalletImageView1
                    1 -> popularWalletImageView2
                    2 -> popularWalletImageView3
                    3 -> popularWalletImageView4
                    else -> null
                }
                imageView?.setImageResource(walletDetail.walletImage)
                constraintLayout.setOnClickListener {
                    liveDataPopularWalletSelectedOrNot.value = true
                    if (popularWalletsSelected && popularWalletsSelectedIndex == index) {
                        // If the same constraint layout is clicked again
                        constraintLayout.setBackgroundResource(0)
                        popularWalletsSelected = false
                        proceedButtonIsEnabled.value = false
                    } else {
                        // Remove background from the previously selected constraint layout
                        if (popularWalletsSelectedIndex != -1)
                            fetchConstraintLayout(popularWalletsSelectedIndex).setBackgroundResource(0)
                        // Set background for the clicked constraint layout
                        constraintLayout.setBackgroundResource(R.drawable.selected_popular_item_bg)
                        popularWalletsSelected = true
                        proceedButtonIsEnabled.value = true
                        popularWalletsSelectedIndex = index
                    }
                }
            }
        }
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment = parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
    }

    private fun fetchConstraintLayout(num: Int): ConstraintLayout {
        Log.d("Number Called",num.toString())
        val constraintLayout: ConstraintLayout = when (num) {
            0 ->
                binding.popularWalletConstraintLayout1
            1 ->
                binding.popularWalletConstraintLayout2
            2 ->
                binding.popularWalletConstraintLayout3
            3 ->
                binding.popularWalletConstraintLayout4
            else -> throw IllegalArgumentException("Invalid number")
        }
        return constraintLayout
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(requireContext())
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWalletBottomSheetBinding.inflate(layoutInflater, container, false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        fetchTransactionDetailsFromSharedPreferences()
        walletDetailsOriginal = arrayListOf()

        if(successScreenFullReferencePath != null){
            Log.d("WalletBottomSheetReference",successScreenFullReferencePath!!)
        }

        allWalletAdapter = WalletAdapter(walletDetailsFiltered, binding.walletsRecyclerView,liveDataPopularWalletSelectedOrNot)
        binding.walletsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.walletsRecyclerView.adapter = allWalletAdapter

        binding.boxPayLogoLottieAnimation.playAnimation()
        startBackgroundAnimation()
        disableProceedButton()
        hideLoadingInButton()
        fetchWalletDetails()



        binding.searchView.setOnQueryTextListener(/*listener (comment) */ object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if(query.isEmpty()){
                    removeRecyclerViewFromBelowEditText()
                }else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterWallets(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if(newText.isEmpty()){
                    removeRecyclerViewFromBelowEditText()
                }else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterWallets(newText)
                return true
            }
        })


        binding.backButton.setOnClickListener() {
            dismissAndMakeButtonsOfMainBottomSheetEnabled()
        }
        binding.proceedButton.isEnabled = false

        binding.checkingTextView.setOnClickListener() {
            var enabled = false
            if (!enabled)
                enableProceedButton()
            else
                disableProceedButton()

            enabled = !enabled
        }
        proceedButtonIsEnabled.observe(this, Observer { enableProceedButton ->
            if (enableProceedButton) {
                enableProceedButton()
            } else {
                disableProceedButton()
            }
        })
        allWalletAdapter.checkPositionLiveData.observe(this, Observer { checkPositionObserved ->
            if(checkPositionObserved == null){
                disableProceedButton()
            }else{
                enableProceedButton()
                checkedPosition = checkPositionObserved
            }
        })
        binding.proceedButton.setOnClickListener(){
            showLoadingInButton()
            var walletInstrumentTypeValue = ""
            if(!!liveDataPopularWalletSelectedOrNot.value!!) {
                walletInstrumentTypeValue =
                    walletDetailsOriginal[popularWalletsSelectedIndex].instrumentTypeValue
            }else{
                walletInstrumentTypeValue =
                    walletDetailsFiltered[checkedPosition!!].instrumentTypeValue
            }
            Log.d("Selected bank is : ", walletInstrumentTypeValue)

            postRequest(requireContext(), walletInstrumentTypeValue)
        }
        liveDataPopularWalletSelectedOrNot.observe(this, Observer {
            if(it){
                allWalletAdapter.deselectSelectedItem()
            }else{
                unselectItemsInPopularLayout()
            }
        })


        return binding.root
    }
    private fun startBackgroundAnimation() {
        val colorStart = resources.getColor(R.color.colorStart)
        val colorEnd = resources.getColor(R.color.colorEnd)

        colorAnimation = createColorAnimation(colorStart, colorEnd)
        colorAnimation.start()
    }

    private fun createColorAnimation(startColor: Int, endColor: Int): ValueAnimator {

        val layouts = Array<ConstraintLayout?>(4) { null }
        layouts[0] = binding.popularWalletConstraintLayout1
        layouts[1] = binding.popularWalletConstraintLayout2
        layouts[2] = binding.popularWalletConstraintLayout3
        layouts[3] = binding.popularWalletConstraintLayout4
        return ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor).apply {
            duration = 500 // duration in milliseconds
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                // Update the background color of all layouts
                layouts.forEach { layout ->
                    layout?.setBackgroundColor(animator.animatedValue as Int)
                }
            }
        }
    }

    private fun fetchWalletDetails(){
        val url = "https://test-apis.boxpay.tech/v0/checkout/sessions/${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {
                val jsonObject = response

                // Get the payment methods array
                val paymentMethodsArray = jsonObject.getJSONObject("configs").getJSONArray("paymentMethods")

                // Filter payment methods based on type equal to "Wallet"
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    if (paymentMethod.getString("type") == "Wallet") {
                        val walletName = paymentMethod.getString("title")
                        val walletImage = R.drawable.wallet_sample_logo
                        val walletBrand = paymentMethod.getString("brand")
                        val walletInstrumentTypeValue = paymentMethod.getString("instrumentTypeValue")
                        walletDetailsOriginal.add(WalletDataClass(walletName,walletImage,walletBrand,walletInstrumentTypeValue))
                    }
                }

                // Print the filtered wallet payment methods
                showAllWallets()
                fetchAndUpdateApiInPopularWallets()
                removeLoadingScreenState()


            } catch (e: Exception) {
                Log.d("Error Occured", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", " fetching wallets error response: $errorResponse")
                binding.errorField.visibility = View.VISIBLE
                binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
                hideLoadingInButton()
            }
        })
        queue.add(jsonObjectAll)
    }
    private fun applyLoadingScreenState(){

    }
    private fun removeLoadingScreenState(){
        Log.d("removeLoadingScreenState","called")
        binding.walletsRecyclerView.visibility = View.VISIBLE
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.popularWalletConstraintLayout1.setBackgroundResource(0)
        binding.popularWalletConstraintLayout2.setBackgroundResource(0)
        binding.popularWalletConstraintLayout3.setBackgroundResource(0)
        binding.popularWalletConstraintLayout4.setBackgroundResource(0)
        colorAnimation.cancel()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        colorAnimation.cancel()
        super.onDismiss(dialog)
    }

    private fun filterWallets(query: String?) {
        walletDetailsFiltered.clear()
        for (wallet in walletDetailsOriginal) {
            if (query.toString().isBlank() || query.toString().isBlank()) {
                showAllWallets()
            } else if (wallet.walletName.contains(query.toString(), ignoreCase = true)) {
                walletDetailsFiltered.add(WalletDataClass(wallet.walletName, wallet.walletImage,wallet.walletBrand,wallet.instrumentTypeValue))
            }
        }




        allWalletAdapter.notifyDataSetChanged()
    }

    fun showAllWallets() {
        walletDetailsFiltered.clear()
        for (bank in walletDetailsOriginal) {
            walletDetailsFiltered.add(bank)
        }
        allWalletAdapter.notifyDataSetChanged()
    }

    fun makeRecyclerViewJustBelowEditText(){

        Log.d("View will be gone now","working fine")
        binding.textView19.visibility = View.GONE
        binding.textView24.visibility = View.GONE
        binding.linearLayout2.visibility = View.GONE
    }

    fun removeRecyclerViewFromBelowEditText(){
        Log.d("View will be gone now","working fine")
        binding.textView19.visibility = View.VISIBLE
        binding.textView24.visibility = View.VISIBLE
        binding.linearLayout2.visibility = View.VISIBLE

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
    fun postRequest(context: Context, instrumentTypeValue : String) {
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
                put("type", instrumentTypeValue)

                val tokenObject = JSONObject().apply {
                    put("token", token) // Replace with the actual shopper VPA value
                }
                put("wallet", tokenObject)
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
                    logJsonObject(response)

                    // Retrieve the "actions" array
                    val actionsArray = jsonObject.getJSONArray("actions")
                    val status = jsonObject.getJSONObject("status").getString("status")
                    var url = ""
                    // Loop through the actions array to find the URL
                    for (i in 0 until actionsArray.length()) {
                        val actionObject = actionsArray.getJSONObject(i)
                        url = actionObject.getString("url")
                        // Do something with the URL
                        Log.d("url and status", url+"\n"+status)
                    }


                    if(status.equals("Approved")) {
                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(parentFragmentManager,"PaymentSuccessfulWithDetailsBottomSheet")
                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                    }else{
                        val intent = Intent(requireContext(), OTPScreenWebView::class.java)
                        intent.putExtra("url", url)
                        startActivity(intent)
                    }

                } catch (e: JSONException) {
                    binding.errorField.visibility = View.VISIBLE
                    binding.textView4.text = e.toString()
                    e.printStackTrace()
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    binding.errorField.visibility = View.VISIBLE
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
    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Wallet", jsonStr)
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

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token","empty")
        Log.d("data fetched from sharedPreferences",token.toString())
        successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")
        Log.d("success screen path fetched from sharedPreferences",successScreenFullReferencePath.toString())
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
    companion object {

    }
}