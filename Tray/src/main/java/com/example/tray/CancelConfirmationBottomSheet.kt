package com.example.tray

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.tray.ViewModels.SharedViewModel
import com.example.tray.databinding.FragmentCancelConfirmationBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CancelConfirmationBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentCancelConfirmationBottomSheetBinding
    val sharedViewModel: SharedViewModel by activityViewModels()

    interface ConfirmationListener {
        fun onConfirmation()
    }

    private var confirmationListener: ConfirmationListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCancelConfirmationBottomSheetBinding.inflate(layoutInflater,container,false)
        binding.yesButton.setOnClickListener {
            sharedViewModel.dismissBottomSheet()
            dismiss()
        }
        binding.noButton.setOnClickListener(){
            Log.d("cancel confirmation bottom sheet","no button")
            dismiss()
        }
        return binding.root
    }

    companion object {

    }
}