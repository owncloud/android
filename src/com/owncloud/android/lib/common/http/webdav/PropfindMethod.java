package com.owncloud.android.lib.common.http.webdav;

import java.io.IOException;
import java.util.Set;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.PropertyUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.exception.UnauthorizedException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class PropfindMethod extends DavMethod {

    private int mDepth;
    private Set<DavResource> mMembers;

    public PropfindMethod(OkHttpClient okHttpClient, HttpUrl httpUrl, int depth) {
        super(okHttpClient, httpUrl);
        mDepth = depth;
    };

    @Override
    public int execute() throws IOException, HttpException, DavException {

        try {
            mDavResource.propfind(mDepth, PropertyUtils.INSTANCE.getAllPropSet());
            mMembers = mDavResource.getMembers();
        } catch (UnauthorizedException davException) {
            // Do nothing, we will use the 401 code to handle the situation
        }

        mRequest = mDavResource.getRequest();
        mResponse = mDavResource.getResponse();

        return mResponse.code();
    }

    public int getDepth() {
        return mDepth;
    }

    public Set<DavResource> getMembers() {
        return mMembers;
    }
}