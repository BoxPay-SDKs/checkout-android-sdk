package com.example.tray

import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.FragmentUPITimerBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONException

class UPITimerBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentUPITimerBottomSheetBinding
    private lateinit var countdownTimer: CountDownTimer
    private lateinit var requestQueue: RequestQueue
    private var token : String ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestQueue = Volley.newRequestQueue(requireContext())
        arguments?.let {
            token = it.getString("token")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentUPITimerBottomSheetBinding.inflate(layoutInflater,container,false)
        binding.circularProgressBar.startAngle=90f
        binding.cancelPaymentTextView.setOnClickListener(){
            dismiss()
        }
        startTimer()
        startTimerForAPICalls()

        return binding.root
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
                binding.circularProgressBar.progress = progress*1.0f
// or with animation // =1s

// Set Progress Max
                binding.circularProgressBar.progressMax = 100f
            }

            override fun onFinish() {
                // Handle onFinish event if needed
                binding.progressTextView.text = "00:00"
                binding.circularProgressBar.progressMax = 0f
            }
        }

        countdownTimer.start()
    }
    private fun startTimerForAPICalls() {
        countdownTimer = object : CountDownTimer(300000, 2000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update TextView with the remaining time
                fetchStatusAndReason("https://test-apis.boxpay.tech/v0/checkout/sessions/1db55c0b-a8c3-4aa7-90d6-5ae6c483f715/status")
            }

            override fun onFinish() {
                // Handle onFinish event if needed

            }
        }

        countdownTimer.start()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? AddUPIID)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }
    private fun fetchStatusAndReason(url: String) {
        Log.d("fetching called correctly","Fine")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val status = response.getString("status")
                    val statusReason = response.getString("statusReason")

                    // Do something with status and statusReason
                    // For example, log them
                    Log.d("Status", status)
                    Log.d("Status Reason", statusReason)

                    // Check if status is success, if yes, dismiss the bottom sheet
                    if (statusReason.contains("Received by BoxPay for processing",ignoreCase = true) || statusReason.contains("Approved by PSP",ignoreCase = true) || status.contains("PAID",ignoreCase = true)) {
                        val bottomSheet = PaymentStatusBottomSheet()
                        bottomSheet.show(parentFragmentManager,"SuccessBottomSheet")
                    }else if(status.contains("PENDING",ignoreCase = true)) {
                        //do nothing
                    }else if(status.contains("EXPIRED",ignoreCase = true)){

                    }else if(status.contains("PROCESSING",ignoreCase = true)){

                    }
                    else if(status.contains("FAILED",ignoreCase = true)){

                    }
//                    }else{
//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(parentFragmentManager,"FailureBottomSheet")
//                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { error ->
            Log.d("Error here",error.toString())
            error.printStackTrace()

            // Handle errors here
        }
        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }


    companion object {
        fun newInstance(data: String?): UPITimerBottomSheet {
            val fragment = UPITimerBottomSheet()
            val args = Bundle()
            args.putString("token", data)
            fragment.arguments = args
            return fragment
        }
    }
}