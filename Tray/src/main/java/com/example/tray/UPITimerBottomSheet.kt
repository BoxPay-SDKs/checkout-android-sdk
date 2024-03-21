package com.example.tray

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.SharedViewModel
import com.example.tray.databinding.FragmentUPITimerBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONException
import org.json.JSONObject

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
    val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestQueue = Volley.newRequestQueue(requireContext())
        arguments?.let {
            virtualPaymentAddress = it.getString("virtualPaymentAddress")
        }
    }
    fun explicitDismiss(){
        Log.d("cancel confirmation bottom sheet","explicit dismiss called")
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
                Log.d("onResume called", "not back")
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

            if (bottomSheetBehavior == null)
                Log.d("bottomSheetBehavior is null", "check here")


            val screenHeight = resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.7 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

//        // Adjust the height of the bottom sheet content view
//        val layoutParams = bottomSheetContent.layoutParams
//        layoutParams.height = desiredHeight
//        bottomSheetContent.layoutParams = layoutParams
//            bottomSheetBehavior?.maxHeight = desiredHeight
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentUPITimerBottomSheetBinding.inflate(layoutInflater, container, false)
        sharedViewModel.dismissBottomSheetEvent.observe(viewLifecycleOwner) { dismissed ->
            if (dismissed) {
                explicitDismiss()
                sharedViewModel.bottomSheetDismissed()
            }
        }
        fetchTransactionDetailsFromSharedPreferences()

        binding.UPIIDTextView.text = "UPI ID : ${virtualPaymentAddress}"

        binding.circularProgressBar.startAngle = 90f
        binding.cancelPaymentTextView.setOnClickListener() {
            val bottomsheet = CancelConfirmationBottomSheet()
            bottomsheet.show(parentFragmentManager,"CancellationConfirmation")
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
        Log.d("data fetched from sharedPreferences", token.toString())
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
        Log.d(
            "success screen path fetched from sharedPreferences",
            successScreenFullReferencePath.toString()
        )
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
                fetchStatusAndReason("https://test-apis.boxpay.tech/v0/checkout/sessions/${token}/status")
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
    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Fetch Status", jsonStr)
    }

    private fun fetchStatusAndReason(url: String) {
        Log.d("fetching function called correctly", "Fine")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    logJsonObject(response)
                    val status = response.getString("status")
                    val statusReason = response.getString("statusReason")

                    // Do something with status and statusReason
                    // For example, log them
                    Log.d("Status", status)
                    Log.d("Status Reason", statusReason)



                    // Check if status is success, if yes, dismiss the bottom sheet
                    if (statusReason.contains(
                            "Received by BoxPay for processing",
                            ignoreCase = true
                        ) || statusReason.contains(
                            "Approved by PSP",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {
//                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
//                        bottomSheet.show(parentFragmentManager, "SuccessBottomSheetWithDetails")
                        val callback =  SingletonClass.getInstance().getYourObject()
                        if(callback == null){
                            Log.d("call back is null","Success")
                        }else{
                            callback.onPaymentResult("Success")
                        }
                        countdownTimer.cancel()
                        countdownTimerForAPI.cancel()
                        dismiss()
                    } else if (status.contains("PENDING", ignoreCase = true)) {

                    } else if (status.contains("EXPIRED", ignoreCase = true)) {
//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(parentFragmentManager, "Payment Failure")

//                        val callback =  SingletonClass.getInstance().getYourObject()
//                        if(callback == null){
//                            Log.d("call back is null","failed")
//                        }else{
//                            callback.onPaymentResult("Failure")
//                        }

//                        countdownTimer.cancel()
//                        countdownTimerForAPI.cancel()
//                        dismiss()

                    } else if (status.contains("PROCESSING", ignoreCase = true)) {

                    } else if (status.contains("FAILED", ignoreCase = true)) {
//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(parentFragmentManager, "Payment Failure")


                        val callback =  SingletonClass.getInstance().getYourObject()
                        if(callback == null){
                            Log.d("call back is null","failed")
                        }
                        else{
                            callback.onPaymentResult("Failure")
                        }
                        countdownTimer.cancel()
                        countdownTimerForAPI.cancel()
                        dismiss()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { error ->
            Log.d("Error here", error.toString())
            error.printStackTrace()
        }
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
        Log.d("parent called successfully","onConfirmation")
        dismiss()
    }
}