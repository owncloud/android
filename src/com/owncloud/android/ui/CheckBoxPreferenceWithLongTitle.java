/**
 *    @author |"[insert key contributors here, as we wish or delete the line]"
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

package com.owncloud.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.preference.CheckBoxPreference;

public class CheckBoxPreferenceWithLongTitle extends CheckBoxPreference{

    public CheckBoxPreferenceWithLongTitle(Context context) {
        super(context);
    }

    public CheckBoxPreferenceWithLongTitle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CheckBoxPreferenceWithLongTitle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setSingleLine(false);
        titleView.setMaxLines(3);
        titleView.setEllipsize(null);
    }
}