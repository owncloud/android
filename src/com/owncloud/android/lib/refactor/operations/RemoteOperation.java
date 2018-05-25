package com.owncloud.android.lib.refactor.operations;
import android.net.Uri;

import com.owncloud.android.lib.refactor.OCContext;

import java.io.IOException;
import java.util.Map;

import at.bitfire.dav4android.UrlUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class RemoteOperation<I extends Object> {
    private final OCContext mContext;
    // TODO Move to a constants file
    private static final String USER_AGENT_HEADER = "User-Agent";
    public static final String WEBDAV_PATH_4_0 = "remote.php/dav/files";
    private static OkHttpClient mClient = null;


    public class Result extends RemoteOperationResult {
        public Result(ResultCode code) {
            this(code, null);
        }

        public Result(ResultCode code, I data) {
            super(code);
            mData = data;
        }

        public Result(Exception e) {
            super(e);
            mData = null;
        }

        public Result(boolean success, Request request, Response response) throws IOException {
            this(success, request, response, null);;
        }

        public Result(boolean success, Request request, Response response, I data) throws IOException {
            super(success, request, response);
            mData = data;
        }

        private final I mData;

        public I getData() {
            return mData;
        }
    }


    protected RemoteOperation(OCContext context) {
        mContext = context;
        if(mClient == null) {
            mClient = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
        }
    }

    public abstract Result exec();

    protected OCContext getOCContext() {
        return mContext;
    }

    protected OkHttpClient getClient() {
        return mClient.newBuilder()
                .addInterceptor(chain ->
                        chain.proceed(
                                addRequestCredentials(chain.request())
                                        .addHeader(USER_AGENT_HEADER, mContext.getUserAgent())
                                        .build()
                        )
                )
                .followRedirects(false)
                .build();
    }

    private Uri.Builder getBaseUriBuilder() {
        return mContext.getOCAccount().getBaseUri().buildUpon();
    }

    protected HttpUrl getHttpUrl(String endpointPath) {
        return UrlUtils.INSTANCE.omitTrailingSlash(HttpUrl.parse(
                getBaseUriBuilder()
                        .appendEncodedPath(endpointPath)
                        .build()
                        .toString()));
    }

    protected HttpUrl getWebDavHttpUrl(String resourcePath) {
        return UrlUtils.INSTANCE.omitTrailingSlash(HttpUrl.parse(
                getBaseUriBuilder()
                        .appendEncodedPath(WEBDAV_PATH_4_0)
                        .appendEncodedPath(mContext.getOCAccount().getDisplayName())
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