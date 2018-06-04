package com.owncloud.android.lib.common.methods;

import at.bitfire.dav4android.DavResource;

public abstract class DavMethod implements HttpBaseMethod {

    protected DavResource mDavResource;

    public DavMethod(DavResource davResource) {
        mDavResource = davResource;
    }
}