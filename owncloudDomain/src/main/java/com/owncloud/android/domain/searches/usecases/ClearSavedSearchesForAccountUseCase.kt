package com.owncloud.android.domain.searches.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.searches.SavedSearchesRepository

class ClearSavedSearchesForAccountUseCase(
    private val repository: SavedSearchesRepository
) : BaseUseCaseWithResult<Unit, ClearSavedSearchesForAccountUseCase.Params>() {

    override fun run(params: Params) {
        repository.clearForAccount(params.accountName)
    }

    data class Params(val accountName: String)
}
