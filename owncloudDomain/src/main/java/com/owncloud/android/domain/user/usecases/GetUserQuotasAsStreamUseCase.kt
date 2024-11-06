package com.owncloud.android.domain.user.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.user.UserRepository
import com.owncloud.android.domain.user.model.UserQuota
import kotlinx.coroutines.flow.Flow

class GetUserQuotasAsStreamUseCase(
    private val userRepository: UserRepository
) : BaseUseCase<Flow<List<UserQuota>>, Unit>() {
    override fun run(params: Unit): Flow<List<UserQuota>> =
        userRepository.getAllUserQuotasAsStream()
}
