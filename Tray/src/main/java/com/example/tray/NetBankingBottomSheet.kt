package com.example.tray

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.adapters.NetbankingBanksAdapter
import com.example.tray.databinding.FragmentNetBankingBottomSheetBinding
import com.example.tray.databinding.NetbankingBanksItemBinding
import com.example.tray.dataclasses.NetbankingDataClass
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONArray
import org.json.JSONObject

class NetBankingBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentNetBankingBottomSheetBinding
    private lateinit var allBanksAdapter : NetbankingBanksAdapter
    private var banksDetailsOriginal: ArrayList<NetbankingDataClass> = ArrayList()
    private var banksDetailsFiltered: ArrayList<NetbankingDataClass> = ArrayList()

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(requireContext())
    }

    fun filterNetBankingMethods(paymentMethodsArray: JSONArray): List<JSONObject> {
        val netBankingMethods = mutableListOf<JSONObject>()

        for (i in 0 until paymentMethodsArray.length()) {
            val paymentMethodObject = paymentMethodsArray.optJSONObject(i)

            // Check if the payment method has type "NetBanking"
            if (paymentMethodObject?.optString("type") == "NetBanking") {
                netBankingMethods.add(paymentMethodObject)
            }
        }

        return netBankingMethods
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNetBankingBottomSheetBinding.inflate(layoutInflater,container,false)
        banksDetailsOriginal = arrayListOf()
        banksDetailsOriginal.add(NetbankingDataClass("Punjab National Bank \nCorporate Banking",R.drawable.netbanking_sample_logo))


        allBanksAdapter = NetbankingBanksAdapter(banksDetailsFiltered,binding.banksRecyclerView)
        binding.banksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.banksRecyclerView.adapter = allBanksAdapter
        val url = "https://test-apis.boxpay.tech/v0/platform/payment-methods"
        val queue : RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonArrayAll = JsonArrayRequest(Request.Method.GET, url, null, { response ->

            try {
                val netBankingObjects = filterNetBankingMethods(response)

                // Process NetBanking objects as needed
                for (netBankingObject in netBankingObjects) {
                    // Extract information from the NetBanking object
                    val id = netBankingObject.optString("id")
                    val brand = netBankingObject.optString("title")

                    // Do something with the extracted information
                    banksDetailsOriginal.add(NetbankingDataClass(brand,R.drawable.netbanking_sample_logo))
                }
                showAllBanks()

            } catch (e: Exception) {
                Log.d("Error Occured",e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("error here", "RESPONSE IS $error")
            Toast.makeText(requireContext(), "Fail to get response", Toast.LENGTH_SHORT)
                .show()
        })
        queue.add(jsonArrayAll)




        binding.searchView.setOnQueryTextListener(/*listener (comment) */ object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
                filterBanks(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filterBanks(newText)
                return true
            }
        })

        binding.imageView2.setOnClickListener(){
            dismiss()
        }

        return binding.root
    }

    private fun filterBanks(query: String?) {
        banksDetailsFiltered.clear()
        for(bank in banksDetailsOriginal){
            if(query.toString().isBlank() || query.toString().isBlank()){
                showAllBanks()
            }
            else if(bank.bankName.contains(query.toString(), ignoreCase = true)){
                banksDetailsFiltered.add(NetbankingDataClass(bank.bankName,bank.bankImage))
            }
        }




        allBanksAdapter.notifyDataSetChanged()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }
    fun showAllBanks(){
        banksDetailsFiltered.clear()
        for(bank in banksDetailsOriginal){
            banksDetailsFiltered.add(bank)
        }
        allBanksAdapter.notifyDataSetChanged()
    }

    fun filterNetBankingObjects(jsonArrayString: String): List<JSONObject> {
        val jsonArray = JSONArray(jsonArrayString)
        val netBankingObjects = mutableListOf<JSONObject>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            if (jsonObject.getString("type") == "NetBanking") {
                netBankingObjects.add(jsonObject)
            }
        }

        return netBankingObjects
    }

    companion object {

    }
}