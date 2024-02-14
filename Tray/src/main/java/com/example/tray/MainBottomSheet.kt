package com.example.tray


import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tray.ViewModels.OverlayViewModel
import com.example.tray.adapters.OrderSummaryItemsAdapter
import com.example.tray.databinding.FragmentMainBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import org.w3c.dom.Text


class MainBottomSheet : BottomSheetDialogFragment() {
    private var overlayViewMainBottomSheet: View? = null
    private lateinit var binding: FragmentMainBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private val overlayViewModel: OverlayViewModel by activityViewModels()
    private var overlayViewCurrentBottomSheet: View? = null
    private var token: String? = null
    private var successScreenFullReferencePath: String? = null
    private val UPIAppsPackageNameList: MutableList<String> = mutableListOf()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
            successScreenFullReferencePath = it.getString("successScreenFullReferencePath")
        }
    }

    override fun onStart() {
        super.onStart()


        // Observe the LiveData and show/hide overlay accordingly
        overlayViewModel.showOverlay.observe(this, Observer { showOverlay ->
            if (showOverlay) {
                addOverlayToActivity()
            } else {
                removeOverlayFromActivity()
            }
        })
        overlayViewModel.setShowOverlay(true)
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Notify ViewModel to hide the overlay when dismissed
        Log.d("Overlay", "Bottom sheet dismissed")
        overlayViewModel.setShowOverlay(false)
        super.onDismiss(dialog)
    }
    fun getAllInstalledApps(packageManager: PackageManager) {
        Log.d("getAllInstalledApps", "here")
        val apps = packageManager.getInstalledApplications(PackageManager.GET_GIDS)

        for (app in apps) {
            val appName = packageManager.getApplicationLabel(app).toString()

            // Check if the app supports UPI transactions
            val upiIntent = Intent(Intent.ACTION_VIEW)
            upiIntent.data = Uri.parse("upi://pay")
            upiIntent.setPackage(app.packageName)
            val upiApps = packageManager.queryIntentActivities(upiIntent, 0)

            // If the app can handle the UPI intent, it's a UPI app
            if (!upiApps.isEmpty()) {
                Log.d("UPI App", appName)
                Log.d("UPI App Package Name", app.packageName)
                UPIAppsPackageNameList.add(app.packageName)
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

    fun launchUPIPayment(context: Context, packageName: String) {
        // UPI payment data
        val uri = Uri.parse("upi://pay")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        intent.putExtra("pa", "7986266095@paytm") // UPI ID of the recipient
        intent.putExtra("pn", "Piyush Sharma") // Name of the recipient
        intent.putExtra("mc", "yourmerchantcode") // Merchant code
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
        hideUPIOptions()
        hidePriceBreakUp()

        dialog?.setCanceledOnTouchOutside(true)
        getAndSetOrderDetails()
        showUPIOptions()
        val packageManager = requireContext().packageManager
        getAllInstalledApps(packageManager)
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val resultCode = result.resultCode
                val data = result.data
                Log.d("data of activityResultLauncher",data.toString())
                Log.d("successScreenReference",successScreenFullReferencePath!!)
                if (resultCode == Activity.RESULT_OK) {
                    val bottomSheet = PaymentStatusBottomSheet.newInstance(token, successScreenFullReferencePath)
                    bottomSheet.show(parentFragmentManager,"Payment Success Screen")
                } else {
                    val bottomSheet = PaymentFailureScreen()
                    bottomSheet.show(parentFragmentManager,"Payment Failure Screen")
                }
            }


        val items = mutableListOf(
            "Truly Madly Monthly Plan"
        )
        val prices = mutableListOf(
            "₹599.00"
        )
        val images = mutableListOf(
            R.drawable.truly_madly_logo
        )
        val orderSummaryAdapter = OrderSummaryItemsAdapter(images, items, prices)
        binding.itemsInOrderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itemsInOrderRecyclerView.adapter = orderSummaryAdapter


        // Set click listeners
        var priceBreakUpVisible = false
        binding.arrowIcon.setOnClickListener { // Toggle visibility of the price break-up card
            if (!priceBreakUpVisible) {
                showPriceBreakUp()
                priceBreakUpVisible = true
            } else {
                hidePriceBreakUp()
                priceBreakUpVisible = false
            }
        }

        binding.imageView222.setOnClickListener() {
            dismiss()
        }
        var upiOptionsShown = false
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
//            launchUPIPayment(requireContext(),UPIAppsPackageNameList[1])

            openDefaultUPIIntentBottomSheetFromAndroid()
        }

        binding.addNewUPIIDConstraint.setOnClickListener() {
            openAddUPIIDBottomSheet()
        }

        binding.cardConstraint.setOnClickListener() {
            openAddCardBottomSheet()
        }
        binding.walletConstraint.setOnClickListener() {
            openWalletBottomSheet()
        }

        binding.netBankingConstraint.setOnClickListener() {
            openNetBankingBottomSheet()
        }

        return binding.root
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
    }

    private fun hideUPIOptions() {
        binding.upiConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.upiOptionsLinearLayout.visibility = View.GONE
        binding.textView20.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins)
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
        val bottomSheetFragment = AddUPIID.newInstance(token)
        bottomSheetFragment.show(parentFragmentManager, "AddUPIBottomSheet")
    }

    private fun openAddCardBottomSheet() {
        val bottomSheetFragment =
            AddCardBottomSheet.newInstance(token, successScreenFullReferencePath)
        bottomSheetFragment.show(parentFragmentManager, "AddCardBottomSheet")
    }

    private fun openNetBankingBottomSheet() {
        val bottomSheetFragment = NetBankingBottomSheet.newInstance(token)
        bottomSheetFragment.show(parentFragmentManager, "NetBankingBottomSheet")
    }

    private fun openWalletBottomSheet() {
        val bottomSheetFragment = WalletBottomSheet.newInstance(token)
        bottomSheetFragment.show(parentFragmentManager, "WalletBottomSheet")
    }

    private fun getAndSetOrderDetails() {
        val response = JSONObject(
            """{
        "context": {
            "countryCode": "IN",
            "legalEntity": {
                "code": "demo_merchant"
            },
            "orderId": "test12"
        },
        "paymentType": "A",
        "money": {
            "amount": "1",
            "currencyCode": "INR"
        },
        "descriptor": {
            "line1": "Some descriptor"
        },
        "billingAddress": {
            "address1": "first address line",
            "address2": "second address line",
            "city": "Faridabad",
            "state": "Haryana",
            "countryCode": "IN",
            "postalCode": "121004"
        },
        "shopper": {
            "firstName": "test",
            "lastName": "last",
            "email": "test123@gmail.com",
            "uniqueReference": "x123y",
            "phoneNumber": "",
            "deliveryAddress": {
                "address1": "first address line",
                "address2": "second address line",
                "city": "Faridabad",
                "state": "Haryana",
                "countryCode": "IN",
                "postalCode": "121004"
            }
        },
        "order": {
            "originalAmount" : 1697,
            "shippingAmount" : 500,
            "voucherCode" : "VOUCHER",
            "items": [
                {
                    "id": "test",
                    "itemName" : "test_name",
                    "description": "testProduct",
                    "quantity": 1,
                    "imageUrl" : "https://test-merchant.boxpay.tech/boxpay logo.svg",
                    "amountWithoutTax" : 699.00
                },
                {
                    "id": "test2",
                    "itemName" : "test_name2",
                    "description": "testProduct2",
                    "quantity": 2,
                    "imageUrl" : "https://test-merchant.boxpay.tech/boxpay logo.svg",
                    "amountWithoutTax" : 499.00
                }
            ]
        },
        "frontendReturnUrl": "https://www.boxpay.tech",
        "frontendBackUrl": "https://www.boxpay.tech",
        "expiryDurationSec": 7200
    }"""
        )


        val orderObject = response.getJSONObject("order")
        val originalAmount = orderObject.getDouble("originalAmount")

        val itemsArray = orderObject.getJSONArray("items")
        var totalQuantity = 0
        for (i in 0 until itemsArray.length()) {
            val itemObject = itemsArray.getJSONObject(i)
            val quantity = itemObject.getInt("quantity")
            totalQuantity += quantity
        }


        Log.d("totalQuantity", totalQuantity.toString())
        Log.d("originalAmount", originalAmount.toString())

        binding.unopenedTotalValue.text = "₹" + originalAmount.toString()
        binding.numberOfItems.text = totalQuantity.toString() + " items"
        binding.ItemsPrice.text = "₹${originalAmount.toString()}"
    }

    companion object {
        fun newInstance(data: String?, successScreenFullReferencePath: String?): MainBottomSheet {
            val fragment = MainBottomSheet()
            val args = Bundle()
            args.putString("token", data)
            args.putString("successScreenFullReferencePath", successScreenFullReferencePath)
            fragment.arguments = args
            return fragment
        }
    }
}