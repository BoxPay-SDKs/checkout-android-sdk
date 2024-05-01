package com.example.tray

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.example.tray.ViewModels.SharedViewModel
import com.example.tray.databinding.FragmentCancelConfirmationBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CancelConfirmationBottomSheet : BottomSheetDialogFragment() {
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
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


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        Log.d("userAgentHeader in MainBottom Sheet onCreateView",userAgentHeader)
        if(userAgentHeader.contains("Mobile",ignoreCase = true)){
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        return binding.root
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

            if (bottomSheetBehavior == null)
                Log.d("bottomSheetBehavior is null", "check here")

            val window = d.window
            window?.apply {
                // Apply dim effect
                setDimAmount(0.5f) // 50% dimming
                setBackgroundDrawable(ColorDrawable(Color.argb(128, 0, 0, 0))) // Semi-transparent black background
            }

            val screenHeight = resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.7 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

//            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//            dialog.window?.setDimAmount(0.5f)


//            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Set transparent background
//            dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(R.drawable.button_bg)



//        // Adjust the height of the bottom sheet content view
//        val layoutParams = bottomSheetContent.layoutParams
//        layoutParams.height = desiredHeight
//        bottomSheetContent.layoutParams = layoutParams
            bottomSheetBehavior?.maxHeight = desiredHeight
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false





            bottomSheetBehavior?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Handle state changes
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            // Fully expanded
                        }

                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            // Collapsed

                        }

                        BottomSheetBehavior.STATE_DRAGGING -> {
                            // The BottomSheet is being dragged
//                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        BottomSheetBehavior.STATE_SETTLING -> {
                            // The BottomSheet is settling
//                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
                            //Hidden

                        }
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    companion object {

    }
}