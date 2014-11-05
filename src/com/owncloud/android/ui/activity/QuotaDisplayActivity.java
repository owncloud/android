package com.owncloud.android.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.QuotaDisplayFragment;
import com.owncloud.android.utils.DisplayUtils;

public class QuotaDisplayActivity extends HookActivity {

    private final String TAG = QuotaDisplayActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setHomeButtonEnabled(true);       // mandatory since Android ICS, according to the official documentation
        setSupportProgressBarIndeterminateVisibility(true);

        Fragment f = new QuotaDisplayFragment();
        setContentView(R.layout.activity_quota_display);
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, f)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportActionBar().setIcon(DisplayUtils.getSeasonalIconId());
        getSupportActionBar().setTitle(getResources().getString(R.string.actionbar_quota_statistics));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(R.menu.menu_quota_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retVal = true;
        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent settingsIntent = new Intent(this, Preferences.class);
                startActivity(settingsIntent);
                break;
            }
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            default:
                retVal = super.onOptionsItemSelected(item);
        }
        return retVal;
    }

}
