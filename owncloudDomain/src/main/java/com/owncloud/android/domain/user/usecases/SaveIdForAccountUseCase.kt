/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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
package com.owncloud.android.domain.user.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.user.UserRepository

class SaveIdForAccountUseCase(
    private val userRepository: UserRepository,
) : BaseUseCase<Unit, SaveIdForAccountUseCase.Params>() {

    override fun run(params: Params) = userRepository.saveUserId(params.accountName)

    data class Params(val accountName: String)
}
