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

package com.owncloud.android.data

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.lib.common.operations.RemoteOperationResult

abstract class NetworkBoundResource<ResultType, RequestType>(
    private val executors: Executors
) {
    /**
     * A generic class that can provide a resource backed by both the sqlite database and the network.
     * - Shares livedata from Room to detect changes in database
     * - Errors livedata from remote operations
     * - Loading status
     */
    private val result = MediatorLiveData<DataResult<ResultType>>()

    init {
        @Suppress("LeakingThis")
        val dbSource = loadFromDb()
        result.addSource(dbSource) { data ->
            result.removeSource(dbSource)
            if (shouldFetchFromNetwork(data)) {
                fetchFromNetwork(dbSource)
            } else {
                result.addSource(dbSource) { newData ->
                    setValue(DataResult.success(newData))
                }
            }
        }
    }

    @MainThread
    private fun setValue(newValue: DataResult<ResultType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    private fun fetchFromNetwork(dbSource: LiveData<ResultType>) {
        // Let's dispatch dbSource value quickly while network operation is performed
        result.addSource(dbSource) { newData ->
            if (newData != null) {
                setValue(DataResult.loading(newData))
            }
        }

        try {
            executors.networkIO().execute() {
                val remoteOperationResult = createCall()

                executors.mainThread().execute() {
                    result.removeSource(dbSource)
                }

                if (remoteOperationResult.isSuccess) {
                    saveCallResult(remoteOperationResult.data)
                    // we specially request a new live data,
                    // otherwise we will get immediately last cached value,
                    // which may not be updated with latest results received from network.
                    executors.mainThread().execute() {
                        result.addSource(loadFromDb()) { newData ->
                            setValue(DataResult.success(newData))
                        }
                    }
                } else {
                    executors.mainThread().execute() {
                        result.addSource(dbSource) { newData ->
                            setValue(
                                DataResult.error(
                                    remoteOperationResult.code,
                                    newData,
                                    msg = remoteOperationResult.httpPhrase,
                                    exception = remoteOperationResult.exception
                                )
                            )
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            executors.mainThread().execute() {
                result.removeSource(dbSource)
            }

            result.addSource(dbSource) {
                setValue(
                    DataResult.error(
                        msg = ex.localizedMessage
                    )
                )
            }
        }
    }

    fun asLiveData() = result as LiveData<DataResult<ResultType>>
    fun asMutableLiveData() = result as MutableLiveData<DataResult<ResultType>>

    @WorkerThread
    protected abstract fun saveCallResult(item: RequestType)

    @MainThread
    protected abstract fun shouldFetchFromNetwork(data: ResultType?): Boolean

    @MainThread
    protected abstract fun loadFromDb(): LiveData<ResultType>

    @MainThread
    protected abstract fun createCall(): RemoteOperationResult<RequestType>
}
