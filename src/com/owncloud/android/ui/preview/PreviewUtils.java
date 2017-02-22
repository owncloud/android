/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
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

package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudBasicCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudSamlSsoCredentials;
import com.owncloud.android.lib.common.accounts.AccountUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * Created by davidgonzalez on 22/2/17.
 *
 * An utils provider for building data sources used in video preview
 */

public class PreviewUtils {

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    protected static DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter,
                                                               Context context, OCFile file,
                                                               Account account) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null, context, file, account);
    }

    protected static DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter,
                                                               Context context, OCFile file, Account account) {
        return new DefaultDataSourceFactory(context, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter, file, account));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param bandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    protected static HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter,
                                                                       OCFile file, Account account) {

        if (file.isDown()) {

            return new DefaultHttpDataSourceFactory(MainApp.getUserAgent(), bandwidthMeter);

        } else {

            try {

                //Get account credentials asynchronously
                final GetCredentialsTask task = new GetCredentialsTask();
                task.execute(account);

                OwnCloudCredentials credentials = task.get();

                String login = credentials.getUsername();
                String password = credentials.getAuthToken();

                Map<String, String> params = new HashMap<String, String>(1);

                if (credentials instanceof OwnCloudBasicCredentials) {
                    // Basic auth
                    String cred = login + ":" + password;
                    String auth = "Basic " + Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE);
                    params.put("Authorization", auth);
                } else if (credentials instanceof OwnCloudSamlSsoCredentials) {
                    // SAML SSO
                    params.put("Cookie", password);
                }

                return new CustomHttpDataSourceFactory(MainApp.getUserAgent(), bandwidthMeter, params);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Task for getting account credentials asynchronously
     */
    private static class GetCredentialsTask extends AsyncTask<Object, Void, OwnCloudCredentials> {
        @Override
        protected OwnCloudCredentials doInBackground(Object... params) {
            Object account = params[0];
            try {
                OwnCloudCredentials ocCredentials = AccountUtils.getCredentialsForAccount(MainApp.getAppContext(), (Account) account);
                return ocCredentials;
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
