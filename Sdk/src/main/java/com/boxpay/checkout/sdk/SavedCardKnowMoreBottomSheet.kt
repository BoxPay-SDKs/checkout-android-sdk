package com.boxpay.checkout.sdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.boxpay.checkout.sdk.composeScreens.components.KnowMoreBottomSheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

internal class SavedCardKnowMoreBottomSheet(
    private val selectedTextColor: Color,
    private val selectedColor: Color
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                KnowMoreBottomSheet(
                    selectedColor = selectedColor,
                    selectedTextColor = selectedTextColor,
                    onClickBack = { dismiss() },
                    instructions = listOf(
                        Pair(
                            R.drawable.ic_saved_card,
                            "Your bank/card network will securely save your card information via tokenization if you consent for the same."
                        ),
                        Pair(
                            R.drawable.ic_add_new_card,
                            "In case you choose to not tokenize, you’ll have to enter card details every time you pay."
                        )
                    )
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            behavior.isFitToContents = true // Ensures it fits to its content
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT // Full screen height
        }
    }
}