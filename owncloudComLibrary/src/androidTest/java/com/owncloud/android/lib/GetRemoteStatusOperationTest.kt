package com.owncloud.android.lib

import android.net.Uri
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import com.owncloud.android.lib.resources.status.HttpScheme.HTTPS_PREFIX
import com.owncloud.android.lib.resources.status.HttpScheme.HTTP_PREFIX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetRemoteStatusOperationTest {

    @Test
    fun urlStartingWithHttpMustBeDetectedAsSuch() {
        assertTrue(GetRemoteStatusOperation.usesHttpOrHttps(Uri.parse(HTTP_SOME_OWNCLOUD)))
    }

    @Test
    fun urlStartingWithHttpsMustBeDetectedAsSuch() {
        assertTrue(GetRemoteStatusOperation.usesHttpOrHttps(Uri.parse(HTTPS_SOME_OWNCLOUD)))
    }

    @Test
    fun incompleteUrlWithoutHttpsOrHttpSchemeMustBeDetectedAsSuch() {
        assertFalse(GetRemoteStatusOperation.usesHttpOrHttps(Uri.parse(SOME_OWNCLOUD)))
    }

    @Test
    fun completeUrlWithHttpMustBeReturnedAsSuch() {
        assertEquals(
            Uri.parse(HTTP_SOME_OWNCLOUD),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTP_SOME_OWNCLOUD))
        )
    }

    @Test
    fun completeUrlWithHttpsMustBeReturnedAsSuch() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTPS_SOME_OWNCLOUD))
        )
    }

    @Test
    fun incompleteUrlWithoutHttpPrefixMustBeConvertedToProperUrlWithHttpsPrefix() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(SOME_OWNCLOUD))
        )
    }

    @Test
    fun completeUrlWithSubdirAndHttpsMustBeReturnedAsSuch() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD_WITH_SUBDIR), GetRemoteStatusOperation.buildFullHttpsUrl(
                Uri.parse(
                    HTTPS_SOME_OWNCLOUD_WITH_SUBDIR
                )
            )
        )
    }

    @Test
    fun incompleteUrlWithSubdirAndWithoutHttpPrefixMustBeConvertedToProperUrlWithHttpsPrefix() {
        assertEquals(
            Uri.parse(HTTPS_SOME_OWNCLOUD_WITH_SUBDIR), GetRemoteStatusOperation.buildFullHttpsUrl(
                Uri.parse(
                    SOME_OWNCLOUD_WITH_SUBDIR
                )
            )
        )
    }

    @Test
    fun ipMustBeConvertedToProperUrl() {
        assertEquals(Uri.parse(HTTPS_SOME_IP), GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(SOME_IP)))
    }

    @Test
    fun urlContainingIpAndHttpPrefixMustBeReturnedAsSuch() {
        assertEquals(Uri.parse(HTTP_SOME_IP), GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTP_SOME_IP)))
    }

    @Test
    fun ipAndPortMustBeConvertedToProperUrl() {
        assertEquals(
            Uri.parse(HTTPS_SOME_IP_WITH_PORT),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(SOME_IP_WITH_PORT))
        )
    }

    @Test
    fun urlContainingIpAndPortAndHttpPrefixMustBeReturnedAsSuch() {
        assertEquals(
            Uri.parse(HTTP_SOME_IP_WITH_PORT),
            GetRemoteStatusOperation.buildFullHttpsUrl(Uri.parse(HTTP_SOME_IP_WITH_PORT))
        )
    }

    companion object {
        val SOME_OWNCLOUD = "some_owncloud.com"
        val HTTP_SOME_OWNCLOUD = "$HTTP_PREFIX$SOME_OWNCLOUD"
        val HTTPS_SOME_OWNCLOUD = "$HTTPS_PREFIX$SOME_OWNCLOUD"

        val SOME_OWNCLOUD_WITH_SUBDIR = "some_owncloud.com/subdir"
        val HTTP_SOME_OWNCLOUD_WITH_SUBDIR = "$HTTP_PREFIX$SOME_OWNCLOUD_WITH_SUBDIR"
        val HTTPS_SOME_OWNCLOUD_WITH_SUBDIR = "$HTTPS_PREFIX$SOME_OWNCLOUD_WITH_SUBDIR"

        val SOME_IP = "184.123.185.12"
        val HTTP_SOME_IP = "$HTTP_PREFIX$SOME_IP"
        val HTTPS_SOME_IP = "$HTTPS_PREFIX$SOME_IP"

        val SOME_IP_WITH_PORT = "184.123.185.12:5678"
        val HTTP_SOME_IP_WITH_PORT = "$HTTP_PREFIX$SOME_IP_WITH_PORT"
        val HTTPS_SOME_IP_WITH_PORT = "$HTTPS_PREFIX$SOME_IP_WITH_PORT"
    }
}