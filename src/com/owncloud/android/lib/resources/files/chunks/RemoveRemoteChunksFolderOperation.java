package com.owncloud.android.lib.resources.files.chunks;

import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;

public class RemoveRemoteChunksFolderOperation extends RemoveRemoteFileOperation {

    /**
     * Constructor
     *
     * @param remotePath RemotePath of the remote file or folder to remove from the server
     */
    public RemoveRemoteChunksFolderOperation(String remotePath) {
        super(remotePath);
        removeChunksFolder = true;
    }
}