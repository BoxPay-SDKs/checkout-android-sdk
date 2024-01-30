package com.example.tray

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.example.tray.databinding.FragmentPaymentStatusBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class PaymentStatusBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentPaymentStatusBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentPaymentStatusBottomSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
        val animationView: LottieAnimationView = binding.lottieAnimationView


        animationView.playAnimation()


        animationView.cancelAnimation()


        animationView.pauseAnimation()

        animationView.progress = 0.5f
    }

    companion object {

    }
}