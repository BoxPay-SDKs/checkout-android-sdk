package com.boxpay.checkout.sdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boxpay.checkout.sdk.databinding.FragmentLoadingBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


internal class LoadingBottomSheet : BottomSheetDialogFragment()  {
    private lateinit var binding: FragmentLoadingBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoadingBottomSheetBinding.inflate(inflater,container,false)
        return binding.root
    }


    companion object {

    }
}