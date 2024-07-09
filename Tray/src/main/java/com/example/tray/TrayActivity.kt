package com.example.tray


import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.ActivityTrayBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


internal class TrayActivity : BottomSheetDialogFragment()  {
    private var currencySymbol : String ?= null
    private var currencyCode : String ?= null
    private var price : Int ?= 0
    private var quantity : Int ?= 0
    private var rotated = false
    private lateinit var binding: ActivityTrayBinding
    private var isCardExpanded : Boolean = false
    private var url : String?= null
    private var bottomSheetBehavior : BottomSheetBehavior<LinearLayout> ?= null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = ActivityTrayBinding.inflate(inflater,container,false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        url = arguments?.getString("REQUIRED_STRING").toString()
        val view = binding.root
        JsonObject()

//        binding.imageView.setOnClickListener(){
//            if(!rotated) {
//                rotated = true
//                it.animate().rotation(180f).start()
//                toggleCardViewHeight(binding.cardView)
//            }else{
//                it.animate().rotation(0f).start()
//                toggleCardViewHeight(binding.cardView)
//                rotated = false
//            }
//        }
        return binding.root
    }
    private fun JsonObject(){
        val queue : RequestQueue = Volley.newRequestQueue(requireContext())
        val request = JsonObjectRequest(Request.Method.GET, url, null, { _ ->

        }, { _ ->
            Toast.makeText(requireContext(), "Fail to get response", Toast.LENGTH_SHORT)
                .show()
        })
        queue.add(request)
    }
    private fun toggleCardViewHeight(cardView: CardView) {
        val initialHeight = cardView.height
        val targetHeight = if (isCardExpanded) {
            // If the card is expanded, collapse it
            resources.getDimensionPixelSize(R.dimen.collapsed_card_height)
        } else {
            // If the card is collapsed, expand it to WRAP_CONTENT
            resources.getDimensionPixelSize(R.dimen.expanded_card_height)
        }
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val newHeight = (initialHeight + (targetHeight - initialHeight) * interpolatedTime).toInt()
                cardView.layoutParams.height = newHeight
                cardView.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        animation.duration = 300

        cardView.startAnimation(animation)
        isCardExpanded = !isCardExpanded
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = super.onCreateDialog(savedInstanceState)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val view = binding.root

        val bottomSheetContent = binding.clayout

        val screenHeight = resources.displayMetrics.heightPixels
        val percentageOfScreenHeight = 0.9 // 90%
        val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

        // Adjust the height of the bottom sheet content view
        val layoutParams = bottomSheetContent.layoutParams
        layoutParams.height = desiredHeight
        bottomSheetContent.layoutParams = layoutParams


        val coordinatorLayout = dialog?.findViewById<LinearLayout>(R.id.clayout)

        coordinatorLayout?.minimumHeight = Resources.getSystem().displayMetrics.heightPixels
    }

    override fun onStart() {
        super.onStart()
    }

}