package com.example.tray

import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.example.tray.databinding.FragmentUPITimerBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UPITimerBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentUPITimerBottomSheetBinding
    private lateinit var countdownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentUPITimerBottomSheetBinding.inflate(layoutInflater,container,false)
        binding.circularProgressBar.startAngle=90f
        binding.cancelPaymentTextView.setOnClickListener(){
            dismiss()
        }
        startTimer()

        return binding.root
    }
    private fun startTimer() {
        countdownTimer = object : CountDownTimer(300000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update TextView with the remaining time
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                binding.progressTextView.text = timeString

                // Update ProgressBar
                val progress = ((millisUntilFinished.toFloat() / 300000) * 100).toInt()
                binding.circularProgressBar.progress = progress*1.0f
// or with animation // =1s

// Set Progress Max
                binding.circularProgressBar.progressMax = 100f
            }

            override fun onFinish() {
                // Handle onFinish event if needed
                binding.progressTextView.text = "00:00"
                binding.circularProgressBar.progressMax = 0f
            }
        }

        countdownTimer.start()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? AddUPIID)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }


    companion object {

    }
}