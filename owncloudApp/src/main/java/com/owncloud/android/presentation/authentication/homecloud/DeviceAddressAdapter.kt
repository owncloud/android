package com.owncloud.android.presentation.authentication.homecloud

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.owncloud.android.domain.device.model.Device

class DeviceAddressAdapter(
    private val context: Context,
    private val devices: MutableList<Device> = mutableListOf()
) : ArrayAdapter<Device>(context, android.R.layout.simple_dropdown_item_1line, devices) {


    override fun getCount(): Int {
        return devices.size
    }

    override fun getItem(position: Int): Device {
        return devices[position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val device = getItem(position)
        textView.text = device.name
        return view
    }

    fun setDevices(devices: List<Device>) {
        this.devices.clear()
        this.devices.addAll(devices)
        notifyDataSetChanged()
    }
}

