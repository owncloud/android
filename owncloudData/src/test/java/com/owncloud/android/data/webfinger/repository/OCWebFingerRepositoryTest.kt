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

package com.owncloud.android.data.webfinger.repository

import com.owncloud.android.data.webfinger.datasources.RemoteWebFingerDataSource
import com.owncloud.android.domain.webfinger.model.WebFingerRel
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT_ID
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Test

class OCWebFingerRepositoryTest {

    private val remoteWebFingerDatasource = mockk<RemoteWebFingerDataSource>()
    private val ocWebFingerRepository = OCWebFingerRepository(remoteWebFingerDatasource)

    private val webFingerInstance = "http://webfinger.owncloud/tests/server-instance1"

    @Test
    fun `getInstancesFromWebFinger returns a list of String of webfinger url`() {
        val webFingerResource = "admin"

        every {
            remoteWebFingerDatasource.getInstancesFromWebFinger(
                OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                WebFingerRel.OIDC_ISSUER_DISCOVERY,
                webFingerResource
            )
        } returns listOf(webFingerInstance)

        val webFingerResult = ocWebFingerRepository.getInstancesFromWebFinger(
            OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            WebFingerRel.OIDC_ISSUER_DISCOVERY,
            webFingerResource
        )
        assertEquals(listOf(webFingerInstance), webFingerResult)

        verify(exactly = 1) {
            remoteWebFingerDatasource.getInstancesFromWebFinger(
                OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                WebFingerRel.OIDC_ISSUER_DISCOVERY,
                webFingerResource
            )
        }
    }

    @Test
    fun `getInstancesFromAuthenticatedWebFinger returns a list of String of webfinger url`() {
        val webFingerAuthenticatedResource = "acct:me@demo.owncloud.com"

        every {
            remoteWebFingerDatasource.getInstancesFromAuthenticatedWebFinger(
                OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                WebFingerRel.OIDC_ISSUER_DISCOVERY,
                webFingerAuthenticatedResource,
                OC_ACCOUNT_ID,
                OC_ACCESS_TOKEN
            )
        } returns listOf(webFingerInstance)

        val webFingerResult = ocWebFingerRepository.getInstancesFromAuthenticatedWebFinger(
            OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            WebFingerRel.OIDC_ISSUER_DISCOVERY,
            webFingerAuthenticatedResource,
            OC_ACCOUNT_ID,
            OC_ACCESS_TOKEN
        )
        assertEquals(listOf(webFingerInstance), webFingerResult)

        verify(exactly = 1) {
            remoteWebFingerDatasource.getInstancesFromAuthenticatedWebFinger(
                OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                WebFingerRel.OIDC_ISSUER_DISCOVERY,
                webFingerAuthenticatedResource,
                OC_ACCOUNT_ID,
                OC_ACCESS_TOKEN
            )
        }
    }

}
