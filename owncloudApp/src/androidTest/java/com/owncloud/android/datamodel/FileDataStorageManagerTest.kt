package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.owncloud.android.testutil.OC_ACCOUNT
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test

class FileDataStorageManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contentResolver = mockk<ContentResolver>()

    @Test
    fun testGetParentDirectory() {
       val manager = FileDataStorageManager(context, OC_ACCOUNT, contentResolver)
        assertEquals("/some/path/with/", manager.getParentPath("/some/path/with/tailing/"))
        assertEquals("/some/path/with/no/", manager.getParentPath("/some/path/with/no/tailing"))
        assertEquals("", manager.getParentPath("/"))
        assertEquals("", manager.getParentPath(""))
    }
}