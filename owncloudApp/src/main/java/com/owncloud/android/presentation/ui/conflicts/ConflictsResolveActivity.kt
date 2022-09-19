/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
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

package com.owncloud.android.presentation.ui.conflicts

import com.owncloud.android.presentation.viewmodels.conflicts.ConflictsResolveViewModel
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ConflictsResolveActivity: FileActivity(), OnConflictDecisionMadeListener {

    private val conflictsResolveViewModel by viewModel<ConflictsResolveViewModel>()

    override fun conflictDecisionMade(decision: ConflictsResolveDialog.Decision) {
        var forceOverwrite = false

        when (decision) {
            ConflictsResolveDialog.Decision.CANCEL -> {
                finish()
                return
            }
            ConflictsResolveDialog.Decision.LOCAL -> forceOverwrite = true
            ConflictsResolveDialog.Decision.KEEP_BOTH -> {}
            ConflictsResolveDialog.Decision.SERVER -> {
                conflictsResolveViewModel.downloadFile(account.name, file)
                finish()
                return
            }
        }

        if (forceOverwrite) {
            conflictsResolveViewModel.uploadFileInConflict(file.owner, file.storagePath!!, file.remotePath)
        } else {
            conflictsResolveViewModel.uploadFilesFromSystem(file.owner, listOf(file.storagePath!!), file.remotePath)
        }

        finish()
    }

    override fun onAccountSet(stateWasRecovered: Boolean) {
        super.onAccountSet(stateWasRecovered)
        if (account != null) {
            if (file == null) {
                Timber.e("No conflictive file received")
                finish()
            } else {
                // Check if the file handled by the activity belongs to the current account
                val fileInConflict = storageManager.getFileByPath(file.remotePath)
                if (fileInConflict != null) {
                    file = fileInConflict
                    ConflictsResolveDialog.newInstance(file.remotePath, this).showDialog(this)
                } else {
                    // Account was changed to a different one - just finish
                    finish()
                }
            }
        } else {
            finish()
        }
    }
}
