package com.owncloud.android.lib.common.methods;

import java.io.IOException;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.PropertyUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;

public class PropfindMethod extends DavMethod {

    private int mDepth;

    public PropfindMethod(DavResource davResource, int depth) {
        super(davResource);
        mDepth = depth;
    };

    public int execute() throws DavException, IOException, HttpException {
        mDavResource.propfind(mDepth, PropertyUtils.INSTANCE.getAllPropSet());
        return mDavResource.getResponse().code();
    }
}
