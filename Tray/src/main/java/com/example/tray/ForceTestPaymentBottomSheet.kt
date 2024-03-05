package com.example.tray

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tray.databinding.FragmentForceTestPaymentBottomSheetBinding
import com.example.tray.databinding.FragmentNetBankingBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ForceTestPaymentBottomSheet : BottomSheetDialogFragment(){
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