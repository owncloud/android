/*
 * Copyright 2017 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.owncloud.android.authentication.oauth

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.AnyThread
import net.openid.appauth.AuthState
import org.json.JSONException
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

/**
 * An example persistence mechanism for an [AuthState] instance.
 * This stores the instance in a shared preferences file, and provides thread-safe access and
 * mutation.
 */
class AuthStateManager private constructor(context: Context) {

    companion object {
        private val INSTANCE_REF = AtomicReference(WeakReference<AuthStateManager?>(null))
        private const val STORE_NAME = "AuthState"
        @JvmStatic
        @AnyThread
        fun getInstance(context: Context): AuthStateManager {
            var manager = INSTANCE_REF.get().get()
            if (manager == null) {
                manager = AuthStateManager(context.applicationContext)
                INSTANCE_REF.set(WeakReference(manager))
            }
            return manager
        }
    }

    private val prefs: SharedPreferences
    private val prefsLock: ReentrantLock

    init {
        prefs = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
        prefsLock = ReentrantLock()
    }

    @AnyThread
    fun replace(accountName: String, state: AuthState): AuthState {
        writeState(accountName, state)
        return state
    }

    @AnyThread
    fun readState(accountName: String): AuthState {
        prefsLock.lock()
        return try {
            val currentState = prefs.getString(accountName, null) ?: return AuthState()
            try {
                AuthState.jsonDeserialize(currentState)
            } catch (exception: JSONException) {
                Timber.w("Failed to deserialize stored auth state - discarding")
                AuthState()
            }
        } finally {
            prefsLock.unlock()
        }
    }

    @AnyThread
    private fun writeState(accountName: String, authState: AuthState?) {
        prefsLock.lock()
        try {
            val editor = prefs.edit()
            if (authState == null) {
                editor.remove(accountName)
            } else {
                editor.putString(accountName, authState.jsonSerializeString())
            }
            check(editor.commit()) { "Failed to write state to shared prefs" }
        } finally {
            prefsLock.unlock()
        }
    }
}
