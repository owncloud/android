/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.datamodel;

import java.util.ArrayList;
import java.util.List;

public class OCFilesForAccount {

    private List<OCFileForAccount> mFilesForAccount = new ArrayList<>();

    public OCFilesForAccount(List<OCFileForAccount> filesForAccount) {
        for (OCFileForAccount ocFileForAccount : filesForAccount) {
            mFilesForAccount.add(ocFileForAccount);
        }
    }

    public List<OCFileForAccount> getFilesForAccount() {
        return mFilesForAccount;
    }

    public static class OCFileForAccount {
        private OCFile mFile;
        private String mAccountName;

        public OCFileForAccount(OCFile file, String accountName) {
            this.mFile = file;
            this.mAccountName = accountName;
        }

        public OCFile getFile() {
            return mFile;
        }

        public void setFile(OCFile file) {
            this.mFile = file;
        }

        public String getAccountName() {
            return mAccountName;
        }

        public void setAccountName(String accountName) {
            this.mAccountName = accountName;
        }
    }
}