package com.boxpay.checkout.sdk

import FailureScreenSharedViewModel
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import coil.decode.SvgDecoder
import coil.load
import coil.transform.CircleCropTransformation
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.adapters.WalletAdapter
import com.boxpay.checkout.sdk.databinding.FragmentWalletBottomSheetBinding
import com.boxpay.checkout.sdk.dataclasses.WalletDataClass
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
import kotlin.random.Random

internal class WalletBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentWalletBottomSheetBinding
    private lateinit var allWalletAdapter: WalletAdapter
    private var walletDetailsOriginal: ArrayList<WalletDataClass> = ArrayList()
    private var walletDetailsFiltered: ArrayList<WalletDataClass> = ArrayList()
    private var overlayViewCurrentBottomSheet: View? = null
    private var token: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private var checkedPosition: Int? = null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var bottomSheet: FrameLayout? = null
    private var successScreenFullReferencePath: String? = null
    private var transactionId: String? = null
    private var shippingEnabled : Boolean = false
    var liveDataPopularWalletSelectedOrNot: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply {
            value = false
        }
    private var popularWalletsSelected: Boolean = false
    private var popularWalletsSelectedIndex: Int = -1
    private lateinit var colorAnimation: ValueAnimator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var Base_Session_API_URL : String

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
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }

            val window = d.window
            window?.apply {
                // Apply dim effect
                setDimAmount(0.5f) // 50% dimming
                setBackgroundDrawable(ColorDrawable(Color.argb(128, 0, 0, 0))) // Semi-transparent black background
            }


            val screenHeight = resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.9 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight
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

    private fun unselectItemsInPopularLayout() {
        if (popularWalletsSelectedIndex != -1) {
            fetchRelativeLayout(popularWalletsSelectedIndex).setBackgroundResource(R.drawable.popular_item_unselected_bg)
        }
        popularWalletsSelected = false
    }

    private fun getConstraintLayoutByNum(num: Int): ConstraintLayout {
        val constraintLayout: ConstraintLayout = when (num) {
            0 ->
                binding.popularWalletConstraintLayout1

            1 ->
                binding.popularWalletConstraintLayout2

            2 ->
                binding.popularWalletConstraintLayout3

            3 ->
                binding.popularWalletConstraintLayout4

            else -> throw IllegalArgumentException("Invalid number Relative layout")
        }
        return constraintLayout
    }

    private fun fetchAndUpdateApiInPopularWallets() {
        binding.apply {
            for (index in 0 until 4) {
                val constraintLayout = getConstraintLayoutByNum(index)

                if (index < walletDetailsOriginal.size) {
                    val walletDetail = walletDetailsOriginal[index]

                    val relativeLayout = fetchRelativeLayout(index)
                    val imageView = when (index) {
                        0 -> popularWalletImageView1
                        1 -> popularWalletImageView2
                        2 -> popularWalletImageView3
                        3 -> popularWalletImageView4
                        else -> null
                    }


                    imageView?.load(walletDetail.walletImage){
                        decoderFactory{result,options,_ -> SvgDecoder(result.source,options) }
                        transformations( CircleCropTransformation())
                        size(80, 80)
                    }


                    getPopularTextViewByNum(index + 1).text =
                        walletDetailsOriginal[index].walletName

                    constraintLayout.setOnClickListener {
                        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
                        liveDataPopularWalletSelectedOrNot.value = true
                        if (popularWalletsSelected && popularWalletsSelectedIndex == index) {
                            // If the same constraint layout is clicked again
                            relativeLayout.setBackgroundResource(R.drawable.popular_item_unselected_bg)
                            popularWalletsSelected = false
                            proceedButtonIsEnabled.value = false
                        } else {
                            // Remove background from the previously selected constraint layout
                            if (popularWalletsSelectedIndex != -1)
                                fetchRelativeLayout(popularWalletsSelectedIndex).setBackgroundResource(
                                    R.drawable.popular_item_unselected_bg
                                )
                            // Set background for the clicked constraint layout
                            relativeLayout.setBackgroundResource(R.drawable.selected_popular_item_bg)
                            popularWalletsSelected = true
                            proceedButtonIsEnabled.value = true
                            popularWalletsSelectedIndex = index
                        }
                    }

                    constraintLayout.setOnLongClickListener() {
                        showToolTipPopularWallets(
                            constraintLayout,
                            walletDetailsOriginal[index].walletName
                        )
                        true
                    }
                } else {
                    constraintLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun showToolTipPopularWallets(constraintLayout: ConstraintLayout, walletName: String) {
        val balloon = createBalloon(requireContext()) {
            setArrowSize(10)
            setWidthRatio(0.3f)
            setHeight(65)
            setArrowPosition(0.5f)
            setCornerRadius(4f)
            setAlpha(0.9f)
            setText(walletName)
            setTextColorResource(R.color.colorEnd)
            setBackgroundColorResource(R.color.tooltip_bg)
            setBalloonAnimation(BalloonAnimation.FADE)
            setLifecycleOwner(lifecycleOwner)
        }


        balloon.showAtCenter(constraintLayout, 0, 0, BalloonCenterAlign.BOTTOM)
        balloon.dismissWithDelay(2000L)
    }

    private fun removeLoadingScreenState() {
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.walletsRecyclerView.visibility = View.VISIBLE
        binding.popularItemRelativeLayout1.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        binding.popularItemRelativeLayout2.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        binding.popularItemRelativeLayout3.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        binding.popularItemRelativeLayout4.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        colorAnimation.cancel()
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
    }

    private fun fetchRelativeLayout(num: Int): RelativeLayout {
        val relativeLayout: RelativeLayout = when (num) {
            0 ->
                binding.popularItemRelativeLayout1

            1 ->
                binding.popularItemRelativeLayout2

            2 ->
                binding.popularItemRelativeLayout3

            3 ->
                binding.popularItemRelativeLayout4

            else -> throw IllegalArgumentException("Invalid number Relative layout")
        }
        return relativeLayout
    }


    private fun getPopularTextViewByNum(num: Int): TextView {
        return when (num) {
            1 -> binding.popularWalletsNameTextView1
            2 -> binding.popularWalletsNameTextView2
            3 -> binding.popularWalletsNameTextView3
            4 -> binding.popularWalletsNameTextView4
            else -> throw IllegalArgumentException("Invalid number: $num  TextView1234")
        }
    }
    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.putString("operationId",transactionIdArg)
        editor.apply()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWalletBottomSheetBinding.inflate(layoutInflater, container, false)


        val failureScreenSharedViewModelCallback = FailureScreenSharedViewModel(::failurePaymentFunction)
        FailureScreenCallBackSingletonClass.getInstance().callBackFunctions = failureScreenSharedViewModelCallback


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if(userAgentHeader.contains("Mobile",ignoreCase = true)){
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val screenHeight = requireContext().resources.displayMetrics.heightPixels
        val percentageOfScreenHeight = 0.45 // 70%
        val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()



        val layoutParams = binding.nestedScrollView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.height = desiredHeight
        binding.nestedScrollView.layoutParams = layoutParams


        val layoutParamsLoading = binding.loadingRelativeLayout.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsLoading.height = desiredHeight
        binding.loadingRelativeLayout.layoutParams = layoutParamsLoading

        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()


        val baseUrl = sharedPreferences.getString("baseUrl","null")

        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"

        fetchTransactionDetailsFromSharedPreferences()
        walletDetailsOriginal = arrayListOf()


        allWalletAdapter = WalletAdapter(
            walletDetailsFiltered,
            binding.walletsRecyclerView,
            liveDataPopularWalletSelectedOrNot,
            requireContext(),
            binding.searchView,
            token.toString()
        )
        binding.walletsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.walletsRecyclerView.adapter = allWalletAdapter

        binding.boxPayLogoLottieAnimation.playAnimation()
        startBackgroundAnimation()
        disableProceedButton()


        if(!shippingEnabled)
            fetchWalletDetails()
        else
            callPaymentMethodRules(requireContext())




        binding.searchView.setOnQueryTextListener(/*listener (comment) */ object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty()) {
                    removeRecyclerViewFromBelowEditText()
                } else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterWallets(query)
                disableProceedButton()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    removeRecyclerViewFromBelowEditText()
                } else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterWallets(newText)
                disableProceedButton()
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
            if (checkPositionObserved == null) {
                disableProceedButton()
            } else {
                enableProceedButton()
                checkedPosition = checkPositionObserved
            }
        })

        binding.proceedButton.setOnClickListener() {
            showLoadingInButton()
            var walletInstrumentTypeValue = ""
            if (!!liveDataPopularWalletSelectedOrNot.value!!) {
                walletInstrumentTypeValue =
                    walletDetailsOriginal[popularWalletsSelectedIndex].instrumentTypeValue
                callUIAnalytics(requireContext(),"PAYMENT_INITIATED",walletDetailsOriginal[popularWalletsSelectedIndex].walletBrand,"Wallet")
            } else {
                walletInstrumentTypeValue =
                    walletDetailsFiltered[checkedPosition!!].instrumentTypeValue
                callUIAnalytics(requireContext(),"PAYMENT_INITIATED",walletDetailsOriginal[checkedPosition!!].walletBrand,"Wallet")
            }

            binding.errorField.visibility = View.GONE

            postRequest(requireContext(), walletInstrumentTypeValue)
        }

        liveDataPopularWalletSelectedOrNot.observe(this, Observer {
            if (it) {
                allWalletAdapter.deselectSelectedItem()
            } else {
                unselectItemsInPopularLayout()
            }
        })
        binding.textView19.setOnClickListener() {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
        }


        binding.searchView.setOnCloseListener() {
            true
        }



        return binding.root
    }

    private fun startBackgroundAnimation() {
        val colorStart = resources.getColor(R.color.colorStart)
        val colorEnd = resources.getColor(R.color.colorEnd)

        colorAnimation = createColorAnimation(colorStart, colorEnd)
        colorAnimation.start()
    }
    private fun callUIAnalytics(context: Context, event: String,paymentSubType : String, paymentType : String) {
        val baseUrl = sharedPreferences.getString("baseUrl", "null")

        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("callerToken", token)
            put("uiEvent", event)

            // Create eventAttrs JSON object
            val eventAttrs = JSONObject().apply {
                put("paymentType", paymentType)
                put("paymentSubType", paymentSubType)
            }
            put("eventAttrs", eventAttrs)

            // Create browserData JSON object
            val browserData = JSONObject().apply {
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", browserLanguage)
            }
            put("browserData", browserData)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://${baseUrl}/v0/ui-analytics", requestBody,
            Response.Listener { /*no response handling */ },
            Response.ErrorListener { /*no response handling */}) {}.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }

    private fun createColorAnimation(startColor: Int, endColor: Int): ValueAnimator {

        val layouts = Array<RelativeLayout?>(4) { null }
        layouts[0] = binding.popularItemRelativeLayout1
        layouts[1] = binding.popularItemRelativeLayout2
        layouts[2] = binding.popularItemRelativeLayout3
        layouts[3] = binding.popularItemRelativeLayout4
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
    fun failurePaymentFunction(){

        // Start a coroutine with a delay of 5 seconds
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // Delay for 1 seconds

            // Code inside this block will execute after the delay
            // Code inside this block will execute after the delay
            val bottomSheet = PaymentFailureScreen()
            bottomSheet.show(parentFragmentManager, "PaymentFailureScreen")
        }

    }

    private fun fetchWalletDetails() {
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
                    if (paymentMethod.getString("type") == "Wallet") {
                        val walletName = paymentMethod.getString("title")
                        var walletImage = paymentMethod.getString("logoUrl")
                        if(walletImage.startsWith("/assets")) {
                            walletImage =
                                "https://checkout.boxpay.in" + paymentMethod.getString("logoUrl")
                        }
                        val walletBrand = paymentMethod.getString("brand")
                        val walletInstrumentTypeValue =
                            paymentMethod.getString("instrumentTypeValue")
                        walletDetailsOriginal.add(
                            WalletDataClass(
                                walletName,
                                walletImage,
                                walletBrand,
                                walletInstrumentTypeValue
                            )
                        )
                    }
                }
                walletDetailsOriginal = ArrayList(walletDetailsOriginal.sortedBy { it.walletBrand })

                // Print the filtered wallet payment methods
                showAllWallets()
                fetchAndUpdateApiInPopularWallets()
                removeLoadingScreenState()
