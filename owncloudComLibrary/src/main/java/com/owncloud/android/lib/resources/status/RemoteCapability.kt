/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David González Verdugo
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
package com.owncloud.android.lib.resources.status

/**
 * Contains data of the Capabilities for an account, from the Capabilities API
 */
class RemoteCapability {
    var accountName: String

    // Server version
    var versionMayor: Int
    var versionMinor: Int
    var versionMicro: Int
    var versionString: String
    var versionEdition: String

    // Core PollInterval
    var corePollinterval: Int

    // Files Sharing
    var filesSharingApiEnabled: CapabilityBooleanType
    var filesSharingPublicEnabled: CapabilityBooleanType
    var filesSharingPublicPasswordEnforced: CapabilityBooleanType
    var filesSharingPublicPasswordEnforcedReadOnly: CapabilityBooleanType
    var filesSharingPublicPasswordEnforcedReadWrite: CapabilityBooleanType
    var filesSharingPublicPasswordEnforcedUploadOnly: CapabilityBooleanType
    var filesSharingPublicExpireDateEnabled: CapabilityBooleanType
    var filesSharingPublicExpireDateDays: Int
    var filesSharingPublicExpireDateEnforced: CapabilityBooleanType
    var filesSharingPublicSendMail: CapabilityBooleanType
    var filesSharingPublicUpload: CapabilityBooleanType
    var filesSharingPublicMultiple: CapabilityBooleanType
    var filesSharingPublicSupportsUploadOnly: CapabilityBooleanType
    var filesSharingUserSendMail: CapabilityBooleanType
    var filesSharingResharing: CapabilityBooleanType
    var filesSharingFederationOutgoing: CapabilityBooleanType
    var filesSharingFederationIncoming: CapabilityBooleanType

    // Files
    var filesBigFileChunking: CapabilityBooleanType
    var filesUndelete: CapabilityBooleanType
    var filesVersioning: CapabilityBooleanType

    init {
        accountName = ""

        versionMayor = 0
        versionMinor = 0
        versionMicro = 0
        versionString = ""
        versionEdition = ""

        corePollinterval = 0

        filesSharingApiEnabled = CapabilityBooleanType.UNKNOWN
        filesSharingPublicEnabled = CapabilityBooleanType.UNKNOWN
        filesSharingPublicPasswordEnforced = CapabilityBooleanType.UNKNOWN
        filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.UNKNOWN
        filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.UNKNOWN
        filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.UNKNOWN
        filesSharingPublicExpireDateEnabled = CapabilityBooleanType.UNKNOWN
        filesSharingPublicExpireDateDays = 0
        filesSharingPublicExpireDateEnforced = CapabilityBooleanType.UNKNOWN
        filesSharingPublicSendMail = CapabilityBooleanType.UNKNOWN
        filesSharingPublicUpload = CapabilityBooleanType.UNKNOWN
        filesSharingPublicMultiple = CapabilityBooleanType.UNKNOWN
        filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.UNKNOWN
        filesSharingUserSendMail = CapabilityBooleanType.UNKNOWN
        filesSharingResharing = CapabilityBooleanType.UNKNOWN
        filesSharingFederationOutgoing = CapabilityBooleanType.UNKNOWN
        filesSharingFederationIncoming = CapabilityBooleanType.UNKNOWN

        filesBigFileChunking = CapabilityBooleanType.UNKNOWN
        filesUndelete = CapabilityBooleanType.UNKNOWN
        filesVersioning = CapabilityBooleanType.UNKNOWN
    }
}
