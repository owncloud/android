package com.owncloud.android.extensions

import android.widget.EditText

fun EditText.updateTextIfDiffers(text: String) {
    if (this.text?.toString() != text) {
        setText(text)
    }
}