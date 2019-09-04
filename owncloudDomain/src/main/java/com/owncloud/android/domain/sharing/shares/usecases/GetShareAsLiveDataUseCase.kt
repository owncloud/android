package com.owncloud.android.domain.sharing.shares.usecases

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import com.owncloud.android.data.sharing.shares.ShareRepository
import com.owncloud.android.data.sharing.shares.datasources.OCLocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.OCRemoteShareDataSource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.sharing.shares.OCShareRepository
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory

class GetShareAsLiveDataUseCase(
    context: Context,
    account: Account,
    private val shareRepository: ShareRepository = OCShareRepository(
        localShareDataSource = OCLocalShareDataSource(context),
        remoteShareDataSource = OCRemoteShareDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, context),
                context
            )
        )
    )
): BaseUseCase<LiveData<OCShareEntity>, GetShareAsLiveDataUseCase.Params>() {
    override fun run(params: Params): UseCaseResult<LiveData<OCShareEntity>> {
        shareRepository.getShareAsLiveData(
            params.remoteId
        ).also { shareAsLiveData ->
            return UseCaseResult.success(shareAsLiveData) // Always successful here, data comes from database
        }
    }

    data class Params(
        val remoteId: Long
    )
}
