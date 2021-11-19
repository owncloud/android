/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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
 *
 */

package com.owncloud.android.extensions

import android.annotation.SuppressLint
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

@SuppressLint("CheckResult")
fun ImageView.setPicture(imageToLoad: Int) {
    Glide.with(this)
        .load(imageToLoad)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .into(this)
}
