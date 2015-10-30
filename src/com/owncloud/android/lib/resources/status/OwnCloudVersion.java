/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
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

public class OwnCloudVersion implements Comparable<OwnCloudVersion> {
    public static final OwnCloudVersion owncloud_v1 = new OwnCloudVersion(
            0x01000000);
    public static final OwnCloudVersion owncloud_v2 = new OwnCloudVersion(
            0x02000000);
    public static final OwnCloudVersion owncloud_v3 = new OwnCloudVersion(
            0x03000000);
    public static final OwnCloudVersion owncloud_v4 = new OwnCloudVersion(
            0x04000000);
    public static final OwnCloudVersion owncloud_v4_5 = new OwnCloudVersion(
            0x04050000);
    
    public static final int MINIMUM_VERSION_FOR_SHARING_API = 0x05001B00; // 5.0.27

    public static final int MINIMUM_VERSION_WITH_FORBIDDEN_CHARS = 0x08010000; // 8.1

    public static final int MINIMUM_SERVER_VERSION_FOR_REMOTE_THUMBNAILS = 0x07080000; // 7.8.0

    public static final int MINIMUM_VERSION_FOR_SEARCHING_USERS = 0x08020000; //8.2

    public static final int VERSION_8 = 0x08000000; // 8.0

    public static final int MINIMUM_VERSION_CAPABILITIES_API = 0x08010000; // 8.1
    
    private static final int MAX_DOTS = 3;
    
    // format is in version
    // 0xAABBCCDD
    // for version AA.BB.CC.DD
    // ie version 2.0.3 will be stored as 0x02000300
    private int mVersion;
    private boolean mIsValid;

    protected OwnCloudVersion(int version) {
        mVersion = version;
        mIsValid = true;
    }
    
    public OwnCloudVersion(String version){
    	 mVersion = 0;
         mIsValid = false;
         int countDots = version.length() - version.replace(".", "").length();

         // Complete the version. Version must have 3 dots
         for (int i = countDots; i < MAX_DOTS; i++) {
        	 version = version + ".0";
         }
         
         parseVersion(version);

    }
    
    public String toString() {
    	String versionToString = String.valueOf((mVersion >> (8*MAX_DOTS)) % 256);
    	for (int i = MAX_DOTS - 1; i >= 0; i-- ) {
    		versionToString = versionToString + "." + String.valueOf((mVersion >> (8*i)) % 256);
    	}
        return versionToString;
    }
    
    public String getVersion() {
    	return toString();
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
    	for (int i = 0; i < nums.length && i <= MAX_DOTS; i++) {
    		versionValue += Integer.parseInt(nums[i]);
    		if (i < nums.length - 1) {
    			versionValue = versionValue << 8;
    		}
    	}

    	return versionValue; 
    }
    
    
    public boolean isSharedSupported() {
    	return (mVersion >= MINIMUM_VERSION_FOR_SHARING_API);
    }

    public boolean isVersionWithForbiddenCharacters() {
        return (mVersion >= MINIMUM_VERSION_WITH_FORBIDDEN_CHARS);
    }

    public boolean supportsRemoteThumbnails() {
        return (mVersion >= MINIMUM_SERVER_VERSION_FOR_REMOTE_THUMBNAILS);
    }

    public boolean isAfter8Version(){
        return (mVersion >= VERSION_8);
    }

    public boolean isSearchUsersSupported() {
        return (mVersion >= MINIMUM_VERSION_FOR_SEARCHING_USERS);
    }

    public boolean isVersionWithCapabilitiesAPI(){
        return (mVersion>= MINIMUM_VERSION_CAPABILITIES_API);
    }
    
    
}
