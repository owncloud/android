/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/) 
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

package com.owncloud.android.lib.utils;

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
    
    public static final int MINIMUM_VERSION_STRING_FOR_SHARING_API = 0x05000D;
    
    // format is in version
    // 0xAABBCC
    // for version AA.BB.CC
    // ie version 2.0.3 will be stored as 0x020003
    private int mVersion;
    private boolean mIsValid;
    // not parsed, saved same value offered by the server
    private String mVersionString;

    protected OwnCloudVersion(int version) {
        mVersion = version;
        mIsValid = true;
        mVersionString = "";
    }

    public OwnCloudVersion(String version, String versionString) {
        mVersion = 0;
        mIsValid = false;
        parseVersionString(version);
        if (versionString != null && versionString.length() > 0) {
        	mVersionString = versionString;
        	
        } else if (mIsValid) {
        	mVersionString = version;
        }
    }
    
    public String toString() {
        return ((mVersion >> 16) % 256) + "." + ((mVersion >> 8) % 256) + "."
                + ((mVersion) % 256);
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

    private void parseVersionString(String versionString) {
    	try {
    		mVersion = getParsedVersionString(versionString);
    		mIsValid = true;
    		
    	} catch (Exception e) {
    		mIsValid = false;
        }
    }
    
    private int getParsedVersionString(String versionString) throws NumberFormatException {
		int version = 0;
		
    	// get only numeric part 
		versionString = versionString.replaceAll("[^\\d.]", "");
		
		String[] nums = versionString.split("\\.");
		if (nums.length > 0) {
			version += Integer.parseInt(nums[0]);
		}
		version = version << 8;
		if (nums.length > 1) {
			version += Integer.parseInt(nums[1]);
		}
		version = version << 8;
		if (nums.length > 2) {
			version += Integer.parseInt(nums[2]);
		}
		return version; 
    }
    
    
    public boolean isSharedSupported() {
    	int versionString = 0;
    	try {
    		versionString = getParsedVersionString(mVersionString);
    		
    	} catch (Exception e) {
    		// nothing to do here
    	}
    	return (versionString >= MINIMUM_VERSION_STRING_FOR_SHARING_API);
    }
    
    
}
