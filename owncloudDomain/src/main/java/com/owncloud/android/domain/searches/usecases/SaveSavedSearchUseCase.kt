package com.owncloud.android.domain.searches.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.searches.SavedSearchesRepository
import com.owncloud.android.domain.searches.model.SavedSearch

class SaveSavedSearchUseCase(
    private val repository: SavedSearchesRepository
) : BaseUseCaseWithResult<SavedSearch, SaveSavedSearchUseCase.Params>() {

    override fun run(params: Params): SavedSearch =
        repository.save(params.savedSearch)

    data class Params(val savedSearch: SavedSearch)
}
