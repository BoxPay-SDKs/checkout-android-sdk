package com.boxpay.checkout.sdk

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.SharedViewModel
import com.boxpay.checkout.sdk.ViewModels.SingletonForDismissMainSheet
import com.boxpay.checkout.sdk.databinding.FragmentUPITimerBottomSheetBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONException

internal class UPITimerBottomSheet : BottomSheetDialogFragment(),
    CancelConfirmationBottomSheet.ConfirmationListener {
    private lateinit var binding: FragmentUPITimerBottomSheetBinding
    private lateinit var countdownTimer: CountDownTimer
    private lateinit var countdownTimerForAPI: CountDownTimer
    private lateinit var requestQueue: RequestQueue
    private var token: String? = null
    private var successScreenFullReferencePath: String? = null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var virtualPaymentAddress: String? = null
    var isBottomSheetShown = false
    private lateinit var Base_Session_API_URL: String
    val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestQueue = Volley.newRequestQueue(requireContext())
        arguments?.let {
            virtualPaymentAddress = it.getString("virtualPaymentAddress")
        }
    }

    fun explicitDismiss() {
        countdownTimer.cancel()
        countdownTimerForAPI.cancel()
        dismiss()
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && !isBottomSheetShown) {
                val bottomSheet = CancelConfirmationBottomSheet()
                bottomSheet.show(parentFragmentManager, "CancelConfirmationBottomSheet")
                isBottomSheetShown = true
                true
            } else {
                isBottomSheetShown = false
                false
            }
        }
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


            val screenHeight = resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.8 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

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

//        // Adjust the height of the bottom sheet content view
//        val layoutParams = bottomSheetContent.layoutParams
//        layoutParams.height = desiredHeight
//        bottomSheetContent.layoutParams = layoutParams

            bottomSheetBehavior?.maxHeight = desiredHeight
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUPITimerBottomSheetBinding.inflate(layoutInflater, container, false)
        sharedViewModel.dismissBottomSheetEvent.observe(viewLifecycleOwner) { dismissed ->
            if (dismissed) {
                explicitDismiss()
                sharedViewModel.bottomSheetDismissed()
            }
        }


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }


        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()


        val environmentFetched = sharedPreferences.getString("environment", "null")
        Base_Session_API_URL = "https://${environmentFetched}apis.boxpay.tech/v0/checkout/sessions/"

        fetchTransactionDetailsFromSharedPreferences()

        binding.UPIIDTextView.text = "UPI ID : ${virtualPaymentAddress}"

        binding.circularProgressBar.startAngle = 90f
        binding.cancelPaymentTextView.setOnClickListener() {
            val bottomsheet = CancelConfirmationBottomSheet()
            bottomsheet.show(parentFragmentManager, "CancellationConfirmation")
        }
        startTimer()
        startTimerForAPICalls()

        var goneOrVisible = false

        binding.textView.setOnClickListener() {
            if (goneOrVisible) {
                binding.retryButton.visibility = View.VISIBLE
            } else {
                binding.retryButton.visibility = View.GONE
            }
            goneOrVisible = !goneOrVisible
        }

        binding.retryButton.setOnClickListener() {
            countdownTimer.cancel()
            countdownTimerForAPI.cancel()
            dismiss()
        }

        return binding.root
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    private fun startTimer() {
        countdownTimer = object : CountDownTimer(300000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update TextView with the remaining time
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                binding.progressTextView.text = timeString

                // Update ProgressBar
                val progress = ((millisUntilFinished.toFloat() / 300000) * 100).toInt()
                binding.circularProgressBar.progress = progress * 1.0f
//                binding.circularProgressBar.progressMax = 100f
            }

            override fun onFinish() {
                // Handle onFinish event if needed
                binding.progressTextView.text = "00:00"

//                val bottomSheet = PaymentFailureScreen
                binding.circularProgressBar.progress = 0f
//                bottomSheet.show(parentFragmentManager,"Payment Failed due to timeout")

                binding.cancelPaymentTextView.visibility = View.GONE
                binding.retryButton.visibility = View.VISIBLE
            }
        }
        countdownTimer.start()
    }

    private fun startTimerForAPICalls() {
        countdownTimerForAPI = object : CountDownTimer(300000, 3000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update TextView with the remaining time
                fetchStatusAndReason("${Base_Session_API_URL}${token}/status")
            }

            override fun onFinish() {
                // Handle onFinish event if needed
            }
        }

        countdownTimerForAPI.start()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        super.onDismiss(dialog)
        dismiss()
    }

    private fun fetchStatusAndReason(url: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val status = response.getString("status")
                    val statusReason = response.getString("statusReason")
                    val transactionId = response.getString("transactionId")

                    // Check if status is success, if yes, dismiss the bottom sheet
                    if (status.contains(
                            "Approved",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {
                        editor.putString("status", "Success")
                        editor.apply()
                        countdownTimer.cancel()
                        countdownTimerForAPI.cancel()
                        val callback = SingletonClass.getInstance().getYourObject()
                        val callbackForDismissing =
                            SingletonForDismissMainSheet.getInstance().getYourObject()
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

                        dismiss()
                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status", "RequiresAction")
                        editor.apply()
                    } else if (status.contains("Processing", ignoreCase = true)) {
                        editor.putString("status", "Posted")
                        editor.apply()
                    } else if (status.contains("FAILED", ignoreCase = true)) {
                        editor.putString("status", "Failed")
                        editor.apply()
                        countdownTimer.cancel()
                        countdownTimerForAPI.cancel()
                        val callback =
                            FailureScreenCallBackSingletonClass.getInstance().getYourObject()
                        if(callback!=null) {
                            callback.openFailureScreen()
                        }
                        dismiss()

                    }
                    editor.apply()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { error ->
            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", "Detailed error response: $errorResponse")
            }
            // Handle errors here
        }
        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }

    companion object {
        fun newInstance(virtualPaymentAddress: String?): UPITimerBottomSheet {
            val fragment = UPITimerBottomSheet()
            val args = Bundle()
            args.putString("virtualPaymentAddress", virtualPaymentAddress)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onConfirmation() {
        dismiss()
    }
}