package com.owncloud.android.lib.refactor.operations;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.refactor.RemoteOperation;
import com.owncloud.android.lib.refactor.RemoteOperationResult;
import java.io.IOException;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.DisplayName;
import okhttp3.HttpUrl;

public class PropfindOperation extends RemoteOperation {

    private String mRemotePath;

    public PropfindOperation(OCContext context, String remotePath) {
        super(context);

        mRemotePath = remotePath;
    }

    @Override
    public RemoteOperationResult exec() {
        DavResource davResource = new DavResource(
                getClient(),
                HttpUrl.parse(getWebDAVUriBuilder() + WebdavUtils.encodePath(mRemotePath)),
                null);

        try {
            davResource.propfind(1, DisplayName.NAME);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (DavException e) {
            e.printStackTrace();
        }

        davResource.getProperties();

        return null;
    }
}