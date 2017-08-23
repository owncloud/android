/* ownCloud Android Library is available under MIT license
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.common.authentication.oauth;


import java.util.HashMap;
import java.util.Map;

public class OAuth2ProvidersRegistry {

    private Map<String, OAuth2Provider> mProviders = new HashMap<>();

    private OAuth2Provider mDefaultProvider = null;

    private OAuth2ProvidersRegistry () {
    }

    /**
     * See https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
     */
    private static class LazyHolder {
        private static final OAuth2ProvidersRegistry INSTANCE = new OAuth2ProvidersRegistry();
    }

    /**
     * Singleton accesor.
     *
     * @return     Singleton isntance of {@link OAuth2ProvidersRegistry}
     */
    public static OAuth2ProvidersRegistry getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Register an {@link OAuth2Provider} with the name passed as parameter.
     *
     * @param name              Name to bind 'oAuthProvider' in the registry.
     * @param oAuth2Provider    An {@link OAuth2Provider} instance to keep in the registry.
     * @throws IllegalArgumentException if 'name' or 'oAuthProvider' are null.
     */
    public void registerProvider(String name, OAuth2Provider oAuth2Provider) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be NULL");
        }
        if (oAuth2Provider == null) {
            throw new IllegalArgumentException("oAuth2Provider must not be NULL");
        }

        mProviders.put(name, oAuth2Provider);
        if (mProviders.size() == 1) {
            mDefaultProvider = oAuth2Provider;
        }
    }

    public OAuth2Provider unregisterProvider(String name) {
        OAuth2Provider unregisteredProvider = mProviders.remove(name);
        if (mProviders.size() == 0) {
            mDefaultProvider = null;
        } else if (unregisteredProvider != null && unregisteredProvider == mDefaultProvider) {
            mDefaultProvider = mProviders.values().iterator().next();
        }
        return unregisteredProvider;
    }

    /**
     * Get default {@link OAuth2Provider}.
     *
     * @return      Default provider, or NULL if there is no provider.
     */
    public OAuth2Provider getProvider() {
        return mDefaultProvider;
    }

    /**
     * Get {@link OAuth2Provider} registered with the name passed as parameter.
     *
     * @param name  Name used to register the desired {@link OAuth2Provider}
     * @return      {@link OAuth2Provider} registered with the name 'name'
     */
    public OAuth2Provider getProvider(String name) {
        return mProviders.get(name);
    }

    /**
     * Sets the {@link OAuth2Provider} registered with the name passed as parameter as the default provider
     *
     * @param name  Name used to register the {@link OAuth2Provider} to set as default.
     * @return      {@link OAuth2Provider} set as default, or NULL if no provider was registered with 'name'.
     */
    public OAuth2Provider setDefaultProvider(String name) {
        OAuth2Provider toDefault = mProviders.get(name);
        if (toDefault != null) {
            mDefaultProvider = toDefault;
        }
        return toDefault;
    }

}
