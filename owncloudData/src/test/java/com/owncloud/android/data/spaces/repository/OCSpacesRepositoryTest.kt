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

package com.owncloud.android.data.spaces.repository

import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.spaces.datasources.LocalSpacesDataSource
import com.owncloud.android.data.spaces.datasources.RemoteSpacesDataSource
import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.OC_CAPABILITY_WITH_MULTIPERSONAL_ENABLED
import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import com.owncloud.android.testutil.OC_SPACE_PERSONAL_WITH_LIMITED_QUOTA
import com.owncloud.android.testutil.OC_SPACE_PERSONAL_WITH_UNLIMITED_QUOTA
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.testutil.OC_USER_QUOTA_LIMITED
import com.owncloud.android.testutil.OC_USER_QUOTA_UNLIMITED
import com.owncloud.android.testutil.OC_USER_QUOTA_WITHOUT_PERSONAL
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OCSpacesRepositoryTest {

    private val localSpacesDataSource = mockk<LocalSpacesDataSource>(relaxUnitFun = true)
    private val localUserDataSource = mockk<LocalUserDataSource>(relaxUnitFun = true)
    private val remoteSpacesDataSource = mockk<RemoteSpacesDataSource>()
    private val localCapabilitiesDataSource = mockk<LocalCapabilitiesDataSource>(relaxUnitFun = true)
    private val ocSpacesRepository = OCSpacesRepository(localSpacesDataSource, localUserDataSource, remoteSpacesDataSource,
        localCapabilitiesDataSource)

    @Test
    fun `refreshSpacesForAccount refreshes spaces for account correctly when multipersonal is enabled`() {
        every {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
        } returns listOf(OC_SPACE_PERSONAL)

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
        } returns OC_CAPABILITY_WITH_MULTIPERSONAL_ENABLED

        ocSpacesRepository.refreshSpacesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
            localSpacesDataSource.saveSpacesForAccount(listOf(OC_SPACE_PERSONAL))
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA_WITHOUT_PERSONAL)
        }
    }

    @Test
    fun `refreshSpacesForAccount refreshes spaces for account correctly when quota is unlimited`() {
        every {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
        } returns listOf(OC_SPACE_PERSONAL_WITH_UNLIMITED_QUOTA)

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
        } returns OC_CAPABILITY

        ocSpacesRepository.refreshSpacesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
            localSpacesDataSource.saveSpacesForAccount(listOf(OC_SPACE_PERSONAL_WITH_UNLIMITED_QUOTA))
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA_UNLIMITED)
        }
    }

    @Test
    fun `refreshSpacesForAccount refreshes spaces for account correctly when quota is limited`() {
        every {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
        } returns listOf(OC_SPACE_PERSONAL_WITH_LIMITED_QUOTA)

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
        } returns OC_CAPABILITY

        ocSpacesRepository.refreshSpacesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
            localSpacesDataSource.saveSpacesForAccount(listOf(OC_SPACE_PERSONAL_WITH_LIMITED_QUOTA))
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA_LIMITED)
        }
    }

    @Test
    fun `refreshSpacesForAccount refreshes spaces for account correctly when personal space does not exist`() {
        every {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
        } returns listOf(OC_SPACE_PROJECT_WITH_IMAGE)

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
        } returns OC_CAPABILITY

        ocSpacesRepository.refreshSpacesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
            localSpacesDataSource.saveSpacesForAccount(listOf(OC_SPACE_PROJECT_WITH_IMAGE))
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
            localUserDataSource.saveQuotaForAccount(OC_ACCOUNT_NAME, OC_USER_QUOTA_WITHOUT_PERSONAL)
        }
    }

    @Test
    fun `getSpacesFromEveryAccountAsStream returns a Flow with a list of OCSpace`() = runTest {
        every {
            localSpacesDataSource.getSpacesFromEveryAccountAsStream()
        } returns flowOf(listOf(OC_SPACE_PROJECT_WITH_IMAGE))

        val listOfSpaces = ocSpacesRepository.getSpacesFromEveryAccountAsStream().first()
        assertEquals(listOf(OC_SPACE_PROJECT_WITH_IMAGE), listOfSpaces)

        verify(exactly = 1) {
            localSpacesDataSource.getSpacesFromEveryAccountAsStream()
        }
    }

    @Test
    fun `getSpacesByDriveTypeWithSpecialsForAccountAsFlow returns a Flow with a list of OCSpace`() = runTest {
        every {
            localSpacesDataSource.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(OC_ACCOUNT_NAME, setOf(OC_SPACE_PROJECT_WITH_IMAGE.driveType))
        } returns flowOf(listOf(OC_SPACE_PROJECT_WITH_IMAGE))

        val listOfSpaces = ocSpacesRepository.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(OC_ACCOUNT_NAME,
            setOf(OC_SPACE_PROJECT_WITH_IMAGE.driveType)).first()
        assertEquals(listOf(OC_SPACE_PROJECT_WITH_IMAGE), listOfSpaces)

        verify(exactly = 1) {
            localSpacesDataSource.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(OC_ACCOUNT_NAME, setOf(OC_SPACE_PROJECT_WITH_IMAGE.driveType))
        }
    }

    @Test
    fun `getPersonalSpaceForAccount returns an OCSpace`() {
        every {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_ACCOUNT_NAME)
        } returns OC_SPACE_PERSONAL

        val personalSpace = ocSpacesRepository.getPersonalSpaceForAccount(OC_ACCOUNT_NAME)
        assertEquals(OC_SPACE_PERSONAL, personalSpace)

        verify(exactly = 1) {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getPersonalSpaceForAccount returns null when local datasource returns a null personal space`() {
        every {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_ACCOUNT_NAME)
        } returns null

        val personalSpace = ocSpacesRepository.getPersonalSpaceForAccount(OC_ACCOUNT_NAME)
        assertNull(personalSpace)

        verify(exactly = 1) {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getPersonalAndProjectSpacesForAccount returns a list of OCSpace`() {
        every {
            localSpacesDataSource.getPersonalAndProjectSpacesForAccount(OC_ACCOUNT_NAME)
        } returns listOf(OC_SPACE_PERSONAL, OC_SPACE_PROJECT_WITH_IMAGE)

        val listOfSpaces = ocSpacesRepository.getPersonalAndProjectSpacesForAccount(OC_ACCOUNT_NAME)
        assertEquals(listOf(OC_SPACE_PERSONAL, OC_SPACE_PROJECT_WITH_IMAGE), listOfSpaces)

        verify(exactly = 1) {
            localSpacesDataSource.getPersonalAndProjectSpacesForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getSpaceWithSpecialsByIdForAccount returns an OCSpace`() {
        every {
            localSpacesDataSource.getSpaceWithSpecialsByIdForAccount(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        } returns OC_SPACE_PROJECT_WITH_IMAGE

        val spaceWithSpecials = ocSpacesRepository.getSpaceWithSpecialsByIdForAccount(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        assertEquals(OC_SPACE_PROJECT_WITH_IMAGE, spaceWithSpecials)

        verify(exactly = 1) {
            localSpacesDataSource.getSpaceWithSpecialsByIdForAccount(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getSpaceByIdForAccount returns an OCSpace`() {
        every {
            localSpacesDataSource.getSpaceByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)
        } returns OC_SPACE_PERSONAL

        val space = ocSpacesRepository.getSpaceByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)
        assertEquals(OC_SPACE_PERSONAL, space)

        verify(exactly = 1) {
            localSpacesDataSource.getSpaceByIdForAccount(OC_SPACE_PERSONAL.id, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getSpaceByIdForAccount returns null when local datasource returns a null space`() {
        every {
            localSpacesDataSource.getSpaceByIdForAccount(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        } returns null

        val space = ocSpacesRepository.getSpaceByIdForAccount(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        assertNull(space)

        verify(exactly = 1) {
            localSpacesDataSource.getSpaceByIdForAccount(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getWebDavUrlForSpace returns a String of webdav url`() {
        every {
            localSpacesDataSource.getWebDavUrlForSpace(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        } returns OC_SPACE_PROJECT_WITH_IMAGE.webUrl

        val webDavUrl = ocSpacesRepository.getWebDavUrlForSpace(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id)
        assertEquals(OC_SPACE_PROJECT_WITH_IMAGE.webUrl, webDavUrl)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getWebDavUrlForSpace returns null when local datasource returns null`() {
        every {
            localSpacesDataSource.getWebDavUrlForSpace(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        } returns null

        val webDavUrl = ocSpacesRepository.getWebDavUrlForSpace(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id)
        assertNull(webDavUrl)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(OC_SPACE_PROJECT_WITH_IMAGE.id, OC_ACCOUNT_NAME)
        }
    }

}
