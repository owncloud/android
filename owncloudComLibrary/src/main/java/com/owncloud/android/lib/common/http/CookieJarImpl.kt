/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.lib.common.http

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieJarImpl(
    private val cookieStore: HashMap<String, List<Cookie>>
) : CookieJar {

    fun containsCookieWithName(cookies: List<Cookie>, name: String): Boolean {
        for (cookie: Cookie in cookies) {
            if (cookie.name == name) {
                return true
            }
        }
        return false
    }

    fun getUpdatedCookies(oldCookies: List<Cookie>, newCookies: List<Cookie>): List<Cookie> {
        val updatedList = ArrayList<Cookie>(newCookies)
        for (oldCookie: Cookie in oldCookies) {
            if (!containsCookieWithName(updatedList, oldCookie.name)) {
                updatedList.add(oldCookie)
            }
        }
        return updatedList
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Avoid duplicated cookies but update
        val currentCookies: List<Cookie> = cookieStore[url.host] ?: ArrayList()
        val updatedCookies: List<Cookie> = getUpdatedCookies(currentCookies, cookies)
        cookieStore[url.host] = updatedCookies
    }

    override fun loadForRequest(url: HttpUrl) =
        cookieStore[url.host] ?: ArrayList()
}