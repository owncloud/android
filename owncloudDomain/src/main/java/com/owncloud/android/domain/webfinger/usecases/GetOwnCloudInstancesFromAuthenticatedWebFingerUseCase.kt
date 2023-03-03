/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.domain.webfinger.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.webfinger.WebfingerRepository
import com.owncloud.android.domain.webfinger.model.WebFingerRel

class GetOwnCloudInstancesFromAuthenticatedWebFingerUseCase(
    private val webfingerRepository: WebfingerRepository
) : BaseUseCaseWithResult<List<String>, GetOwnCloudInstancesFromAuthenticatedWebFingerUseCase.Params>() {

    override fun run(params: Params): List<String> =
        webfingerRepository.getInstancesFromAuthenticatedWebFinger(
            server = params.server,
            rel = WebFingerRel.OWNCLOUD_INSTANCE,
            username = "$PREFIX_ACCT_URI_SCHEME:${params.username}",
            accessToken = params.accessToken,
        )

    data class Params(
        val server: String,
        val username: String,
        val accessToken: String,
    )

    companion object {
        // https://datatracker.ietf.org/doc/html/rfc7565#section-4
        private const val PREFIX_ACCT_URI_SCHEME = "acct"
    }
}
