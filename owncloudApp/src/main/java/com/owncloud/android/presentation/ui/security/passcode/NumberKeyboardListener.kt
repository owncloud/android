package com.owncloud.android.presentation.ui.security.passcode

/**
 * Enables to listen keyboard events.
 */
interface NumberKeyboardListener {

    /**
     * Invoked when a number key is clicked.
     */
    fun onNumberClicked(number: Int)

    /**
     * Invoked when the left auxiliary button is clicked.
     */
    fun onBiometricButtonClicked()

    /**
     * Invoked when the right auxiliary button is clicked.
     */
    fun onBackspaceButtonClicked()
}
