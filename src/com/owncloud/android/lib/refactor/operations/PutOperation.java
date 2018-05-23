package com.owncloud.android.lib.refactor.operations;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.RemoteOperation;
import com.owncloud.android.lib.refactor.RemoteOperationResult;

import at.bitfire.dav4android.DavOCResource;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.PropertyUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class PutOperation extends RemoteOperation {

    private String mRemotePath;
    private String mMimeType;
    private String mTimeStamp;

    public PutOperation(OCContext context, String remotePath, String mimetype, String timestamp) {
        super(context);

        mRemotePath = remotePath;
        mMimeType = mimetype;
        mTimeStamp = timestamp;
    }

    @Override
    public RemoteOperationResult exec() {

        try {

            MediaType mediaType = MediaType.parse(mMimeType);
            RequestBody requestBody = RequestBody.create(mediaType, mRemotePath);

            DavOCResource davOCResource = new DavOCResource(getClient(), getWebDavHttpUrl(mRemotePath), null);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}