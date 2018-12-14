/**
 *   ownCloud Android client application
 *
 *   @author Christian Schabesberger
 *   Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.operations;

public enum AuthenticationMethod {
    NONE,
    BASIC_HTTP_AUTH,
    SAML_WEB_SSO,
    BEARER_TOKEN;

    public int getValue() {
        return ordinal();
    }

    public static AuthenticationMethod fromValue(int value) {
        if (value > -1 && value < values().length) {
            return values()[value];
        } else {
            return null;
        }
    }
}