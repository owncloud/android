package com.owncloud.android.lib

import com.owncloud.android.lib.resources.CommonOcsResponse
import com.owncloud.android.lib.resources.shares.responses.ShareeOcsResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Type

class ShareeResponseTest {

    var response: CommonOcsResponse<ShareeOcsResponse>? = null

    @Before
    fun prepare() {
        val moshi = Moshi.Builder().build()
        val type: Type = Types.newParameterizedType(CommonOcsResponse::class.java, ShareeOcsResponse::class.java)
        val adapter: JsonAdapter<CommonOcsResponse<ShareeOcsResponse>> = moshi.adapter(type)
        response = adapter.fromJson(EXAMPLE_RESPONSE)
    }

    @Test
    fun `check structure - ok - contains meta`() {
        assertEquals("OK", response?.ocs?.meta?.message!!)
        assertEquals(200, response?.ocs?.meta?.statusCode!!)
        assertEquals("ok", response?.ocs?.meta?.status!!)
        assertTrue(response?.ocs?.meta?.itemsPerPage?.isEmpty()!!)
        assertTrue(response?.ocs?.meta?.totalItems?.isEmpty()!!)
    }

    @Test
    fun `check structure - ok - contains exact`() {
        assertNotEquals(null, response?.ocs?.data?.exact)
    }

    @Test
    fun `check structure - ok - contains groups`() {
        assertNotEquals(null, response?.ocs?.data?.groups)
    }

    @Test
    fun `check structure - ok - contains remotes`() {
        assertNotEquals(null, response?.ocs?.data?.remotes)
    }

    @Test
    fun `check structure - ok - contains users`() {
        assertNotEquals(null, response?.ocs?.data?.users)
    }

    @Test
    fun `check structure - ok - groups contains two items`() {
        assertEquals(2, response?.ocs?.data?.groups?.size)
    }

    @Test
    fun `check structure - ok - users contains two items`() {
        assertEquals(2, response?.ocs?.data?.users?.size)
    }

    @Test
    fun `check structure - ok - exact_users contains one item`() {
        assertEquals(1, response?.ocs?.data?.exact?.users?.size)
    }

    companion object {
        val EXAMPLE_RESPONSE = """
{
    "ocs": {
        "data": {
            "exact": {
                "groups": [],
                "remotes": [],
                "users": [
                    {
                        "label": "admin",
                        "value": {
                            "shareType": 0,
                            "shareWith": "admin"
                        }
                    }
                ]
            },
            "groups": [
                {
                    "label": "group1",
                    "value": {
                        "shareType": 1,
                        "shareWith": "group1"
                    }
                },
                {
                    "label": "group2",
                    "value": {
                        "shareType": 1,
                        "shareWith": "group2"
                    }
                }
            ],
            "remotes": [],
            "users": [
                {
                    "label": "user1",
                    "value": {
                        "shareType": 0,
                        "shareWith": "user1"
                    }
                },
                {
                    "label": "user2",
                    "value": {
                        "shareType": 0,
                        "shareWith": "user2"
                    }
                }
            ]
        },
        "meta": {
            "itemsperpage": "",
            "message": "OK",
            "status": "ok",
            "statuscode": 200,
            "totalitems": ""
        }
    }
}
        """
    }
}