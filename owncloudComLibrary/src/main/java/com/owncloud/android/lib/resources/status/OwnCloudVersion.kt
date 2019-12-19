/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.lib.resources.status

import android.os.Parcel
import android.os.Parcelable

class OwnCloudVersion(version: String) : Comparable<OwnCloudVersion>, Parcelable {

    // format is in version
    // 0xAABBCCDD
    // for version AA.BB.CC.DD
    // ie version 2.0.3 will be stored as 0x02000300
    private var mVersion: Int = 0
    var isVersionValid: Boolean = false
        set

    val version: String
        get() = if (isVersionValid) {
            toString()
        } else {
            INVALID_ZERO_VERSION
        }

    val isChunkedUploadSupported: Boolean
        get() = mVersion >= MINIMUN_VERSION_FOR_CHUNKED_UPLOADS

    val isSharedSupported: Boolean
        get() = mVersion >= MINIMUM_VERSION_FOR_SHARING_API

    val isVersionWithForbiddenCharacters: Boolean
        get() = mVersion >= MINIMUM_VERSION_WITH_FORBIDDEN_CHARS

    val isAfter8Version: Boolean
        get() = mVersion >= VERSION_8

    val isSearchUsersSupported: Boolean
        get() = mVersion >= MINIMUM_VERSION_FOR_SEARCHING_USERS

    val isVersionWithCapabilitiesAPI: Boolean
        get() = mVersion >= MINIMUM_VERSION_CAPABILITIES_API

    val isNotReshareableFederatedSupported: Boolean
        get() = mVersion >= MINIMUM_VERSION_WITH_NOT_RESHAREABLE_FEDERATED

    val isSessionMonitoringSupported: Boolean
        get() = mVersion >= MINIMUM_VERSION_WITH_SESSION_MONITORING

    /**
     * From OC 9.1 session tracking is a feature, but to get it working in the OC app we need the preemptive
     * mode of basic authentication is disabled. This changes in OC 9.1.3, where preemptive mode is compatible
     * with session tracking again.
     *
     * @return True for every version before 9.1 and from 9.1.3, false otherwise
     */
    val isPreemptiveAuthenticationPreferred: Boolean
        get() = mVersion < MINIMUM_VERSION_WITH_SESSION_MONITORING || mVersion >= MINIMUM_VERSION_WITH_SESSION_MONITORING_WORKING_IN_PREEMPTIVE_MODE

    val isVersionLowerThan10: Boolean
        get() = mVersion < VERSION_10

    val isMultiplePublicSharingSupported: Boolean
        get() = mVersion >= MINIMUM_VERSION_WITH_MULTIPLE_PUBLIC_SHARING

    val isPublicSharingWriteOnlySupported: Boolean
        get() = mVersion >= MINIMUM_VERSION_WITH_WRITE_ONLY_PUBLIC_SHARING

    val isPublicUploadPermissionNeeded: Boolean
        get() = mVersion >= MINIMUN_MAJOR_VERSION_WITHOUT_PUBLIC_UPLOAD_PERMISSION &&
                (mVersion > MINIMUN_MINOR_VERSION_WITHOUT_PUBLIC_UPLOAD_PERMISSION ||
                        mVersion > MINIMUN_MICRO_VERSION_WITHOUT_PUBLIC_UPLOAD_PERMISSION)

    init {
        var version = version
        mVersion = 0
        isVersionValid = false
        val countDots = version.length - version.replace(".", "").length

        // Complete the version. Version must have 3 dots
        for (i in countDots until MAX_DOTS) {
            version = "$version.0"
        }

        parseVersion(version)

    }

    override fun toString(): String {
        // gets the first digit of version, shifting hexadecimal version to right 'til max position
        var versionToString = ((mVersion shr 8 * MAX_DOTS) % 256).toString()
        for (i in MAX_DOTS - 1 downTo 0) {
            // gets another digit of version, shifting hexadecimal version to right 8*i bits and...
            // ...discarding left part with mod 256
            versionToString = versionToString + "." + ((mVersion shr 8 * i) % 256).toString()
        }
        if (!isVersionValid) {
            versionToString += " INVALID"
        }
        return versionToString
    }

