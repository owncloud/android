package com.owncloud.android.presentation.observers

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class EmptyDataObserver constructor(recyclerView: RecyclerView?, emptyView: View?): RecyclerView.AdapterDataObserver() {

    private var emptyView: View? = null
    private var recyclerView: RecyclerView? = null

    init {
        this.recyclerView = recyclerView
        this.emptyView = emptyView
        checkIfEmpty()
    }


    private fun checkIfEmpty() {
        if (emptyView != null && recyclerView!!.adapter != null) {
            val emptyViewVisible = recyclerView!!.adapter!!.itemCount == 0
            emptyView!!.isVisible = emptyViewVisible
            recyclerView!!.isVisible = !emptyViewVisible
        }
    }

    override fun onChanged() {
        super.onChanged()
        checkIfEmpty()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        super.onItemRangeChanged(positionStart, itemCount)
    }

}