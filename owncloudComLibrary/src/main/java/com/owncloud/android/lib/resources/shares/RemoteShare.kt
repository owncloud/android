/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2019 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.shares

import android.os.Parcel
import android.os.Parcelable
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.FileUtils
import java.io.Serializable

/**
 * Contains the data of a Share from the Share API
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */
class RemoteShare : Parcelable, Serializable {
    var id: Long = 0
    var shareWith: String = ""
    var path: String = ""
    var token: String = ""
    var sharedWithDisplayName: String = ""
    var sharedWithAdditionalInfo: String = ""
    var name: String = ""
    var shareLink: String = ""
    var fileSource: Long = 0
    var itemSource: Long = 0
    var shareType: ShareType? = null
    var permissions: Int = DEFAULT_PERMISSION
    var sharedDate: Long = INIT_SHARED_DATE
    var expirationDate: Long = INIT_EXPIRATION_DATE_IN_MILLIS
    var isFolder: Boolean = path.endsWith(FileUtils.PATH_SEPARATOR)
    var userId: Long = 0

    val isValid: Boolean = id > -1

    constructor() : super() {
        resetData()
    }

    constructor(path: String?) {
        resetData()
        if (path.isNullOrEmpty() || !path.startsWith(FileUtils.PATH_SEPARATOR)) {
            Log_OC.e(TAG, "Trying to create a RemoteShare with a non valid path")
            throw IllegalArgumentException("Trying to create a RemoteShare with a non valid path: " + path!!)
        }
        this.path = path
    }

    /**
     * Used internally. Reset all file properties
     */
    private fun resetData() {
        id = -1
        shareWith = ""
        path = ""
        token = ""
        sharedWithDisplayName = ""
        sharedWithAdditionalInfo = ""
        name = ""
        shareLink = ""
        fileSource = 0
        itemSource = 0
        shareType = ShareType.NO_SHARED
        permissions = DEFAULT_PERMISSION
        sharedDate = INIT_SHARED_DATE
        expirationDate = INIT_EXPIRATION_DATE_IN_MILLIS
        sharedWithAdditionalInfo = ""
        isFolder = false
        userId = -1
    }

    /**
     * Reconstruct from parcel
     *
     * @param source The source parcel
     */
    protected constructor(source: Parcel) {
        readFromParcel(source)
    }

    fun readFromParcel(source: Parcel) {
        id = source.readLong()
        shareWith = source.readString()
        path = source.readString()
        token = source.readString()
        sharedWithDisplayName = source.readString()
        sharedWithAdditionalInfo = source.readString()
        name = source.readString()
        shareLink = source.readString()
        fileSource = source.readLong()
        itemSource = source.readLong()
        try {
            shareType = ShareType.valueOf(source.readString())
        } catch (x: IllegalArgumentException) {
            shareType = ShareType.NO_SHARED
        }
        permissions = source.readInt()
        sharedDate = source.readLong()
        expirationDate = source.readLong()
        isFolder = source.readInt() == 0
        userId = source.readLong()
    }

    override fun describeContents(): Int = this.hashCode()

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(shareWith)
        dest.writeString(path)
        dest.writeString(token)
        dest.writeString(sharedWithDisplayName)
        dest.writeString(sharedWithAdditionalInfo)
        dest.writeString(name)
        dest.writeString(shareLink)
        dest.writeLong(fileSource)
        dest.writeLong(itemSource)
        dest.writeString(shareType?.name ?: "")
        dest.writeInt(permissions)
        dest.writeLong(sharedDate)
        dest.writeLong(expirationDate)
        dest.writeInt(if (isFolder) 1 else 0)
        dest.writeLong(userId)
    }

    companion object {

        /**
         * Generated - should be refreshed every time the class changes!!
         */
        private const val serialVersionUID = 4124975224281327921L

        private val TAG = RemoteShare::class.java.simpleName

        const val DEFAULT_PERMISSION = -1
        const val READ_PERMISSION_FLAG = 1
        const val UPDATE_PERMISSION_FLAG = 2
        const val CREATE_PERMISSION_FLAG = 4
        const val DELETE_PERMISSION_FLAG = 8
        const val SHARE_PERMISSION_FLAG = 16
        const val MAXIMUM_PERMISSIONS_FOR_FILE = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        const val MAXIMUM_PERMISSIONS_FOR_FOLDER = MAXIMUM_PERMISSIONS_FOR_FILE +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9 = READ_PERMISSION_FLAG + UPDATE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FILE_AFTER_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FOLDER_AFTER_OC9 =
            FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 + SHARE_PERMISSION_FLAG

        const val INIT_EXPIRATION_DATE_IN_MILLIS: Long = 0
        const val INIT_SHARED_DATE: Long = 0

        /**
         * Parcelable Methods
         */
        @JvmField
        val CREATOR: Parcelable.Creator<RemoteShare> = object : Parcelable.Creator<RemoteShare> {
            override fun createFromParcel(source: Parcel): RemoteShare {
                return RemoteShare(source)
            }

            override fun newArray(size: Int): Array<RemoteShare?> {
                return arrayOfNulls(size)
            }
        }
    }
}
