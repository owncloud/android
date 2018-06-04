package com.owncloud.android.lib.common.methods;

import java.io.IOException;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.PropertyUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.exception.UnauthorizedException;

public class PropfindMethod extends DavMethod {

    private int mDepth;

    public PropfindMethod(DavResource davResource, int depth) {
        super(davResource);
        mDepth = depth;
    };

    public int execute() throws IOException, HttpException, DavException {
        try {
            mDavResource.propfind(mDepth, PropertyUtils.INSTANCE.getAllPropSet());
        } catch (UnauthorizedException davException) {
            return 401;
        }
        return mDavResource.getResponse().code();
    }
}