package com.boxpay.checkout.sdk

import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.SharedViewModel
import com.boxpay.checkout.sdk.databinding.FragmentUPITimerBottomSheetBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.generateRandomAlphanumericString
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONException

internal class OnlyUPITimerBottomSheet : BottomSheetDialogFragment(),
    CancelConfirmationBottomSheet.ConfirmationListener {
    private lateinit var binding: FragmentUPITimerBottomSheetBinding
    private lateinit var countdownTimer: CountDownTimer
    private lateinit var countdownTimerForAPI: CountDownTimer
    private lateinit var requestQueue: RequestQueue
    private var token: String? = null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var virtualPaymentAddress: String? = null
    var isBottomSheetShown = false
    private lateinit var Base_Session_API_URL: String
    private var onAloneCallBackFunction: ((PaymentResultObject) -> Unit)? = null
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
        onAloneCallBackFunction?.let {
            it(
                PaymentResultObject(
                    resultFetched = "Failed",
                    transactionIdFetched = "",
                    operationIdFetched = ""
                )
            )
        }
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
                )
            }

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
                        }

                        BottomSheetBehavior.STATE_COLLAPSED -> {
                        }

                        BottomSheetBehavior.STATE_DRAGGING -> {
                        }

                        BottomSheetBehavior.STATE_SETTLING -> {
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
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

        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        sharedViewModel.dismissBottomSheetEvent.observe(viewLifecycleOwner) { dismissed ->
            if (dismissed) {
                explicitDismiss()
                sharedViewModel.bottomSheetDismissed()
            }
        }

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

    private fun startTimer() {
        countdownTimer = object : CountDownTimer(300000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update TextView with the remaining time
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                binding.progressTextView.text = timeString
                val progress = ((millisUntilFinished.toFloat() / 300000) * 100).toInt()
                binding.circularProgressBar.progress = progress * 1.0f
            }

            override fun onFinish() {
                binding.progressTextView.text = "00:00"
                binding.circularProgressBar.progress = 0f
                binding.cancelPaymentTextView.visibility = View.GONE
                binding.retryButton.visibility = View.VISIBLE
            }
        }
        countdownTimer.start()
    }

    private fun startTimerForAPICalls() {
        var elapsedTime = 0L
        countdownTimerForAPI = object : CountDownTimer(300000, 3000) {

            override fun onTick(millisUntilFinished: Long) {
                elapsedTime += 3000
                // Update TextView with the remaining time
                if (elapsedTime >= 4000) {
                    fetchStatusAndReason("${Base_Session_API_URL}${token}/status")
                }
            }

            override fun onFinish() {
                // Handle onFinish event if needed
            }
        }

        countdownTimerForAPI.start()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismiss()
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
                        onAloneCallBackFunction?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Success",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                        countdownTimer.cancel()
                        countdownTimerForAPI.cancel()
                        dismiss()
                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        onAloneCallBackFunction?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = status,
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                    } else if (status.contains("Processing", ignoreCase = true)) {
                        onAloneCallBackFunction?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = status,
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                    } else if (status.contains(
                            "FAILED",
                            ignoreCase = true
                        ) || status.contains("REJECTED", ignoreCase = true)
                    ) {
                        onAloneCallBackFunction?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Failed",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                        countdownTimer.cancel()
                        countdownTimerForAPI.cancel()
                        dismiss()
                    }

                } catch (e: JSONException) {

                }
            },
            Response.ErrorListener {

            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                return headers
            }
        }
        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }

    companion object {
        fun newInstance(virtualPaymentAddress: String?): OnlyUPITimerBottomSheet {
            val fragment = OnlyUPITimerBottomSheet()
            val args = Bundle()
            args.putString("virtualPaymentAddress", virtualPaymentAddress)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onConfirmation() {
        dismiss()
    }

    fun setCallbackFunction(
        callback: (PaymentResultObject) -> Unit,
        baseUrl: String,
        setToken: String
    ) {
        onAloneCallBackFunction = callback
        Base_Session_API_URL = baseUrl
        token = setToken
    }
}