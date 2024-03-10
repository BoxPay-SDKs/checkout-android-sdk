package com.example.tray


import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.OverlayViewModel
import com.example.tray.adapters.OrderSummaryItemsAdapter
import com.example.tray.databinding.FragmentMainBottomSheetBinding
import com.example.tray.dataclasses.WalletDataClass
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject

internal class MainBottomSheet : BottomSheetDialogFragment() {
    private var overlayViewMainBottomSheet: View? = null
    private lateinit var binding: FragmentMainBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private val overlayViewModel: OverlayViewModel by activityViewModels()
    private var overlayViewCurrentBottomSheet: View? = null
    private var token: String? = null
    private var successScreenFullReferencePath: String? = null
    private var UPIAppsAndPackageMap: MutableMap<String, String> = mutableMapOf()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var i = 1
    private var transactionAmount: String? = null
    private var upiAvailable = false
    private var upiCollectMethod = false
    private var upiIntentMethod = false
    private var cardsMethod = false
    private var walletMethods = false
    private var netBankingMethods = false
    private var overLayPresent = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            token = it.getString("token")
//            successScreenFullReferencePath = it.getString("successScreenFullReferencePath")
//        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Notify ViewModel to hide the overlay when dismissed
        Log.d("Overlay", "Bottom sheet dismissed")
        overlayViewModel.setShowOverlay(false)
        super.onDismiss(dialog)
    }

    private fun getAllInstalledApps(packageManager: PackageManager) {
        Log.d("getAllInstalledApps", "here")
        val apps = packageManager.getInstalledApplications(PackageManager.GET_GIDS)

        for (app in apps) {
            val appName = packageManager.getApplicationLabel(app).toString()
            Log.d("all apps", "allApps $appName")

            // Check if the app supports UPI transactions
            val upiIntent = Intent(Intent.ACTION_VIEW)
            upiIntent.data = Uri.parse("upi://pay")
            upiIntent.setPackage(app.packageName)
            val upiApps = packageManager.queryIntentActivities(upiIntent, 0)

            if (appName == "PhonePe") {
                i++;
                Log.d("UPI App", appName)
                Log.d("UPI App Package Name", app.packageName)

                UPIAppsAndPackageMap[appName] = app.packageName
            }

            // If the app can handle the UPI intent, it's a UPI app
            if (!upiApps.isEmpty()) {
                i++;
                Log.d("UPI App", appName)
                Log.d("UPI App Package Name", app.packageName)

                UPIAppsAndPackageMap[appName] = app.packageName
            }

            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                // apps with launcher intent
                if (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
                    // updated system apps
                } else if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    // system apps
                } else {
                    // user installed apps
                }
            }
        }
    }

    private fun launchUPIPayment(context: Context, packageName: String) {
        // UPI payment data
        val uri = Uri.parse("upi://pay")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        intent.putExtra("pa", "7986266095@paytm") // UPI ID of the recipient
        intent.putExtra("pn", "Piyush Sharma") // Name of the recipient
        intent.putExtra("mc", "hK3JrVc6ys") // Merchant code
        intent.putExtra("tr", "123456789") // Transaction ID
        intent.putExtra("tn", "Test Transaction") // Transaction note
        intent.putExtra("am", "10.00") // Transaction amount
        intent.putExtra("cu", "INR") // Currency code

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Handle case where UPI app is not installed or could not handle the intent
            Toast.makeText(context, "UPI app not found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMainBottomSheetBinding.inflate(inflater, container, false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT



        fetchTransactionDetailsFromSharedPreferences()
        overlayViewModel.showOverlay.observe(this, Observer { showOverlay ->
            if (showOverlay) {
                addOverlayToActivity()
            } else {
                removeOverlayFromActivity()
            }
        })
        overlayViewModel.setShowOverlay(true)





        hidePriceBreakUp()
        getAndSetOrderDetails()



        fetchAllPaymentMethods()
        val packageManager = requireContext().packageManager
        getAllInstalledApps(packageManager)


        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val resultCode = result.resultCode
                val data = result.data
                Log.d("data of activityResultLauncher", data.toString())
                Log.d("successScreenReference", successScreenFullReferencePath!!)
                if (resultCode == Activity.RESULT_OK) {
                    val bottomSheet = PaymentStatusBottomSheet()
                    bottomSheet.show(parentFragmentManager, "Payment Success Screen")
                } else {
                    val bottomSheet = PaymentFailureScreen()
                    bottomSheet.show(parentFragmentManager, "Payment Failure Screen")
                }
                Log.d("Making payusing any other upi apps enabled","here")
                binding.payUsingAnyUPIConstraint.isEnabled = true
            }

        updateTransactionAmountInSharedPreferences("₹" + transactionAmount.toString())

        showUPIOptions()


        val items = mutableListOf(
            "Truly Madly Monthly Plan"
        )
        val prices = mutableListOf(
            "₹1697.00"
        )
        val images = mutableListOf(
            R.drawable.truly_madly_logo
        )


        val orderSummaryAdapter = OrderSummaryItemsAdapter(images, items, prices)
        binding.itemsInOrderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itemsInOrderRecyclerView.adapter = orderSummaryAdapter


        // Set click listeners
        var priceBreakUpVisible = false
        binding.orderSummaryConstraintLayout.setOnClickListener { // Toggle visibility of the price break-up card
            if (!priceBreakUpVisible) {
                showPriceBreakUp()
                priceBreakUpVisible = true
            } else {
                hidePriceBreakUp()
                priceBreakUpVisible = false
            }
        }
        binding.itemsInOrderRecyclerView.setOnClickListener() {
            //Just to preventing user from clicking here and closing the order summary
        }

        binding.backButton.setOnClickListener() {
            removeOverlayFromActivity()
            callFunctionInActivity()
            dismiss()
        }
        var upiOptionsShown = true
        binding.upiLinearLayout.setOnClickListener() {
            if (!upiOptionsShown) {
                upiOptionsShown = true
                showUPIOptions()
            } else {
                upiOptionsShown = false
                hideUPIOptions()
            }
        }

        binding.payUsingAnyUPIConstraint.setOnClickListener {

            binding.payUsingAnyUPIConstraint.isEnabled = false
            openDefaultUPIIntentBottomSheetFromAndroid()
        }


        binding.addNewUPIIDConstraint.setOnClickListener() {
            binding.addNewUPIIDConstraint.isEnabled = false
            openAddUPIIDBottomSheet()
        }

        binding.cardConstraint.setOnClickListener() {
            binding.cardConstraint.isEnabled = false
            openAddCardBottomSheet()
        }
        binding.walletConstraint.setOnClickListener() {
            binding.walletConstraint.isEnabled = false
            openWalletBottomSheet()
        }

        binding.netBankingConstraint.setOnClickListener() {
            binding.netBankingConstraint.isEnabled = false
            openNetBankingBottomSheet()
        }

        populatePopularUPIApps()

        binding.popularUPIAppsConstraint.setOnClickListener {
            // Do nothing , Just for the sake that it doesnt close the UPI options
        }


        return binding.root
    }
    private fun fetchAllPaymentMethods() {
        val url = "https://test-apis.boxpay.tech/v0/checkout/sessions/${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {

                // Get the payment methods array
                val paymentMethodsArray =
                    response.getJSONObject("configs").getJSONArray("paymentMethods")

                // Filter payment methods based on type equal to "Wallet"
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    val paymentMethodName = paymentMethod.getString("type")
                    Log.d("paymentMethodName",paymentMethodName)
                    if(paymentMethodName == "Upi"){
                        val brand = paymentMethod.getString("brand")
                        if(brand == "UpiCollect"){
                            upiCollectMethod = true
                            upiAvailable = true
                        }else if(brand == "UpiIntent"){
                            upiIntentMethod = true
                            upiAvailable = true
                        }else{
                            upiAvailable = false
                        }
                    }else if(paymentMethodName == "Card"){
                        cardsMethod = true
                    }else if(paymentMethodName == "Wallet"){
                        walletMethods = true
                    }else if(paymentMethodName == "NetBanking"){
                        netBankingMethods = true
                    }
                }
                Log.d("paymentMethods : ",upiAvailable.toString()+cardsMethod.toString())


                if(upiAvailable){
                    binding.cardView4.visibility = View.VISIBLE
                    if(upiIntentMethod){
                        binding.payUsingAnyUPIConstraint.visibility = View.VISIBLE
                    }
                    if(upiCollectMethod){
                        binding.addNewUPIIDConstraint.visibility = View.VISIBLE
                    }
                }
                if(cardsMethod){
                    binding.cardView5.visibility = View.VISIBLE
                }
                if(walletMethods){
                    binding.cardView6.visibility = View.VISIBLE
                }

                if(netBankingMethods){
                    binding.cardView7.visibility = View.VISIBLE
                }



            } catch (e: Exception) {
                Log.d("Error Occurred", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", " fetching MainBottomSheet error response: $errorResponse")
//                binding.errorField.visibility = View.VISIBLE
//                binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
//                hideLoadingInButton()
            }
        })
        queue.add(jsonObjectAll)
    }

    fun enabledButtonsForAllPaymentMethods(){
        binding.payUsingAnyUPIConstraint.isEnabled = true
        binding.addNewUPIIDConstraint.isEnabled = true
        binding.cardConstraint.isEnabled = true
        binding.walletConstraint.isEnabled = true
        binding.netBankingConstraint.isEnabled = true
    }


    private fun putTransactionDetailsInSharedPreferences() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        Log.d("token added to sharedPreferences", token.toString())
        editor.putString("successScreenFullReferencePath", successScreenFullReferencePath)
        Log.d(
            "success Screen added to sharedPreferences",
            successScreenFullReferencePath.toString()
        )
        editor.apply()
    }


    private fun populatePopularUPIApps() {
        var i = 1

//            Log.d("App in loop",appName)
        if (UPIAppsAndPackageMap.containsKey("PhonePe")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.phonepe_logo)
            textView.text = "PhonePe"
            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                overlayViewModel.setShowOverlay(false)
                launchUPIPayment(requireContext(), UPIAppsAndPackageMap["PhonePe"].toString())
            }
            Log.d("i and app inside if statement", "$i and app = PhonePe")
            i++
        }

        if (UPIAppsAndPackageMap.containsKey("GPay")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.google_pay_seeklogo)
            textView.text = "GPay"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                overlayViewModel.setShowOverlay(false)
                launchUPIPayment(requireContext(), UPIAppsAndPackageMap["GPay"].toString())
            }
            Log.d("i and app inside if statement", "$i and app = GPay")
            i++
        }
        if (UPIAppsAndPackageMap.containsKey("Paytm")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.paytm_upi_logo)
            textView.text = "Paytm"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                overlayViewModel.setShowOverlay(false)
                launchUPIPayment(requireContext(), UPIAppsAndPackageMap["Paytm"].toString())
            }
            Log.d("i and app inside if statement", "$i and app = Paytm")
            i++
        }
        if (UPIAppsAndPackageMap.containsKey("CRED")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.cred_upi_logo)
            textView.text = "CRED"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                overlayViewModel.setShowOverlay(false)
                launchUPIPayment(requireContext(), UPIAppsAndPackageMap["CRED"].toString())
            }
            Log.d("i and app inside if statement", "$i and app = CRED")
            i++
        }

        if (i == 1) {
            binding.popularUPIAppsConstraint.visibility = View.GONE
        }
    }

    private fun getPopularImageViewByNum(num: Int): ImageView {
        return when (num) {
            1 -> binding.popularUPIImageView1
            2 -> binding.popularUPIImageView2
            3 -> binding.popularUPIImageView3
            4 -> binding.popularUPIImageView4
            else -> throw IllegalArgumentException("Invalid number: $num")
        }
    }

    private fun getPopularConstraintLayoutByNum(num: Int): LinearLayout {
        return when (num) {
            1 -> binding.PopularUPILinearLayout1
            2 -> binding.PopularUPILinearLayout2
            3 -> binding.PopularUPILinearLayout3
            4 -> binding.PopularUPILinearLayout4
            else -> throw IllegalArgumentException("Invalid number: $num")
        }
    }

    private fun getPopularTextViewByNum(num: Int): TextView {
        return when (num) {
            1 -> binding.popularUPITextView1
            2 -> binding.popularUPITextView2
            3 -> binding.popularUPITextView3
            4 -> binding.popularUPITextView4
            else -> throw IllegalArgumentException("Invalid number: $num")
        }
    }

    private fun openDefaultUPIIntentBottomSheetFromAndroid() {
        val upiPaymentUri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", "9711668479@xxx")
            .appendQueryParameter("pn", "Piyush Sharma")
            .appendQueryParameter("mc", "12345678")
            .appendQueryParameter("tr", "12345678")
            .appendQueryParameter("tn", "Testing Payment")
            .appendQueryParameter("am", "10.0")
            .appendQueryParameter("cu", "INR")
            .build()

        val genericUpiPaymentIntent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_VIEW
                data = upiPaymentUri
            },
            "Pay with"
        )
        activityResultLauncher.launch(genericUpiPaymentIntent)

    }

    private fun addOverlayToActivity() {
        overLayPresent = true
        Log.d("Overlay", "overlay added......")
        // Create a translucent overlay view
        overlayViewMainBottomSheet = View(requireContext())
        overlayViewMainBottomSheet?.setBackgroundColor(Color.parseColor("#80000000")) // Adjust the color and transparency as needed

        // Get WindowManager from the parent activity's context
        val windowManager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Set layout parameters for the overlay view
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Add overlay view to the WindowManager
        windowManager.addView(overlayViewMainBottomSheet, layoutParams)
    }

    private fun removeOverlayFromActivity() {
        // Remove the overlay view from the parent activity
        overlayViewMainBottomSheet?.let {
            val windowManager =
                requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
        }
        overlayViewMainBottomSheet = null
    }

    // Method to show overlay in the first BottomSheet
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

    // Method to remove overlay from the first BottomSheet
    public fun removeOverlayFromCurrentBottomSheet() {
        overlayViewCurrentBottomSheet?.let {
            // Remove the overlay view directly from the root view
            binding.root.removeView(it)
        }
    }


    private fun showPriceBreakUp() {
        binding.itemsInOrderRecyclerView.visibility = View.VISIBLE
        binding.textView18.visibility = View.VISIBLE
        binding.ItemsPrice.visibility = View.VISIBLE
        binding.arrowIcon.animate()
            .rotation(180f)
            .setDuration(500) // Set the duration of the animation in milliseconds
            .withEndAction {
                // Code to be executed when the animation ends
            }
            .start()
    }

    private fun hidePriceBreakUp() {
        binding.itemsInOrderRecyclerView.visibility = View.GONE
        binding.textView18.visibility = View.GONE
        binding.ItemsPrice.visibility = View.GONE
        binding.arrowIcon.animate()
            .rotation(0f)
            .setDuration(500) // Set the duration of the animation in milliseconds
            .withEndAction {
                // Code to be executed when the animation ends
            }
            .start()
    }

    private fun showUPIOptions() {
        binding.upiConstraint.setBackgroundColor(Color.parseColor("#E0F1FF"))
        binding.upiOptionsLinearLayout.visibility = View.VISIBLE
        binding.textView20.typeface =
            ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        Log.d("made visible", i.toString())

        if (i > 1) {
            binding.popularUPIAppsConstraint.visibility = View.VISIBLE
        }
    }


    private fun hideUPIOptions() {
        binding.upiConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.upiOptionsLinearLayout.visibility = View.GONE
        binding.textView20.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins)
        binding.popularUPIAppsConstraint.visibility = View.GONE
        binding.imageView12.animate()
            .rotation(0f)
            .setDuration(500) // Set the duration of the animation in milliseconds
            .withEndAction {
                // Code to be executed when the animation ends
            }
            .start()
    }

    fun extractSum(prices: MutableList<String>): String {
        var finalSum = 0
        for (price in prices) {

            val numericPart = price.replace("[^0-9]".toRegex(), "")
            if (numericPart.isEmpty()) {
                return 0.toString()
            } else {
                finalSum += numericPart.toInt()
            }
        }

        val formattedSum = String.format("₹%.2f", finalSum / 100.0)
        return formattedSum

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                            dismiss()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    private fun openAddUPIIDBottomSheet() {
        val bottomSheetFragment = AddUPIID()
        bottomSheetFragment.show(parentFragmentManager, "AddUPIBottomSheet")
    }

    private fun openAddCardBottomSheet() {
        val bottomSheetFragment =
            AddCardBottomSheet()
        bottomSheetFragment.show(parentFragmentManager, "AddCardBottomSheet")
    }

    private fun openNetBankingBottomSheet() {
        val bottomSheetFragment = NetBankingBottomSheet()
        bottomSheetFragment.show(parentFragmentManager, "NetBankingBottomSheet")
    }

    private fun openWalletBottomSheet() {
        val bottomSheetFragment = WalletBottomSheet()
        bottomSheetFragment.show(parentFragmentManager, "WalletBottomSheet")
    }

    private fun getAndSetOrderDetails() {

        val url = "https://test-apis.boxpay.tech/v0/checkout/sessions/${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {

                val paymentDetailsObject = response.getJSONObject("paymentDetails")
                val orderObject = paymentDetailsObject.getJSONObject("order")
                val originalAmount = orderObject.getString("originalAmount")

                val itemsArray = orderObject.getJSONArray("items")
                var totalQuantity = 0
                for (i in 0 until itemsArray.length()) {
                    val itemObject = itemsArray.getJSONObject(i)
                    val quantity = itemObject.getInt("quantity")
                    totalQuantity += quantity
                }


                Log.d("totalQuantity", totalQuantity.toString())
                Log.d("originalAmount", originalAmount.toString())

                transactionAmount = originalAmount.toString()

                binding.unopenedTotalValue.text = "₹ ${transactionAmount}"
                binding.numberOfItems.text = "${totalQuantity} items"
                binding.ItemsPrice.text = "₹${originalAmount}"


            } catch (e: Exception) {
                Log.d("Error Occurred in MainBottomSheet", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", " fetching wallets error response: $errorResponse")
            }
        })
        queue.add(jsonObjectAll)
    }
    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token","empty")
        Log.d("data fetched from sharedPreferences",token.toString())
        successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")
        Log.d("success screen path fetched from sharedPreferences",successScreenFullReferencePath.toString())
    }


    private fun updateTransactionAmountInSharedPreferences(transactionAmountArgs: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("transactionAmount", transactionAmountArgs)
        editor.apply()
    }


    //To enable proceed button in check activity
    private fun callFunctionInActivity() {
        val activity = activity
        if (activity is Check) {
            activity.removeLoadingAndEnabledProceedButton()
        }
    }




    companion object {
//        fun newInstance(data: String?, successScreenFullReferencePath: String?): MainBottomSheet {
//            val fragment = MainBottomSheet()
//            val args = Bundle()
//            args.putString("token", data)
//            args.putString("successScreenFullReferencePath", successScreenFullReferencePath)
//            fragment.arguments = args
//            return fragment
//        }

    }
}