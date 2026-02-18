/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.domain.members.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.members.MembersRepository

class EditMemberUseCase(
    private val membersRepository: MembersRepository
): BaseUseCaseWithResult<Unit, EditMemberUseCase.Params>() {

    override fun run(params: Params) {
        membersRepository.editMember(params.accountName, params.spaceId, params.memberId, params.roleId, params.expirationDate)
    }

    data class Params(val accountName: String, val spaceId: String, val memberId: String, val roleId: String, val expirationDate: String?)
}
