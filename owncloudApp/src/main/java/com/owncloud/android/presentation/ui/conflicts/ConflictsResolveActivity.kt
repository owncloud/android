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

import android.accounts.Account
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.ui.conflicts.fragments.ConflictsResolveDialogFragment
import com.owncloud.android.presentation.viewmodels.conflicts.ConflictsResolveViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConflictsResolveActivity : AppCompatActivity(), ConflictsResolveDialogFragment.OnConflictDecisionMadeListener {

    private val conflictsResolveViewModel by viewModel<ConflictsResolveViewModel>()
    private lateinit var account: Account
    private lateinit var file: OCFile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = intent.getParcelableExtra(EXTRA_ACCOUNT)!!
        file = intent.getParcelableExtra(EXTRA_FILE)!!
        ConflictsResolveDialogFragment.newInstance(file.remotePath, this).showDialog(this)
    }

    override fun conflictDecisionMade(decision: ConflictsResolveDialogFragment.Decision) {
        var forceOverwrite = false

        when (decision) {
            ConflictsResolveDialogFragment.Decision.CANCEL -> {
                finish()
                return
            }
            ConflictsResolveDialogFragment.Decision.LOCAL -> forceOverwrite = true
            ConflictsResolveDialogFragment.Decision.KEEP_BOTH -> {}
            ConflictsResolveDialogFragment.Decision.SERVER -> {
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

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_FILE = "EXTRA_FILE"
    }
}
