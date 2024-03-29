package com.example.tray

import FailureScreenSharedViewModel
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
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
import com.example.tray.adapters.NetbankingBanksAdapter
import com.example.tray.databinding.FragmentNetBankingBottomSheetBinding
import com.example.tray.dataclasses.NetbankingDataClass
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonCenterAlign
import com.skydoves.balloon.createBalloon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


internal class NetBankingBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentNetBankingBottomSheetBinding
    private lateinit var allBanksAdapter: NetbankingBanksAdapter
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var banksDetailsOriginal: ArrayList<NetbankingDataClass> = ArrayList()
    private var banksDetailsFiltered: ArrayList<NetbankingDataClass> = ArrayList()
    private var token: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private var checkedPosition: Int? = null
    private var successScreenFullReferencePath: String? = null
    var liveDataPopularBankSelectedOrNot: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply {
            value = false
        }
    private lateinit var Base_Session_API_URL : String
    var popularBanksSelected: Boolean = false
    private var popularBanksSelectedIndex: Int = -1
    private lateinit var colorAnimation: ValueAnimator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var transactionId: String? = null


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

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dismissAndMakeButtonsOfMainBottomSheetEnabled()
    }

    private fun fetchBanksDetails() {
        val url = "${Base_Session_API_URL}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {

                // Get the payment methods array
                val paymentMethodsArray =
                    response.getJSONObject("configs").getJSONArray("paymentMethods")

                // Filter payment methods based on type equal to "Wallet"
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    if (paymentMethod.getString("type") == "NetBanking") {
                        val bankName = paymentMethod.getString("title")
                        val bankImage = R.drawable.wallet_sample_logo
                        val bankBrand = paymentMethod.getString("brand")
                        val bankInstrumentTypeValue = paymentMethod.getString("instrumentTypeValue")
                        banksDetailsOriginal.add(
                            NetbankingDataClass(
                                bankName,
                                bankImage,
                                bankBrand,
                                bankInstrumentTypeValue
                            )
                        )
                    }
                }
                showAllBanks()
                removeLoadingScreenState()
                fetchAndUpdateApiInPopularBanks()

            } catch (e: Exception) {
                Log.d("Error Occured", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("error here", "RESPONSE IS $error")
            Toast.makeText(requireContext(), "Fail to get response", Toast.LENGTH_SHORT)
                .show()
        })
        queue.add(jsonObjectAll)
    }

    private fun unselectItemsInPopularLayout() {
        if (popularBanksSelectedIndex != -1) {
            fetchRelativeLayout(popularBanksSelectedIndex).setBackgroundResource(R.drawable.popular_item_unselected_bg)
        }
        popularBanksSelected = false
    }

    private fun applyLoadingScreenState() {

    }

    private fun removeLoadingScreenState() {
        Log.d("removeLoadingScreenState", "called")
        binding.banksRecyclerView.visibility = View.VISIBLE
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.popularBanksRelativeLayout1.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        binding.popularBanksRelativeLayout2.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        binding.popularBanksRelativeLayout3.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        binding.popularBanksRelativeLayout4.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        colorAnimation.cancel()
    }

    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNetBankingBottomSheetBinding.inflate(layoutInflater, container, false)

        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()


        val environmentFetched = sharedPreferences.getString("environment","null")
        Log.d("environment is $environmentFetched","Add UPI ID")
        Base_Session_API_URL = "https://${environmentFetched}-apis.boxpay.tech/v0/checkout/sessions/"

        fetchTransactionDetailsFromSharedPreferences()

        banksDetailsOriginal = arrayListOf()
        allBanksAdapter = NetbankingBanksAdapter(
            banksDetailsFiltered,
            binding.banksRecyclerView,
            liveDataPopularBankSelectedOrNot,
            requireContext(),
            binding.searchView
        )
        binding.banksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.banksRecyclerView.adapter = allBanksAdapter
        binding.boxPayLogoLottieAnimation.playAnimation()
        startBackgroundAnimation()
        fetchBanksDetails()

        if (successScreenFullReferencePath != null) {
            Log.d("NetBankingBottomSheetReference", successScreenFullReferencePath!!)
        }
        var enabled = false
        binding.checkingTextView.setOnClickListener() {
            if (!enabled)
                enableProceedButton()
            else
                disableProceedButton()

            enabled = !enabled
        }

        val failureScreenSharedViewModelCallback = FailureScreenSharedViewModel(::failurePaymentFunction)
        FailureScreenCallBackSingletonClass.getInstance().callBackFunctions = failureScreenSharedViewModelCallback
        proceedButtonIsEnabled.observe(this, Observer { enableProceedButton ->
            if (enableProceedButton) {
                enableProceedButton()
            } else {
                disableProceedButton()
            }
        })


        liveDataPopularBankSelectedOrNot.observe(this, Observer {
            if (it) {
                allBanksAdapter.deselectSelectedItem()
            } else {
                unselectItemsInPopularLayout()
            }
        })
        allBanksAdapter.checkPositionLiveData.observe(this, Observer { checkPositionObserved ->
            if (checkPositionObserved == null) {
                disableProceedButton()
            } else {
                enableProceedButton()
                checkedPosition = checkPositionObserved
            }
        })

        binding.searchView.setOnQueryTextListener(/*listener (comment) */ object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty()) {
                    removeRecyclerViewFromBelowEditText()
                } else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterBanks(query)
                disableProceedButton()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    removeRecyclerViewFromBelowEditText()
                } else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterBanks(newText)
                disableProceedButton()
                return true
            }
        })

        binding.backButton.setOnClickListener() {
            dismissAndMakeButtonsOfMainBottomSheetEnabled()
        }
        binding.proceedButton.setOnClickListener() {
            showLoadingInButton()
            var bankInstrumentTypeValue = ""
            if (!!liveDataPopularBankSelectedOrNot.value!!) {
                bankInstrumentTypeValue =
                    banksDetailsOriginal[popularBanksSelectedIndex].bankInstrumentTypeValue
            } else {
                bankInstrumentTypeValue =
                    banksDetailsFiltered[checkedPosition!!].bankInstrumentTypeValue
            }
            Log.d("Selected bank is : ", bankInstrumentTypeValue)
            binding.errorField.visibility = View.GONE

            postRequest(requireContext(), bankInstrumentTypeValue)
        }

        return binding.root
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
    }
    private fun filterBanks(query: String?) {
        banksDetailsFiltered.clear()
        for (bank in banksDetailsOriginal) {
            if (query.toString().isBlank() || query.toString().isBlank()) {
                showAllBanks()
            } else if (bank.bankName.contains(query.toString(), ignoreCase = true)) {
                banksDetailsFiltered.add(
                    NetbankingDataClass(
                        bank.bankName,
                        bank.bankImage,
                        bank.bankBrand,
                        bank.bankInstrumentTypeValue
                    )
                )
            }
        }

        if (banksDetailsFiltered.size == 0) {
            binding.noResultsFoundTextView.visibility = View.VISIBLE
        } else {
            binding.noResultsFoundTextView.visibility = View.GONE
        }
        allBanksAdapter.deselectSelectedItem()
        allBanksAdapter.notifyDataSetChanged()
    }
    fun failurePaymentFunction(){
        Log.d("Failure Screen View Model", "failurePaymentFunction")

        // Start a coroutine with a delay of 5 seconds
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // Delay for 1 seconds

            // Code inside this block will execute after the delay
            val bottomSheet = PaymentFailureScreen()
            bottomSheet.show(parentFragmentManager, "PaymentFailureScreen")
        }

    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        colorAnimation.cancel()
        super.onDismiss(dialog)
    }

    fun showAllBanks() {
        banksDetailsFiltered.clear()
        for (bank in banksDetailsOriginal) {
            banksDetailsFiltered.add(bank)
        }
        allBanksAdapter.deselectSelectedItem()
        allBanksAdapter.notifyDataSetChanged()
    }

    fun makeRecyclerViewJustBelowEditText() {
        binding.textView19.visibility = View.GONE
        binding.textView24.visibility = View.GONE
        binding.linearLayout2.visibility = View.GONE
    }


    fun removeRecyclerViewFromBelowEditText() {
        binding.textView19.visibility = View.VISIBLE
        binding.textView24.visibility = View.VISIBLE
        binding.linearLayout2.visibility = View.VISIBLE
    }

    private fun fetchAndUpdateApiInPopularBanks() {
        binding.apply {
            for (index in 0 until 4) {
                val constraintLayout = getConstraintLayoutByNum(index)

                if (index < banksDetailsOriginal.size) {
                    val bankDetail = banksDetailsOriginal[index]
                    val relativeLayout = fetchRelativeLayout(index)
                    val imageView = when (index) {
                        0 -> popularBanksImageView1
                        1 -> popularBanksImageView2
                        2 -> popularBanksImageView3
                        3 -> popularBanksImageView4
                        else -> null
                    }
                    imageView?.setImageResource(bankDetail.bankImage)

                    getPopularTextViewByNum(index + 1).text =
                        banksDetailsOriginal[index].bankName

                    constraintLayout.setOnClickListener {
                        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
                        liveDataPopularBankSelectedOrNot.value = true

                        if (popularBanksSelected && popularBanksSelectedIndex == index) {
                            // If the same constraint layout is clicked again
                            relativeLayout.setBackgroundResource(R.drawable.popular_item_unselected_bg)
                            popularBanksSelected = false
                            proceedButtonIsEnabled.value = false
                        } else {
                            // Remove background from the previously selected constraint layout
                            if (popularBanksSelectedIndex != -1)
                                fetchRelativeLayout(popularBanksSelectedIndex).setBackgroundResource(
                                    R.drawable.popular_item_unselected_bg
                                )
                            // Set background for the clicked constraint layout
                            relativeLayout.setBackgroundResource(R.drawable.selected_popular_item_bg)
                            popularBanksSelected = true
                            proceedButtonIsEnabled.value = true
                            popularBanksSelectedIndex = index

                        }
                    }

                    constraintLayout.setOnLongClickListener() {
                        showToolTipPopularBanks(
                            constraintLayout,
                            banksDetailsOriginal[index].bankName
                        )
                        true
                    }
                } else {
                    constraintLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun getConstraintLayoutByNum(num: Int): ConstraintLayout {
        val constraintLayout: ConstraintLayout = when (num) {
            0 ->
                binding.popularBanksConstraintLayout1

            1 ->
                binding.popularBanksConstraintLayout2

            2 ->
                binding.popularBanksConstraintLayout3

            3 ->
                binding.popularBanksConstraintLayout4

            else -> throw IllegalArgumentException("Invalid number Relative layout")
        }
        return constraintLayout
    }

    private fun showToolTipPopularBanks(constraintLayout: ConstraintLayout, bankName: String) {
        val balloon = createBalloon(requireContext()) {
            setArrowSize(10)
            setWidthRatio(0.3f)
            setHeight(65)
            setArrowPosition(0.5f)
            setCornerRadius(4f)
            setAlpha(0.9f)
            setText(bankName)
            setTextColorResource(R.color.colorEnd)
//                    setIconDrawable(ContextCompat.getDrawable(context, R.drawable.ic_profile))
            setBackgroundColorResource(R.color.tooltip_bg)
//                    setOnBalloonClickListener(onBalloonClickListener)
            setBalloonAnimation(BalloonAnimation.FADE)
            setLifecycleOwner(lifecycleOwner)
        }

        Log.d("long click detected", "popular Net banking")
        balloon.showAtCenter(constraintLayout, 0, 0, BalloonCenterAlign.TOP)
        balloon.dismissWithDelay(2000L)
    }

    private fun getPopularTextViewByNum(num: Int): TextView {
        return when (num) {
            1 -> binding.popularBanksNameTextView1
            2 -> binding.popularBanksNameTextView2
            3 -> binding.popularBanksNameTextView3
            4 -> binding.popularBanksNameTextView4
            else -> throw IllegalArgumentException("Invalid number: $num  TextView1234")
        }
    }

    private fun startBackgroundAnimation() {
        val colorStart = resources.getColor(R.color.colorStart)
        val colorEnd = resources.getColor(R.color.colorEnd)

        colorAnimation = createColorAnimation(colorStart, colorEnd)
        colorAnimation.start()
    }

    private fun createColorAnimation(startColor: Int, endColor: Int): ValueAnimator {

        val layouts = Array<RelativeLayout?>(4) { null }
        layouts[0] = binding.popularBanksRelativeLayout1
        layouts[1] = binding.popularBanksRelativeLayout2
        layouts[2] = binding.popularBanksRelativeLayout3
        layouts[3] = binding.popularBanksRelativeLayout4
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

    private fun fetchRelativeLayout(num: Int): RelativeLayout {
        Log.d("Number Called", num.toString())
        val relativeLayout: RelativeLayout = when (num) {
            0 ->
                binding.popularBanksRelativeLayout1

            1 ->
                binding.popularBanksRelativeLayout2

            2 ->
                binding.popularBanksRelativeLayout3

            3 ->
                binding.popularBanksRelativeLayout4

            else -> throw IllegalArgumentException("Invalid number Relative layout")
        }
        return relativeLayout
    }

    private fun postRequest(context: Context, bankInstrumentTypeValue: String) {
        Log.d("postRequestCalled", System.currentTimeMillis().toString())
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
            // Billing Address
            val billingAddressObject = JSONObject().apply {
                put("address1", sharedPreferences.getString("address1", "null"))
                put("address2", sharedPreferences.getString("address2", "null"))
                put("address3", sharedPreferences.getString("address3", "null"))
                put("city", sharedPreferences.getString("city", "null"))
                put("countryCode", sharedPreferences.getString("countryCode", "null"))
                put("countryName", sharedPreferences.getString("countryName", "null"))
                put("postalCode", sharedPreferences.getString("postalCode", "null"))
                put("state", sharedPreferences.getString("state", "null"))
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
                put("ipAddress", sharedPreferences.getString("ipAddress", "null"))
                put("colorDepth", 24) // Example value
                put("javaEnabled", true) // Example value
                put("timeZoneOffSet", 330) // Example value
            }
            put("browserData", browserData)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", bankInstrumentTypeValue)
            }
            put("instrumentDetails", instrumentDetailsObject)
            // Shopper
            val shopperObject = JSONObject().apply {
                val deliveryAddressObject = JSONObject().apply {

                    put("address1", sharedPreferences.getString("address1", "null"))
                    put("address2", sharedPreferences.getString("address2", "null"))
                    put("address3", sharedPreferences.getString("address3", "null"))
                    put("city", sharedPreferences.getString("city", "null"))
                    put("countryCode", sharedPreferences.getString("countryCode", "null"))
                    put("countryName", sharedPreferences.getString("countryName", "null"))
                    put("postalCode", sharedPreferences.getString("postalCode", "null"))
                    put("state", sharedPreferences.getString("state", "null"))

                }


                put("deliveryAddress", deliveryAddressObject)
                put("email", sharedPreferences.getString("email", "null"))
                put("firstName", sharedPreferences.getString("firstName", "null"))
                if (sharedPreferences.getString("gender", "null") == "null")
                    put("gender", JSONObject.NULL)
                else
                    put("gender", sharedPreferences.getString("gender", "null"))
                put("lastName", sharedPreferences.getString("lastName", "null"))
                put("phoneNumber", sharedPreferences.getString("phoneNumber", "null"))
                put("uniqueReference", sharedPreferences.getString("uniqueReference", "null"))
            }
            put("shopper", shopperObject)
        }
        Log.d("request Body","Netbanking")
        logJsonObject(requestBody)

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->


                hideLoadingInButton()

                try {
                    logJsonObject(response)
                    // Parse the JSON response
                    transactionId = response.getString("transactionId").toString()
                    updateTransactionIDInSharedPreferences(transactionId!!)

                    // Retrieve the "actions" array
                    val actionsArray = response.getJSONArray("actions")
                    val status = response.getJSONObject("status").getString("status")
                    var url = ""
                    // Loop through the actions array to find the URL
                    for (i in 0 until actionsArray.length()) {
                        val actionObject = actionsArray.getJSONObject(i)
                        url = actionObject.getString("url")
                        // Do something with the URL
                        Log.d("url and status", url + "\n" + status)
                    }


                    if (status.equals("Approved")) {
                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentSuccessfulWithDetailsBottomSheet"
                        )
                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                    } else {
                        val intent = Intent(requireContext(), OTPScreenWebView::class.java)
                        intent.putExtra("url", url)
                        startActivity(intent)
                    }

                } catch (e: JSONException) {
                    binding.errorField.visibility = View.VISIBLE
                    binding.textView4.text = "Error requesting payment"
                    Log.e("Error in handling response",e.toString())
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
        Log.d("Request Body Netbanking", jsonStr)
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
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
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

    companion object {

    }
}