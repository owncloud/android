package com.owncloud.android.data.user.datasources.implementation

import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.datasources.mapper.UserQuotaMapper
import com.owncloud.android.data.user.db.UserDao
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserQuota

class OCLocalUserDataSource(
    private val userDao: UserDao,
    private val userQuotaMapper: UserQuotaMapper
) : LocalUserDataSource {

    override fun saveQuotaForAccount(accountName: String, userQuota: UserQuota) =
        userDao.insert(userQuotaMapper.toEntity(userQuota)!!.copy(accountName = accountName))

    override fun getQuotaForAccount(accountName: String): UserQuota? =
        userQuotaMapper.toModel(userDao.getQuotaForAccount(accountName))

    override fun saveAvatarForAccount(accountName: String, userAvatar: UserAvatar) {
        TODO("Not yet implemented")
    }

    override fun getAvatarForAccount(accountName: String): UserAvatar? {
        TODO("Not yet implemented")
    }

}
