package com.owncloud.android.lib.common.http.methods.webdav;

import at.bitfire.dav4jvm.Property;
import at.bitfire.dav4jvm.PropertyUtils;

public class DavUtils {

    public static final Property.Name[] getAllPropset() {
        return PropertyUtils.INSTANCE.getAllPropSet();
    }

    public static final Property.Name[] getQuotaPropSet() {
        return PropertyUtils.INSTANCE.getQuotaPropset();
    }
}