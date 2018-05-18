package com.owncloud.android.lib.refactor;

import android.net.Uri;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public abstract class RemoteOperation {
    private final OCContext mContext;
    private static OkHttpClient httpClient = null;
    private static final String WEBDAV_PATH_4_0 = "/remote.php/dav";

    protected RemoteOperation(OCContext context) {
        mContext = context;

        if(httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
        }
    }

    public abstract RemoteOperationResult exec();

    protected OCContext getOCContext() {
        return mContext;
    }

    protected OkHttpClient getClient() {
        return httpClient;
    }

    protected Uri.Builder getBaseUriBuilder() {
        return mContext.getOCAccount().getBaseUri().buildUpon();
    }

    protected Uri.Builder getWebDAVUriBuilder() {
        return getBaseUriBuilder().appendEncodedPath(WEBDAV_PATH_4_0);
    }

    protected Request.Builder getRequestBuilder() {
        Request.Builder builder = new Request.Builder();

        for(Map.Entry<String, String> header
                : mContext.getOCAccount()
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
        if(credentialCookie == null) {
            builder.addHeader("Cookie", credentialCookie);
        }

        return builder;
    }
}