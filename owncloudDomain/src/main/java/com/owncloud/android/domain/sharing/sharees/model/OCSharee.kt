package com.owncloud.android.domain.sharing.sharees.model

import com.owncloud.android.domain.sharing.shares.model.ShareType

data class OCSharee(
    val label: String,
    val shareType: ShareType,
    val shareWith: String,
    val additionalInfo: String,
    val isExactMatch: Boolean
)