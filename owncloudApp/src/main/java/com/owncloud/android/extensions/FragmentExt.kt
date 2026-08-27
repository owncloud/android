/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.icu.util.Calendar
import android.view.Menu
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.databinding.AddMemberFragmentBinding
import com.owncloud.android.domain.appregistry.model.AppRegistryProvider
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun Fragment.showErrorInSnackbar(genericErrorMessageId: Int, throwable: Throwable?) =
    throwable?.let {
        showMessageInSnackbar(it.parseError(getString(genericErrorMessageId), resources))
    }

fun Fragment.showMessageInSnackbar(
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_LONG,
) {
    val requiredView = view ?: return
    val rootView = view?.rootView ?: return
    val bottomNavView = rootView.findViewById<View?>(R.id.bottom_nav_view)
    val snackbar = Snackbar.make(requiredView, message, duration)
    if (bottomNavView?.isVisible == true) { snackbar.setAnchorView(bottomNavView) }
    snackbar.show()
}

fun Fragment.showSnackbarWithAction(
    message: CharSequence,
    actionText: CharSequence,
    action: () -> Unit,
    duration: Int = Snackbar.LENGTH_LONG
) {
    val requiredView = view ?: return
    Snackbar.make(requiredView, message, duration)
        .setAction(actionText) { action() }
        .show()
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

fun Fragment.bindDatePickerDialog(
    binding: AddMemberFragmentBinding,
    expirationDate: String?,
    onExpirationDateSelected: (String?) -> Unit,
) {
    binding.expirationDateLayout.expirationDateSwitch.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            openDatePickerDialog(binding, expirationDate, onExpirationDateSelected)
        } else {
            binding.expirationDateLayout.expirationDateValue.visibility = View.GONE
            onExpirationDateSelected(null)
        }
    }
}

fun Fragment.openDatePickerDialog(
    binding: AddMemberFragmentBinding,
    expirationDate: String?,
    onExpirationDateSelected: (String?) -> Unit,
) {
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat(DisplayUtils.DATE_FORMAT_ISO, Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    expirationDate?.let {
        calendar.time = formatter.parse(it)
    }

    DatePickerDialog(
        requireContext(),
        { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val isoExpirationDate = formatter.format(calendar.time)
            onExpirationDateSelected(isoExpirationDate)
            binding.expirationDateLayout.expirationDateValue.apply {
                visibility = View.VISIBLE
                text = DisplayUtils.displayDateToHumanReadable(isoExpirationDate)
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = Calendar.getInstance().timeInMillis
        show()
        setOnCancelListener {
            if (expirationDate == null) {
                binding.expirationDateLayout.expirationDateSwitch.isChecked = false
            }
        }
    }
}

fun Fragment.addOpenInWebMenuOptions(
    menu: Menu,
    openInWebProviders: Map<String, Int> = emptyMap(),
    appRegistryProviders: List<AppRegistryProvider>? = emptyList(),
): Map<String, Int> {
    val newOpenInWebProviders = emptyMap<String, Int>().toMutableMap()
    // Remove "open in web" dynamic menu items and add them again to avoid duplications
    openInWebProviders.forEach { (_, menuItemId) ->
        menu.removeItem(menuItemId)
    }
    appRegistryProviders?.forEachIndexed { index, appRegistryProvider ->
        menu.add(Menu.NONE, index, 0, getString(R.string.ic_action_open_with_web, appRegistryProvider.name)).also {
            it.setShowAsAction(SHOW_AS_ACTION_NEVER)
            newOpenInWebProviders[appRegistryProvider.name] = it.itemId
        }
    }
    return newOpenInWebProviders
}
