/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud Inc. 
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.lib.resources.status;

import android.util.Log;

public class OwnCloudVersion implements Comparable<OwnCloudVersion> {
    public static final OwnCloudVersion owncloud_v1 = new OwnCloudVersion(
            0x010000);
    public static final OwnCloudVersion owncloud_v2 = new OwnCloudVersion(
            0x020000);
    public static final OwnCloudVersion owncloud_v3 = new OwnCloudVersion(
            0x030000);
    public static final OwnCloudVersion owncloud_v4 = new OwnCloudVersion(
            0x040000);
    public static final OwnCloudVersion owncloud_v4_5 = new OwnCloudVersion(
            0x040500);
    
    public static final int MINIMUM_VERSION_FOR_SHARING_API = 0x05001B; // 5.0.27
    
    // format is in version
    // 0xAABBCC
    // for version AA.BB.CC
    // ie version 2.0.3 will be stored as 0x020003
    private int mVersion;
    private boolean mIsValid;
    // not parsed, saved same value offered by the server
    private String mVersionString;
    private int mCountDots;

    protected OwnCloudVersion(int version) {
        mVersion = version;
        mIsValid = true;
        mVersionString = "";
    }
    
    public OwnCloudVersion(String version){
    	 mVersion = 0;
         mIsValid = false;
         mCountDots = version.length() - version.replace(".", "").length();
         Log.d("OwnCloudVersion", "VERSION "+ version + " Asign dots = " + String.valueOf(mCountDots));
         parseVersion(version);

    }

//    public OwnCloudVersion(String version, String versionString) {
//        mVersion = 0;
//        mIsValid = false;
//        //mCountDots = version.length() - version.replace(".", "").length();
//        mCountDots = 2;
//        Log.d("OwnCloudVersion", "VERSION "+ version + " Asign dots = " + String.valueOf(mCountDots));
//        parseVersion(version);
//        if (versionString != null && versionString.length() > 0) {
//        	mVersionString = versionString;
//        	
//        } else if (mIsValid) {
//        	mVersionString = version;
//        }
//    }
    
    public String toString() {
    	String versionToString = String.valueOf((mVersion >> (8*mCountDots)) % 256);
    	Log.d("OwnCloudVersion", " versionToString " + versionToString);
    	for (int i = mCountDots - 1; i >= 0; i-- ) {
        	Log.d("OwnCloudVersion", "i = "+ i + " versionToString " + versionToString);
    		versionToString = versionToString + "." + String.valueOf((mVersion >> (8*i)) % 256);
    	}
    	//versionToString = versionToString + "." + String.valueOf((mVersion) % 256);
    	
    	Log.d("OwnCloudVersion", "dots = " + String.valueOf(mCountDots));
    	Log.d("OwnCloudVersion", " versionToString " + versionToString);
        Log.d("OwncloudVersion", ((mVersion >> 24) % 256) + "." + ((mVersion >> 16) % 256) + "." + ((mVersion >> 8) % 256) + "."
                + ((mVersion) % 256));
        return versionToString;
    }
    
    public String getVersion() {
    	return toString();
    }
    
    public String getVersionString() {
    	return mVersionString;
    }

    public boolean isVersionValid() {
        return mIsValid;
    }

    @Override
    public int compareTo(OwnCloudVersion another) {
        return another.mVersion == mVersion ? 0
                : another.mVersion < mVersion ? 1 : -1;
    }

    private void parseVersion(String version) {
    	try {
    		mVersion = getParsedVersion(version);
    		mIsValid = true;
    		
    	} catch (Exception e) {
    		mIsValid = false;
        }
    }
    
    private int getParsedVersion(String version) throws NumberFormatException {
		int versionValue = 0;
		
    	// get only numeric part 
		version = version.replaceAll("[^\\d.]", "");
		
		String[] nums = version.split("\\.");
		for (int i = 0; i < nums.length; i++) {
			versionValue += Integer.parseInt(nums[i]);
			if (i< nums.length -1) {
				versionValue = versionValue << 8;
			}
		}
    	Log.d("OwnCloudVersion", " ---- version " + String.valueOf(versionValue));
    	
//    	versionValue = 0;
//		if (nums.length > 0) {
//			versionValue += Integer.parseInt(nums[0]);
//		}
//		versionValue = versionValue << 8;
//		if (nums.length > 1) {
//			versionValue += Integer.parseInt(nums[1]);
//		}
//		versionValue = versionValue << 8;
//		if (nums.length > 2) {
//			versionValue += Integer.parseInt(nums[2]);
//		}
//		versionValue = versionValue << 8;
//		if (nums.length > 3) {
//			versionValue += Integer.parseInt(nums[3]);
//		}
//		Log.d("OwnCloudVersion", " -----version " + String.valueOf(versionValue));
		return versionValue; 
    }
    
    
    public boolean isSharedSupported() {
//    	int version = 0;
//    	try {
//    		version = getParsedVersion(mVersion);
//    		
//    	} catch (Exception e) {
//    		// nothing to do here
//    	}
    	return (mVersion >= MINIMUM_VERSION_FOR_SHARING_API);
    }
    
    
}
