package com.owncloud.android.presentation.authentication.homecloud

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.owncloud.android.domain.server.model.Server

class ServerAddressAdapter(
    context: Context,
    private val servers: MutableList<Server> = mutableListOf()
) : ArrayAdapter<Server>(context, android.R.layout.simple_dropdown_item_1line, servers) {

    private val filter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val suggestions = mutableListOf<Server>()

            if (constraint == null || constraint.isEmpty()) {
                suggestions.addAll(servers)
            } else {
                val filterPattern = constraint.toString().lowercase().trim()
                for (server in servers) {
                    if (server.hostName.lowercase().contains(filterPattern)) {
                        suggestions.add(server)
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
                addAll(results.values as List<Server>)
            }
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as Server).hostName
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val server = getItem(position)
        textView.text = server?.hostName
        return view
    }

    override fun getFilter(): Filter {
        return filter
    }

    fun setServers(servers: List<Server>) {
        this.servers.clear()
        this.servers.addAll(servers)
        notifyDataSetChanged()
    }
}