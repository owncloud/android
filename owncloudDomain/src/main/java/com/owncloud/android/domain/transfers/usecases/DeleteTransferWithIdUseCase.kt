/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
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

package com.owncloud.android.domain.transfers.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.transfers.TransferRepository

class DeleteTransferWithIdUseCase(
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, DeleteTransferWithIdUseCase.Params>() {
    override fun run(params: Params) =
        transferRepository.removeTransferById(params.id)

    data class Params(val id: Long)
}
