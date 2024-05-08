package com.example.tray

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.example.tray.databinding.FragmentQuickPayBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuickPayBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentQuickPayBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = FragmentQuickPayBottomSheetBinding.inflate(layoutInflater,container,false)
        val seekBar = binding.sliderButton

        val maxProgress = 100
        val desiredMinProgress = (maxProgress * 0.15).toInt() // 15% progress
        val desiredMaxProgress = (maxProgress * 0.85).toInt() // 85% progress

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Check if progress exceeds desired progress
                if (progress > desiredMaxProgress) {
                    seekBar?.progress = desiredMaxProgress
                }
                if(progress < desiredMinProgress) {
                    seekBar?.progress = desiredMinProgress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed for this implementation
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not needed for this implementation
                var progress = 0
                if(seekBar == null){
                    progress = 0
                }else{
                    progress = seekBar.progress
                }

                if(progress < 80){
                    seekBar?.progress = desiredMinProgress
                }else{
                    seekBar?.progress = desiredMaxProgress
                }
            }
        })
        return binding.root
    }

    companion object {

    }
}