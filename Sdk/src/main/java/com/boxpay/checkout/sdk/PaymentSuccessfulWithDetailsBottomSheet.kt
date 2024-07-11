package com.boxpay.checkout.sdk

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.sdk.databinding.FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date

internal class PaymentSuccessfulWithDetailsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
    private var token: String? = null
    private var transactionID: String? = null
    private var amount: String? = null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPaymentSuccessfulWithDetailsBottomSheetBinding.inflate(layoutInflater,container,false)

        binding.lottieAnimationView.playAnimation()
        fetchTransactionDetailsFromSharedPreferences()
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        binding.textView6.setTextColor(Color.parseColor(sharedPreferences.getString("buttonTextColor","#000000")))
        binding.transactionAmountTextView.text = amount
        binding.transactionIDTextView.text = transactionID
        binding.proceedButtonRelativeLayout.setBackgroundColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#000000")))
        binding.transactionDateAndTimeTextView.text = getCurrentDateAndTimeInFormattedString()
        binding. proceedButton.setOnClickListener(){
//            openActivity(successScreenFullReferencePath.toString(),requireContext())
            val callback =  SingletonClass.getInstance().getYourObject()
            if(callback == null){
                Log.d("call back is null","Failed")
            }else{
                val transactionId = sharedPreferences.getString("transactionId","").toString()
                val operationId = sharedPreferences.getString("operationId","").toString()
                callback.onPaymentResult(PaymentResultObject("Success",transactionId,operationId))

                val mainBottomSheetFragment = parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
                mainBottomSheetFragment?.dismissTheSheetAfterSuccess()
                Log.d("dismissViewModel","Payment Successful Sheet dismiss called Works fine")
                dismiss()
            }
//            callFunctionInActivity()
        }
        return binding.root
    }
//    private fun callFunctionInActivity() {
//        val activity = activity
//        if (activity is OTPScreenWebView) {
//            activity.killOTPWeViewActivity()
//        }
//    }

    private fun openActivity(activityPath: String, context: Context) {
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

    private fun getCurrentDateAndTimeInFormattedString() : String{
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        return dateFormat.format(currentDateTime)
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token","empty")
        Log.d("data fetched from sharedPreferences",token.toString())
//        successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")
//        Log.d("success screen path fetched from sharedPreferences",successScreenFullReferencePath.toString())
        transactionID = sharedPreferences.getString("transactionId","empty")
        Log.d("transactionID fetched from sharedPreferences",transactionID.toString())
        amount = sharedPreferences.getString("currencySymbol","₹")+sharedPreferences.getString("amount","empty")
        Log.d("success screen path fetched from sharedPreferences",amount.toString())
    }

    object SharedPreferencesHelper {

        private const val SHARED_PREF_NAME = "TransactionDetails"
        fun getAllKeyValuePairs(context: Context): Map<String, *> {
            val sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.all
        }
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

//        // Adjust the height of the bottom sheet content view
//        val layoutParams = bottomSheetContent.layoutParams
//        layoutParams.height = desiredHeight
//        bottomSheetContent.layoutParams = layoutParams
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.setCancelable(false)



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
                            dismiss()
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