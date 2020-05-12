package com.owncloud.android.data.user.datasources.mapper

import com.owncloud.android.data.user.db.UserQuotaEntity
import com.owncloud.android.domain.mappers.Mapper
import com.owncloud.android.domain.user.model.UserQuota

class UserQuotaMapper : Mapper<UserQuota, UserQuotaEntity> {
    override fun toModel(entity: UserQuotaEntity?): UserQuota? =
        entity?.let {
            UserQuota(
                available = it.available,
                used = it.used
            )
        }

    override fun toEntity(model: UserQuota?): UserQuotaEntity? =
        model?.let {
            UserQuotaEntity(
                accountName = "",
                available = it.available,
                used = it.used
            )
        }
}
