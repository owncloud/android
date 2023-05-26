/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.extensions

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.FileMenuOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun Fragment.showErrorInSnackbar(genericErrorMessageId: Int, throwable: Throwable?) =
    throwable?.let {
        showMessageInSnackbar(it.parseError(getString(genericErrorMessageId), resources))
    }

fun Fragment.showMessageInSnackbar(
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_LONG
) {
    val requiredView = view ?: return
    Snackbar.make(requiredView, message, duration).show()
}

fun Fragment.showAlertDialog(
    title: String,
    message: String,
    positiveButtonText: String = getString(android.R.string.ok),
    positiveButtonListener: ((DialogInterface, Int) -> Unit)? = null,
    negativeButtonText: String = "",
    negativeButtonListener: ((DialogInterface, Int) -> Unit)? = null
) {
    val requiredActivity = activity ?: return
    AlertDialog.Builder(requiredActivity)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveButtonText, positiveButtonListener)
        .setNegativeButton(negativeButtonText, negativeButtonListener)
        .show()
        .avoidScreenshotsIfNeeded()
}

fun Fragment.hideSoftKeyboard() {
    val focusedView = requireActivity().currentFocus
    focusedView?.let {
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            focusedView.windowToken,
            0
        )
    }
}

fun <T> Fragment.collectLatestLifecycleFlow(
    flow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collect: suspend (T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            flow.collectLatest(collect)
        }
    }
}

fun Fragment.filterMenuOptions(
    menu: Menu,
    optionsToShow: List<FileMenuOption>,
    hasWritePermission: Boolean,
) {
    FileMenuOption.values().forEach { fileMenuOption ->
        val item = menu.findItem(fileMenuOption.toResId())
        item?.let {
            if (optionsToShow.contains(fileMenuOption)) {
                it.isVisible = true
                it.isEnabled = true
                if (fileMenuOption.toResId() == R.id.action_open_file_with) {
                    if (!hasWritePermission) {
                        item.setTitle(R.string.actionbar_open_with_read_only)
                    } else {
                        item.setTitle(R.string.actionbar_open_with)
                    }
                }
            } else {
                it.isVisible = false
                it.isEnabled = false
            }
        }

    }
}
