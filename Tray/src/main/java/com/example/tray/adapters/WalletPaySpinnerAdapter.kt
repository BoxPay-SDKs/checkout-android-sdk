package com.example.tray.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.tray.databinding.WalletPaySpinnerItemBinding

class WalletPaySpinnerAdapter(private val context: Context, private val data: ArrayList<String>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Any {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: WalletPaySpinnerItemBinding =
            WalletPaySpinnerItemBinding.inflate(LayoutInflater.from(context), parent, false)

        // Bind data to the layout
        binding.walletName.text = data[position]

        binding.imageView9.setImageResource(com.example.tray.R.drawable.black_down_arrow)

        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: WalletPaySpinnerItemBinding =
            WalletPaySpinnerItemBinding.inflate(LayoutInflater.from(context), parent, false)

        binding.walletName.text = data[position]
        binding.imageView9.setImageResource(0)
        return binding.root
    }
}
