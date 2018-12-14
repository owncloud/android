/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
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

    public static final int MINIMUN_VERSION_FOR_CHUNKED_UPLOADS = 0x04050000; // 4.5

    public static final int MINIMUM_VERSION_FOR_SHARING_API = 0x05001B00; // 5.0.27

    public static final int MINIMUM_VERSION_WITH_FORBIDDEN_CHARS = 0x08010000; // 8.1

    public static final int MINIMUM_SERVER_VERSION_FOR_REMOTE_THUMBNAILS = 0x07080000; // 7.8.0

    public static final int MINIMUM_VERSION_FOR_SEARCHING_USERS = 0x08020000; //8.2

    public static final int VERSION_8 = 0x08000000; // 8.0

    public static final int MINIMUM_VERSION_CAPABILITIES_API = 0x08010000; // 8.1

    private static final int MINIMUM_VERSION_WITH_NOT_RESHAREABLE_FEDERATED = 0x09010000;   // 9.1

    private static final int MINIMUM_VERSION_WITH_SESSION_MONITORING = 0x09010000;   // 9.1

    private static final int MINIMUM_VERSION_WITH_SESSION_MONITORING_WORKING_IN_PREEMPTIVE_MODE = 0x09010301;
    // 9.1.3.1, final 9.1.3: https://github.com/owncloud/core/commit/f9a867b70c217463289a741d4d26079eb2a80dfd

    private static final int MINIMUM_VERSION_WITH_MULTIPLE_PUBLIC_SHARING = 0xA000000; // 10.0.0

    private static final int MINIMUM_VERSION_WITH_WRITE_ONLY_PUBLIC_SHARING = 0xA000100; // 10.0.1

    private static final String INVALID_ZERO_VERSION = "0.0.0";

    private static final int MAX_DOTS = 3;

    // format is in version
    // 0xAABBCCDD
    // for version AA.BB.CC.DD
    // ie version 2.0.3 will be stored as 0x02000300
    private int mVersion;
    private boolean mIsValid;

    public OwnCloudVersion(String version) {
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
        // gets the first digit of version, shifting hexadecimal version to right 'til max position
        String versionToString = String.valueOf((mVersion >> (8 * MAX_DOTS)) % 256);
        for (int i = MAX_DOTS - 1; i >= 0; i--) {
            // gets another digit of version, shifting hexadecimal version to right 8*i bits and...
            // ...discarding left part with mod 256
            versionToString = versionToString + "." + String.valueOf((mVersion >> (8 * i)) % 256);
        }
        if (!mIsValid) {
            versionToString += " INVALID";
        }
        return versionToString;
    }

    public String getVersion() {
        if (mIsValid) {
            return toString();
        } else {
            return INVALID_ZERO_VERSION;
        }
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
            // if invalid, the instance will respond as if server is 8.1, minimum with capabilities API,
            // and "dead" : https://github.com/owncloud/core/wiki/Maintenance-and-Release-Schedule
            mVersion = MINIMUM_VERSION_CAPABILITIES_API;
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


    public boolean isChunkedUploadSupported() {
        return (mVersion >= MINIMUN_VERSION_FOR_CHUNKED_UPLOADS);
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

    public boolean isAfter8Version() {
        return (mVersion >= VERSION_8);
    }

    public boolean isSearchUsersSupported() {
        return (mVersion >= MINIMUM_VERSION_FOR_SEARCHING_USERS);
    }

    public boolean isVersionWithCapabilitiesAPI() {
        return (mVersion >= MINIMUM_VERSION_CAPABILITIES_API);
    }

    public boolean isNotReshareableFederatedSupported() {
        return (mVersion >= MINIMUM_VERSION_WITH_NOT_RESHAREABLE_FEDERATED);
    }

    public boolean isSessionMonitoringSupported() {
        return (mVersion >= MINIMUM_VERSION_WITH_SESSION_MONITORING);
    }

    /**
     * From OC 9.1 session tracking is a feature, but to get it working in the OC app we need the preemptive
     * mode of basic authentication is disabled. This changes in OC 9.1.3, where preemptive mode is compatible
     * with session tracking again.
     *
     * @return True for every version before 9.1 and from 9.1.3, false otherwise
     */
    public boolean isPreemptiveAuthenticationPreferred() {
        return (
            (mVersion < MINIMUM_VERSION_WITH_SESSION_MONITORING) ||
                (mVersion >= MINIMUM_VERSION_WITH_SESSION_MONITORING_WORKING_IN_PREEMPTIVE_MODE)
        );
    }

    public boolean isMultiplePublicSharingSupported() {
        return (mVersion >= MINIMUM_VERSION_WITH_MULTIPLE_PUBLIC_SHARING);
    }

    public boolean isPublicSharingWriteOnlySupported() {
        return (mVersion >= MINIMUM_VERSION_WITH_WRITE_ONLY_PUBLIC_SHARING);
    }
}
