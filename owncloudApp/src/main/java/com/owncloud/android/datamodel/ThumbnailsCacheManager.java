/**
 * ownCloud Android client application
 *
 * @author Tobias Kaminsky
 * @author David A. Velasco
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.DefaultAvatarTextDrawable;
import com.owncloud.android.ui.adapter.DiskLruImageCache;
import com.owncloud.android.utils.BitmapUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * Manager for concurrent access to thumbnails cache.
 */
public class ThumbnailsCacheManager {

    private static final String TAG = ThumbnailsCacheManager.class.getSimpleName();

    private static final String CACHE_FOLDER = "thumbnailCache";

    private static final Object mThumbnailsDiskCacheLock = new Object();
    private static DiskLruImageCache mThumbnailCache = null;
    private static boolean mThumbnailCacheStarting = true;

    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final CompressFormat mCompressFormat = CompressFormat.JPEG;
    private static final int mCompressQuality = 70;
    private static OwnCloudClient mClient = null;

    public static Bitmap mDefaultImg =
            BitmapFactory.decodeResource(
                    MainApp.Companion.getAppContext().getResources(),
                    R.drawable.file_image
            );

    public static class InitDiskCacheTask extends AsyncTask<File, Void, Void> {

        @Override
        protected Void doInBackground(File... params) {
            synchronized (mThumbnailsDiskCacheLock) {
                mThumbnailCacheStarting = true;

                if (mThumbnailCache == null) {
                    try {
                        // Check if media is mounted or storage is built-in, if so, 
                        // try and use external cache dir; otherwise use internal cache dir
                        final String cachePath =
                                MainApp.Companion.getAppContext().getExternalCacheDir().getPath() +
                                        File.separator + CACHE_FOLDER;
                        Log_OC.d(TAG, "create dir: " + cachePath);
                        final File diskCacheDir = new File(cachePath);
                        mThumbnailCache = new DiskLruImageCache(
                                diskCacheDir,
                                DISK_CACHE_SIZE,
                                mCompressFormat,
                                mCompressQuality
                        );
                    } catch (Exception e) {
                        Log_OC.d(TAG, "Thumbnail cache could not be opened ", e);
                        mThumbnailCache = null;
                    }
                }
                mThumbnailCacheStarting = false; // Finished initialization
                mThumbnailsDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }

    public static void addBitmapToCache(String key, Bitmap bitmap) {
        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache != null) {
                mThumbnailCache.put(key, bitmap);
            }
        }
    }

    public static void removeBitmapFromCache(String key) {
        synchronized (mThumbnailsDiskCacheLock) {
            if (mThumbnailCache != null) {
                mThumbnailCache.removeKey(key);
            }
        }
    }

