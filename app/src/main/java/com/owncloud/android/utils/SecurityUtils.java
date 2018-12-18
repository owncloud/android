/**
 *  ownCloud Android client application
 *
 *  @author David González Verdugo
 *  Copyright (C) 2018 ownCloud GmbH.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtils {

    private static final String TAG = SecurityUtils.class.getSimpleName();

    public static String stringToMD5Hash(String stringToTransform) {
        MessageDigest messageDigest;
        String hash = null;

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(stringToTransform.getBytes(),0,stringToTransform.length());
            hash = new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log_OC.d(TAG, "It's been not possible to generate the MD5 hash because of " + e);
        }

        return hash;
    }
}