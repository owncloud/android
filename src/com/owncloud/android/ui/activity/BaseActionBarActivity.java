package com.owncloud.android.ui.activity;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.owncloud.android.R;

/**
 * Abstract base activity implementation to detect the toolbar within an activity and setting up the
 * action bar.
 */
public abstract class BaseActionBarActivity extends AppCompatActivity {
    protected Toolbar toolbar;

    protected void setupActionBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }
}
