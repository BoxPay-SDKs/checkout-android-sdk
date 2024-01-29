package com.example.tray

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup

import android.widget.Button


class Check : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check)
        val openBottomSheet = findViewById<Button>(R.id.openButton)

        openBottomSheet.setOnClickListener {
//            val bottomSheet = MainBottomSheet()
            showBottomSheetWithOverlay()


        }



    }

    fun showBottomSheetWithOverlay() {
        // Show the BottomSheetDialogFragment
        val bottomSheetFragment = MainBottomSheet()
        bottomSheetFragment.show(supportFragmentManager, "YourBottomSheetTag")
    }
}
