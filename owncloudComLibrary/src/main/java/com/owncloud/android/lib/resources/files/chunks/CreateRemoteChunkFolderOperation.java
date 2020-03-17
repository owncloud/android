/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.files.chunks;

import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;

/**
 * Remote operation performing the creation of a new folder to save chunks during an upload to the ownCloud server.
 *
 * @author David González Verdugo
 */
public class CreateRemoteChunkFolderOperation extends CreateRemoteFolderOperation {
    /**
     * Constructor
     *
     * @param remotePath     Full path to the new directory to create in the remote server.
     * @param createFullPath 'True' means that all the ancestor folders should be created.
     */
    public CreateRemoteChunkFolderOperation(String remotePath, boolean createFullPath) {
        super(remotePath, createFullPath);
        createChunksFolder = true;
    }
}