/**
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

package com.owncloud.android.notifications;

import java.util.Random;

import android.app.NotificationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

public class NotificationDelayer {
    
    public static void cancelWithDelay(
            final NotificationManager notificationManager,
            final int notificationId,
            long delayInMillis) {
    
        HandlerThread thread = new HandlerThread(
                "NotificationDelayerThread_" + (new Random(System.currentTimeMillis())).nextInt(),
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        
        Handler handler = new Handler(thread.getLooper()); 
        handler.postDelayed(new Runnable() { 
             public void run() { 
                 notificationManager.cancel(notificationId);
                 ((HandlerThread)Thread.currentThread()).getLooper().quit();
             } 
        }, delayInMillis); 
    
    }
    

}
