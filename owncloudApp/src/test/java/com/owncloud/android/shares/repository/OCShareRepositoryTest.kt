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

package com.owncloud.android.shares.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasources.OCLocalSharesDataSource
import com.owncloud.android.shares.datasources.OCRemoteSharesDataSource
import com.owncloud.android.shares.datasources.RemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.db.OCShareDao
import com.owncloud.android.utils.mock
import com.owncloud.android.vo.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*

@RunWith(JUnit4::class)
class OCShareRepositoryTest {
    private lateinit var ocShareRepository: OCShareRepository

    private val ocShareDao = mock(OCShareDao::class.java)
    private val ocLocalSharesDataSource = OCLocalSharesDataSource(ocShareDao)
    private val ocRemoteSharesDataSource = mock(OCRemoteSharesDataSource::class.java)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        ocShareRepository = OCShareRepository.create(ocLocalSharesDataSource, ocRemoteSharesDataSource)
    }

    @Test
    fun loadSharesForFileFromNetwork() {
        runBlocking {
            val dbData = MutableLiveData<List<OCShare>>()

            `when`(
                ocShareDao.getSharesForFileAsLiveData(
                    "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
                )
            ).thenReturn(dbData)

            // Load shares
            val data = ocShareRepository.loadSharesForFile(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK), true, false
            )

            verify(ocShareDao).getSharesForFileAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )

            val observer = mock<Observer<Resource<List<OCShare>>>>()
            data.observeForever(observer)
            verifyNoMoreInteractions(ocRemoteSharesDataSource)

            dbData.postValue(listOf())
        }
    }
}