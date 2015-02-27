/**
 *    @author |"[insert key contributors here, as we wish or delete the line]"
 *    Copyright (C) 2011  Bartek Przybylski
 *    Copyright (C) 2015 ownCloud, Inc.
 *
 *    This code is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License, version 3,
 *    as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License, version 3,
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Background service for synchronizing remote files with their local state.
 * 
 * Serves as a connector to an instance of {@link FileSyncAdapter}, as required by standard Android APIs. 
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class FileSyncService extends Service {
    
    // Storage for an instance of the sync adapter
    private static FileSyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();
    
    /*
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new FileSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
       return sSyncAdapter.getSyncAdapterBinder();
    }
    
}
