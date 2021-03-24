/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import com.owncloud.android.R
import com.owncloud.android.utils.DOWNLOAD_NOTIFICATION_ID_DEFAULT
import com.owncloud.android.utils.NOTIFICATION_TIMEOUT_STANDARD

fun CoroutineWorker.showNotificationWithProgress(
    progress: Int,
    maxValue: Int,
    contentTitle: String,
    contentText: String,
    notificationChannelId: String,
    fileId: Long?,
    pendingIntent: PendingIntent,
) {

    val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val notificationBuilder = NotificationCompat.Builder(
        applicationContext,
        notificationChannelId
    ).setContentTitle(contentTitle)
        .setSmallIcon(R.drawable.notification_icon)
        .setWhen(System.currentTimeMillis())
        .setContentText(contentText)
        .setProgress(maxValue, progress, false)
        .setContentIntent(pendingIntent)

    if (progress == maxValue) {
        notificationBuilder
            .setTimeoutAfter(NOTIFICATION_TIMEOUT_STANDARD)
            .setOngoing(false)
    } else {
        notificationBuilder
            .setOngoing(true)
    }

    val notificationId = fileId?.toInt() ?: DOWNLOAD_NOTIFICATION_ID_DEFAULT
    notificationManager.notify(notificationId, notificationBuilder.build())
}
