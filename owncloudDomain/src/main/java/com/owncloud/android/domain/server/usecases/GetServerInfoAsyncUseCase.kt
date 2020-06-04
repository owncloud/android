/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.domain.server.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.server.ServerInfoRepository
import com.owncloud.android.domain.server.model.ServerInfo

class GetServerInfoAsyncUseCase(
    private val serverInfoRepository: ServerInfoRepository
) : BaseUseCaseWithResult<ServerInfo, GetServerInfoAsyncUseCase.Params>() {
    override fun run(params: Params): ServerInfo =
        serverInfoRepository.getServerInfo(params.serverPath.trimEnd(TRAILING_SLASH))

    data class Params(
        val serverPath: String
    )

    companion object {
        const val TRAILING_SLASH = '/'
    }
}
