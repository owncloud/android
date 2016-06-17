/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
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

/**
 * Interface for both recursive and non recursive folder observers used by {@link FileObserverService}
 */
public interface AvailableOfflineObserver {


    /**
     * Starts the observation
     */
    void startWatching();


    /**
     * Stops the observation
     */
    public void stopWatching();


    /**
     * Adds a child file to the list of files observed by the folder observer.
     * 
     * @param relativePath      Relative path to a file inside the observed folder.
     */
    void startWatching(String relativePath);

    
    /**
     * Removes a child file from the list of files observed by the folder observer.
     * 
     * @param relativePath      Relative path to a file inside the observed folder.
     */
    void stopWatching(String relativePath);


    /**
     * @return      'True' when the folder is not watching any file inside.
     */
    boolean isEmpty();

}
