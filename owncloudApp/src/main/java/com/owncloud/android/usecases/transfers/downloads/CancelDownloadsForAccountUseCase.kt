/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.usecases.transfers.downloads

import android.accounts.Account
import androidx.work.WorkManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.extensions.getWorkInfoByTags
import com.owncloud.android.usecases.transfers.TRANSFER_TAG_DOWNLOAD

/**
 * Cancel every pending download for an account. Note that cancellation is a best-effort
 * policy and work that is already executing may continue to run.
 */
class CancelDownloadsForAccountUseCase(
    private val workManager: WorkManager
) : BaseUseCase<Unit, CancelDownloadsForAccountUseCase.Params>() {

    override fun run(params: Params) {
        val account = params.account
        val workersToCancel = workManager.getWorkInfoByTags(listOf(TRANSFER_TAG_DOWNLOAD, account.name))
        workersToCancel.forEach {
            workManager.cancelWorkById(it.id)
        }
    }

    data class Params(
        val account: Account
    )
}