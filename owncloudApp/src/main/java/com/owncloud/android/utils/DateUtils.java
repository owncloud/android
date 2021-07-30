/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Static methods to help in date handling
 */
public class DateUtils {

    /**
     * Add a number of days to a specific date
     * @param defaultDate
     * @param days
     * @return
     */
    public static Date addDaysToDate(Date defaultDate, int days) {

        Calendar c = Calendar.getInstance();
        c.setTime(defaultDate);
        c.add(Calendar.DATE, days);
        defaultDate = c.getTime();

        return defaultDate;
    }
}
