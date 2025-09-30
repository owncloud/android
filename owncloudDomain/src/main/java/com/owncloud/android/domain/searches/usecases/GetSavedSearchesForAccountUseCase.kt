package com.owncloud.android.domain.searches.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.searches.SavedSearchesRepository
import com.owncloud.android.domain.searches.model.SavedSearch

class GetSavedSearchesForAccountUseCase(
    private val repository: SavedSearchesRepository
) : BaseUseCase<List<SavedSearch>, GetSavedSearchesForAccountUseCase.Params>() {

    override fun run(params: Params): List<SavedSearch> =
        repository.getForAccount(params.accountName)

    data class Params(val accountName: String)
}
