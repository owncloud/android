package com.owncloud.android.presentation.authentication.homecloud

import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import timber.log.Timber

/**
 * Extension function to set the text cursor color for an EditText.
 * @param color The color to apply to the cursor
 */
fun EditText.setTextCursorDrawableCompat(@ColorInt color: Int) {
    try {
        // For API 29+ we can use textCursorDrawable directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable?.let { cursorDrawable ->
                val wrappedDrawable = DrawableCompat.wrap(cursorDrawable)
                DrawableCompat.setTint(wrappedDrawable, color)
                textCursorDrawable = wrappedDrawable
            }
        } else {
            // For older versions, use reflection
            val field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            field.isAccessible = true
            val drawableResId = field.getInt(this)
            
            val editorField = TextView::class.java.getDeclaredField("mEditor")
            editorField.isAccessible = true
            val editor = editorField.get(this)
            
            val drawables = arrayOfNulls<Drawable>(2)
            drawables[0] = AppCompatResources.getDrawable(context, drawableResId)
            drawables[1] = AppCompatResources.getDrawable(context, drawableResId)
            
            drawables.forEach { drawable ->
                drawable?.let {
                    DrawableCompat.setTint(it, color)
                }
            }
            
            val cursorDrawableField = editor.javaClass.getDeclaredField("mCursorDrawable")
            cursorDrawableField.isAccessible = true
            cursorDrawableField.set(editor, drawables)
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to set cursor color")
    }
}

