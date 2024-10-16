package com.boxpay.checkout.sdk

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.boxpay.checkout.sdk.databinding.FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
import com.boxpay.checkout.sdk.dataclasses.DCCResponse
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date


internal class PaymentSuccessfulWithDetailsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentPaymentSuccessfulWithDetailsBottomSheetBinding
    private var token: String? = null
    private var transactionID: String? = null
    private var amount: String? = null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    var savedDccResponse : DCCResponse? = null
    var isDccEnabled : Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedDccResponse = getDCCResponse(requireContext())
        if (savedDccResponse != null) {
            isDccEnabled = true
        }
    }
    @SuppressLint("SetTextI18n")
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
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "primaryButtonColor",
                    "#000000"
                )
            )
        )
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
        binding.tvMerchantSite.setTextColor(Color.parseColor(
            sharedPreferences.getString("primaryButtonColor", "#000000")
        ))
        binding.tvMerchantSite.setOnClickListener(){
            val callback =  SingletonClass.getInstance().getYourObject()
            if(callback != null){
                val transactionId = sharedPreferences.getString("transactionId","").toString()
                val operationId = sharedPreferences.getString("operationId","").toString()
                callback.onPaymentResult(PaymentResultObject("Success",transactionId,operationId))
                val mainBottomSheetFragment = parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
                mainBottomSheetFragment?.dismissTheSheetAfterSuccess()
                dismiss()
            }
        }
        binding. proceedButton.setOnClickListener(){
            val callback =  SingletonClass.getInstance().getYourObject()
            if(callback != null){
                val transactionId = sharedPreferences.getString("transactionId","").toString()
                val operationId = sharedPreferences.getString("operationId","").toString()
                callback.onPaymentResult(PaymentResultObject("Success",transactionId,operationId))
                val mainBottomSheetFragment = parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
                mainBottomSheetFragment?.dismissTheSheetAfterSuccess()
                dismiss()
            }
        }
        if (isDccEnabled){
            binding.tvCardType.text = savedDccResponse!!.brand
            binding.tvCardHolderName.text = "AnkushTest"
            binding.transTotalDCC.text = "Transaction Total " + savedDccResponse!!.baseMoney!!.currencyCode
            binding.tvTransTotal.text =  savedDccResponse!!.baseMoney!!.currencyCode + " " +  savedDccResponse!!.baseMoney!!.amount
            binding.tvExchangeRate.text = "1 " + savedDccResponse!!.baseMoney!!.currencyCode + " = " + savedDccResponse!!.dccQuotationDetails!!.fxRate + " " + savedDccResponse!!.dccQuotationDetails!!.dccMoney!!.currencyCode
            binding.tvTransCurrency.text = savedDccResponse!!.dccQuotationDetails!!.dccMoney!!.currencyCode
            binding.transactionAmountTextView.text = savedDccResponse!!.dccQuotationDetails!!.dccMoney!!.currencyCode + " " + savedDccResponse!!.dccQuotationDetails!!.dccMoney!!.amount
            binding.tvPaymentSuccess.text = "Payment Successful\n" + savedDccResponse!!.dccQuotationDetails!!.dccMoney!!.currencyCode + " " + savedDccResponse!!.dccQuotationDetails!!.dccMoney!!.amount
            binding.tvCardHolderName.text = getDCCResponse(requireActivity(),"CARD_HOLDER_NAME")
            binding.tvMerchantName.text = getDCCResponse(requireActivity(),"MERCHANT_NAME_SESSION")
            binding.tvMerchantSite.paintFlags = binding.tvMerchantSite.paintFlags or Paint.UNDERLINE_TEXT_FLAG


            if (savedDccResponse!!.brand.equals("VISA",true)){
                binding.llMargin.visibility = View.VISIBLE
                binding.tvMargin.text = savedDccResponse!!.dccQuotationDetails!!.marginPercent.toString() + "%"
                binding.tvInfo.text = "I have been offered a choice of currencies and agree to pay in " + savedDccResponse!!.dccQuotationDetails!!.dccMoney!!.currencyCode + ". This currency conversion service is provide by " + getDCCResponse(requireActivity(),"MERCHANT_NAME") +".\n" +
                        "\n" +
                        "Please print and retain for your records."
            }
        }else{
            binding.apply {
                llMerchantName.visibility = View.GONE
                llCardType.visibility = View.GONE
                llCardHolderName.visibility = View.GONE
                llTransTotal.visibility = View.GONE
                llExchangeRate.visibility = View.GONE
                llTransCurrency.visibility = View.GONE
                tvInfo.visibility = View.GONE
                dottedLast.visibility = View.GONE
                transactionAmountTextView.text = getNonDCCResponse(requireActivity(),"CURRENCY_TYPE") +  " " +getNonDCCResponse(requireActivity(),"AMOUNT")
            }
        }
        return binding.root
    }

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

    private fun getDCCResponse(context: Context): DCCResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("DCC_PREF", Context.MODE_PRIVATE)

        // Get the JSON string from SharedPreferences
        val json = sharedPreferences.getString("DCC_RESPONSE_KEY", null) ?: return null

        // Convert JSON string back to DCCResponse object
        val gson = Gson()
        return gson.fromJson(json, DCCResponse::class.java)
    }

    private fun getDCCResponse(context: Context, code:String): String {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("DCC_PREF", Context.MODE_PRIVATE)
        val value = sharedPreferences.getString(code, null)
        return value!!
    }

    private fun getNonDCCResponse(context: Context, code:String): String {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("NON_DCC_PREF", Context.MODE_PRIVATE)
        val value = sharedPreferences.getString(code, null)
        return value!!
    }


    private fun getCurrentDateAndTimeInFormattedString() : String{
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        return dateFormat.format(currentDateTime)
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token","empty")
        transactionID = sharedPreferences.getString("transactionId","empty")
        amount = sharedPreferences.getString("currencySymbol","₹")+sharedPreferences.getString("amount","empty")
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
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }

            val window = d.window
            window?.apply {
                // Apply dim effect
                setDimAmount(0.5f) // 50% dimming
                setBackgroundDrawable(ColorDrawable(Color.argb(128, 0, 0, 0))) // Semi-transparent black background
            }

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
}