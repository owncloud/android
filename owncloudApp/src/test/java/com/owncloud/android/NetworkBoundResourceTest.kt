package com.owncloud.android

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.utils.TestUtil
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class NetworkBoundResourceTest {
    private lateinit var handleSaveCallResult: (ShareParserResult) -> Unit
    private lateinit var handleShouldFetch: (List<OCShare>?) -> Boolean
    private val dbData = MutableLiveData<List<OCShare>>()
    private lateinit var handleCreateCall: () -> RemoteOperationResult<ShareParserResult>

    private lateinit var networkBoundResource: NetworkBoundResource<List<OCShare>, ShareParserResult>
    private val fetchedOnce = AtomicBoolean(false)

    @Before
    fun init() {
//        networkBoundResource = object : NetworkBoundResource<List<OCShare>, ShareParserResult>() {
//            override fun saveCallResult(item: ShareParserResult) {
//                handleSaveCallResult(item)
//            }
//
//            override fun shouldFetch(data: List<OCShare>?): Boolean {
//                return handleShouldFetch(data)
//            }
//
//            override fun loadFromDb(): LiveData<List<OCShare>> {
//                return dbData
//            }
//
//            override fun createCall(): RemoteOperationResult<ShareParserResult> {
//                return handleCreateCall()
//            }
//        }
    }

    @Test
    fun basicFromNetwork() {
//        handleShouldFetch = { it == null }
//        val fetchedDbValue = listOf(
//            TestUtil.createPublicShare(
//                path = "/Photos/image.jpg",
//                isFolder = true,
//                name = "Photos 1 link",
//                shareLink = "http://server:port/s/1"
//            )
//        )
//
//        handleSaveCallResult = { foo ->
//            saved.set(foo)
//            dbData.setValue(fetchedDbValue)
//        }
//        val networkResult = Foo(1)
//        handleCreateCall = { ApiUtil.createCall(Response.success(networkResult)) }
//
//        val observer = mock<Observer<Resource<Foo>>>()
//        networkBoundResource.asLiveData().observeForever(observer)
//        drain()
//        verify(observer).onChanged(Resource.loading(null))
//        reset(observer)
//        dbData.value = null
//        drain()
//        assertThat(saved.get(), `is`(networkResult))
//        verify(observer).onChanged(Resource.success(fetchedDbValue))
    }
}