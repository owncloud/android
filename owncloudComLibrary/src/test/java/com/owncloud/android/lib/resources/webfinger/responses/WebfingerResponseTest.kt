package com.owncloud.android.lib.resources.webfinger.responses

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class WebfingerResponseTest {
    lateinit var adapter: JsonAdapter<WebfingerJrdResponse>

    private fun loadResponses(fileName: String) =
        adapter.fromJson(File(fileName).readText())

    @Before
    fun prepare() {
        val moshi = Moshi.Builder().build()
        adapter = moshi.adapter(WebfingerJrdResponse::class.java)
    }

    @Test
    fun `check rel in to much information - ok - correct rell is returned`() {
        val response = loadResponses(TO_MUCH_INFORMATION_JSON)!!
        Assert.assertEquals("https://gast.somedomain.de", response.links[0].href)
        Assert.assertEquals("http://webfinger.owncloud/rel/server-instance", response.links[0].rel)
    }

    @Test(expected = JsonDataException::class)
    fun `check key value pairs - ko - no href key`() {
        val response = loadResponses(BROKEN_JSON)!!
        Assert.assertEquals("https://gast.somedomain.de", response.links[0].href)
    }

    @Test(expected = JsonDataException::class)
    fun `check key value pairs - ko - no rel key`() {
        val response = loadResponses(BROKEN_JSON)!!
        Assert.assertEquals("https://gast.somedomain.de", response.links[0].href)
    }

    companion object {
        val RESOURCES_PATH =
            "src/test/responses/com.owncloud.android.lib.resources.webfinger.responses"
        val EXAMPLE_RESPONSE_JSON = "$RESOURCES_PATH/simple_response.json"
        val TO_MUCH_INFORMATION_JSON = "$RESOURCES_PATH/to_much_information_response.json"
        val BROKEN_JSON = "$RESOURCES_PATH/broken_response.json"
        val NOT_CONTAINING_RELEVANT_INFORMATION_JSON = "$RESOURCES_PATH/not_containing_relevant_info_response.json"
    }
}
