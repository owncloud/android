package com.owncloud.android.presentation.authentication.homecloud

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.owncloud.android.domain.device.model.Device

class DeviceAddressAdapter(
    context: Context,
    private val devices: MutableList<Device> = mutableListOf()
) : ArrayAdapter<Device>(context, android.R.layout.simple_dropdown_item_1line, devices) {

    private val filter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val suggestions = mutableListOf<Device>()

            if (constraint == null || constraint.isEmpty()) {
                suggestions.addAll(devices)
            } else {
                val filterPattern = constraint.toString().lowercase().trim()
                for (device in devices) {
                    if (device.name.lowercase().contains(filterPattern)) {
                        suggestions.add(device)
                    }
                }
            }

            results.values = suggestions
            results.count = suggestions.size
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            if (results != null && results.count > 0) {
                addAll(results.values as List<Device>)
            }
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as Device).name
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val device = getItem(position)
        textView.text = device?.name
        return view
    }

    override fun getFilter(): Filter {
        return filter
    }

    fun setDevices(devices: List<Device>) {
        this.devices.clear()
        this.devices.addAll(devices)
        notifyDataSetChanged()
    }
}

