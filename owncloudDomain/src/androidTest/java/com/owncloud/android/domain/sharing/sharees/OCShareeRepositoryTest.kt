/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.domain.sharing.sharees

import com.owncloud.android.data.sharing.sharees.OCShareeRepository
import com.owncloud.android.domain.utils.DomainTestUtil
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Test

class OCShareeRepositoryTest {
    @Test
    fun loadShareesFromNetworkSuccessfully() {
        val remoteSharees: ArrayList<JSONObject> = arrayListOf(
            DomainTestUtil.createSharee("Thor", "0", "thor", "thor@avengers.com"),
            DomainTestUtil.createSharee("Iron Man", "0", "ironman", "ironman@avengers.com"),
            DomainTestUtil.createSharee("Avengers", "1", "avengers", "everybody@avengers.com")
        )

        val getRemoteShareesOperationResult =
            DomainTestUtil.createRemoteOperationResultMock(arrayListOf(remoteSharees[1]), true)

        val ocShareeRepository = createShareeRepositoryWithRemoteDataSource(getRemoteShareesOperationResult)

        val resource = ocShareeRepository.getSharees(
            "Iron",
            1,
            30
        )

        assertEquals(resource.code, RemoteOperationResult.ResultCode.OK)

        assertEquals(resource.data?.get(0)?.getString(GetRemoteShareesOperation.PROPERTY_LABEL), "Iron Man")
        val value = resource.data?.get(0)?.getJSONObject(GetRemoteShareesOperation.NODE_VALUE)
        assertEquals(value?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE), "0")
        assertEquals(value?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH), "ironman")
        assertEquals(
            value?.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO),
            "ironman@avengers.com"
        )
    }

    @Test
    fun loadEmptyShareesFromNetwork() {
        val remoteSharees: ArrayList<JSONObject> = arrayListOf()

        val getRemoteShareesOperationResult =
            DomainTestUtil.createRemoteOperationResultMock(remoteSharees, true)

        val ocShareeRepository = createShareeRepositoryWithRemoteDataSource(getRemoteShareesOperationResult)

        val resource = ocShareeRepository.getSharees(
            "Thor",
            1,
            30
        )

        assertEquals(resource.code, RemoteOperationResult.ResultCode.OK)
        assertEquals(resource.data?.size, 0)
    }

    @Test
    fun loadEmptyShareesFromNetworkWithError() {
        val remoteSharees: ArrayList<JSONObject> = arrayListOf(
            DomainTestUtil.createSharee("Joker", "0", "joker", "joker@dc.com")
        )

        val exception = Exception("Error when retrieving sharees")

        val getRemoteShareesOperationResult =
            DomainTestUtil.createRemoteOperationResultMock(
                remoteSharees,
                false,
                resultCode = RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE,
                exception = exception
            )

        val ocShareeRepository = createShareeRepositoryWithRemoteDataSource(getRemoteShareesOperationResult)

        val resource = ocShareeRepository.getSharees(
            "Joker",
            1,
            30
        )

        assertEquals(resource.code, RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE)
        assertEquals(resource.exception?.message, exception.message)
    }

    private fun createShareeRepositoryWithRemoteDataSource(
        getRemoteShareesOperationResult: RemoteOperationResult<ArrayList<JSONObject>>
    ): OCShareeRepository {
        val remoteShareesDataSource = RemoteShareesDataSourceTest(getRemoteShareesOperationResult)

        return OCShareeRepository(remoteShareesDataSource)
    }
}
