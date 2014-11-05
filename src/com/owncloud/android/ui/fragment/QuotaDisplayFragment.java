package com.owncloud.android.ui.fragment;


import android.accounts.Account;
import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation;
import com.owncloud.android.ui.activity.QuotaDisplayActivity;
import com.owncloud.android.utils.DisplayUtils;

import java.util.ArrayList;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;

public class QuotaDisplayFragment extends SherlockFragment implements OnRemoteOperationListener {

    private static final String TAG = "QuotaDisplayFragment";

    private QuotaDisplayActivity containerActivity = null;

    private ProgressBar mProgressBar = null;
    private TextView mFree = null;
    private TextView mUsed = null;
    private TextView mTotal = null;
    private TextView mPercentage = null;

    private Double valPercentage = null;
    private Long valFree = null;
    private Long valTotal = null;
    private Long valUsed = null;

    final static String EXTRA_QUOTA_PERCENTAGE = "quotaPercentage";
    final static String EXTRA_QUOTA_FREE = "quotaFree";
    final static String EXTRA_QUOTA_USED = "quotaUsed";
    final static String EXTRA_QUOTA_TOTAL = "quotaTotal";


    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };


    public QuotaDisplayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            valPercentage = savedInstanceState.getDouble(EXTRA_QUOTA_PERCENTAGE);
            valFree = savedInstanceState.getLong(EXTRA_QUOTA_FREE);
            valTotal = savedInstanceState.getLong(EXTRA_QUOTA_TOTAL);
            valUsed = savedInstanceState.getLong(EXTRA_QUOTA_USED);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        containerActivity = (QuotaDisplayActivity) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (valTotal == null || valUsed == null || valFree == null || valPercentage == null) {
            Log.d(TAG, "fetching quota data from remote server");
            Account ac = containerActivity.getAccount();
            GetRemoteUserQuotaOperation userQuotaOperation = new GetRemoteUserQuotaOperation();
            userQuotaOperation.execute(ac, getActivity(), this, handler);
        } else {
            setValuesToFields();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(EXTRA_QUOTA_PERCENTAGE, valPercentage);
        outState.putLong(EXTRA_QUOTA_FREE, valFree);
        outState.putLong(EXTRA_QUOTA_USED, valUsed);
        outState.putLong(EXTRA_QUOTA_TOTAL, valTotal);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.quota_display_layout, container, false);
        mProgressBar = (ProgressBar) v.findViewById(R.id.quota_usage_progress_bar);
        mFree = (TextView) v.findViewById(R.id.quota_free_text_view);
        mUsed = (TextView) v.findViewById(R.id.quota_used_text_view);
        mTotal = (TextView) v.findViewById(R.id.quota_total_text_view);
        mPercentage = (TextView) v.findViewById(R.id.quota_percentage_text_view);
        return v;
    }

    /**
     * Method that applies the values to their respective text fields
     */
    protected void setValuesToFields() {
        if (mProgressBar != null && mPercentage != null && mFree != null && mTotal != null && mUsed != null) {
            int percentage = (Double.valueOf(Math.ceil(valPercentage))).intValue();
            setColorAndUsage(percentage, mProgressBar);
            mFree.setText(DisplayUtils.bytesToHumanReadable(valFree));
            mUsed.setText(DisplayUtils.bytesToHumanReadable(valUsed));
            mTotal.setText(DisplayUtils.bytesToHumanReadable(valTotal));
            mPercentage.setText(valPercentage + "%");
        } else {
            Log_OC.wtf(TAG, "Setting values when one or more widgets are NULL");
        }
    }

    /**
     * Set the progressBar color and percentage according to the percentage int
     * @param percentage The int representing the percentage of space used
     * @param progressBar The instance of the progress bar used
     */
    protected void setColorAndUsage(int percentage, ProgressBar progressBar) {
        GradientDrawable d = ((GradientDrawable) ((RotateDrawable) ((LayerDrawable) progressBar.getProgressDrawable()).getDrawable(1)).getDrawable());
        if (percentage > 50 && percentage < 70) {
            d.setColor(getResources().getColor(R.color.progressbar_green));
        } else if (percentage >= 70 && percentage < 85) {
            d.setColor(getResources().getColor(R.color.progressbar_orange));
        } else if (percentage >= 85 && percentage <= 100) {
            d.setColor(getResources().getColor(R.color.progressbar_red));
        }
        progressBar.setProgress(percentage);
        //progressBar.refreshDrawableState();
    }

    /**
     * Listener invoked when the remote operation is over. Error handling must be
     * done internally
     * @param caller the remote operation that return the result
     * @param result object containing the result itself
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation caller, RemoteOperationResult result) {
        //For now only GetRemoteUserQuotaOperation should be allowed here
        if (!(caller instanceof GetRemoteUserQuotaOperation))
            return;

        if (result.isSuccess()) {
            ArrayList<Object> data = result.getData();
            //Get views and set their fields
            valFree = (Long) data.get(0);
            valUsed = (Long) data.get(1);
            valTotal = (Long) data.get((2));
            valPercentage = ((Double) data.get(3));
            setValuesToFields();
        } else {
            //Some error handling. Still don't know what the possible errors might be
            String error = "";
            ResultCode code = result.getCode();
            if (result.isException())
                error = result.getException().getLocalizedMessage();
            else if (code == ResultCode.CANCELLED)
                error = getString(R.string.common_cancel);
            else if (code == ResultCode.FORBIDDEN)
                error = getString(R.string.forbidden_permissions);
            else if (code == ResultCode.NO_NETWORK_CONNECTION)
                error = getString(R.string.network_error_socket_exception);
            else
                error = getString(R.string.common_error_unknown);

            Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
        }
    }

}
