package com.boxpay.checkout.sdk

import FailureScreenSharedViewModel
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import coil.decode.SvgDecoder
import coil.load
import coil.transform.CircleCropTransformation
import com.airbnb.lottie.LottieDrawable
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.SingletonForDismissMainSheet
import com.boxpay.checkout.sdk.adapters.NetbankingBanksAdapter
import com.boxpay.checkout.sdk.databinding.FragmentNetBankingBottomSheetBinding
import com.boxpay.checkout.sdk.dataclasses.NetbankingDataClass
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonCenterAlign
import com.skydoves.balloon.createBalloon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random


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
    val progressBarVisible = MutableLiveData<Boolean>()

    private lateinit var Base_Session_API_URL: String
    private lateinit var requestQueue: RequestQueue
    private var job: Job? = null
    var popularBanksSelected: Boolean = false
    private var popularBanksSelectedIndex: Int = -1
    private lateinit var colorAnimation: ValueAnimator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var transactionId: String? = null
    private var shippingEnabled: Boolean = false


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

            val screenHeight = requireContext().resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.9 // 70%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight

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

            bottomSheetBehavior?.maxHeight = desiredHeight

            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

            dialog.setCancelable(!binding.progressBar.isVisible && !binding.loaderCardView.isVisible)

            dialog.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.progressBar.isVisible) {
                    // Prevent dialog from being dismissed if loader is active
                    true
                } else {
                    // Allow dialog to be dismissed if loader is not active
                    false
                }
            }

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

                        var bankImage = paymentMethod.getString("logoUrl")
                        if (bankImage.startsWith("/assets")) {
                            bankImage =
                                "https://checkout.boxpay.in" + paymentMethod.getString("logoUrl")
                        }
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
                        banksDetailsOriginal =
                            ArrayList(banksDetailsOriginal.sortedBy { it.bankBrand })
                    }
                }
                showAllBanks()
                removeLoadingScreenState()
                fetchAndUpdateApiInPopularBanks()

            } catch (e: Exception) {

            }

        }, { _ ->
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

    private fun removeLoadingScreenState() {
        binding.banksRecyclerView.visibility = View.VISIBLE
        binding.cardView.visibility = View.VISIBLE
        binding.loaderCardView.visibility = View.INVISIBLE
        binding.recyclerViewShimmer.visibility = View.GONE
        binding.popularBanksRelativeLayout1.setBackgroundResource(if (popularBanksSelectedIndex == 0) R.drawable.selected_popular_item_bg else R.drawable.popular_item_unselected_bg)
        binding.popularBanksRelativeLayout2.setBackgroundResource(if (popularBanksSelectedIndex == 1) R.drawable.selected_popular_item_bg else R.drawable.popular_item_unselected_bg)
        binding.popularBanksRelativeLayout3.setBackgroundResource(if (popularBanksSelectedIndex == 2) R.drawable.selected_popular_item_bg else R.drawable.popular_item_unselected_bg)
        binding.popularBanksRelativeLayout4.setBackgroundResource(if (popularBanksSelectedIndex == 3) R.drawable.selected_popular_item_bg else R.drawable.popular_item_unselected_bg)
        binding.popularBanksImageView1.visibility = View.VISIBLE
        binding.popularBanksImageView2.visibility = View.VISIBLE
        binding.popularBanksImageView3.visibility = View.VISIBLE
        binding.popularBanksImageView4.visibility = View.VISIBLE
        binding.popularBanksNameTextView4.visibility = View.VISIBLE
        binding.popularBanksNameTextView3.visibility = View.VISIBLE
        binding.popularBanksNameTextView2.visibility = View.VISIBLE
        binding.popularBanksNameTextView1.visibility = View.VISIBLE
        if (checkedPosition != null || popularBanksSelectedIndex != -1) {
            enableProceedButton()
        } else {
            disableProceedButton()
        }
    }

    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.putString("operationId", transactionIdArg)
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

        requestQueue = Volley.newRequestQueue(context)

        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val screenHeight = requireContext().resources.displayMetrics.heightPixels
        val percentageOfScreenHeight = 0.45 // 70%
        val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()


        val layoutParams = binding.nestedScrollView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.height = desiredHeight
        binding.nestedScrollView.layoutParams = layoutParams

        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"

        fetchTransactionDetailsFromSharedPreferences()

        banksDetailsOriginal = arrayListOf()
        allBanksAdapter = NetbankingBanksAdapter(
            banksDetailsFiltered,
            binding.banksRecyclerView,
            liveDataPopularBankSelectedOrNot,
            requireContext(),
            binding.searchView,
            token.toString(),
            progressBarVisible
        )
        binding.banksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.banksRecyclerView.adapter = allBanksAdapter

        if (!shippingEnabled)
            fetchBanksDetails()
        else
            callPaymentMethodRules(requireContext())


        var enabled = false

        val failureScreenSharedViewModelCallback =
            FailureScreenSharedViewModel(::failurePaymentFunction)
        FailureScreenCallBackSingletonClass.getInstance().callBackFunctions =
            failureScreenSharedViewModelCallback
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
            if (!binding.progressBar.isVisible && !binding.loaderCardView.isVisible) {
                dismissAndMakeButtonsOfMainBottomSheetEnabled()
            }
        }
        binding.proceedButton.setOnClickListener() {
            showLoadingInButton()
            var bankInstrumentTypeValue = ""
            if (!!liveDataPopularBankSelectedOrNot.value!!) {
                bankInstrumentTypeValue =
                    banksDetailsOriginal[popularBanksSelectedIndex].bankInstrumentTypeValue

                callUIAnalytics(
                    requireContext(),
                    "PAYMENT_INITIATED",
                    banksDetailsOriginal[popularBanksSelectedIndex].bankBrand,
                    "NetBanking"
                )
                checkedPosition = null
            } else {
                bankInstrumentTypeValue =
                    banksDetailsFiltered[checkedPosition!!].bankInstrumentTypeValue
                callUIAnalytics(
                    requireContext(),
                    "PAYMENT_INITIATED",
                    banksDetailsFiltered[checkedPosition!!].bankBrand,
                    "NetBanking"
                )
                popularBanksSelectedIndex = -1
            }

            binding.errorField.visibility = View.GONE

            postRequest(requireContext(), bankInstrumentTypeValue)
        }

        return binding.root
    }

    private fun callUIAnalytics(
        context: Context,
        event: String,
        paymentSubType: String,
        paymentType: String
    ) {
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
            Response.ErrorListener { /*no response handling */ }) {}.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
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
            } else if (bank.bankName.startsWith(query.toString(), ignoreCase = true)) {
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

    private fun callPaymentMethodRules(context: Context) {

        val requestQueue = Volley.newRequestQueue(context)

        val countryName = sharedPreferences.getString("countryCode", null)

        val jsonArrayRequest = object : JsonArrayRequest(
            Method.GET,
            Base_Session_API_URL + token + "/payment-methods?customerCountryCode=$countryName",
            null,
            Response.Listener { response ->
                for (i in 0 until response.length()) {
                    val paymentMethod = response.getJSONObject(i)
                    if (paymentMethod.getString("type") == "NetBanking") {
                        val bankName = paymentMethod.getString("title")

                        var bankImage = paymentMethod.getString("logoUrl")
                        if (bankImage.startsWith("/assets")) {
                            bankImage =
                                "https://checkout.boxpay.in" + paymentMethod.getString("logoUrl")
                        }

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

    fun failurePaymentFunction() {


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

                    imageView?.load(bankDetail.bankImage) {
                        decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }
                        transformations(CircleCropTransformation())
                        size(80, 80)
                    }

                    getPopularTextViewByNum(index + 1).text =
                        banksDetailsOriginal[index].bankName

                    constraintLayout.setOnClickListener {
                        if (!binding.progressBar.isVisible && !binding.loaderCardView.isVisible) {
                            val inputMethodManager =
                                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(
                                binding.searchView.windowToken,
                                0
                            )
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


        balloon.showAtCenter(constraintLayout, 0, 0, BalloonCenterAlign.BOTTOM)
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

        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {

            // Create the browserData JSON object
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
                val displayMetrics = resources.displayMetrics
                put("screenHeight", displayMetrics.heightPixels.toString())
                put("screenWidth", displayMetrics.widthPixels.toString())
                put("acceptHeader", "application/json")
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", Locale.getDefault().toString())
                put("ipAddress", sharedPreferences.getString("ipAddress", "null"))
                put("javaEnabled", true) // Example value
                put("packageId", requireActivity().packageName)
            }
            put("browserData", browserData)

            val instrumentDetailsObject = JSONObject().apply {
                put("type", bankInstrumentTypeValue)
            }
            put("instrumentDetails", instrumentDetailsObject)

            val shopperObject = JSONObject().apply {
                put("email", sharedPreferences.getString("email", null))
                put("firstName", sharedPreferences.getString("firstName", null))

                if (sharedPreferences.getString("gender", null) == null)
                    put("gender", JSONObject.NULL)
                else
                    put("gender", sharedPreferences.getString("gender", null))

                put("lastName", sharedPreferences.getString("lastName", null))
                put("phoneNumber", sharedPreferences.getString("phoneNumber", null))
                put("uniqueReference", sharedPreferences.getString("uniqueReference", null))

                if (shippingEnabled) {
                    val deliveryAddressObject = JSONObject().apply {
                        put("address1", sharedPreferences.getString("address1", null))
                        put("address2", sharedPreferences.getString("address2", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("countryCode", sharedPreferences.getString("countryCode", null))
                        put("postalCode", sharedPreferences.getString("postalCode", null))
                        put("state", sharedPreferences.getString("state", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("email", sharedPreferences.getString("email", null))
                        put("phoneNumber", sharedPreferences.getString("phoneNumber", null))
                        put("countryName", sharedPreferences.getString("countryName", null))
                    }
                    put("deliveryAddress", deliveryAddressObject)
                }
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
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->


                hideLoadingInButton()

                try {
                    logJsonObject(response)
                    // Parse the JSON response
                    transactionId = response.getString("transactionId").toString()
                    updateTransactionIDInSharedPreferences(transactionId!!)

                    // Retrieve the "actions" array
                    val status = response.getJSONObject("status").getString("status")
                    var url = ""
                    // Loop through the actions array to find the URL
                    if (status.equals("Approved")) {
                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentSuccessfulWithDetailsBottomSheet"
                        )
                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                    } else {
                        showLoadingState()
                        if (!response.isNull("actions") && response.getJSONArray("actions")
                                .length() != 0
                        ) {
                            val type =
                                response.getJSONArray("actions").getJSONObject(0)
                                    .getString("type")
                            if (status.contains("RequiresAction", ignoreCase = true)) {
                                editor.putString("status", "RequiresAction")
                            }
                            if (type.contains("html", true)) {
                                url = response
                                    .getJSONArray("actions")
                                    .getJSONObject(0)
                                    .getString("htmlPageString")
                            } else {
                                url = response
                                    .getJSONArray("actions")
                                    .getJSONObject(0)
                                    .getString("url")
                            }
                            val intent = Intent(requireContext(), OTPScreenWebView::class.java)
                            intent.putExtra("url", url)
                            intent.putExtra("type", type)
                            startFunctionCalls()
                            startActivityForResult(intent, 333)
                        } else {
                            job?.cancel()
                            removeLoadingScreenState()
                            PaymentFailureScreen(
                                errorMessage = "Please retry using other payment method or try again in sometime"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }
                    editor.apply()

                } catch (e: JSONException) {
                    binding.errorField.visibility = View.VISIBLE
                    binding.textView4.text = "Error requesting payment"
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired",true) == true) {
                        val callback = SingletonClass.getInstance().getYourObject()
                        val callbackForDismissing =
                            SingletonForDismissMainSheet.getInstance().getYourObject()
                        if (callback != null) {
                            callback.onPaymentResult(
                                PaymentResultObject(
                                    "Expired",
                                    transactionId ?: "",
                                    transactionId ?: ""
                                )
                            )
                        }
                        if (callbackForDismissing != null) {
                            callbackForDismissing.dismissFunction()
                        }
                        job?.cancel()
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        job?.cancel()
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
                hideLoadingInButton()
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

    fun dismissCurrentBottomSheet() {
        dismiss()
    }

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)

    }

    private fun enableProceedButton() {
        binding.proceedButtonRelativeLayout.isEnabled = true
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString("primaryButtonColor", "#000000")
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

    private fun disableProceedButton() {
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButton.isEnabled = false
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }

    fun hideLoadingInButton() {
        binding.progressBar.visibility = View.INVISIBLE
        progressBarVisible.value = false
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString("primaryButtonColor", "#000000")
            )
        )
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.isEnabled = true
    }

    fun showLoadingInButton() {
        binding.textView6.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        progressBarVisible.value = true
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
        }
        return null
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {


        token = sharedPreferences.getString("token", "empty")

        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    companion object {
        fun newInstance(
            shippingEnabled: Boolean
        ): NetBankingBottomSheet {
            val fragment = NetBankingBottomSheet()
            fragment.shippingEnabled = shippingEnabled
            return fragment
        }
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun fetchStatusAndReason(url: String) {

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                try {
                    val status = response.getString("status")
                    val transactionId = response.getString("transactionId").toString()

                    if (status.contains(
                            "Approved",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {
                        editor.putString("status", "Success")
                        editor.putString("amount", response.getString("amount").toString())
                        editor.putString("transactionId", transactionId)
                        editor.apply()

                        if (isAdded && isResumed && !isStateSaved) {
                            removeLoadingScreenState()
                            val callback = SingletonClass.getInstance().getYourObject()
                            val callbackForDismissing =
                                SingletonForDismissMainSheet.getInstance().getYourObject()
                            job?.cancel()
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            if (callback != null) {
                                callback.onPaymentResult(
                                    PaymentResultObject(
                                        "Success",
                                        transactionId,
                                        transactionId
                                    )
                                )
                            }
                            if (callbackForDismissing != null) {
                                callbackForDismissing.dismissFunction()
                            }
                        }

                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status", "RequiresAction")
                        editor.apply()
                    } else if (status.contains("Processing", ignoreCase = true)) {
                        editor.putString("status", "Posted")
                        editor.apply()
                    } else if (status.contains("FAILED", ignoreCase = true)) {

                        editor.putString("status", "Failed")
                        editor.apply()

                        if (isAdded && isResumed && !isStateSaved) {
                            removeLoadingScreenState()
                            job?.cancel()
                            job?.cancel()
                            job?.cancel()
                            PaymentFailureScreen(
                                errorMessage = "Please retry using other payment method or try again in sometime"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }

                } catch (e: JSONException) {

                }
            },
            Response.ErrorListener {
                // no op
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                return headers
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun startFunctionCalls() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3000)
                fetchStatusAndReason("${Base_Session_API_URL}${token}/status")
                // Delay for 5 seconds
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 333) {
            if (resultCode == Activity.RESULT_OK) {
                job?.cancel()
                removeLoadingScreenState()
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")
            }
        }
    }
    private fun showLoadingState() {
        binding.boxPayLogoLottieAnimation.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.loaderCardView.visibility = View.VISIBLE
        binding.cardView.visibility = View.GONE
        disableProceedButton()
    }
}