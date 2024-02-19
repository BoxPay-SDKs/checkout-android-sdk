package com.example.tray

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tray.databinding.FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaymentSuccessfulWithDetailsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
    private var token: String? = null
    private var successScreenFullReferencePath : String ?= null
    private var transactionID: String? = null
    private var transactionAmount: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
            successScreenFullReferencePath= it.getString("successScreenFullReferencePath")
            transactionID = it.getString("transactionID")
            transactionAmount= it.getString("transactionAmount")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPaymentSuccessfulWithDetailsBottomSheetBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    companion object {
        fun newInstance(data: String?, successScreenFullReferencePath : String?, transactionID : String?, transactionAmount : String?): AddCardBottomSheet {
            val fragment = AddCardBottomSheet()
            val args = Bundle()
            args.putString("token", data)
            args.putString("successScreenFullReferencePath",successScreenFullReferencePath)
            args.putString("transactionID",transactionID)
            args.putString("transactionAmount",transactionAmount)
            fragment.arguments = args
            return fragment
        }
    }
}