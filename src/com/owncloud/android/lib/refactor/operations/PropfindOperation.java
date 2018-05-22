package com.owncloud.android.lib.refactor.operations;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.refactor.RemoteOperation;
import com.owncloud.android.lib.refactor.RemoteOperationResult;
import java.io.IOException;
import java.util.logging.Logger;

import at.bitfire.dav4android.Constants;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.DisplayName;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class PropfindOperation extends RemoteOperation {

    private String mRemotePath;

    public PropfindOperation(OCContext context, String remotePath) {
        super(context);

        mRemotePath = remotePath;
    }

    @Override
    public RemoteOperationResult exec() {

        try {
            HttpUrl location = HttpUrl.parse(getBaseUriBuilder().build().toString());

            DavResource davResource = new DavResource(getClient(), getWebDavHttpUrl("/"));
            davResource.propfind(0, DisplayName.NAME);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}