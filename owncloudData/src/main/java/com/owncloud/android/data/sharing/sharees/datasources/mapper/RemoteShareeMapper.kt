package com.owncloud.android.data.sharing.sharees.datasources.mapper

import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.domain.sharing.sharees.model.OCSharee
import com.owncloud.android.domain.sharing.sharees.model.ShareeType
import com.owncloud.android.lib.resources.shares.responses.ShareeItem
import com.owncloud.android.lib.resources.shares.responses.ShareeOcsResponse

class RemoteUserShareeMapper : RemoteMapper<List<OCSharee>, ShareeOcsResponse> {
    private fun mapShareeItemToOCShare(item: ShareeItem, isExactMatch: Boolean) =
        OCSharee(
            label = item.label!!,
            shareType = ShareeType.getByValue(item.value?.shareType!!)!!,
            shareWith = item.value?.shareWith!!,
            additionalInfo = item.value?.additionalInfo ?: "",
            isExactMatch = isExactMatch
        )

    override fun toModel(remote: ShareeOcsResponse?): List<OCSharee>? {
        val exactMatches = remote?.exact?.getFlatRepresentation()?.map {
            mapShareeItemToOCShare(it, isExactMatch = true)
        }
        val nonExactMatches = remote?.getFlatRepresentationWithoutExact()?.map {
            mapShareeItemToOCShare(it, isExactMatch = false)
        }
        return ArrayList<OCSharee>().apply {
            if (exactMatches != null) {
                addAll(exactMatches)
            }
            if (nonExactMatches != null) {
                addAll(nonExactMatches)
            }
        }
    }

    // not needed
    override fun toRemote(model: List<OCSharee>?): ShareeOcsResponse? = null
}