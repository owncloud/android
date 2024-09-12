/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.security.passcode

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.owncloud.android.R

/**
 * Number keyboard to enter the passcode.
 */
class NumberKeyboard(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private lateinit var numericKeys: MutableList<TextView>
    private lateinit var backspaceBtn: ImageView

    private var listener: NumberKeyboardListener? = null

    init {
        inflateView()
    }

    /**
     * Sets keyboard listener.
     */
    fun setListener(listener: NumberKeyboardListener?) {
        this.listener = listener
    }

    /**
     * Inflates layout.
     */
    private fun inflateView() {
        val view = View.inflate(context, R.layout.numberkeyboard_layout, this)

        numericKeys = ArrayList(10)
        numericKeys.add(view.findViewById(R.id.key0))
        numericKeys.add(view.findViewById(R.id.key1))
        numericKeys.add(view.findViewById(R.id.key2))
        numericKeys.add(view.findViewById(R.id.key3))
        numericKeys.add(view.findViewById(R.id.key4))
        numericKeys.add(view.findViewById(R.id.key5))
        numericKeys.add(view.findViewById(R.id.key6))
        numericKeys.add(view.findViewById(R.id.key7))
        numericKeys.add(view.findViewById(R.id.key8))
        numericKeys.add(view.findViewById(R.id.key9))

        backspaceBtn = view.findViewById(R.id.backspaceBtn)
        // Set listeners
        setupListeners()
    }

    fun setFocusOnKey(number: Int) {
        if (number in 0..9) {
            numericKeys[number].requestFocus()
        }
    }

    /**
     * Setup on click listeners.
     */
    private fun setupListeners() {
        for (i in numericKeys.indices) {
            val key = numericKeys[i]
            key.setOnClickListener {
                listener?.onNumberClicked(i)
            }
        }

        backspaceBtn.setOnClickListener {
            listener?.onBackspaceButtonClicked()
        }
    }
}
