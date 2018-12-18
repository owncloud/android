/**
 *   ownCloud Android client application
 *
 *   @author Jesús Recio @jesmrec
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils;

public enum ServerType {
        /*
         * Server with http
         */
        HTTP(1),

        /*
         * Server with https, but non-secure certificate
         */
        HTTPS_NON_SECURE(2),

        /*
         * Server with https
         */
        HTTPS_SECURE(3),

        /*
         * Server redirected to a non-secure server
         */
        REDIRECTED_NON_SECURE(4);

        private final int status;

        ServerType(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public static ServerType fromValue(int value) {
            switch (value) {
                case 1:
                    return HTTP;
                case 2:
                    return HTTPS_NON_SECURE;
                case 3:
                    return HTTPS_SECURE;
                case 4:
                    return REDIRECTED_NON_SECURE;
            }
            return null;
        }
}
