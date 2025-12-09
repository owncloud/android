package com.owncloud.android.ui.custom

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.owncloud.android.R

class LoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val button: MaterialButton
    private val progressIndicator: CircularProgressIndicator

    private var buttonText: CharSequence? = null
    private var isLoading: Boolean = false

    private val loadingBackgroundColor: Int
    private val defaultBackgroundTint: ColorStateList?

    enum class State {
        ENABLED,
        LOADING,
        DISABLED,
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_loading_button, this, true)

        button = findViewById(R.id.loading_button_content)
        progressIndicator = findViewById(R.id.loading_button_progress)

        defaultBackgroundTint = button.backgroundTintList
            ?: ContextCompat.getColorStateList(context, R.color.selector_button_primary_background)

        context.obtainStyledAttributes(attrs, R.styleable.LoadingButton).apply {
            try {
                buttonText = getString(R.styleable.LoadingButton_buttonText)
                isLoading = getBoolean(R.styleable.LoadingButton_isLoading, false)
                loadingBackgroundColor = getColor(
                    R.styleable.LoadingButton_loadingBackgroundColor,
                    ContextCompat.getColor(context, R.color.homecloud_button_primary_background_loading)
                )

                val indicatorColor = getColor(
                    R.styleable.LoadingButton_loadingIndicatorColor,
                    ContextCompat.getColor(context, R.color.homecloud_button_primary_text)
                )
                progressIndicator.setIndicatorColor(indicatorColor)

                val indicatorSize = getDimensionPixelSize(
                    R.styleable.LoadingButton_loadingIndicatorSize,
                    resources.getDimensionPixelSize(R.dimen.loading_button_indicator_size)
                )
                progressIndicator.indicatorSize = indicatorSize

            } finally {
                recycle()
            }
        }

        button.text = buttonText
        updateLoadingState()
    }

    fun setText(text: CharSequence?) {
        buttonText = text
        if (!isLoading) {
            button.text = text
        }
    }

    fun setText(resId: Int) {
        setText(context.getString(resId))
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        updateLoadingState()
    }

    fun setState(state: State) {
        when(state) {
            State.ENABLED -> {
                isEnabled = true
                setLoading(false)
            }
            State.LOADING -> {
                isEnabled = true
                setLoading(true)
            }
            State.DISABLED -> {
                setLoading(false)
                isEnabled = false
            }
        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        button.setOnClickListener { view ->
            if (!isLoading && isEnabled) {
                listener?.onClick(view)
            }
        }
    }

    private fun updateLoadingState() {
        if (isLoading) {
            button.text = ""
            button.isClickable = false
            button.isEnabled = true // Keep enabled appearance while loading
            progressIndicator.isVisible = true
            button.backgroundTintList = ColorStateList.valueOf(loadingBackgroundColor)
        } else {
            button.text = buttonText
            button.isClickable = true
            button.isEnabled = isEnabled
            progressIndicator.isVisible = false
            button.backgroundTintList = defaultBackgroundTint
        }
    }
}

