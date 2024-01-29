package com.example.tray

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.tray.ViewModels.OverlayViewModel
import com.example.tray.databinding.FragmentAddUPIIDBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class AddUPIID : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddUPIIDBinding
    private var bottomSheetBehavior : BottomSheetBehavior<FrameLayout> ?= null
    private var overlayViewCurrentBottomSheet: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddUPIIDBinding.inflate(inflater,container,false)
        var checked = false
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.progressBar.visibility = View.INVISIBLE
        binding.imageView3.setOnClickListener(){
            if(!checked) {
                binding.imageView3.setImageResource(R.drawable.checkbox)
                checked = true
            }
            else {
                binding.imageView3.setImageResource(0)
                checked = false
            }
        }
        binding.imageView2.setOnClickListener(){
            dismiss()
        }
        binding.proceedButton.isEnabled = false

        binding.editTextText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("beforeTextChanged",s.toString())
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                Log.d("onTextChanged",s.toString())
                if(textNow.isNotBlank()){
                    binding.proceedButtonRelativeLayout.isEnabled = true
                    binding.proceedButton.isEnabled = true
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
                    binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
                    binding.textView6.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                Log.d("afterTextChanged",s.toString())
                if(textNow.isBlank()){
                    binding.proceedButtonRelativeLayout.isEnabled = false
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                    binding.ll1InvalidUPI.visibility = View.GONE
                }
            }

        })

        binding.proceedButton.setOnClickListener(){


            binding.textView6.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
            val rotateAnimation = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
            rotateAnimation.duration = 3000 // Set the duration of the rotation in milliseconds
            rotateAnimation.repeatCount = ObjectAnimator.INFINITE // Set to repeat indefinitely
            binding.proceedButton.isEnabled = false

            rotateAnimation.start()

            Handler(Looper.getMainLooper()).postDelayed({
                binding.progressBar.visibility = View.INVISIBLE
                binding.ll1InvalidUPI.visibility = View.VISIBLE
                binding.textView6.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                binding.textView6.visibility = View.VISIBLE
                binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
                binding.textView6.setTextColor(Color.parseColor("#ADACB0"))

                showOverlayInCurrentBottomSheet()
                val bottomSheet = UPITimerBottomSheet()
                bottomSheet.show(childFragmentManager, "LoadingBottomSheet")
            }, 3000)

        }
        binding.ll1InvalidUPI.visibility = View.GONE



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
//                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        binding.editTextText.requestFocus()
    }
    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }

    private fun showOverlayInCurrentBottomSheet() {
        // Create a semi-transparent overlay view
        overlayViewCurrentBottomSheet = View(requireContext())
        overlayViewCurrentBottomSheet?.setBackgroundColor(Color.parseColor("#80000000")) // Adjust color and transparency as needed

        // Add overlay view directly to the root view of the BottomSheet
        binding.root.addView(
            overlayViewCurrentBottomSheet,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
    public fun removeOverlayFromCurrentBottomSheet() {
        overlayViewCurrentBottomSheet?.let {
            // Remove the overlay view directly from the root view
            binding.root.removeView(it)
        }
    }

    companion object {

    }
}