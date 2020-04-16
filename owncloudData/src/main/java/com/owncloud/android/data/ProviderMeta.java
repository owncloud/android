/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.data;

import android.provider.BaseColumns;

/**
 * Meta-Class that holds various static field information
 */
public class ProviderMeta {

    public static final String DB_NAME = "filelist";
    public static final String NEW_DB_NAME = "owncloud_database";
    public static final int DB_VERSION = 29;

    private ProviderMeta() {
    }

    static public class ProviderTableMeta implements BaseColumns {
        public static final String OCSHARES_TABLE_NAME = "ocshares";
        public static final String CAPABILITIES_TABLE_NAME = "capabilities";

        // Columns of ocshares table
        public static final String OCSHARES_FILE_SOURCE = "file_source";
        public static final String OCSHARES_ITEM_SOURCE = "item_source";
        public static final String OCSHARES_SHARE_TYPE = "share_type";
        public static final String OCSHARES_SHARE_WITH = "shate_with";
        public static final String OCSHARES_PATH = "path";
        public static final String OCSHARES_PERMISSIONS = "permissions";
        public static final String OCSHARES_SHARED_DATE = "shared_date";
        public static final String OCSHARES_EXPIRATION_DATE = "expiration_date";
        public static final String OCSHARES_TOKEN = "token";
        public static final String OCSHARES_SHARE_WITH_DISPLAY_NAME = "shared_with_display_name";
        public static final String OCSHARES_SHARE_WITH_ADDITIONAL_INFO = "share_with_additional_info";
        public static final String OCSHARES_IS_DIRECTORY = "is_directory";
        public static final String OCSHARES_USER_ID = "user_id";
        public static final String OCSHARES_ID_REMOTE_SHARED = "id_remote_shared";
        public static final String OCSHARES_ACCOUNT_OWNER = "owner_share";
        public static final String OCSHARES_NAME = "name";
        public static final String OCSHARES_URL = "url";

        // Columns of capabilities table
        public static final String CAPABILITIES_ACCOUNT_NAME = "account";
        public static final String CAPABILITIES_VERSION_MAYOR = "version_mayor";
        public static final String CAPABILITIES_VERSION_MINOR = "version_minor";
        public static final String CAPABILITIES_VERSION_MICRO = "version_micro";
        public static final String CAPABILITIES_VERSION_STRING = "version_string";
        public static final String CAPABILITIES_VERSION_EDITION = "version_edition";
        public static final String CAPABILITIES_CORE_POLLINTERVAL = "core_pollinterval";
        public static final String CAPABILITIES_SHARING_API_ENABLED = "sharing_api_enabled";
        public static final String CAPABILITIES_SHARING_SEARCH_MIN_LENGTH = "search_min_length";
        public static final String CAPABILITIES_SHARING_PUBLIC_ENABLED = "sharing_public_enabled";
        public static final String CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED = "sharing_public_password_enforced";
        public static final String CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY =
                "sharing_public_password_enforced_read_only";
        public static final String CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE =
                "sharing_public_password_enforced_read_write";
        public static final String CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY =
                "sharing_public_password_enforced_public_only";
        public static final String CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED =
                "sharing_public_expire_date_enabled";
        public static final String CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS =
                "sharing_public_expire_date_days";
        public static final String CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED =
                "sharing_public_expire_date_enforced";
        public static final String CAPABILITIES_SHARING_PUBLIC_SEND_MAIL = "sharing_public_send_mail";
        public static final String CAPABILITIES_SHARING_PUBLIC_UPLOAD = "sharing_public_upload";
        public static final String CAPABILITIES_SHARING_PUBLIC_MULTIPLE = "sharing_public_multiple";
        public static final String CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY = "supports_upload_only";
        public static final String CAPABILITIES_SHARING_USER_SEND_MAIL = "sharing_user_send_mail";
        public static final String CAPABILITIES_SHARING_RESHARING = "sharing_resharing";
        public static final String CAPABILITIES_SHARING_FEDERATION_OUTGOING = "sharing_federation_outgoing";
        public static final String CAPABILITIES_SHARING_FEDERATION_INCOMING = "sharing_federation_incoming";
        public static final String CAPABILITIES_FILES_BIGFILECHUNKING = "files_bigfilechunking";
        public static final String CAPABILITIES_FILES_UNDELETE = "files_undelete";
        public static final String CAPABILITIES_FILES_VERSIONING = "files_versioning";
    }
}
