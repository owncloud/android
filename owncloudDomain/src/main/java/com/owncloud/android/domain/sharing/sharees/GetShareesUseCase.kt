package com.owncloud.android.domain.sharing.sharees

import android.accounts.Account
import android.content.Context
import com.owncloud.android.data.sharing.sharees.ShareeRepository
import com.owncloud.android.data.sharing.sharees.datasources.OCRemoteShareeDataSource
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.sharing.shares.usecases.BaseUseCase
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import org.json.JSONObject

class GetShareesUseCase(
    context: Context,
    account: Account,
    private val shareeRepository: ShareeRepository = OCShareeRepository(
        remoteShareDataSource = OCRemoteShareeDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, context),
                context
            )
        )
    )
) : BaseUseCase<ArrayList<JSONObject>, GetShareesUseCase.Params>() {
    override fun run(params: Params): UseCaseResult<ArrayList<JSONObject>> {
        shareeRepository.getSharees(
            params.searchString,
            params.page,
            params.perPage
        ).also { dataResult ->
            if (!dataResult.isSuccess()) {
                return UseCaseResult.error(
                    code = dataResult.code,
                    msg = dataResult.msg,
                    exception = dataResult.exception
                )
            }

            return UseCaseResult.success(data = dataResult.data)
        }
    }

    data class Params(
        val searchString: String,
        val page: Int,
        val perPage: Int
    )
}
