package com.owncloud.android.domain.searches.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.searches.SavedSearchesRepository

class RemoveSavedSearchUseCase(
    private val repository: SavedSearchesRepository
) : BaseUseCaseWithResult<Unit, RemoveSavedSearchUseCase.Params>() {

    override fun run(params: Params) {
        repository.removeById(params.id)
    }

    data class Params(val id: Long)
}
