/* ownCloud Android Library is available under MIT license
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
package com.owncloud.android.lib.common.http.methods.webdav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyUtils.getQuotaPropset
import at.bitfire.dav4jvm.property.CreationDate
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetETag
import at.bitfire.dav4jvm.property.GetLastModified
import at.bitfire.dav4jvm.property.OCId
import at.bitfire.dav4jvm.property.OCPermissions
import at.bitfire.dav4jvm.property.OCPrivatelink
import at.bitfire.dav4jvm.property.OCSize
import at.bitfire.dav4jvm.property.ResourceType
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCShareTypes

object DavUtils {
    @JvmStatic val allPropSet: Array<Property.Name>
        get() = arrayOf(
            DisplayName.NAME,
            GetContentType.NAME,
            ResourceType.NAME,
            GetContentLength.NAME,
            GetLastModified.NAME,
            CreationDate.NAME,
            GetETag.NAME,
            OCPermissions.NAME,
            OCId.NAME,
            OCSize.NAME,
            OCPrivatelink.NAME,
            OCShareTypes.NAME,
        )

    val quotaPropSet: Array<Property.Name>
        get() = getQuotaPropset()
}
