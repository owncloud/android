/**
 * ownCloud Android client application
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.lib.common.network;

import android.content.Context;

import okhttp3.internal.tls.OkHostnameVerifier;
import timber.log.Timber;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
public class KnownServersHostnameVerifier implements HostnameVerifier {

    private final Context mContext;
    private final HostnameVerifier mDelegate;

    public KnownServersHostnameVerifier(Context context) {
        this(context, OkHostnameVerifier.INSTANCE);
    }

    KnownServersHostnameVerifier(Context context, HostnameVerifier delegate) {
        if (context == null) {
            throw new IllegalArgumentException("Context may not be NULL!");
        }
        mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        mDelegate = delegate;
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        if (mDelegate.verify(hostname, session)) {
            return true;
        }
        try {
            Certificate[] peerCerts = session.getPeerCertificates();
            if (peerCerts.length > 0 && peerCerts[0] instanceof X509Certificate) {
                return NetworkUtils.isCertInKnownServersStore(peerCerts[0], mContext);
            }
        } catch (SSLPeerUnverifiedException e) {
            Timber.d(e, "No peer certificates during hostname verification for %s", hostname);
        }
        return false;
    }
}
