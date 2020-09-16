package com.owncloud.android.lib

import com.owncloud.android.lib.resources.status.StatusRequestor
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusRequestorTest {
    private val requestor = StatusRequestor()

    @Test
    fun `update location with an absolute path`() {
        val newLocation = requestor.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com", "https://cloud.somewhere.com/subdir"
        )
        assertEquals("https://cloud.somewhere.com/subdir", newLocation)
    }

    @Test

    fun `update location with a smaler aboslute path`() {
        val newLocation = requestor.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com/subdir", "https://cloud.somewhere.com/"
        )
        assertEquals("https://cloud.somewhere.com/", newLocation)
    }

    @Test
    fun `update location with a relative path`() {
        val newLocation = requestor.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com", "/subdir"
        )
        assertEquals("https://cloud.somewhere.com/subdir", newLocation)
    }

    @Test
    fun `update location by replacing the relative path`() {
        val newLocation = requestor.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com/some/other/subdir", "/subdir"
        )
        assertEquals("https://cloud.somewhere.com/subdir", newLocation)
    }
}