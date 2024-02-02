package com.example.tray

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.tray.adapters.WalletAdapter
import com.example.tray.databinding.FragmentWalletBottomSheetBinding
import com.example.tray.dataclasses.WalletDataClass
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONArray
import org.json.JSONObject


class WalletBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentWalletBottomSheetBinding
    private lateinit var allBanksAdapter: WalletAdapter
    private var walletDetailsOriginal: ArrayList<WalletDataClass> = ArrayList()
    private var walletDetailsFiltered: ArrayList<WalletDataClass> = ArrayList()
    private var overlayViewCurrentBottomSheet: View? = null
    private var buttonEnabledOrNot : Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(requireContext())
    }

    private fun filterWalletMethods(paymentMethodsArray: JSONArray): List<JSONObject> {
        val walletMethods = mutableListOf<JSONObject>()

        for (i in 0 until paymentMethodsArray.length()) {
            val paymentMethodObject = paymentMethodsArray.optJSONObject(i)

            // Check if the payment method has type "NetBanking"
            if (paymentMethodObject?.optString("type") == "Wallet") {
                walletMethods.add(paymentMethodObject)
            }
        }

        return walletMethods
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWalletBottomSheetBinding.inflate(layoutInflater, container, false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        walletDetailsOriginal = arrayListOf()


        allBanksAdapter = WalletAdapter(walletDetailsFiltered, binding.walletsRecyclerView)
        binding.walletsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.walletsRecyclerView.adapter = allBanksAdapter


        disableProceedButton()




        val url = "https://test-apis.boxpay.tech/v0/platform/payment-methods"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonArrayAll = JsonArrayRequest(Request.Method.GET, url, null, { response ->

            try {
                val walletMethodsObjects = filterWalletMethods(response)

                // Process NetBanking objects as needed
                for (netBankingObject in walletMethodsObjects) {
                    // Extract information from the NetBanking object
                    val id = netBankingObject.optString("id")
                    val brand = netBankingObject.optString("title")

                    // Do something with the extracted information
                    walletDetailsOriginal.add(WalletDataClass(brand, R.drawable.wallet_sample_logo))
                }
                showAllWallets()

            } catch (e: Exception) {
                Log.d("Error Occured", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("error here", "RESPONSE IS $error")
            Toast.makeText(requireContext(), "Fail to get response", Toast.LENGTH_SHORT)
                .show()
        })
        queue.add(jsonArrayAll)




        binding.searchView.setOnQueryTextListener(/*listener (comment) */ object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if(query.isEmpty()){
                    removeRecyclerViewFromBelowEditText()
                }else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterWallets(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if(newText.isEmpty()){
                    removeRecyclerViewFromBelowEditText()
                }else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterWallets(newText)
                return true
            }
        })


        binding.imageView2.setOnClickListener() {
            dismiss()
        }
        binding.proceedButton.isEnabled = false
        binding.textView2.setOnClickListener(){
            if(buttonEnabledOrNot){
                disableProceedButton()
            }else{
                enableProceedButton()
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
                binding.textView6.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                binding.textView6.visibility = View.VISIBLE
                binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
                binding.textView6.setTextColor(Color.parseColor("#ADACB0"))

                showOverlayInCurrentBottomSheet()
                val bottomSheet = WalletLoadingBottomSheet()
                bottomSheet.show(childFragmentManager, "LoadingBottomSheet")
            }, 2000)

        }

        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }

    private fun filterWallets(query: String?) {
        walletDetailsFiltered.clear()
        for (wallet in walletDetailsOriginal) {
            if (query.toString().isBlank() || query.toString().isBlank()) {
                showAllWallets()
            } else if (wallet.walletName.contains(query.toString(), ignoreCase = true)) {
                walletDetailsFiltered.add(WalletDataClass(wallet.walletName, wallet.walletImage))
            }
        }




        allBanksAdapter.notifyDataSetChanged()
    }

    fun showAllWallets() {
        walletDetailsFiltered.clear()
        for (bank in walletDetailsOriginal) {
            walletDetailsFiltered.add(bank)
        }
        allBanksAdapter.notifyDataSetChanged()
    }

    companion object {

    }

    fun makeRecyclerViewJustBelowEditText(){

        Log.d("View will be gone now","working fine")
        binding.textView19.visibility = View.GONE
        binding.textView24.visibility = View.GONE
        binding.linearLayout2.visibility = View.GONE
    }

    fun removeRecyclerViewFromBelowEditText(){
        Log.d("View will be gone now","working fine")
        binding.textView19.visibility = View.VISIBLE
        binding.textView24.visibility = View.VISIBLE
        binding.linearLayout2.visibility = View.VISIBLE

    }
    private fun showOverlayInCurrentBottomSheet() {
        // Create a semi-transparent overlay view
        overlayViewCurrentBottomSheet = View(requireContext())
        overlayViewCurrentBottomSheet?.setBackgroundColor(Color.parseColor("#80000000")) // Adjust color and transparency as needed

        // Add overlay view directly to the root view of the BottomSheet
        binding.root.addView(
            overlayViewCurrentBottomSheet,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
    public fun removeOverlayFromCurrentBottomSheet() {
        overlayViewCurrentBottomSheet?.let {
            // Remove the overlay view directly from the root view
            binding.root.removeView(it)
        }
    }
    private fun enableProceedButton(){
        binding.proceedButton.isEnabled = true
        binding.textView6.visibility = View.VISIBLE
        binding.progressBar.visibility = View.INVISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }
    private fun disableProceedButton(){
        binding.proceedButton.isEnabled = false
        binding.textView6.visibility = View.VISIBLE
        binding.progressBar.visibility = View.INVISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }
}