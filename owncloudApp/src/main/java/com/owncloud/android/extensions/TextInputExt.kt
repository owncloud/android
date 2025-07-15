package com.owncloud.android.extensions

import com.google.android.material.textfield.TextInputEditText

fun TextInputEditText.updateTextIfDiffers(text: String) {
    if (this.text?.toString() != text) {
        this.setText(text)
    }
}