package com.example.tray

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.example.tray.databinding.FragmentPaymentFailureScreenBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class PaymentFailureScreen : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentPaymentFailureScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = FragmentPaymentFailureScreenBinding.inflate(layoutInflater,container,false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        return binding.root
    }

    companion object {

    }
}