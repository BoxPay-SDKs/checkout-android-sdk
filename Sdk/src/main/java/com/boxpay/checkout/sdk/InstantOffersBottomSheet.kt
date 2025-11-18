package com.boxpay.checkout.sdk

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.boxpay.checkout.sdk.composeScreens.screen.InstantOfferScreen
import com.boxpay.checkout.sdk.databinding.FragmentChooseMultipleOffersBinding
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersResponse
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


internal class InstantOffersBottomSheet() : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChooseMultipleOffersBinding
    private var offersList : List<GetInstantOffersResponse> = emptyList()
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

    private var onClickCode : (code : String) -> Unit = {}
    private var onRemoveCode : () -> Unit = {}

    private var selectedCouponCode : String? = null

    private lateinit var sharedPreferences: SharedPreferences


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
            dialog.setCanceledOnTouchOutside(false)


            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
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
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChooseMultipleOffersBinding.inflate(layoutInflater, container, false)
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)

        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val screenHeight = requireContext().resources.displayMetrics.heightPixels
        val percentageOfScreenHeight = 0.90 // 70%
        val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

        binding.backButton.setOnClickListener {
            dismiss()
        }

        bottomSheetBehavior?.maxHeight = desiredHeight

        lifecycleScope.launchWhenStarted {
            binding.composeView.setContent {
                InstantOfferScreen(
                    onClickCoupon = {code ->
                        onClickCode(code)
                        dismiss()
                    },
                    couponList = offersList,
                    selectedColor = androidx.compose.ui.graphics.Color(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "primaryButtonColor",
                                "#000000"
                            )
                        )
                    ),
                    selectedCouponCode = selectedCouponCode ?: "",
                    onClickRemoveCoupon = {
                        onRemoveCode()
                        dismiss()
                    }
                )
            }
        }

        return binding.root
    }


    companion object {
        fun newInstance(
            offersList: List<GetInstantOffersResponse>,
            onClickCode : (code : String) -> Unit,
            onRemoveCode : () -> Unit,
            selectedCouponCode : String?
        ): InstantOffersBottomSheet {
            val fragment = InstantOffersBottomSheet()
            fragment.offersList = offersList
            fragment.onClickCode = onClickCode
            fragment.onRemoveCode = onRemoveCode
            fragment.selectedCouponCode = selectedCouponCode
            return fragment
        }
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
    }

}