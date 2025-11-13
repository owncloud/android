package com.owncloud.android.presentation.authentication.homecloud

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * EditText subclass that notifies a listener when text is pasted.
 */
class PasteAwareEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private var pasteListener: ((String) -> Unit)? = null
    private val clipboard by lazy { context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }

    fun setOnPasteListener(listener: (String) -> Unit) {
        pasteListener = listener
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pastedText = clip.getItemAt(0).coerceToText(context).toString()
                pasteListener?.invoke(pastedText)
            }
        }
        return super.onTextContextMenuItem(id)
    }
}