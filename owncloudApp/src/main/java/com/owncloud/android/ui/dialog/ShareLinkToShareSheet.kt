package com.owncloud.android.ui.dialog

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LabeledIntent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Parcelable
import com.owncloud.android.R
import com.owncloud.android.ui.activity.CopyToClipboardActivity
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet

class ShareLinkToShareSheet() {

    private var intentToShareLink: Intent? = null

    private var mFileActivity: Activity? = null
    private var mComponentNameFilter: ComponentNameFilter? = null

    companion object {
        fun getInstance(
            intentToShareLink: Intent,
            mFileActivity: Activity,
            mComponentNameFilter: ComponentNameFilter
        ): ShareLinkToShareSheet {

            val shareLinkToShareSheet = ShareLinkToShareSheet()
            shareLinkToShareSheet.mFileActivity = mFileActivity
            shareLinkToShareSheet.mComponentNameFilter = mComponentNameFilter
            shareLinkToShareSheet.intentToShareLink = intentToShareLink

            return shareLinkToShareSheet
        }
    }

    private fun getIntentChooser(): Intent {

        val resolveInfoList =
            mFileActivity?.baseContext?.packageManager?.queryIntentActivities(intentToShareLink, PackageManager.MATCH_DEFAULT_ONLY)

        val chooserIntent: Intent
        val targetIntents: MutableList<Intent>

        val titleId: Int
        val sendAction: Boolean = intentToShareLink?.getBooleanExtra(Intent.ACTION_SEND, false)!!
        titleId = if (sendAction) {
            R.string.activity_chooser_send_file_title
        } else {
            R.string.activity_chooser_title
        }
        val chooserTitle = mFileActivity?.resources?.getString(titleId)


        resolveInfoList?.let {
            val excludedComponentNames = getExcludedComponentNames(resolveInfoList, mComponentNameFilter!!)

            // add activity for copy to clipboard
            if (!sendAction && !getCopyToClipboardResolveInfoList().isNullOrEmpty()) {
                resolveInfoList.add(getCopyToClipboardResolveInfoList()?.get(0))
            }

            Collections.sort(resolveInfoList, ResolveInfo.DisplayNameComparator(mFileActivity?.baseContext?.packageManager))
            targetIntents = getTargetIntents(intentToShareLink!!, resolveInfoList, excludedComponentNames)

            // deal with M list separate problem
            chooserIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // create chooser with empty intent in M could fix the empty cells problem
                Intent.createChooser(Intent(), chooserTitle)
            } else {
                // create chooser with one target intent below M
                Intent.createChooser(targetIntents.removeAt(0), chooserTitle)
            }

            // add initial intents
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toTypedArray<Parcelable>())
            return chooserIntent
        }

        return Intent.createChooser(Intent(), chooserTitle)

    }

    private fun getExcludedComponentNames(
        resolveInfoList: MutableList<ResolveInfo>,
        componentNameFilter: ComponentNameFilter
    ): HashSet<ComponentName> {
        val excludedComponentNames: HashSet<ComponentName> = HashSet<ComponentName>()
        for (i in resolveInfoList.indices) {
            val activityInfo = resolveInfoList[i].activityInfo
            val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
            if (componentNameFilter.shouldBeFilteredOut(componentName)) {
                excludedComponentNames.add(componentName)
            }
        }
        return excludedComponentNames
    }

    /**
     * @param intent to assosiate target with
     * @param resolveInfo list of All Target Intents
     * @param excludedComponentNames HasSet of components to exclude
     * return an List<intent> for each intent we explicitly  associate it  with desired App target
     * */
    private fun getTargetIntents(
        intent: Intent,
        resolveInfo: List<ResolveInfo>,
        excludedComponentNames: HashSet<ComponentName>
    ): MutableList<Intent> {
        val targetIntents: MutableList<Intent> = ArrayList()
        for (i in resolveInfo.indices) {
            val activityInfo = resolveInfo[i].activityInfo
            if (excludedComponentNames.contains(ComponentName(activityInfo.packageName, activityInfo.name))) {
                continue
            }
            //---> adds every intent not excluded
            val targetIntent = Intent(intent)
            targetIntent.setPackage(activityInfo.packageName)
            targetIntent.component = ComponentName(activityInfo.packageName, activityInfo.name)
            // wrap with LabeledIntent to show correct name and icon
            val labeledIntent = LabeledIntent(targetIntent, activityInfo.packageName, resolveInfo[i].labelRes, resolveInfo[i].icon)
            // add filtered intent to a list
            targetIntents.add(labeledIntent)
        }
        return targetIntents
    }

    fun show() {
        mFileActivity?.startActivity(getIntentChooser())
    }
    private fun getCopyToClipboardResolveInfoList(): MutableList<ResolveInfo>? {
        val copyToClipboardIntent = Intent(mFileActivity, CopyToClipboardActivity::class.java)
        val copyToClipboard: MutableList<ResolveInfo>? =
            mFileActivity?.baseContext?.packageManager?.queryIntentActivities(copyToClipboardIntent, 0)
        return copyToClipboard
    }

    interface ComponentNameFilter {
        fun shouldBeFilteredOut(componentName: ComponentName?): Boolean
    }
}

