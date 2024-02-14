package com.example.tray

import android.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.example.tray.databinding.FragmentPaymentStatusBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class PaymentStatusBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentPaymentStatusBottomSheetBinding
    private var token: String? = null
    private var successScreenFullReferencePath : String ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
            successScreenFullReferencePath= it.getString("successScreenFullReferencePath")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentPaymentStatusBottomSheetBinding.inflate(layoutInflater, container, false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val animationView: LottieAnimationView = binding.lottieAnimationView


        animationView.playAnimation()
        val handler = Handler()
        val startAnimationRunnable = Runnable {
            try{
                openActivity(successScreenFullReferencePath!!,requireContext())
            }
            catch (e : Exception){
                Toast.makeText(requireContext(), "Failed to open\nmerchants success screen", Toast.LENGTH_SHORT).show()
            }
        }

        // Delay execution by 1000 milliseconds (1 second)
        handler.postDelayed(startAnimationRunnable, 3000)

        return binding.root

    }
    fun openActivity(activityPath: String, context: Context) {
        if (context is AppCompatActivity) {
            try {
                // Get the class object for the activity using reflection
                val activityClass = Class.forName(activityPath)
                // Create an instance of the activity using Kotlin reflection
                val activityInstance = activityClass.getDeclaredConstructor().newInstance() as AppCompatActivity

                // Check if the activity is a subclass of AppCompatActivity
                if (activityInstance is AppCompatActivity) {
                    // Start the activity
                    context.startActivity(Intent(context, activityClass))
                } else {
                    // Log an error or handle the case where the activity is not a subclass of AppCompatActivity
                }
            } catch (e: ClassNotFoundException) {
                // Log an error or handle the case where the activity class cannot be found
                Toast.makeText(requireContext(), "Failed to open\nmerchants success screen", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Log an error or handle the case where the context is not an AppCompatActivity
        }
    }

    companion object {
        fun newInstance(data: String?, successScreenFullReferencePath : String?): PaymentStatusBottomSheet {
            val fragment = PaymentStatusBottomSheet()
            val args = Bundle()
            args.putString("token", data)
            args.putString("successScreenFullReferencePath",successScreenFullReferencePath)
            fragment.arguments = args
            return fragment
        }
    }
}