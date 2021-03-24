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
import androidx.core.content.ContextCompat
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import java.util.Random

object NotificationUtils {

    @JvmStatic
    fun newNotificationBuilder(context: Context, channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId).apply {
            color = ContextCompat.getColor(context, R.color.primary)
            setSmallIcon(R.drawable.notification_icon)
        }
    }

    fun createBasicNotification(
        context: Context,
        contentTitle: String,
        contentText: String,
        notificationChannelId: String,
        notificationId: Int,
        intent: PendingIntent?,
        onGoing: Boolean = false,
        timeOut: Long?,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = newNotificationBuilder(context, notificationChannelId).apply {
            setContentTitle(contentTitle)
            color = ContextCompat.getColor(context, R.color.primary)
            setSmallIcon(R.drawable.notification_icon)
            setWhen(System.currentTimeMillis())
            setContentText(contentText)
            setOngoing(onGoing)
            setAutoCancel(true)
        }

        intent?.let {
            notificationBuilder.setContentIntent(it)
        }

        timeOut?.let {
            notificationBuilder.setTimeoutAfter(it)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
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
        val notificationBuilder = newNotificationBuilder(context, FILE_SYNC_CONFLICT_CHANNEL_ID)
        notificationBuilder
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
        if (fileInConflict.id!!.toInt() >= Int.MIN_VALUE && fileInConflict.id!!.toInt() <= Int.MAX_VALUE) {
            notificationId = fileInConflict.id!!.toInt()
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
