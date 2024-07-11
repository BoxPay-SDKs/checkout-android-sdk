package com.boxpay.checkout.sdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boxpay.checkout.sdk.databinding.FragmentForceTestPaymentBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

internal class ForceTestPaymentBottomSheet : BottomSheetDialogFragment(){
    private lateinit var binding: FragmentForceTestPaymentBottomSheetBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentForceTestPaymentBottomSheetBinding.inflate(layoutInflater,container,false)
        binding.successButton.setOnClickListener(){
            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
            bottomSheet.show(parentFragmentManager,"PaymentSuccessBottomSheetOpenByForceTest")
        }
        binding.failureButton.setOnClickListener(){
            val bottomSheet = PaymentFailureScreen()
            bottomSheet.show(parentFragmentManager,"PaymentFailureBottomSheetOpenByForceTest")
        }
        return binding.root
    }

    companion object {

    }
}