package com.owncloud.android.lib

import com.owncloud.android.lib.common.http.CookieJarImpl
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class CookieJarImplTest {

    private val oldCookies = ArrayList<Cookie>().apply {
        add(COOKIE_A)
        add(COOKIE_B_OLD)
    }

    private val newCookies = ArrayList<Cookie>().apply {
        add(COOKIE_B_NEW)
    }

    private val updatedCookies = ArrayList<Cookie>().apply {
        add(COOKIE_A)
        add(COOKIE_B_NEW)
    }

    private val cookieStore = HashMap<String, List<Cookie>>().apply {
        put(SOME_HOST, oldCookies)
    }

    private val cookieJarImpl = CookieJarImpl(cookieStore)

    @Test
    fun testContainsCookieWithNameReturnsTrue() {
        assertTrue(cookieJarImpl.containsCookieWithName(oldCookies, COOKIE_B_OLD.name))
    }

    @Test
    fun testContainsCookieWithNameReturnsFalse() {
        assertFalse(cookieJarImpl.containsCookieWithName(newCookies, COOKIE_A.name))
    }

    @Test
    fun testGetUpdatedCookies() {
        val generatedUpdatedCookies = cookieJarImpl.getUpdatedCookies(oldCookies, newCookies)
        assertEquals(2, generatedUpdatedCookies.size)
        assertEquals(updatedCookies[0], generatedUpdatedCookies[1])
        assertEquals(updatedCookies[1], generatedUpdatedCookies[0])
    }

    @Test
    fun testCookieStoreUpdateViaSaveFromResponse() {
        cookieJarImpl.saveFromResponse(SOME_URL, newCookies)
        val generatedUpdatedCookies = cookieStore[SOME_HOST]
        assertEquals(2, generatedUpdatedCookies?.size)
        assertEquals(updatedCookies[0], generatedUpdatedCookies?.get(1))
        assertEquals(updatedCookies[1], generatedUpdatedCookies?.get(0))
    }

    @Test
    fun testLoadForRequest() {
        val cookies = cookieJarImpl.loadForRequest(SOME_URL)
        assertEquals(oldCookies[0], cookies[0])
        assertEquals(oldCookies[1], cookies[1])
    }

    companion object {
        const val SOME_HOST = "some.host.com"
        val SOME_URL = "https://$SOME_HOST".toHttpUrl()
        val COOKIE_A = Cookie.parse(SOME_URL, "CookieA=CookieValueA")!!
        val COOKIE_B_OLD = Cookie.parse(SOME_URL, "CookieB=CookieOldValueB")!!
        val COOKIE_B_NEW = Cookie.parse(SOME_URL, "CookieB=CookieNewValueB")!!
    }
}