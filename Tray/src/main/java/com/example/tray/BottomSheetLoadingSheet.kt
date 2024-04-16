package com.example.tray

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.LottieDrawable
import com.example.tray.ViewModels.CallBackFunctions
import com.example.tray.ViewModels.SingletonClassForLoadingState
import com.example.tray.ViewModels.callBackFunctionForLoadingState
import com.example.tray.databinding.FragmentBottomSheetLoadingSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetLoadingSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentBottomSheetLoadingSheetBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBottomSheetLoadingSheetBinding.inflate(layoutInflater,container,false)
        binding.boxpayLogoLottie.apply {
            setAnimation("boxpayLogo.json") // Replace with your Lottie animation file
            repeatCount = LottieDrawable.INFINITE // Set repeat count to infinite
            playAnimation() // Start the animation
        }



        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val callBackFunctions = callBackFunctionForLoadingState(::dismissCurrentBottomSheet)
        SingletonClassForLoadingState.getInstance().callBackFunctions = callBackFunctions
        return binding.root
    }
    fun dismissCurrentBottomSheet(){
        dismiss()
    }

    companion object {

    }
}