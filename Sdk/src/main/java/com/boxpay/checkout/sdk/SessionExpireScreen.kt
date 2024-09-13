package com.boxpay.checkout.sdk

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.boxpay.checkout.sdk.databinding.FragmentSessionExpireBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


internal class SessionExpireScreen(val function: () -> Unit = {}) :
    BottomSheetDialogFragment() {
    private lateinit var binding: FragmentSessionExpireBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        // Inflate the layout for this fragment
        binding = FragmentSessionExpireBinding.inflate(layoutInflater, container, false)
        binding.retryButton.setOnClickListener() {
            val callback =  SingletonClass.getInstance().getYourObject()
            if(callback != null){
                val transactionId = sharedPreferences.getString("transactionId","").toString()
                val operationId = sharedPreferences.getString("operationId","").toString()
                callback.onPaymentResult(PaymentResultObject("Success",transactionId,operationId))

                val mainBottomSheetFragment = parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
                mainBottomSheetFragment?.dismissTheSheetAfterSuccess()
                dismiss()
            }
        }
        return binding.root
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
            val percentageOfScreenHeight = 0.7 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

            bottomSheetBehavior?.maxHeight = desiredHeight
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
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
                        }

                        BottomSheetBehavior.STATE_SETTLING -> {
                            // The BottomSheet is settling
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
                            //Hidden
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }
}