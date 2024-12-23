package com.boxpay.checkout.sdk

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.boxpay.checkout.sdk.databinding.FragmentCardComponentAloneBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.simform.customcomponent.SSCustomEdittextOutlinedBorder
import java.util.Calendar

class BoxPayCardComponent(
    val token: String?,
    val sandboxEnabled: Boolean?,
    val onPaymentResult: ((PaymentResultObject) -> Unit)
) : Fragment() {

    private lateinit var binding: FragmentCardComponentAloneBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCardComponentAloneBinding.inflate(inflater, container, false)
        setupCardNumberFormatting(binding.edtCardNumber)
        setupCardExpiryFormatting(binding.edtExpiry)
        return binding.root
    }

    private fun setupCardNumberFormatting(customEditText: SSCustomEdittextOutlinedBorder) {
        // Use a TextWatcher to react to external text updates
        val textWatcher = object : TextWatcher {

            var isFormatting = false // Flag to prevent reformatting when deleting spaces
            var userDeletingChars = false
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                userDeletingChars = count > after
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isFormatting) {
                    s?.let { editable ->
                        val textNow = editable.toString()
                        val text = textNow.replace("\\s".toRegex(), "")
                        val formattedText = formatCardNumber(text)
                        if (editable.toString() != formattedText && !userDeletingChars) {
                            isFormatting = true // Set flag to prevent reformatting
                            val editText = customEditText.findViewById<EditText>(R.id.editText)
                            editText.setText(formattedText)

                            // Move the cursor to the end of the text
                            editText.setSelection(formattedText.length)
                        } else if (editable.toString().length > 1 && editable.toString()[editable.toString().length - 1] == ' ') {
                            editable.delete(editable.length - 1, editable.length)
                        }

                        isFormatting = false // Reset the flag

                        if (text.isBlank()) {
                        } else if (text.length == 19) {
                            if (isValidCardNumberByLuhn(removeSpaces(text))) {
                            } else {
                            }
                        }

                        if (text.length >= 9) {


                        } else {
                        }
                    }
                }
                if (s.toString().isEmpty()) {

                } else if (s.toString().length < 19) {

                }
            }
        }

        val editText = customEditText.findViewById<EditText>(R.id.editText)
        editText.addTextChangedListener(textWatcher)
    }

    private fun setupCardExpiryFormatting(customEditText: SSCustomEdittextOutlinedBorder) {
        val textWatcher = object : TextWatcher {
            var isFormatting = false
            var userDeletingChars = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                userDeletingChars = count > after
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed in this callback for cursor handling
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isFormatting) {
                    s?.let { editable ->
                        val textNow = editable.toString()
                        val cursorPosition = customEditText.findViewById<EditText>(R.id.editText).selectionStart
                        var text = textNow.replace("/", "")

                        // Logic to format MM/YY
                        if (text.isNotEmpty() && text.length >= 2) {
                            if (text[0].toString().toInt() !in 0..1) {
                                text = "0$text"
                            }
                        }

                        val formattedText = formatMMYY(text)

                        // Set isFormatting to true to prevent infinite loop
                        isFormatting = true
                        val editText = customEditText.findViewById<EditText>(R.id.editText)
                        editText.setText(formattedText)

                        // Calculate new cursor position
                        val newCursorPosition = calculateNewCursorPosition(
                            formattedText,
                            cursorPosition,
                            userDeletingChars
                        )

                        // Restore the cursor position
                        editText.setSelection(newCursorPosition)
                        isFormatting = false
                    }
                }
            }
        }


        val editText = customEditText.findViewById<EditText>(R.id.editText)
        editText.addTextChangedListener(textWatcher)
    }

    private fun isValidCardNumberByLuhn(stringInputCardNumber: String): Boolean {
        val minCardLength = 13
        if (stringInputCardNumber.length < minCardLength) {
            return false
        }
        var sum = 0
        var isSecondDigit = false

        for (i in stringInputCardNumber.length - 1 downTo 0) {
            var d = stringInputCardNumber[i] - '0'

            if (isSecondDigit) {
                d *= 2
            }

            sum += d / 10
            sum += d % 10

            isSecondDigit = !isSecondDigit
        }

        val result: Boolean = (sum % 10 == 0)

        return result
    }

    private fun removeSpaces(stringWithSpaces: String): String {
        return stringWithSpaces.replace(" ", "")
    }

    private fun calculateNewCursorPosition(
        formattedText: String,
        previousCursorPosition: Int,
        userDeletingChars: Boolean
    ): Int {
        return when {
            userDeletingChars -> maxOf(previousCursorPosition - 1, 0) // Move left on delete
            else -> minOf(previousCursorPosition + 1, formattedText.length) // Move right on addition
        }
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

    private fun formatMMYY(date: String): String {
        val formatted = StringBuilder()
        for (i in date.indices) {
            if (i > 0 && i % 2 == 0 && i < 4) {
                formatted.append("/") // Add slash after every 2 digits, but not after the year
            }
            if (formatted.length >= 5) {
                break // Stop appending if length exceeds 5 characters
            }
            formatted.append(date[i])
        }
        return formatted.toString()
    }

    fun isValidExpirationDate(inputExpMonth: String, inputExpYear: String): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        val isValidYearValue = (inputExpYear.toInt() > 0)
        val isValidYearLength = (inputExpYear.length == 2)


        val isMonthValid = (inputExpMonth.toInt() in 1..12)

        val isFutureYear = (("20$inputExpYear").toInt() >= currentYear)

        val isValidMonthRange =
            ((inputExpMonth.toInt() >= currentMonth) || isFutureYear)

        val isSameYear_FutureOrCurrentMonth =
            ((inputExpYear.toInt() == currentYear) && (inputExpMonth.toInt() >= currentMonth))

        val result = ((isValidMonthRange && isValidYearLength && isValidYearValue) &&
                (isFutureYear || isSameYear_FutureOrCurrentMonth) && isMonthValid)

        return result
    }

}