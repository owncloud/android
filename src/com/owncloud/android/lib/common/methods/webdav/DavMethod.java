package com.owncloud.android.lib.common.methods.webdav;

import com.owncloud.android.lib.common.methods.HttpBaseMethod;

import at.bitfire.dav4android.DavResource;

public abstract class DavMethod implements HttpBaseMethod {

    protected DavResource mDavResource;

    public DavMethod(DavResource davResource) {
        mDavResource = davResource;
    }
}