/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Christian Schabesberger
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.presentation.conflicts

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.extensions.avoidScreenshotsIfNeeded

class ConflictsResolveDialogFragment : DialogFragment() {

    private lateinit var listener: OnConflictDecisionMadeListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireActivity())
            .setIcon(R.drawable.ic_warning)
            .setTitle(R.string.conflict_title)
            .setMessage(R.string.conflict_message)
            .setPositiveButton(R.string.conflict_use_local_version) { _, _ ->
                listener.conflictDecisionMade(Decision.KEEP_LOCAL)
            }
            .setNeutralButton(R.string.conflict_keep_both) { _, _ ->
                listener.conflictDecisionMade(Decision.KEEP_BOTH)
            }
            .setNegativeButton(R.string.conflict_use_server_version) { _, _ ->
                listener.conflictDecisionMade(Decision.KEEP_SERVER)
            }
            .create()

        dialog.avoidScreenshotsIfNeeded()

        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        listener.conflictDecisionMade(Decision.CANCEL)
    }

    fun showDialog(activity: AppCompatActivity) {
        val previousFragment = activity.supportFragmentManager.findFragmentByTag("dialog")
        val fragmentTransaction = activity.supportFragmentManager.beginTransaction()
        if (previousFragment != null) {
            fragmentTransaction.remove(previousFragment)
        }
        fragmentTransaction.addToBackStack(null)

        this.show(fragmentTransaction, "dialog")
    }

    interface OnConflictDecisionMadeListener {
        fun conflictDecisionMade(decision: Decision)
    }

    enum class Decision {
        CANCEL,
        KEEP_BOTH,
        KEEP_LOCAL,
        KEEP_SERVER
    }

    companion object {
        fun newInstance(onConflictDecisionMadeListener: OnConflictDecisionMadeListener): ConflictsResolveDialogFragment {
            return ConflictsResolveDialogFragment().apply {
                listener = onConflictDecisionMadeListener
            }
        }
    }
}