    public static Bitmap getBitmapFromDiskCache(String key) {
        synchronized (mThumbnailsDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mThumbnailCacheStarting) {
                try {
                    mThumbnailsDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    Log_OC.e(TAG, "Wait in mThumbnailsDiskCacheLock was interrupted", e);
                }
            }
            if (mThumbnailCache != null) {
                return mThumbnailCache.getBitmap(key);
            }
        }
        return null;
    }

    public static class ThumbnailGenerationTask extends AsyncTask<Object, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private static Account mAccount;
        private Object mFile;
        private FileDataStorageManager mStorageManager;

        public ThumbnailGenerationTask(ImageView imageView, FileDataStorageManager storageManager,
                                       Account account) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<>(imageView);
            if (storageManager == null) {
                throw new IllegalArgumentException("storageManager must not be NULL");
            }
            mStorageManager = storageManager;
            mAccount = account;
        }

        public ThumbnailGenerationTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap thumbnail = null;

            try {
                if (mAccount != null) {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(
                            mAccount,
                            MainApp.Companion.getAppContext()
                    );
                    mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.Companion.getAppContext());
                }

                mFile = params[0];

                if (mFile instanceof OCFile) {
                    thumbnail = doOCFileInBackground();
                } else if (mFile instanceof File) {
                    thumbnail = doFileInBackground();
                    //} else {  do nothing
                }

            } catch (Throwable t) {
                // the app should never break due to a problem with thumbnails
                Log_OC.e(TAG, "Generation of thumbnail for " + mFile + " failed", t);
                if (t instanceof OutOfMemoryError) {
                    System.gc();
                }
            }

            return thumbnail;
        }

        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask) {
                    String tagId = "";
                    if (mFile instanceof OCFile) {
                        tagId = String.valueOf(((OCFile) mFile).getFileId());
                    } else if (mFile instanceof File) {
                        tagId = String.valueOf(mFile.hashCode());
                    }
                    if (String.valueOf(imageView.getTag()).equals(tagId)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }

        /**
         * Add thumbnail to cache
         * @param imageKey: thumb key
         * @param bitmap:   image for extracting thumbnail
         * @param path:     image path
         * @param px:       thumbnail dp
         * @return Bitmap
         */
        private Bitmap addThumbnailToCache(String imageKey, Bitmap bitmap, String path, int px) {

            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);

            // Rotate image, obeying exif tag
            thumbnail = BitmapUtils.rotateImage(thumbnail, path);

            // Add thumbnail to cache
            addBitmapToCache(imageKey, thumbnail);

            return thumbnail;
        }

        /**
         * Converts size of file icon from dp to pixel
         * @return int
         */
        private int getThumbnailDimension() {
            // Converts dp to pixel
            Resources r = MainApp.Companion.getAppContext().getResources();
            return Math.round(r.getDimension(R.dimen.file_icon_size_grid));
        }

        private Bitmap doOCFileInBackground() {
            OCFile file = (OCFile) mFile;

            final String imageKey = String.valueOf(file.getRemoteId());

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null || file.needsUpdateThumbnail()) {

                int px = getThumbnailDimension();

                if (file.isDown()) {
                    Bitmap temp = BitmapUtils.decodeSampledBitmapFromFile(
                            file.getStoragePath(), px, px);
                    Bitmap bitmap = ThumbnailUtils.extractThumbnail(temp, px, px);

                    if (bitmap != null) {
                        // Handle PNG
                        if (file.getMimetype().equalsIgnoreCase("image/png")) {
                            bitmap = handlePNG(bitmap, px);
                        }

                        thumbnail = addThumbnailToCache(imageKey, bitmap, file.getStoragePath(), px);

                        file.setNeedsUpdateThumbnail(false);
                        mStorageManager.saveFile(file);
                    }

                } else {
                    // Download thumbnail from server
                    OwnCloudVersion serverOCVersion = AccountUtils.getServerVersion(mAccount);
                    if (mClient != null && serverOCVersion != null) {
                        if (serverOCVersion.supportsRemoteThumbnails()) {
                            GetMethod get = null;
                            try {
                                String uri = mClient.getBaseUri() + "" +
                                        "/index.php/apps/files/api/v1/thumbnail/" +
                                        px + "/" + px + Uri.encode(file.getRemotePath(), "/");
                                Log_OC.d("Thumbnail", "URI: " + uri);
                                get = new GetMethod(new URL(uri));
                                int status = mClient.executeHttpMethod(get);
                                if (status == HttpConstants.HTTP_OK) {
                                    InputStream inputStream = get.getResponseBodyAsStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                    thumbnail = ThumbnailUtils.extractThumbnail(bitmap, px, px);

                                    // Handle PNG
                                    if (file.getMimetype().equalsIgnoreCase("image/png")) {
                                        thumbnail = handlePNG(thumbnail, px);
                                    }

                                    // Add thumbnail to cache
                                    if (thumbnail != null) {
                                        addBitmapToCache(imageKey, thumbnail);
                                    }
                                } else {
                                    mClient.exhaustResponse(get.getResponseBodyAsStream());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log_OC.d(TAG, "Server too old");
                        }
                    }
                }
            }

            return thumbnail;

        }

        private Bitmap handlePNG(Bitmap bitmap, int px) {
            Bitmap resultBitmap = Bitmap.createBitmap(px,
                    px,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(resultBitmap);

            c.drawColor(ContextCompat.getColor(MainApp.Companion.getAppContext(), R.color.background_color));
            c.drawBitmap(bitmap, 0, 0, null);

            return resultBitmap;
        }

        private Bitmap doFileInBackground() {
            File file = (File) mFile;

            final String imageKey = String.valueOf(file.hashCode());

            // Check disk cache in background thread
            Bitmap thumbnail = getBitmapFromDiskCache(imageKey);

            // Not found in disk cache
            if (thumbnail == null) {

                int px = getThumbnailDimension();

                Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(
                        file.getAbsolutePath(), px, px);

                if (bitmap != null) {
                    thumbnail = addThumbnailToCache(imageKey, bitmap, file.getPath(), px);
                }
            }
            return thumbnail;
        }

    }

    /**
     * Show the avatar corresponding to the received account in an {@link ImageView} ir {@link MenuItem}.
     *
     * The avatar is loaded if available in the cache and bound to the received UI element. The avatar is not
     * fetched from the server if not available, unless the parameter 'fetchFromServer' is set to 'true'.
     *
     * If there is no avatar stored and cannot be fetched, a colored icon is generated with the first
     * letter of the account username.
     *
     * If this is not possible either, a predefined user icon is bound instead.
     */
    public static class GetAvatarTask extends AsyncTask<Object, Void, Drawable> {
        private final WeakReference<ImageView> mImageViewReference;
        private final WeakReference<MenuItem> mMenuItemReference;
        private Account mAccount;
        private float mDisplayRadius;
        private boolean mFetchFromServer;

        private String mUsername;
        private OwnCloudClient mClient;

        /**
         * Builds an instance to show the avatar corresponding to the received account in an {@link ImageView}.
         *
         * @param imageView         The {@link ImageView} to bind the avatar to.
         * @param account           OC account which avatar will be shown.
         * @param displayRadius     The radius of the circle where the avatar will be clipped into.
         * @param fetchFromServer   When 'true', if there is no avatar stored in the cache, it's fetched from
         *                          the server. When 'false', server is not accessed, the fallback avatar is
         *                          generated instead. USE WITH CARE, probably to be removed in the future.
         */
        public GetAvatarTask(ImageView imageView, Account account, float displayRadius, boolean fetchFromServer) {
            if (account == null) {
                throw new IllegalArgumentException("Received NULL account");
            }
            mMenuItemReference = null;
            mImageViewReference = new WeakReference<>(imageView);
            mAccount = account;
            mDisplayRadius = displayRadius;
            mFetchFromServer = fetchFromServer;
        }

        /**
         * Builds an instance to show the avatar corresponding to the received account in an {@link MenuItem}.
         *
         * @param menuItem         The {@ImageView} to bind the avatar to.
         * @param account           OC account which avatar will be shown.
         * @param displayRadius     The radius of the circle where the avatar will be clipped into.
         * @param fetchFromServer   When 'true', if there is no avatar stored in the cache, it's fetched from
         *                          the server. When 'false', server is not accessed, the fallback avatar is
         *                          generated instead. USE WITH CARE, probably to be removed in the future.
         */
        public GetAvatarTask(MenuItem menuItem, Account account, float displayRadius, boolean fetchFromServer) {
            if (account == null) {
                throw new IllegalArgumentException("Received NULL account");
            }
            mImageViewReference = null;
            mMenuItemReference = new WeakReference<>(menuItem);
            mAccount = account;
            mDisplayRadius = displayRadius;
            mFetchFromServer = fetchFromServer;
        }

        @Override
        protected Drawable doInBackground(Object... params) {
            Drawable thumbnail = null;

            try {
                OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount,
                        MainApp.Companion.getAppContext());
                mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, MainApp.Companion.getAppContext());

                mUsername = mAccount.name;
                thumbnail = doAvatarInBackground();

            } catch (Throwable t) {
                // the app should never break due to a problem with avatars
                Log_OC.e(TAG, "Generation of avatar for " + mUsername + " failed", t);
                if (t instanceof OutOfMemoryError) {
                    System.gc();
                }
            }

            return thumbnail;
        }

        @Override
        protected void onPostExecute(Drawable avatar) {
            if (mImageViewReference != null) {
                ImageView imageView = mImageViewReference.get();
                if (imageView != null) {
                    if (avatar != null) {
                        imageView.setImageDrawable(avatar);
                    } else {
                        // really needed?
                        imageView.setImageResource(
                                R.drawable.ic_account_circle
                        );
                    }
                }
            } else if (mMenuItemReference != null) {
                MenuItem menuItem = mMenuItemReference.get();
                if (menuItem != null) {
                    if (avatar != null) {
                        menuItem.setIcon(avatar);
                    } else {
                        // really needed
                        menuItem.setIcon(
                                R.drawable.ic_account_circle
                        );
                    }
                }
            }
        }

        /**
         * Converts size of file icon from dp to pixel
         * @return int
         */
        private int getAvatarDimension() {
            // Converts dp to pixel
            Resources r = MainApp.Companion.getAppContext().getResources();
            return Math.round(r.getDimension(R.dimen.file_avatar_size));
        }

        private Drawable doAvatarInBackground() {

            Drawable avatarDrawable = null;

            final String imageKey = "a_" + mUsername;

            // Check disk cache in background thread
            Bitmap avatarBitmap = getBitmapFromDiskCache(imageKey);

            if (avatarBitmap != null) {
                avatarDrawable = BitmapUtils.bitmapToCircularBitmapDrawable(
                        MainApp.Companion.getAppContext().getResources(),
                        avatarBitmap
                );

            } else {
                // Not found in disk cache
                if (mFetchFromServer) {
                    int px = getAvatarDimension();

                    // Download avatar from server
                    OwnCloudVersion serverOCVersion = AccountUtils.getServerVersion(mAccount);
                    if (mClient != null && serverOCVersion != null) {
                        if (serverOCVersion.supportsRemoteThumbnails()) {
                            GetMethod get = null;
                            try {
                                String uri = mClient.getBaseUri() + "" +
                                        "/index.php/avatar/" + AccountUtils.getUsernameOfAccount(mUsername) + "/" + px;
                                Log_OC.d("Avatar", "URI: " + uri);
                                get = new GetMethod(new URL(uri));
                                int status = mClient.executeHttpMethod(get);
                                if (status == HttpConstants.HTTP_OK) {
                                    InputStream inputStream = get.getResponseBodyAsStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                    avatarBitmap = ThumbnailUtils.extractThumbnail(bitmap, px, px);

                                    // Add avatar to cache
                                    if (avatarBitmap != null) {
                                        addBitmapToCache(imageKey, avatarBitmap);
                                    }
                                } else {
                                    mClient.exhaustResponse(get.getResponseBodyAsStream());
                                }
                            } catch (Exception e) {
                                Log_OC.e(TAG, "Error downloading avatar", e);
                            }
                        } else {
                            Log_OC.d(TAG, "Server too old");
                        }
                    }
                }
                if (avatarBitmap != null) {
                    avatarDrawable = BitmapUtils.bitmapToCircularBitmapDrawable(
                            MainApp.Companion.getAppContext().getResources(),
                            avatarBitmap
                    );

                } else {
                    // generate placeholder from user name
                    try {
                        avatarDrawable = DefaultAvatarTextDrawable.createAvatar(mUsername, mDisplayRadius);

                    } catch (Exception e) {
                        // nothing to do, return null to apply default icon
                        Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e);
                    }
                }
            }
            return avatarDrawable;
        }

    }

    public static String addAvatarToCache(String accountName, byte[] avatarData, int dimension) {
        final String imageKey = "a_" + accountName;

        Bitmap bitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
        // Add avatar to cache
        if (bitmap != null) {
            addBitmapToCache(imageKey, bitmap);
        }
        return imageKey;
    }

    public static void removeAvatarFromCache(String accountName) {
        final String imageKey = "a_" + accountName;
        removeBitmapFromCache(imageKey);
    }

    public static boolean cancelPotentialThumbnailWork(Object file, ImageView imageView) {
        final ThumbnailGenerationTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.mFile;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || bitmapData != file) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
                Log_OC.v(TAG, "Cancelled generation of thumbnail for a reused imageView");
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    public static boolean cancelPotentialAvatarWork(Object file, ImageView imageView) {
        return cancelPotentialAvatarWork(file, getAvatarWorkerTask(imageView));
    }

    public static boolean cancelPotentialAvatarWork(Object file, MenuItem menuItem) {
        return cancelPotentialAvatarWork(file, getAvatarWorkerTask(menuItem));
    }

    public static boolean cancelPotentialAvatarWork(Object file, final GetAvatarTask avatarWorkerTask) {

        if (avatarWorkerTask != null) {
            final Object usernameData = avatarWorkerTask.mUsername;
            // If usernameData is not yet set or it differs from the new data
            if (usernameData == null || usernameData != file) {
                // Cancel previous task
                avatarWorkerTask.cancel(true);
                Log_OC.v(TAG, "Cancelled generation of avatar for a reused imageView");
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static ThumbnailGenerationTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncThumbnailDrawable) {
                final AsyncThumbnailDrawable asyncDrawable = (AsyncThumbnailDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private static GetAvatarTask getAvatarWorkerTask(ImageView imageView) {
        if (imageView != null) {
            return getAvatarWorkerTask(imageView.getDrawable());
        }
        return null;
    }

    private static GetAvatarTask getAvatarWorkerTask(MenuItem menuItem) {
        if (menuItem != null) {
            return getAvatarWorkerTask(menuItem.getIcon());
        }
        return null;
    }

    private static GetAvatarTask getAvatarWorkerTask(Drawable drawable) {
        if (drawable instanceof AsyncAvatarDrawable) {
            final AsyncAvatarDrawable asyncDrawable = (AsyncAvatarDrawable) drawable;
            return asyncDrawable.getAvatarWorkerTask();
        }
        return null;
    }

    public static class AsyncThumbnailDrawable extends BitmapDrawable {
        private final WeakReference<ThumbnailGenerationTask> bitmapWorkerTaskReference;

        public AsyncThumbnailDrawable(
                Resources res, Bitmap bitmap, ThumbnailGenerationTask bitmapWorkerTask
        ) {

            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<ThumbnailGenerationTask>(bitmapWorkerTask);
        }

        public ThumbnailGenerationTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static class AsyncAvatarDrawable extends BitmapDrawable {
        private final WeakReference<GetAvatarTask> avatarWorkerTaskReference;

        public AsyncAvatarDrawable(
                Resources res, Bitmap bitmap, GetAvatarTask avatarWorkerTask
        ) {

            super(res, bitmap);
            avatarWorkerTaskReference =
                    new WeakReference<GetAvatarTask>(avatarWorkerTask);
        }

        public GetAvatarTask getAvatarWorkerTask() {
            return avatarWorkerTaskReference.get();
        }
    }
}
