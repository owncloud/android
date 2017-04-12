/**
 *  ownCloud Android client application
 *
 *  @author David A. Velasco
 *  Copyright (C) 2017 ownCloud GmbH.
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

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

public class PowerUtils {

    public static boolean isDeviceIdle(Context context) {
        return (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isDeviceIdleMode()
        );
    }
}
