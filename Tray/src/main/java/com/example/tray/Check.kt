package com.example.tray

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.tray.databinding.ActivityCheckBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class Check : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check)

        val openBottomSheet = findViewById<Button>(R.id.openButton)

        openBottomSheet.setOnClickListener {
            val bottomSheet = TrayActivity()
            bottomSheet.show(supportFragmentManager, "ModalBottomSheet")
        }
    }
}