//                startAutoScroll()


            } catch (e: Exception) {

            }

        }, { error ->

            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                binding.errorField.visibility = View.VISIBLE
                binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
                hideLoadingInButton()
            }
        })
        queue.add(jsonObjectAll)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Handle the back button press here
        // Dismiss the dialog when the back button is pressed
        dismissAndMakeButtonsOfMainBottomSheetEnabled()
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
            } else if (wallet.walletName.startsWith(query.toString(), ignoreCase = true)) {
                walletDetailsFiltered.add(
                    WalletDataClass(
                        wallet.walletName,
                        wallet.walletImage,
                        wallet.walletBrand,
                        wallet.instrumentTypeValue
                    )
                )
            }
        }

        if (walletDetailsFiltered.size == 0) {
            binding.noResultsFoundTextView.visibility = View.VISIBLE
        } else {
            binding.noResultsFoundTextView.visibility = View.GONE
        }
        allWalletAdapter.deselectSelectedItem()
        allWalletAdapter.notifyDataSetChanged()
    }

    fun showAllWallets() {
        walletDetailsFiltered.clear()
        for (bank in walletDetailsOriginal) {
            walletDetailsFiltered.add(bank)
        }
        allWalletAdapter.deselectSelectedItem()
        allWalletAdapter.notifyDataSetChanged()
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

    private fun callPaymentMethodRules(context: Context) {

        val requestQueue = Volley.newRequestQueue(context)

        val countryName = sharedPreferences.getString("countryCode",null)

        val jsonArrayRequest = object : JsonArrayRequest(
            Method.GET, Base_Session_API_URL + token+"/payment-methods?customerCountryCode=$countryName", null,
            Response.Listener { response ->
                for (i in 0 until response.length()) {
                    val paymentMethod = response.getJSONObject(i)
                    if (paymentMethod.getString("type") == "Wallet") {
                        val walletName = paymentMethod.getString("title")
                        var walletImage = paymentMethod.getString("logoUrl")
                        if(walletImage.startsWith("/assets")) {
                            walletImage =
                                "https://checkout.boxpay.in" + paymentMethod.getString("logoUrl")
                        }
                        val walletBrand = paymentMethod.getString("brand")
                        val walletInstrumentTypeValue =
                            paymentMethod.getString("instrumentTypeValue")
                        walletDetailsOriginal.add(
                            WalletDataClass(
                                walletName,
                                walletImage,
                                walletBrand,
                                walletInstrumentTypeValue
                            )
                        )
                    }
                }

                // Print the filtered wallet payment methods
                showAllWallets()
                fetchAndUpdateApiInPopularWallets()
                removeLoadingScreenState()
            },
            Response.ErrorListener { _ ->

            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                return headers
            }
        }.apply {
            val timeoutMs = 100000
            val maxRetries = 0
            val backoffMultiplier = 1.0f
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        requestQueue.add(jsonArrayRequest)
    }

    private fun postRequest(context: Context, instrumentTypeValue: String) {
        val requestQueue = Volley.newRequestQueue(context)

        val requestBody = JSONObject().apply {

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
                put("packageId",requireActivity().packageName)
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


            val shopperObject = JSONObject().apply {
                put("email", sharedPreferences.getString("email",null))
                put("firstName", sharedPreferences.getString("firstName",null))
                if(sharedPreferences.getString("gender",null) == null)
                    put("gender", JSONObject.NULL)
                else
                    put("gender",sharedPreferences.getString("gender",null))
                put("lastName", sharedPreferences.getString("lastName",null))
                put("phoneNumber", sharedPreferences.getString("phoneNumber",null))
                put("uniqueReference", sharedPreferences.getString("uniqueReference",null))

                if(shippingEnabled){
                    val deliveryAddressObject = JSONObject().apply {

                        put("address1", sharedPreferences.getString("address1", null))
                        put("address2", sharedPreferences.getString("address2", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("countryCode", sharedPreferences.getString("countryCode", null))
                        put("postalCode", sharedPreferences.getString("postalCode", null))
                        put("state", sharedPreferences.getString("state", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("email",sharedPreferences.getString("email",null))
                        put("phoneNumber",sharedPreferences.getString("phoneNumber",null))
                        put("countryName",sharedPreferences.getString("countryName",null))

                    }
                    put("deliveryAddress", deliveryAddressObject)
                }
            }

            put("shopper", shopperObject)


        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)

                hideLoadingInButton()

                try {


                    val actionsArray = response.getJSONArray("actions")
                    val status = response.getJSONObject("status").getString("status")
                    var url = ""
                    // Loop through the actions array to find the URL
                    for (i in 0 until actionsArray.length()) {
                        val actionObject = actionsArray.getJSONObject(i)
                        url = actionObject.getString("url")
                        // Do something with the URL

                    }



                    if (status.equals("Approved")) {
                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentSuccessfulWithDetailsBottomSheet"
                        )
                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                    } else {

                        if (status.contains("RequiresAction", ignoreCase = true)) {
                            editor.putString("status","RequiresAction")
                        }

//                        val intent = Intent(requireContext(), OTPScreenWebView::class.java)
//                        FailureScreenFunctionObject.failureScreenFunction = ::failurePaymentFunction
                        val intent = Intent(context, OTPScreenWebView::class.java)
                        intent.putExtra("url", url)

                        // Check if the context is not null before starting the activity
//                        context?.let { context ->
//                            intent.putExtra("url", url)
//                            val webViewActivity = OTPScreenWebView()
//                            webViewActivity.setWebViewCloseListener(requireContext()) // Pass the current BottomSheetDialogFragment as the listener
//                            context.startActivity(intent)
//                        } // Start the webViewActivity

                        startActivity(intent)
                        editor.apply()
////                        startActivity(intent)


//                        val bottomSheet = ForceTestPaymentBottomSheet()
//                        bottomSheet.show(parentFragmentManager,"ForceTestPaymentOpenByWallet")
                    }

                } catch (e: JSONException) {
                    binding.errorField.visibility = View.VISIBLE
                    binding.textView4.text = "Error requesting payment"
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")
                hideLoadingInButton()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
//                headers["X-Client-Connector-Name"] =  "Android SDK"
//                headers["X-Client-Connector-Version"] =  BuildConfig.SDK_VERSION
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

    fun dismissCurrentBottomSheet(){
        dismiss()
    }

    private fun enableProceedButton() {
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#000000")))
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(Color.parseColor(sharedPreferences.getString("buttonTextColor","#000000")))
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
        binding.proceedButtonRelativeLayout.setBackgroundColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#000000")))
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

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            // Parse the JSON string
            val jsonObject = JSONObject(response)
            // Retrieve the value associated with the "message" key
            return jsonObject.getString("message")
        } catch (e: Exception) {
            // Handle JSON parsing exception

        }
        return null
    }


    companion object {
        fun newInstance(
            shippingEnabled: Boolean
        ): WalletBottomSheet {
            val fragment = WalletBottomSheet()
            fragment.shippingEnabled = shippingEnabled
            return fragment
        }
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool : List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}