    override fun compareTo(another: OwnCloudVersion): Int {
        return if (another.mVersion == mVersion)
            0
        else if (another.mVersion < mVersion) 1 else -1
    }

    private fun parseVersion(version: String) {
        try {
            mVersion = getParsedVersion(version)
            isVersionValid = true

        } catch (e: Exception) {
            isVersionValid = false
            // if invalid, the instance will respond as if server is 8.1, minimum with capabilities API,
            // and "dead" : https://github.com/owncloud/core/wiki/Maintenance-and-Release-Schedule
            mVersion = MINIMUM_VERSION_CAPABILITIES_API
        }

    }

    @Throws(NumberFormatException::class)
    private fun getParsedVersion(version: String): Int {
        var version = version
        var versionValue = 0

        // get only numeric part
        version = version.replace("[^\\d.]".toRegex(), "")

        val nums = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var i = 0
        while (i < nums.size && i <= MAX_DOTS) {
            versionValue += Integer.parseInt(nums[i])
            if (i < nums.size - 1) {
                versionValue = versionValue shl 8
            }
            i++
        }

        return versionValue
    }

    fun supportsRemoteThumbnails(): Boolean {
        return mVersion >= MINIMUM_SERVER_VERSION_FOR_REMOTE_THUMBNAILS
    }

    override fun describeContents(): Int {
        return super.hashCode()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mVersion)
        dest.writeInt(if (isVersionValid) 1 else 0)
    }

    companion object {
        private const val MINIMUN_MINOR_VERSION_WITHOUT_PUBLIC_UPLOAD_PERMISSION = 0x01000000 // 1.0.0

        private const val MINIMUN_MICRO_VERSION_WITHOUT_PUBLIC_UPLOAD_PERMISSION = 0x03000000 // 3.0.0

        const val MINIMUN_VERSION_FOR_CHUNKED_UPLOADS = 0x04050000 // 4.5

        const val MINIMUM_VERSION_FOR_SHARING_API = 0x05001B00 // 5.0.27

        const val MINIMUM_VERSION_WITH_FORBIDDEN_CHARS = 0x08010000 // 8.1

        const val MINIMUM_SERVER_VERSION_FOR_REMOTE_THUMBNAILS = 0x07080000 // 7.8.0

        const val MINIMUM_VERSION_FOR_SEARCHING_USERS = 0x08020000 //8.2

        const val VERSION_8 = 0x08000000 // 8.0

        const  val MINIMUM_VERSION_CAPABILITIES_API = 0x08010000 // 8.1

        private const val MINIMUM_VERSION_WITH_NOT_RESHAREABLE_FEDERATED = 0x09010000   // 9.1

        private const val MINIMUM_VERSION_WITH_SESSION_MONITORING = 0x09010000   // 9.1

        private const val MINIMUM_VERSION_WITH_SESSION_MONITORING_WORKING_IN_PREEMPTIVE_MODE = 0x09010301
        // 9.1.3.1, final 9.1.3: https://github.com/owncloud/core/commit/f9a867b70c217463289a741d4d26079eb2a80dfd

        private const val VERSION_10 = 0xA000000 // 10.0.0

        private const val MINIMUM_VERSION_WITH_MULTIPLE_PUBLIC_SHARING = 0xA000000 // 10.0.0

        private const val MINIMUN_MAJOR_VERSION_WITHOUT_PUBLIC_UPLOAD_PERMISSION = 0xA000000 // 10.0.0

        private const val MINIMUM_VERSION_WITH_WRITE_ONLY_PUBLIC_SHARING = 0xA000100 // 10.0.1

        private const val INVALID_ZERO_VERSION = "0.0.0"

        private const val MAX_DOTS = 3
    }
}
