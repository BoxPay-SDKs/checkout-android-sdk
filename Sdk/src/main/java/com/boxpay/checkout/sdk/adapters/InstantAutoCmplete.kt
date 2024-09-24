package com.boxpay.checkout.sdk.adapters

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

class InstantAutoComplete : AppCompatAutoCompleteTextView {

    constructor(context: Context?) : super(context!!)

    constructor(arg0: Context?, arg1: AttributeSet?) : super(arg0!!, arg1)

    constructor(arg0: Context?, arg1: AttributeSet?, arg2: Int) : super(arg0!!, arg1, arg2)

    override fun enoughToFilter(): Boolean {
        // Always filter, even with empty text
        return true
    }

    override fun onFocusChanged(
        focused: Boolean, direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)

        // Show the dropdown when focused and there's a filter
        if (focused) {
            performFiltering(text, 0)
            showDropDown()  // Ensure dropdown is shown
        } else {
            dismissDropDown()  // Hide dropdown when focus is lost
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Get the total number of items in the adapter
        val itemCount = adapter?.count ?: 0

        if (itemCount > 0) {
            // Calculate the dropdown height based on the visible items
            val maxDropdownHeight = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) * 5 // Limit to 5 items
            dropDownHeight = if (itemCount <= 5) {
                // Adjust height if the whole list is visible
               LinearLayout.LayoutParams.WRAP_CONTENT
            } else {
                maxDropdownHeight
            }
        }
    }
}

