package com.owncloud.android.domain.appregistry.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.appregistry.AppRegistryRepository
import com.owncloud.android.domain.appregistry.model.AppRegistry
import kotlinx.coroutines.flow.Flow

class GetAppRegistriesForAccountUseCase(
    private val appRegistryRepository: AppRegistryRepository,
) : BaseUseCase<Flow<AppRegistry?>, GetAppRegistriesForAccountUseCase.Params>() {

    override fun run(params: Params) =
        appRegistryRepository.getAppRegistriesForAccount(accountName = params.accountName)

    data class Params(
        val accountName: String,
    )
}
