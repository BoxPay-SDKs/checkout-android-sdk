package com.example.tray

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.tray.databinding.FragmentWalletLoadingBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WalletLoadingBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentWalletLoadingBottomSheetBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWalletLoadingBottomSheetBinding.inflate(layoutInflater,container,false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding.imageView2.setOnClickListener(){
            dismiss()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val bottomSheet = PaymentStatusBottomSheet()
            bottomSheet.show(childFragmentManager, "LoadingBottomSheet")
        }, 2000)
        return binding.root
    }
    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
//        (parentFragment as? WalletBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }

    companion object {

    }
}