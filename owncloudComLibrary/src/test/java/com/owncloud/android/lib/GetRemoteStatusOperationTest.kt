package com.owncloud.android.lib

import android.net.Uri
import android.os.Build
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import com.owncloud.android.lib.resources.status.HttpScheme.HTTPS_PREFIX
import com.owncloud.android.lib.resources.status.HttpScheme.HTTP_PREFIX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O], manifest = Config.NONE)
class GetRemoteStatusOperationTest {

    @Test
    fun `uses http or https - ok - http`() {
        assertTrue(GetRemoteStatusOperation.usesHttpOrHttps(Uri.parse(HTTP_SOME_OWNCLOUD)))
    }

    @Test
    fun `uses http or https - ok - https`() {
        assertTrue(GetRemoteStatusOperation.usesHttpOrHttps(Uri.parse(HTTPS_SOME_OWNCLOUD)))
    }

    @Test
    fun `uses http or https - ok - no http or https`() {
        assertFalse(GetRemoteStatusOperation.usesHttpOrHttps(Uri.parse(SOME_OWNCLOUD)))
    }

    @Test
    fun `build full https url - ok - http`() {
        assertEquals(
            Uri.parse(HTTP_SOME_OWNCLOUD),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTP_SOME_OWNCLOUD))
        )
    }

    @Test
    fun `build full https url - ok - https`() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTPS_SOME_OWNCLOUD))
        )
    }

    @Test
    fun `build full https url - ok - no prefix`() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(SOME_OWNCLOUD))
        )
    }

    @Test
    fun `build full https url - ok - no https with subdir`() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD_WITH_SUBDIR), GetRemoteStatusOperation.buildFullHttpsUrl(
                Uri.parse(
                    HTTPS_SOME_OWNCLOUD_WITH_SUBDIR
                )
            )
        )
    }

    @Test
    fun `build full https url - ok - no prefix with subdir`() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD_WITH_SUBDIR), GetRemoteStatusOperation.buildFullHttpsUrl(
                Uri.parse(
                    SOME_OWNCLOUD_WITH_SUBDIR
                )
            )
        )
    }

    @Test
    fun `build full https url - ok - ip`() {
        assertEquals(Uri.parse(HTTPS_SOME_IP), GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(SOME_IP)))
    }

    @Test
    fun `build full https url - ok - http ip`() {
        assertEquals(Uri.parse(HTTP_SOME_IP), GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTP_SOME_IP)))
    }

    @Test
    fun `build full https url - ok - ip with port`() {
        assertEquals(
            Uri.parse(HTTPS_SOME_IP_WITH_PORT),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(SOME_IP_WITH_PORT))
        )
    }

    @Test
    fun `build full https url - ok - ip with http and port`() {
        assertEquals(
            Uri.parse(HTTP_SOME_IP_WITH_PORT),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTP_SOME_IP_WITH_PORT))
        )
    }

    companion object {
        const val SOME_OWNCLOUD = "some_owncloud.com"
        const val HTTP_SOME_OWNCLOUD = "$HTTP_PREFIX$SOME_OWNCLOUD"
        const val HTTPS_SOME_OWNCLOUD = "$HTTPS_PREFIX$SOME_OWNCLOUD"

        const val SOME_OWNCLOUD_WITH_SUBDIR = "some_owncloud.com/subdir"
        const val HTTP_SOME_OWNCLOUD_WITH_SUBDIR = "$HTTP_PREFIX$SOME_OWNCLOUD_WITH_SUBDIR"
        const val HTTPS_SOME_OWNCLOUD_WITH_SUBDIR = "$HTTPS_PREFIX$SOME_OWNCLOUD_WITH_SUBDIR"

        const val SOME_IP = "184.123.185.12"
        const val HTTP_SOME_IP = "$HTTP_PREFIX$SOME_IP"
        const val HTTPS_SOME_IP = "$HTTPS_PREFIX$SOME_IP"

        const val SOME_IP_WITH_PORT = "184.123.185.12:5678"
        const val HTTP_SOME_IP_WITH_PORT = "$HTTP_PREFIX$SOME_IP_WITH_PORT"
        const val HTTPS_SOME_IP_WITH_PORT = "$HTTPS_PREFIX$SOME_IP_WITH_PORT"
    }
}