package com.boxpay.checkout.sdk

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.composeScreens.components.SavedAddressShimmerScreen
import com.boxpay.checkout.sdk.composeScreens.model.Address
import com.boxpay.checkout.sdk.composeScreens.screen.SavedAddressesScreen
import com.boxpay.checkout.sdk.databinding.FragmentChooseEmiOptionBinding
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.boxpay.checkout.sdk.utils.handleException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

internal class SavedAddressBottomSheet : BottomSheetDialogFragment(), UpdateMainBottomSheetInterface {
    private lateinit var binding: FragmentChooseEmiOptionBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private lateinit var requestQueue: RequestQueue
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var Base_Session_API_URL: String
    private var token: String? = null
    private var customerShopperToken: String? = null
    private var uniqueRef: String? = null
    var countryCode: Pair<String, String>? = null
    private var isLoading = true
    private val _addressList: MutableStateFlow<List<Address>?> = MutableStateFlow(null)
    private val addressList: StateFlow<List<Address>?> get() = _addressList
    private var showName = false
    private var showEmail = false
    private var isPANEditable = true
    private var isDOBEditable = true
    private var showPAN = false
    private var showDOB = false
    private var showShipping = false
    private var showPhone = false
    private var isNameEditable = true
    private var isPhoneEditable = true
    private var isEmailEditable = true

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
            val percentageOfScreenHeight = 0.95 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    if (!isLoading) {
                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                    }
                    true
                } else {
                    // Allow dialog to be dismissed if loader is not active
                    false
                }
            }

            dialog.setCancelable(!binding.boxPayLogoLottieAnimation.isVisible)

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

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requestQueue = Volley.newRequestQueue(context)
        binding = FragmentChooseEmiOptionBinding.inflate(layoutInflater, container, false)


        requestQueue = Volley.newRequestQueue(context)

        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val screenHeight = requireContext().resources.displayMetrics.heightPixels
        val percentageOfScreenHeight = 0.90 // 70%
        val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()
        val jsonString = readJsonFromAssets(requireContext(), "countryCodes.json")
        val countryCodeJson = JSONObject(jsonString)


        bottomSheetBehavior?.maxHeight = desiredHeight
        binding.textView.text = "Your Addresses"
        val baseUrl = sharedPreferences.getString("baseUrl", "null")

        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"
        fetchTransactionDetailsFromSharedPreferences()

        lifecycleScope.launchWhenStarted {
            addressList.collectLatest { list ->
                try {
                    binding.composeView.setContent {
                        if (isLoading) {
                            SavedAddressShimmerScreen()
                        } else {
                            SavedAddressesScreen(
                                onClickAddNewAddress = {
                                    onClickAddOrEditAddress(true)
                                },
                                addressList = list ?: emptyList(),
                                onClickEditAddress = {address ->
                                    val nameParts = address?.name?.split(" ")
                                    if (nameParts != null) {
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

                                        editor.putString("firstName", firstName)
                                        editor.putString("lastName", lastName)
                                    }

                                    editor.putString("address1", address?.address1)
                                    editor.putString("address2", address?.address2)
                                    editor.putString("city", address?.city)
                                    editor.putString("state", address?.state)
                                    editor.putString("countryCode", address?.countryCode)
                                    editor.putString("postalCode", address?.postalCode)
                                    editor.putString("email", address?.email)
                                    editor.putString("phoneNumber", address?.phoneNumber)
                                    editor.putString("labelType",address?.labelType)
                                    editor.putString("labelName",address?.labelName)

                                    editor.apply()
                                    countryCode = getCountryName(
                                        countryCodeJson,
                                        if (sharedPreferences.getString("phoneNumber","")?.contains('+') == true) {
                                            sharedPreferences.getString("phoneNumber","") ?: ""
                                        } else {
                                            "+" + sharedPreferences.getString("phoneNumber","")
                                        }
                                    )

                                    val confirmPhoneNumber = sharedPreferences.getString("phoneNumber", "")
                                        ?.removePrefix(countryCode?.second ?: "")
                                    editor.putString("phoneNumber", confirmPhoneNumber)
                                    editor.putString("phoneCode", countryCode?.second)
                                    editor.putString("countryName", countryCode?.first)
                                    editor.apply()
                                    onClickAddOrEditAddress(false)
                                },
                                onClickBack = { dismissAndMakeButtonsOfMainBottomSheetEnabled() },
                                showLoadingInButton = false,
                                onClickDeleteAddress = {
                                    deleteSavedAddress(it)
                                },
                                onClickSetDefault = { address ->
                                    val nameParts = address?.name?.split(" ")
                                    if (nameParts != null) {
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

                                        editor.putString("firstName", firstName)
                                        editor.putString("lastName", lastName)
                                    }

                                    editor.putString("address1", address?.address1)
                                    editor.putString("address2", address?.address2)
                                    editor.putString("city", address?.city)
                                    editor.putString("state", address?.state)
                                    editor.putString("countryCode", address?.countryCode)
                                    editor.putString("postalCode", address?.postalCode)
                                    editor.putString("email", address?.email)
                                    editor.putString("phoneNumber", address?.phoneNumber)
                                    editor.putString("labelType",address?.labelType)
                                    editor.putString("labelName",address?.labelName)

                                    editor.apply()

                                    val mainBottomSheetFragment =
                                        parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
                                    mainBottomSheetFragment?.updateBottomSheet()
                                    dismiss()
                                },
                                selectedTextColor = androidx.compose.ui.graphics.Color(
                                    Color.parseColor(
                                        sharedPreferences.getString(
                                            "buttonTextColor",
                                            "#ffffff"
                                        )
                                    )
                                ),
                                selectedCtaColor = androidx.compose.ui.graphics.Color(
                                    Color.parseColor(
                                        sharedPreferences.getString(
                                            "primaryButtonColor",
                                            "#000000"
                                        )
                                    )
                                ),
                                alreadySavedAddressPostal = sharedPreferences.getString("postalCode","") ?: ""
                            )
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
        return binding.root
    }

    private fun fetchAddressDetails() {
        val url = "${Base_Session_API_URL}${token}/shoppers/${uniqueRef}/addresses"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll =
            object : JsonArrayRequest(Request.Method.GET, url, null, Response.Listener { response ->
                try {
                    val gson = Gson()
                    val addressListType = object : TypeToken<List<Address>>() {}.type
                    _addressList.value = gson.fromJson(response.toString(), addressListType)
                    if (_addressList.value?.isEmpty() == true) {
                        println("======address list ====${_addressList.value}")
                    }
                    isLoading = false
                    hideLoader()
                } catch (e: Exception) {
                    context?.let {
                        handleException(
                            it,
                            e.message ?: "",
                            token ?: "",
                            Base_Session_API_URL,
                            "Saved Address Screen fetching the api details"
                        )
                    }
                }
            }, Response.ErrorListener {
                hideLoader()
                // no op
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Session $customerShopperToken"
                    return headers
                }
            }
        queue.add(jsonObjectAll)
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        token = sharedPreferences.getString("token", "empty")
        uniqueRef = sharedPreferences.getString("uniqueReference", null)
        customerShopperToken = sharedPreferences.getString("shopperToken", "")
        fetchAddressDetails()
    }

    private fun deleteSavedAddress(addressRef: String) {
        showLoadingState()
        val url = "${Base_Session_API_URL}${token}/shoppers/${uniqueRef}/addresses/$addressRef"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll =
            object : JsonObjectRequest(Request.Method.DELETE, url, null, Response.Listener {
                fetchAddressDetails()
            }, Response.ErrorListener { error ->
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Toast.makeText(context, errorResponse, Toast.LENGTH_LONG).show()
                    hideLoader()
                }
            }) {
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
        binding.constraintLayout123.visibility = View.GONE
        binding.composeView.visibility = View.VISIBLE
    }

    private fun showLoadingState() {
        binding.boxPayLogoLottieAnimation.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.constraintLayout123.visibility = View.VISIBLE
        binding.loadingRelativeLayout.visibility = View.VISIBLE
        binding.composeView.visibility = View.GONE
    }

    fun setAddressViewAndEditSettings(
        viewName: Boolean,
        viewEmail: Boolean,
        viewPhone: Boolean,
        viewShipping: Boolean,
        editPan: Boolean,
        editDob: Boolean,
        viewPan: Boolean,
        viewDob: Boolean,
        editName: Boolean,
        editPhone: Boolean,
        editEmail: Boolean
    ) {
        this.showDOB = viewDob
        this.showPAN = viewPan
        this.showName = viewName
        this.showEmail = viewEmail
        this.showPhone = viewPhone
        this.showShipping = viewShipping
        this.isPANEditable = editPan
        this.isDOBEditable = editDob
        this.isNameEditable = editName
        this.isPhoneEditable = editPhone
        this.isEmailEditable = editEmail
    }

    private fun onClickAddOrEditAddress(isFirstTime: Boolean) {
        val bottomSheet = DeliveryAddressBottomSheet.newInstance(
            this,
            isFirstTime,
            showName,
            showPhone,
            showEmail,
            showPAN,
            showDOB,
            showShipping,
            isNameEditable,
            isPhoneEditable,
            isEmailEditable,
            isPANEditable,
            isDOBEditable
        )
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            bottomSheet.show(parentFragmentManager, "DeliveryAddressBottomSheet")
        }
    }

    fun dismissCurrentBottomSheet() {
        dismiss()
    }

    override fun updateBottomSheet() {
        showLoadingState()
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.updateBottomSheet()
        fetchAddressDetails()
    }

    private fun readJsonFromAssets(context: Context, fileName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = inputStream.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun getCountryName(
        countryCodeJson: JSONObject,
        phoneNumber: String
    ): Pair<String, String> {
        var fullName = ""
        var code = ""
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            if (phoneNumber.startsWith(countryDetails.getString("isdCode"))) {
                code = countryDetails.getString("isdCode")
                fullName = countryDetails.getString("fullName")
            }
        }
        return Pair(fullName, code)
    }
}