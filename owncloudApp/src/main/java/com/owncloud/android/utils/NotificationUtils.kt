/**
 * ownCloud Android client application
 *
 *
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils

import android.accounts.Account
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import java.util.Random

object NotificationUtils {
    /**
     * Factory method for [androidx.core.app.NotificationCompat.Builder] instances.
     *
     *
     * Not strictly needed from the moment when the minimum API level supported by the app
     * was raised to 14 (Android 4.0).
     *
     *
     * Formerly, returned a customized implementation of [androidx.core.app.NotificationCompat.Builder]
     * for Android API levels >= 8 and < 14.
     *
     *
     * Kept in place for the extra abstraction level; notifications in the app need a review, and they
     * change a lot in different Android versions.
     *
     * @param context Context that will use the builder to create notifications
     * @return An instance of the regular [NotificationCompat.Builder].
     */
    @JvmStatic
    fun newNotificationBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context).setColor(context.resources.getColor(R.color.primary))
    }

    @JvmStatic
    fun cancelWithDelay(
        notificationManager: NotificationManager,
        notificationId: Int,
        delayInMillis: Long
    ) {
        val thread = HandlerThread(
            "NotificationDelayerThread_" + Random(System.currentTimeMillis()).nextInt(),
            Process.THREAD_PRIORITY_BACKGROUND
        )
        thread.start()
        val handler = Handler(thread.looper)
        handler.postDelayed({
            notificationManager.cancel(notificationId)
            (Thread.currentThread() as HandlerThread).looper.quit()
        }, delayInMillis)
    }

    /**
     * Show a notification with file conflict information, that will open a dialog to solve it when tapping it
     *
     * @param fileInConflict file in conflict
     * @param account        account which the file in conflict belongs to
     */
    @JvmStatic
    fun notifyConflict(fileInConflict: OCFile, account: Account?, context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = newNotificationBuilder(context)
        notificationBuilder
            .setChannelId(FILE_SYNC_CONFLICT_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setTicker(context.getString(R.string.conflict_title))
            .setContentTitle(context.getString(R.string.conflict_title))
            .setContentText(
                String.format(
                    context.getString(R.string.conflict_description),
                    fileInConflict.remotePath
                )
            )
            .setAutoCancel(true)
        val showConflictActivityIntent = Intent(context, ConflictsResolveActivity::class.java)
        showConflictActivityIntent.flags = showConflictActivityIntent.flags or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_FROM_BACKGROUND
        showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_FILE, fileInConflict)
        showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, account)
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
                context, System.currentTimeMillis().toInt(),
                showConflictActivityIntent, 0
            )
        )
        var notificationId = 0

        // We need a notification id for each file in conflict, let's use the file id but in a safe way
        if (fileInConflict.fileId.toInt() >= Int.MIN_VALUE && fileInConflict.fileId.toInt() <= Int.MAX_VALUE) {
            notificationId = fileInConflict.fileId.toInt()
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
