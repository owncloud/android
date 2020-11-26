/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.utils.AvatarUtils;
import com.owncloud.android.utils.DisplayUtils;
import timber.log.Timber;

import java.util.List;

/**
 * This Adapter populates a ListView with all accounts within the app.
 */
public class AccountListAdapter extends ArrayAdapter<AccountListItem> {
    private float mAccountAvatarRadiusDimension;
    private final BaseActivity mContext;
    private List<AccountListItem> mValues;
    private AccountListAdapterListener mListener;
    private Drawable mTintedCheck;

    public AccountListAdapter(BaseActivity context, List<AccountListItem> values, Drawable tintedCheck) {
        super(context, -1, values);
        this.mContext = context;
        this.mValues = values;
        this.mListener = (AccountListAdapterListener) context;
        this.mAccountAvatarRadiusDimension = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
        this.mTintedCheck = tintedCheck;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        AccountViewHolderItem viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            convertView = inflater.inflate(R.layout.account_item, parent, false);

            viewHolder = new AccountViewHolderItem();
            viewHolder.imageViewItem = convertView.findViewById(R.id.icon);
            viewHolder.checkViewItem = convertView.findViewById(R.id.ticker);
            viewHolder.checkViewItem.setImageDrawable(mTintedCheck);
            viewHolder.nameViewItem = convertView.findViewById(R.id.name);
            viewHolder.accountViewItem = convertView.findViewById(R.id.account);
            viewHolder.refreshAccountButtonItem = convertView.findViewById(R.id.refreshAccountButton);
            viewHolder.passwordButtonItem = convertView.findViewById(R.id.passwordButton);
            viewHolder.removeButtonItem = convertView.findViewById(R.id.removeButton);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AccountViewHolderItem) convertView.getTag();
        }

        AccountListItem accountListItem = mValues.get(position);

        if (accountListItem != null) {
            // create account item
            if (AccountListItem.TYPE_ACCOUNT == accountListItem.getType()) {
                Account account = accountListItem.getAccount();
                try {
                    OwnCloudAccount oca = new OwnCloudAccount(account, mContext);
                    viewHolder.nameViewItem.setText(oca.getDisplayName());
                } catch (Exception e) {
                    Timber.w("Account not found right after being read :\\ ; using account name instead of display " +
                            "name");
                    viewHolder.nameViewItem.setText(
                            AccountUtils.getUsernameOfAccount(account.name)
                    );
                }
                viewHolder.nameViewItem.setTag(account.name);

                viewHolder.accountViewItem.setText(
                        DisplayUtils.convertIdn(account.name, false)
                );

                try {
                    AvatarUtils avatarUtils = new AvatarUtils();
                    avatarUtils.loadAvatarForAccount(
                            viewHolder.imageViewItem,
                            account,
                            true,
                            mAccountAvatarRadiusDimension
                    );
                } catch (Exception e) {
                    Timber.e(e, "Error calculating RGB value for account list item.");
                    // use user icon as a fallback
                    viewHolder.imageViewItem.setImageResource(R.drawable.ic_user);
                }

                if (AccountUtils.getCurrentOwnCloudAccount(getContext()).name.equals(account.name)) {
                    viewHolder.checkViewItem.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.checkViewItem.setVisibility(View.INVISIBLE);
                }

                /// bind listener to refresh account
                viewHolder.refreshAccountButtonItem.setOnClickListener(v ->
                        mListener.refreshAccount(mValues.get(position).getAccount()));

                /// bind listener to change password
                viewHolder.passwordButtonItem.setOnClickListener(v ->
                        mListener.changePasswordOfAccount(mValues.get(position).getAccount()));

                /// bind listener to remove account
                viewHolder.removeButtonItem.setOnClickListener(v ->
                        mListener.removeAccount(mValues.get(position).getAccount()));

            } // create add account action item
            else if (AccountListItem.TYPE_ACTION_ADD == accountListItem.getType()) {
                LayoutInflater inflater = mContext.getLayoutInflater();
                View actionView = inflater.inflate(R.layout.account_action, parent, false);
                ((TextView) actionView.findViewById(R.id.name)).setText(R.string.prefs_add_account);
                ((ImageView) actionView.findViewById(R.id.icon)).setImageResource(R.drawable.ic_account_plus);

                // bind action listener
                actionView.setOnClickListener(v -> mListener.createAccount());

                return actionView;
            }
        }

        return convertView;
    }

    /**
     * Listener interface for Activities using the {@link AccountListAdapter}
     */
    public interface AccountListAdapterListener {
        void removeAccount(Account account);

        void changePasswordOfAccount(Account account);

        void refreshAccount(Account account);

        void createAccount();
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    static class AccountViewHolderItem {
        ImageView imageViewItem;
        ImageView checkViewItem;

        TextView nameViewItem;
        TextView accountViewItem;

        ImageView refreshAccountButtonItem;
        ImageView passwordButtonItem;
        ImageView removeButtonItem;
    }
}
