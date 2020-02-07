package com.owncloud.android.data.user.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.testutil.OC_UserInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class OCUserRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val remoteUserDataSource = mockk<RemoteUserDataSource>(relaxed = true)
    private val ocUserRepository: OCUserRepository = OCUserRepository(remoteUserDataSource)

    @Test
    fun getUserInfo() {
        every { remoteUserDataSource.getUserInfo() } returns OC_UserInfo

        ocUserRepository.getUserInfo()

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo()
        }
    }

    @Test(expected = Exception::class)
    fun checkPathExistenceExistsNoConnection() {
        every { remoteUserDataSource.getUserInfo() }  throws Exception()

        ocUserRepository.getUserInfo()

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo()
        }
    }
}