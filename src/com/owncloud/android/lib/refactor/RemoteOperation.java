package com.owncloud.android.lib.refactor;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;

import java.util.Map;

import at.bitfire.dav4android.UrlUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public abstract class RemoteOperation {
    private final OCContext mContext;
    private static final String WEBDAV_PATH_4_0 = "remote.php/dav";
    private static OkHttpClient mClient = null;

    protected RemoteOperation(OCContext context) {
        mContext = context;
        if(mClient == null) {
            mClient = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
        }
    }

    public abstract RemoteOperationResult exec();

    protected OCContext getOCContext() {
        return mContext;
    }

    protected OkHttpClient getClient() {
        return mClient.newBuilder()
                .addInterceptor(chain ->
                        chain.proceed(
                                addRequestCredentials(
                                        chain.request())
                                        .build()))
                .followRedirects(false)
                .build();
    }

    protected Uri.Builder getBaseUriBuilder() {
        return mContext.getOCAccount().getBaseUri().buildUpon();
    }

    protected HttpUrl getWebDavHttpUrl(String resourcePath) {
        return UrlUtils.INSTANCE.omitTrailingSlash(HttpUrl.parse(
                getBaseUriBuilder()
                        .appendEncodedPath(WEBDAV_PATH_4_0)
                        .appendEncodedPath(resourcePath)
                        .build()
                        .toString()));
    }

    protected Request.Builder getRequestBuilder() {
        return new Request.Builder();
    }

    private Request.Builder addRequestCredentials(Request request) {
        Request.Builder builder =  request.newBuilder();

        for(Map.Entry<String, String> header : mContext.getOCAccount()
                .getCredentials()
                .getCredentialHeaders()
                .entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }

        //TODO: Remove this part once SAML is obsolet
        final String credentialCookie = mContext
                .getOCAccount()
                .getCredentials()
                .getCredentialCookie();
        if(credentialCookie != null) {
            System.err.println(credentialCookie);
            builder.addHeader("Cookie", credentialCookie);
        }

        return builder;
    }
}