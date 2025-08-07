/**
 * ownCloud Android client application
 *
 * @author Brtosz Przybylski
 * @author Christian Schabesberger
 * @author David Crespo Ríos
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2020 Bartosz Przybylski
 * Copyright (C) 2025 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.WhatsNewActivityBinding;
import com.owncloud.android.databinding.WhatsNewElementBinding;
import com.owncloud.android.presentation.authentication.homecloud.LoginActivity;
import com.owncloud.android.wizard.FeatureList;
import com.owncloud.android.wizard.FeatureList.FeatureItem;
import com.owncloud.android.wizard.ProgressIndicator;

/**
 * @author Bartosz Przybylski
 */
public class WhatsNewActivity extends FragmentActivity {

    private ImageButton mForwardFinishButton;
    private ProgressIndicator mProgress;
    private ViewPager2 mPager;

    private WhatsNewActivityBinding bindingActivity;

    static public void runIfNeeded(Context context) {
        if (context instanceof WhatsNewActivity) {
            return;
        }

        if (shouldShow(context)) {
            context.startActivity(new Intent(context, WhatsNewActivity.class));
        }
    }

    static private boolean shouldShow(Context context) {
        Boolean wizardEnabled = context.getResources().getBoolean(R.bool.wizard_enabled);
        Boolean isLoginActivity = context instanceof LoginActivity; // When it is LoginActivity to start it only once
        return wizardEnabled && isLoginActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        bindingActivity = WhatsNewActivityBinding.inflate(getLayoutInflater());

        setContentView(bindingActivity.getRoot());

        mProgress = bindingActivity.progressIndicator;
        mPager = bindingActivity.contentPanel;

        FeaturesViewAdapter adapter = new FeaturesViewAdapter(this,
                FeatureList.get());

        mProgress.setNumberOfSteps(adapter.getItemCount());
        mPager.setAdapter(adapter);
        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mProgress.animateToStep(position + 1);
                updateNextButtonIfNeeded();
            }
        });

        mForwardFinishButton = bindingActivity.forward;
        mForwardFinishButton.setOnClickListener(view -> {
            if (mProgress.hasNextStep()) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
                mProgress.animateToStep(mPager.getCurrentItem() + 1);
            }
            updateNextButtonIfNeeded();
        });
        bindingActivity.done.setOnClickListener( view -> {
            finish();
        });
        Button skipButton = bindingActivity.skip;

        skipButton.setOnClickListener(view -> {
            if (mProgress.hasNextStep()) {
                finish();
            }
        });

        updateNextButtonIfNeeded();

        // Wizard already shown
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(MainApp.PREFERENCE_KEY_LAST_SEEN_VERSION_CODE, MainApp.Companion.getVersionCode());
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void updateNextButtonIfNeeded() {
        if (!mProgress.hasNextStep()) {
            bindingActivity.skip.setVisibility(View.INVISIBLE);
            bindingActivity.forward.setVisibility(View.GONE);
            bindingActivity.done.setVisibility(View.VISIBLE);
        } else {
            bindingActivity.skip.setVisibility(View.VISIBLE);
            bindingActivity.forward.setVisibility(View.VISIBLE);
            bindingActivity.done.setVisibility(View.GONE);
        }
    }

    public static class FeatureFragment extends Fragment {
        private FeatureItem mItem;

        private WhatsNewElementBinding bindingElement;

        static public FeatureFragment newInstance(FeatureItem item) {
            FeatureFragment f = new FeatureFragment();
            Bundle args = new Bundle();
            args.putParcelable("feature", item);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mItem = getArguments() != null ? (FeatureItem) getArguments().getParcelable("feature") : null;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            bindingElement = WhatsNewElementBinding.inflate(getLayoutInflater());

            ImageView iv = bindingElement.whatsNewImage;
            if (mItem.shouldShowImage()) {
                iv.setImageResource(mItem.getImage());
            }

            TextView tv2 = bindingElement.whatsNewTitle;
            if (mItem.shouldShowTitleText()) {
                tv2.setText(mItem.getTitleText());
            }

            tv2 = bindingElement.whatsNewText;
            if (mItem.shouldShowContentText()) {
                tv2.setText(mItem.getContentText());
            }

            return bindingElement.getRoot();
        }
    }

    private final class FeaturesViewAdapter extends FragmentStateAdapter {

        FeatureItem[] mFeatures;

        public FeaturesViewAdapter(FragmentActivity activity, FeatureItem[] features) {
            super(activity);
            mFeatures = features;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return FeatureFragment.newInstance(mFeatures[position]);
        }

        @Override
        public int getItemCount() {
            return mFeatures.length;
        }
    }
}
