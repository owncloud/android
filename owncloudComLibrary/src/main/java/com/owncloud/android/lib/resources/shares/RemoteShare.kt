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

    /// Getters and Setters
    var fileSource: Long = 0
    var itemSource: Long = 0
    var shareType: ShareType? = null
    private var mShareWith: String? = null
    private var mPath: String? = null
    var permissions: Int = 0
    var sharedDate: Long = 0
    var expirationDate: Long = 0
    private var mToken: String? = null
    private var mSharedWithDisplayName: String? = null
    var sharedWithAdditionalInfo: String? = null
    private var mName: String? = null
    var isFolder: Boolean = false
    var userId: Long = 0
    var remoteId: Long = 0
        private set
    private var mShareLink: String? = null

    var shareWith: String?
        get() = mShareWith
        set(shareWith) {
            this.mShareWith = shareWith ?: ""
        }

    var path: String?
        get() = mPath
        set(path) {
            this.mPath = path ?: ""
        }

    var token: String?
        get() = mToken
        set(token) {
            this.mToken = token ?: ""
        }

    var sharedWithDisplayName: String?
        get() = mSharedWithDisplayName
        set(sharedWithDisplayName) {
            this.mSharedWithDisplayName = sharedWithDisplayName ?: ""
        }

    var name: String?
        get() = mName
        set(name) {
            mName = name ?: ""
        }

    var shareLink: String?
        get() = this.mShareLink
        set(shareLink) {
            this.mShareLink = shareLink ?: ""
        }

    val isPasswordProtected: Boolean
        get() = ShareType.PUBLIC_LINK == shareType && mShareWith!!.length > 0

    constructor() : super() {
        resetData()
    }

    constructor(path: String?) {
        resetData()
        if (path == null || path.length <= 0 || !path.startsWith(FileUtils.PATH_SEPARATOR)) {
            Log_OC.e(TAG, "Trying to create a RemoteShare with a non valid path")
            throw IllegalArgumentException("Trying to create a RemoteShare with a non valid path: " + path!!)
        }
        mPath = path
    }

    /**
     * Used internally. Reset all file properties
     */
    private fun resetData() {
        fileSource = 0
        itemSource = 0
        shareType = ShareType.NO_SHARED
        mShareWith = ""
        mPath = ""
        permissions = -1
        sharedDate = 0
        expirationDate = 0
        mToken = ""
        mSharedWithDisplayName = ""
        sharedWithAdditionalInfo = ""
        isFolder = false
        userId = -1
        remoteId = -1
        mShareLink = ""
        mName = ""
    }

    fun setIdRemoteShared(remoteId: Long) {
        this.remoteId = remoteId
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
        fileSource = source.readLong()
        itemSource = source.readLong()
        try {
            shareType = ShareType.valueOf(source.readString())
        } catch (x: IllegalArgumentException) {
            shareType = ShareType.NO_SHARED
        }

        mShareWith = source.readString()
        mPath = source.readString()
        permissions = source.readInt()
        sharedDate = source.readLong()
        expirationDate = source.readLong()
        mToken = source.readString()
        mSharedWithDisplayName = source.readString()
        sharedWithAdditionalInfo = source.readString()
        isFolder = source.readInt() == 0
        userId = source.readLong()
        remoteId = source.readLong()
        mShareLink = source.readString()
        mName = source.readString()
    }

    override fun describeContents(): Int {
        return this.hashCode()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(fileSource)
        dest.writeLong(itemSource)
        dest.writeString(if (shareType == null) "" else shareType!!.name)
        dest.writeString(mShareWith)
        dest.writeString(mPath)
        dest.writeInt(permissions)
        dest.writeLong(sharedDate)
        dest.writeLong(expirationDate)
        dest.writeString(mToken)
        dest.writeString(mSharedWithDisplayName)
        dest.writeString(sharedWithAdditionalInfo)
        dest.writeInt(if (isFolder) 1 else 0)
        dest.writeLong(userId)
        dest.writeLong(remoteId)
        dest.writeString(mShareLink)
        dest.writeString(mName)
    }

    companion object {

        /**
         * Generated - should be refreshed every time the class changes!!
         */
        private const val serialVersionUID = 4124975224281327921L

        private val TAG = RemoteShare::class.java.simpleName

        val DEFAULT_PERMISSION = -1
        val READ_PERMISSION_FLAG = 1
        val UPDATE_PERMISSION_FLAG = 2
        val CREATE_PERMISSION_FLAG = 4
        val DELETE_PERMISSION_FLAG = 8
        val SHARE_PERMISSION_FLAG = 16
        val MAXIMUM_PERMISSIONS_FOR_FILE = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        val MAXIMUM_PERMISSIONS_FOR_FOLDER = MAXIMUM_PERMISSIONS_FOR_FILE +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        val FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9 = READ_PERMISSION_FLAG + UPDATE_PERMISSION_FLAG
        val FEDERATED_PERMISSIONS_FOR_FILE_AFTER_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        val FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        val FEDERATED_PERMISSIONS_FOR_FOLDER_AFTER_OC9 =
            FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 + SHARE_PERMISSION_FLAG

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
