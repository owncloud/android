/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.vo.Resource
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class NetworkBoundResource<ResultType, RequestType> : CoroutineScope {
    private val result = MediatorLiveData<Resource<ResultType>>()

    private val errors = MutableLiveData<Resource<ResultType>>()
    private val loading = MutableLiveData<Resource<ResultType>>()

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    init {
        val dbSource = loadFromDb()

        /**
         * Result will observe two different livedata objects and react on change events from them
         * - Shares livedata from Room to detect changes in database
         * - Errors livedata from remote operations
         * - Loading status
         */
        result.addSource(dbSource) { newData ->
            result.value = Resource.success(newData)
        }

        result.addSource(errors) { error ->
            result.value = error
        }

        result.addSource(loading) {
            result.value = it
        }

        fetchFromNetwork()
    }

    private fun fetchFromNetwork() {

        try {
            launch {
                withContext(Dispatchers.IO) {

                    loading.postValue(
                        Resource.loading()
                    )

                    val remoteOperationResult = performCall()

                    if (remoteOperationResult.isSuccess) {
                        saveCallResult(processResponse(remoteOperationResult))

                    } else {
                        errors.postValue(
                            Resource.error(
                                remoteOperationResult.code,
                                exception = remoteOperationResult.exception
                            )
                        )
                    }

                    loading.postValue(
                        Resource.stopLoading()
                    )
                }
            }

        } catch (ex: Exception) {
            errors.postValue(
                Resource.error(
                    msg = ex.localizedMessage
                )
            )
        }
    }

    fun asLiveData() = result as LiveData<Resource<ResultType>>

    @WorkerThread
    protected open fun processResponse(remoteOperationResult: RemoteOperationResult<RequestType>) =
        remoteOperationResult.data

    @WorkerThread
    protected abstract fun saveCallResult(item: RequestType)

    @MainThread
    protected abstract fun loadFromDb(): LiveData<ResultType>

    @MainThread
    protected abstract fun performCall(): RemoteOperationResult<RequestType>
}
