/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
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

package com.owncloud.android.services.observer;

import com.owncloud.android.db.PreferenceManager.InstantUploadsConfiguration;

/**
 * Observer watching a folder to request the upload of new pictures or videos inside it.
 */
interface InstantUploadsObserver {

    /**
     * Starts to observe the folder specified in held {@link InstantUploadsConfiguration}
     */
    void startObserving();

    /**
     * Stops to observe the folder specified in held {@link InstantUploadsConfiguration}
     */
    void stopObserving();

    /**
     * Updates the configuration for instant uploads held by the observer with the one received.
     *
     * Source path of both the new and the current configurations must be the same.
     *
     * @param configuration     New configuration for instant uploads to replace the current one.
     * @return                  'True' if the new configuration could be handled by the observer,
     *                          'false' otherwise.
     */
    boolean updateConfiguration(InstantUploadsConfiguration configuration);
}
