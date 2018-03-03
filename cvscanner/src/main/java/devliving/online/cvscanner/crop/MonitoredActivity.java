/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package devliving.online.cvscanner.crop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

public abstract class MonitoredActivity extends AppCompatActivity {

    private static final String LOG_TAG = MonitoredActivity.class.getSimpleName();

    private final ArrayList<LifeCycleListener> mListeners = new ArrayList<>();

    public interface LifeCycleListener {
        void onActivityCreated(MonitoredActivity activity);

        void onActivityDestroyed(MonitoredActivity activity);

        void onActivityPaused(MonitoredActivity activity);

        void onActivityResumed(MonitoredActivity activity);

        void onActivityStarted(MonitoredActivity activity);

        void onActivityStopped(MonitoredActivity activity);
    }

    public static class LifeCycleAdapter implements LifeCycleListener {
        public void onActivityCreated(MonitoredActivity activity) {
        }

        public void onActivityDestroyed(MonitoredActivity activity) {
        }

        public void onActivityPaused(MonitoredActivity activity) {
        }

        public void onActivityResumed(MonitoredActivity activity) {
        }

        public void onActivityStarted(MonitoredActivity activity) {
        }

        public void onActivityStopped(MonitoredActivity activity) {
        }
    }

    public synchronized void addLifeCycleListener(LifeCycleListener listener) {
        if (mListeners.contains(listener))
            return;
        mListeners.add(listener);
    }

    public synchronized void removeLifeCycleListener(LifeCycleListener listener) {
        mListeners.remove(listener);
    }

    @Override
    protected synchronized void onPause() {
        super.onPause();
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityPaused(this);
        }
    }

    private ArrayList<LifeCycleListener> copyListeners() {
        final ArrayList<LifeCycleListener> lifeCycleListeners = new ArrayList<>(mListeners.size());
        lifeCycleListeners.addAll(mListeners);
        return lifeCycleListeners;
    }

    @Override
    protected synchronized void onResume() {
        super.onResume();
        for (LifeCycleListener listener : mListeners) {
            listener.onActivityResumed(this);
        }
    }

    @Override
    protected synchronized void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();

        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityCreated(this);
        }
    }

    @Override
    protected synchronized void onDestroy() {
        super.onDestroy();

        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityDestroyed(this);
        }
    }

    @Override
    protected synchronized void onStart() {
        super.onStart();
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityStarted(this);
        }
    }

    @Override
    protected synchronized  void onStop() {
        super.onStop();
        final ArrayList<LifeCycleListener> lifeCycleListeners = copyListeners();
        for (LifeCycleListener listener : lifeCycleListeners) {
            listener.onActivityStopped(this);
        }
        Log.i(LOG_TAG, "onStop: " + this.getClass());
    }
}
