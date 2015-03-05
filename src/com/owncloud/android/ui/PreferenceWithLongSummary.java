/**
 * Copyright (C) 2015 ownCloud, Inc.
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License, version 3,
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.preference.Preference;

public class PreferenceWithLongSummary extends Preference{

    public PreferenceWithLongSummary(Context context) {
        super(context);
    }

    public PreferenceWithLongSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public PreferenceWithLongSummary(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = (TextView) view.findViewById(android.R.id.summary);
        titleView.setSingleLine(true);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    }
}