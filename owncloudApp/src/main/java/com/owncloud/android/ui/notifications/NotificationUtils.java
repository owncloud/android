/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2016 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.notifications;

import android.accounts.Account;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import androidx.core.app.NotificationCompat;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;

import java.util.Random;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationUtils {

    private static final String FILE_SYNC_CONFLICT_CHANNEL_ID = "FILE_SYNC_CONFLICT_CHANNEL_ID";

    /**
     * Factory method for {@link androidx.core.app.NotificationCompat.Builder} instances.
     * <p>
     * Not strictly needed from the moment when the minimum API level supported by the app
     * was raised to 14 (Android 4.0).
     * <p>
     * Formerly, returned a customized implementation of {@link androidx.core.app.NotificationCompat.Builder}
     * for Android API levels >= 8 and < 14.
     * <p>
     * Kept in place for the extra abstraction level; notifications in the app need a review, and they
     * change a lot in different Android versions.
     *
     * @param context Context that will use the builder to create notifications
     * @return An instance of the regular {@link NotificationCompat.Builder}.
     */
    public static NotificationCompat.Builder newNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context).
                setColor(context.getResources().getColor(R.color.primary));
    }

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
                ((HandlerThread) Thread.currentThread()).getLooper().quit();
            }
        }, delayInMillis);

    }

    /**
     * Show a notification with file conflict information, that will open a dialog to solve it when tapping it
     *
     * @param fileInConflict file in conflict
     * @param account        account which the file in conflict belongs to
     */
    public static void notifyConflict(OCFile fileInConflict, Account account, Context context) {
        NotificationManager notificationManager = (NotificationManager) context.
                getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = newNotificationBuilder(context);

        // Configure notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel;
            CharSequence name = context.
                    getString(R.string.file_sync_notification_channel_name);
            String description = context.
                    getString(R.string.file_sync_notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            notificationChannel = new NotificationChannel(FILE_SYNC_CONFLICT_CHANNEL_ID,
                    name, importance);
            notificationChannel.setDescription(description);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationBuilder
                .setChannelId(FILE_SYNC_CONFLICT_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setTicker(context.getString(R.string.conflict_title))
                .setContentTitle(context.getString(R.string.conflict_title))
                .setContentText(String.format(
                        context.getString(R.string.conflict_description),
                        fileInConflict.getRemotePath())
                )
                .setAutoCancel(true);

        Intent showConflictActivityIntent = new Intent(context, ConflictsResolveActivity.class);
        showConflictActivityIntent.setFlags(showConflictActivityIntent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_FROM_BACKGROUND);
        showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_FILE, fileInConflict);
        showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, account);

        notificationBuilder.setContentIntent(
                PendingIntent.getActivity(context, (int) System.currentTimeMillis(),
                        showConflictActivityIntent, 0)
        );

        int notificationId = 0;

        // We need a notification id for each file in conflict, let's use the file id but in a safe way
        if ((int) fileInConflict.getFileId() >= Integer.MIN_VALUE && (int) fileInConflict.getFileId() <=
                Integer.MAX_VALUE) {
            notificationId = (int) fileInConflict.getFileId();
        }

        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
