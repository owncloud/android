package com.owncloud.android.lib.refactor.operations;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.RemoteOperation;
import com.owncloud.android.lib.refactor.RemoteOperationResult;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.PropertyUtils;

public class PropfindOperation extends RemoteOperation {

    private String mRemotePath;

    public PropfindOperation(OCContext context, String remotePath) {
        super(context);

        mRemotePath = remotePath;
    }

    @Override
    public RemoteOperationResult exec() {

        try {
            DavResource davResource = new DavResource(getClient(), getWebDavHttpUrl("/"));
            davResource.propfind(1, PropertyUtils.INSTANCE.getAllPropSet());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}