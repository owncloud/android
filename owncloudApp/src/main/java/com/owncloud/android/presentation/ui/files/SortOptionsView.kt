package com.owncloud.android.presentation.ui.files

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.owncloud.android.R
import kotlinx.android.synthetic.main.sort_options_layout.view.*

class SortOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var onSortOptionsListener: SortOptionsListener? = null

    init {
        View.inflate(context, R.layout.sort_options_layout, this)

        sort_type_selector.setOnClickListener{ onSortOptionsListener?.onSortTypeListener() }
        sort_type_mode.setOnClickListener { onSortOptionsListener?.onSortTypeOrderListener() }
        view_type_selector.setOnClickListener { onSortOptionsListener?.onViewTypeListener() }
    }

    interface SortOptionsListener {

        fun onSortTypeListener()
        fun onSortTypeOrderListener()
        fun onViewTypeListener()
    }

}
