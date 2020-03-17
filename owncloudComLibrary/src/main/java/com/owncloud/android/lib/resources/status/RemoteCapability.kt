/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David González Verdugo
 *   @author Abel García de Prada
 *   Copyright (C) 2020 ownCloud GmbH.
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

/**
 * Contains data of the Capabilities for an account, from the Capabilities API
 */
data class RemoteCapability(
    var accountName: String = "",

    // Server version
    var versionMayor: Int = 0,
    var versionMinor: Int = 0,
    var versionMicro: Int = 0,
    var versionString: String = "",
    var versionEdition: String = "",

    // Core PollInterval
    var corePollinterval: Int = 0,

    // Files Sharing
    var filesSharingApiEnabled: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingSearchMinLength: Int? = DEFAULT_MIN_CHARACTERS_TO_SEARCH,
    var filesSharingPublicEnabled: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicPasswordEnforced: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicPasswordEnforcedReadOnly: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicPasswordEnforcedReadWrite: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicPasswordEnforcedUploadOnly: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicExpireDateEnabled: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicExpireDateDays: Int = 0,
    var filesSharingPublicExpireDateEnforced: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicSendMail: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicUpload: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicMultiple: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingPublicSupportsUploadOnly: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingUserSendMail: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingResharing: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingFederationOutgoing: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesSharingFederationIncoming: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,

    // Files
    var filesBigFileChunking: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesUndelete: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN,
    var filesVersioning: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN
) {
    /**
     * Enum for Boolean Type in capabilities, with values:
     * -1 - Unknown
     * 0 - False
     * 1 - True
     */
    enum class CapabilityBooleanType constructor(val value: Int) {
        UNKNOWN(-1),
        FALSE(0),
        TRUE(1);

        companion object {
            fun fromValue(value: Int): CapabilityBooleanType? {
                return when (value) {
                    -1 -> UNKNOWN
                    0 -> FALSE
                    1 -> TRUE
                    else -> null
                }
            }

            fun fromBooleanValue(boolValue: Boolean): CapabilityBooleanType {
                return if (boolValue) {
                    TRUE
                } else {
                    FALSE
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_MIN_CHARACTERS_TO_SEARCH = 2
    }
}
