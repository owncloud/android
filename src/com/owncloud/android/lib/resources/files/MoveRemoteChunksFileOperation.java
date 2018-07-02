package com.owncloud.android.lib.resources.files;

public class MoveRemoteChunksFileOperation extends MoveRemoteFileOperation {

    /**
     * Constructor.
     *
     * @param srcRemotePath    Remote path of the file/folder to move.
     * @param targetRemotePath Remove path desired for the file/folder after moving it.
     * @param overwrite
     */
    public MoveRemoteChunksFileOperation(String srcRemotePath, String targetRemotePath, boolean overwrite,
                                         String fileLastModifTimestamp, long fileLength) {
        super(srcRemotePath, targetRemotePath, overwrite);
        isChunkedFile = true;
        mFileLastModifTimestamp = fileLastModifTimestamp;
        mFileLength = fileLength;
    }
}