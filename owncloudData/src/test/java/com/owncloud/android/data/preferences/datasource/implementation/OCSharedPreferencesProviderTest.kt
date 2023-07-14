package com.owncloud.android.data.preferences.datasource.implementation

import com.owncloud.android.data.preferences.datasources.implementation.OCSharedPreferencesProvider
import io.mockk.every
import org.junit.Before
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OCSharedPreferencesProviderTest {

    private lateinit var ocSharedPreferencesProvider: OCSharedPreferencesProvider
    private lateinit var context: Context
    private val sharedPreferences: SharedPreferences = mockk(relaxed = true)
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        ocSharedPreferencesProvider = OCSharedPreferencesProvider(context)
    }

    @Test
    fun `put String - ok`() {
        val key = "test_key"
        val value = "test_value"
        ocSharedPreferencesProvider.putString(key, value)

        verify { editor.putString(key, value).apply() }
    }

    @Test
    fun `get String - ok`() {
        val key = "test_key"
        val defaultValue = "default_value"
        val savedValue = "saved_value"

        every { sharedPreferences.getString(key, defaultValue) } returns savedValue
        val result = ocSharedPreferencesProvider.getString(key, defaultValue)
        assertEquals(savedValue, result)
        verify { sharedPreferences.getString(key, defaultValue) }
    }

    @Test
    fun `put Int - ok`() {
        val key = "test_key"
        val value = 12
        ocSharedPreferencesProvider.putInt(key, value)
        verify { editor.putInt(key, value).apply() }
    }
    @Test
    fun `get Int - ok`() {
        val key = "test_key"
        val defaultValue = 111
        val savedValue = 233

        every { sharedPreferences.getInt(key, defaultValue) } returns savedValue
        val result = ocSharedPreferencesProvider.getInt(key, defaultValue)
        assertEquals(savedValue, result)
        verify { sharedPreferences.getInt(key, defaultValue) }
    }

    @Test
    fun `put Long - ok`() {
        val key = "test_key"
        val value = 12L
        ocSharedPreferencesProvider.putLong(key, value)
        verify { editor.putLong(key, value).apply() }
    }
    @Test
    fun `get Long - ok`() {
        val key = "test_key"
        val defaultValue = 1411L
        val savedValue = 73L

        every { sharedPreferences.getLong(key, defaultValue) } returns savedValue
        val result = ocSharedPreferencesProvider.getLong(key, defaultValue)
        assertEquals(savedValue, result)
        verify { sharedPreferences.getLong(key, defaultValue) }
    }

    @Test
    fun `put Boolean - ok`() {
        val key = "test_key"
        val value = true
        ocSharedPreferencesProvider.putBoolean(key, value)
        verify { editor.putBoolean(key, value).apply() }
    }

    @Test
    fun `get Boolean - ok`() {
        val key = "test_key"
        val defaultValue = false
        val savedValue = true

        every { sharedPreferences.getBoolean(key, defaultValue) } returns savedValue
        val result = ocSharedPreferencesProvider.getBoolean(key, defaultValue)
        assertEquals(savedValue, result)
        verify { sharedPreferences.getBoolean(key, defaultValue) }
    }

    @Test
    fun `contains preferences - ok`() {
        val key = "test_key"
        every { sharedPreferences.contains(key) } returns true
        val result = ocSharedPreferencesProvider.containsPreference(key)
        assertTrue(result)
        verify { sharedPreferences.contains(key) }
    }

    @Test
    fun `remove preferences - ok`() {
        val key = "test_key"
        ocSharedPreferencesProvider.removePreference(key)
        verify { editor.remove(key).apply() }
    }
}
