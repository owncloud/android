package com.owncloud.android.data.providers.implementation

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OCSharedPreferencesProviderTest {

    private lateinit var ocSharedPreferencesProvider: OCSharedPreferencesProvider
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val key = "test_key"

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        ocSharedPreferencesProvider = OCSharedPreferencesProvider(context)
    }

    @Test
    fun `putString put a String in sharedPreferences`() {
        val value = "test_value"
        ocSharedPreferencesProvider.putString(key, value)

        verify(exactly = 1) {
            editor.putString(key, value).apply()
        }
    }

    @Test
    fun `getString returns a String from sharedPreferences`() {
        val defaultValue = "default_value"
        val savedValue = "saved_value"

        every { sharedPreferences.getString(key, defaultValue) } returns savedValue

        val result = ocSharedPreferencesProvider.getString(key, defaultValue)
        assertEquals(savedValue, result)

        verify(exactly = 1) {
            sharedPreferences.getString(key, defaultValue)
        }
    }

    @Test
    fun `putInt put a Int in sharedPreferences`() {
        val value = 12
        ocSharedPreferencesProvider.putInt(key, value)

        verify(exactly = 1) {
            editor.putInt(key, value).apply()
        }
    }

    @Test
    fun `getInt returns a Int from sharedPreferences`() {
        val defaultValue = 111
        val savedValue = 233

        every { sharedPreferences.getInt(key, defaultValue) } returns savedValue

        val result = ocSharedPreferencesProvider.getInt(key, defaultValue)
        assertEquals(savedValue, result)

        verify(exactly = 1) {
            sharedPreferences.getInt(key, defaultValue)
        }
    }

    @Test
    fun `putLong put a Long in sharedPreferences`() {
        val value = 12L
        ocSharedPreferencesProvider.putLong(key, value)

        verify(exactly = 1) {
            editor.putLong(key, value).apply()
        }
    }

    @Test
    fun `getLong returns a Long from sharedPreferences`() {
        val defaultValue = 1411L
        val savedValue = 73L

        every { sharedPreferences.getLong(key, defaultValue) } returns savedValue

        val result = ocSharedPreferencesProvider.getLong(key, defaultValue)
        assertEquals(savedValue, result)

        verify(exactly = 1) {
            sharedPreferences.getLong(key, defaultValue)
        }
    }

    @Test
    fun `putBoolean put a Boolean in sharedPreferences`() {
        val value = true
        ocSharedPreferencesProvider.putBoolean(key, value)

        verify(exactly = 1) {
            editor.putBoolean(key, value).apply()
        }
    }

    @Test
    fun `getBoolean returns a Boolean from sharedPreferences`() {
        val defaultValue = false
        val savedValue = true

        every { sharedPreferences.getBoolean(key, defaultValue) } returns savedValue

        val result = ocSharedPreferencesProvider.getBoolean(key, defaultValue)
        assertTrue(result)

        verify(exactly = 1) {
            sharedPreferences.getBoolean(key, defaultValue)
        }
    }

    @Test
    fun `containsPreference verify if the value with the key is contained in sharedPreferences`() {
        every { sharedPreferences.contains(key) } returns true

        val result = ocSharedPreferencesProvider.containsPreference(key)
        assertTrue(result)

        verify(exactly = 1) {
            sharedPreferences.contains(key)
        }
    }

    @Test
    fun `removePreferences remove a preference by key`() {
        ocSharedPreferencesProvider.removePreference(key)

        verify(exactly = 1) {
            editor.remove(key).apply()
        }
    }
}
