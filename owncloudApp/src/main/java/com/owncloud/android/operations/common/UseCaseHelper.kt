/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2019 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.operations.common

import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.server.usecases.CheckPathExistenceUseCase
import com.owncloud.android.domain.server.usecases.GetServerInfoUseCase
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.usecases.GetUserInfoUseCase
import org.koin.core.KoinComponent
import org.koin.core.inject

/*
 * Helper to call usecases from java classes.
 * TODO: Remove this and call directly to usecases.
 */
class UseCaseHelper : KoinComponent {
    private val getUserInfoUseCase: GetUserInfoUseCase by inject()
    private val checkPathExistenceUseCase: CheckPathExistenceUseCase by inject()
    private val getServerInfoUseCase: GetServerInfoUseCase by inject()

    fun getUserInfo(): UseCaseResult<UserInfo> = getUserInfoUseCase.execute(Unit)

    fun checkPathExistence(remotePath: String): UseCaseResult<Any> =
        checkPathExistenceUseCase.execute(CheckPathExistenceUseCase.Params(remotePath, false))

    fun getServerInfo(serverUrl: String): UseCaseResult<ServerInfo> =
        getServerInfoUseCase.execute(GetServerInfoUseCase.Params(serverPath = serverUrl))
}
