/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2023 ownCloud GmbH.
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
package com.owncloud.android.lib

import android.os.Build
import com.owncloud.android.lib.resources.files.RemoteFile
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O], manifest = Config.NONE)
class RemoteFileTest {

    @Test
    fun getRemotePathFromUrl_legacyWebDav() {
        val httpUrlToTest = "https://server.url/remote.php/dav/files/username/Documents/text.txt".toHttpUrl()
        val expectedRemotePath = "/Documents/text.txt"

        val actualRemotePath = RemoteFile.Companion.getRemotePathFromUrl(httpUrlToTest, "username")
        assertEquals(expectedRemotePath, actualRemotePath)
    }

    @Test
    fun getRemotePathFromUrl_spacesWebDav() {
        val spaceWebDavUrl = "https://server.url/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$05bca744-d89f-4e9c-a990-25a0d7f03fe9"

        val httpUrlToTest =
            "https://server.url/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$05bca744-d89f-4e9c-a990-25a0d7f03fe9/Documents/text.txt".toHttpUrl()
        val expectedRemotePath = "/Documents/text.txt"

        val actualRemotePath = RemoteFile.Companion.getRemotePathFromUrl(httpUrlToTest, "username", spaceWebDavUrl)
        assertEquals(expectedRemotePath, actualRemotePath)
    }
}
