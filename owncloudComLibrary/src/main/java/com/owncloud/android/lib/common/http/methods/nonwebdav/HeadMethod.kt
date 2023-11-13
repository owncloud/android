package com.owncloud.android.lib.common.http.methods.nonwebdav

import okhttp3.OkHttpClient
import java.net.URL

class HeadMethod(url: URL) : HttpMethod(url) {

    override fun onExecute(okHttpClient: OkHttpClient): Int {
        request = request.newBuilder()
            .head()
            .build()
        return super.onExecute(okHttpClient)
    }

}
