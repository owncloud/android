package com.owncloud.android.data.user.datasources

import com.owncloud.android.domain.user.model.UserQuota

interface LocalUserDataSource {
    fun saveQuotaForAccount(
        accountName: String,
        userQuota: UserQuota
    )

    fun getQuotaForAccount(
        accountName: String
    ): UserQuota?
}
