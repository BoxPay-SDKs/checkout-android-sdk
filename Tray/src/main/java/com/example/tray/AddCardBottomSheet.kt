package com.example.tray

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.tray.databinding.FragmentAddCardBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddCardBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentAddCardBottomSheetBinding
    private var bottomSheetBehavior : BottomSheetBehavior<FrameLayout> ?= null
    private var bottomSheet : FrameLayout ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =  FragmentAddCardBottomSheetBinding.inflate(inflater,container,false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.progressBar.visibility = View.INVISIBLE
        binding.proceedButton.isEnabled = false
        var checked = false
        binding.progressBar.visibility = View.INVISIBLE
        binding.ll1InvalidUPI.visibility = View.GONE
        binding.invalidCardNumber.visibility = View.GONE
        binding.invalidCVV.visibility = View.GONE
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
            }, 3000)

        }













        ////JUST FOR CHECKING PURPOSE....................................................................................................................................................................................................................................................................................................................................................................................................................................

        var enabled = false
        binding.textView2.setOnClickListener(){
            if(!enabled)
                enableProceedButton()
            else
                disableProceedButton()

            enabled = !enabled
        }
        var errorsEnabled = false
        binding.imageView3.setOnClickListener() {

            if (!errorsEnabled) {
                giveErrors()
                //Useful
                binding.imageView3.setImageResource(R.drawable.checkbox)
            }
            else {
                removeErrors()
                binding.imageView3.setImageResource(0)
            }

            errorsEnabled = !errorsEnabled
        }





        //....................................................................................................................................................................................................................................................................................................................................................................................................................................












        binding.imageView2.setOnClickListener(){
            dismiss()
        }

        binding.proceedButton.isEnabled = false
        binding.editTextText.filters = arrayOf(InputFilter.LengthFilter(19))


        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                if(textNow.isNotBlank()){
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }

                binding.editTextText.removeTextChangedListener(this)

                val text = s.toString().replace("\\s".toRegex(), "")
                val formattedText = formatCardNumber(text)

                binding.editTextText.setText(formattedText)
                binding.editTextText.setSelection(formattedText.length)

                binding.editTextText.addTextChangedListener(this)


            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()

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
                giveErrors()
                disableProceedButton()
            }, 3000)

        }

        // Set InputFilter to limit the length and add a slash after every 2 digits
        binding.editTextCardValidity.filters = arrayOf(InputFilter.LengthFilter(7))

        // Set TextWatcher to add slashes dynamically as the user types
        binding.editTextCardValidity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTextCardValidity.removeTextChangedListener(this)

                val text = s.toString().replace("/", "")
                val formattedText = formatMMYYYY(text)

                binding.editTextCardValidity.setText(formattedText)
                binding.editTextCardValidity.setSelection(formattedText.length)

                binding.editTextCardValidity.addTextChangedListener(this)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editTextNameOnCard.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }

            override fun afterTextChanged(s: Editable?) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }

        })


        binding.editTextCardCVV.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.editTextCardCVV.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.editTextCardCVV.filters = arrayOf(AsteriskPasswordInputFilter(), InputFilter.LengthFilter(3))

        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
//                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            }
        }
        return dialog
    }
    private fun enableProceedButton(){
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }
    private fun disableProceedButton(){
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }
    private fun removeErrors(){
        binding.ll1InvalidUPI.visibility = View.GONE
        binding.invalidCardNumber.visibility = View.GONE
        binding.invalidCVV.visibility = View.GONE
    }
    private fun giveErrors(){
        binding.ll1InvalidUPI.visibility = View.VISIBLE
        binding.invalidCardNumber.visibility = View.VISIBLE
        binding.invalidCVV.visibility = View.VISIBLE
    }

    private fun formatCardNumber(cardNumber: String): String {
        val formatted = StringBuilder()
        for (i in cardNumber.indices) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ") // Add space after every 4 digits
            }
            formatted.append(cardNumber[i])
        }
        return formatted.toString()
    }

    private fun formatMMYYYY(date: String): String {
        val formatted = StringBuilder()
        for (i in date.indices) {
            if (i > 0 && i % 2 == 0 && i < 4) {
                formatted.append("/") // Add slash after every 2 digits, but not after the year
            }
            formatted.append(date[i])
        }
        return formatted.toString()
    }

    class AsteriskPasswordTransformationMethod : PasswordTransformationMethod() {

        override fun getTransformation(source: CharSequence, view: android.view.View): CharSequence {
            return AsteriskCharSequence(source)
        }

        private class AsteriskCharSequence(private val source: CharSequence) : CharSequence {

            override val length: Int
                get() = source.length

            override fun get(index: Int): Char {
                return '*' // Replace dot with asterisk
            }

            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
                return AsteriskCharSequence(source.subSequence(startIndex, endIndex))
            }
        }
    }

    class AsteriskPasswordInputFilter : InputFilter {

        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence {
            val filteredStringBuilder = StringBuilder()
            for (i in start until end) {
                filteredStringBuilder.append('*') // Replace dot with asterisk
            }
            return filteredStringBuilder.toString()
        }
    }

    companion object {

    }
}