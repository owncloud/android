package com.owncloud.android.data.user.datasources

import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserQuota

interface LocalUserDataSource {
    fun saveQuotaForAccount(
        accountName: String,
        userQuota: UserQuota
    )

    fun getQuotaForAccount(
        accountName: String
    ): UserQuota?

    fun saveAvatarForAccount(
        accountName: String,
        userAvatar: UserAvatar
    )

    fun getAvatarForAccount(
        accountName: String
    ): UserAvatar?
}
