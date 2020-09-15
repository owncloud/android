package com.owncloud.android.lib

import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class GetRemoteStatusOperationTest {
    private val remoteStatusOperation = GetRemoteStatusOperation()

    @Test
    fun `update location with an absolute path`() {
        val newLocation = remoteStatusOperation.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com", "https://cloud.somewhere.com/subdir"
        )
        assertEquals("https://cloud.somewhere.com/subdir", newLocation)
    }

    @Test
    fun `update location with a smaler aboslute path`() {

        val newLocation = remoteStatusOperation.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com/subdir", "https://cloud.somewhere.com/"
        )
        assertEquals("https://cloud.somewhere.com/", newLocation)
    }

    @Test
    fun `update location with a relative path`() {
        val newLocation = remoteStatusOperation.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com", "/subdir"
        )
        assertEquals("https://cloud.somewhere.com/subdir", newLocation)
    }

    @Test
    fun `update location by replacing the relative path`() {
        val newLocation = remoteStatusOperation.updateLocationWithRedirectPath(
            "https://cloud.somewhere.com/some/other/subdir", "/subdir"
        )
        assertEquals("https://cloud.somewhere.com/subdir", newLocation)
    }
}