package com.example.tray

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.tray.adapters.WalletPaySpinnerAdapter
import com.example.tray.databinding.FragmentWalletBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class WalletBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentWalletBottomSheetBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWalletBottomSheetBinding.inflate(layoutInflater,container,false)


        val walletOptions = arrayListOf("Select wallet", "Jio Money", "Amazon Pay","Paytm")
        val adapter = WalletPaySpinnerAdapter(requireContext(),walletOptions)

        binding.spinnerWallet.adapter = adapter

        if (binding.spinnerWallet.adapter is ArrayAdapter<*>) {
            (binding.spinnerWallet.adapter as ArrayAdapter<*>).getView(0, null, binding.spinnerWallet).isEnabled = false
        }

        binding.spinnerWallet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                // Check if the selected position is 0
                if (position == 0) {
                    // Do nothing or show a message as desired
                } else {
                    // Handle other items
                    // ...
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
        binding.spinnerWallet.setOnTouchListener { _, event ->
            // Check if the touch event is on position 0, and consume the event to prevent selection
            if (event.action == MotionEvent.ACTION_DOWN && isPointInsideView(event.x, event.y, binding.spinnerWallet.getChildAt(0))) {
                true
            } else {
                false
            }
        }

        binding.imageView2.setOnClickListener(){
            dismiss()
        }

        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }

    private fun addItemToSpinner(newItem: String, adapter: ArrayAdapter<String>) {
        // Add the new item to the adapter
        adapter.add(newItem)

        // Notify the adapter about the change
        adapter.notifyDataSetChanged()
    }
    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewX = location[0]
        val viewY = location[1]

        // point is inside view bounds
        return (x > viewX && x < viewX + view.width && y > viewY && y < viewY + view.height)
    }




    companion object {

    }
}