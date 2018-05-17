package com.owncloud.android.lib.refactor;

import com.owncloud.android.lib.refactor.RemoteOperation;

import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public abstract class RemoteOperation {
    private final OwnCloudContext mContext;
    private static OkHttpClient httpClient = null;

    protected RemoteOperation(OwnCloudContext context) {
        mContext = context;

        if(httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
        }
    }

    public abstract RemoteOperationResult exec();

    public OwnCloudContext getOCContext() {
        return mContext;
    }

    public OkHttpClient getClient() {
        return httpClient;
    }

    public Request.Builder getRequestBuilder() {
        Request.Builder builder = new Request.Builder();

        for(Map.Entry<String, String> header
                : mContext.getCredentials().getCredentialHeaders().entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }

        //TODO: Remove this part once SAML is obsolet
        final String credentialCookie = mContext.getCredentials().getCredentialCookie();
        if(credentialCookie == null) {
            builder.addHeader("Cookie", credentialCookie);
        }

        return builder;
    }
}
