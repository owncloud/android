/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import com.owncloud.android.authentication.AccountUtils;
import timber.log.Timber;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import static com.owncloud.android.domain.files.model.MimeTypeConstantsKt.MIME_PREFIX_IMAGE;

/**
 * Utility class with methods for decoding Bitmaps.
 */
public class BitmapUtils {

    /**
     * Decodes a bitmap from a file containing it minimizing the memory use, known that the bitmap
     * will be drawn in a surface of reqWidth x reqHeight
     *
     * @param srcPath       Absolute path to the file containing the image.
     * @param reqWidth      Width of the surface where the Bitmap will be drawn on, in pixels.
     * @param reqHeight     Height of the surface where the Bitmap will be drawn on, in pixels.
     * @return
     */
    public static Bitmap decodeSampledBitmapFromFile(String srcPath, int reqWidth, int reqHeight) {

        // set desired options that will affect the size of the bitmap
        final Options options = new Options();
        options.inScaled = true;
        options.inPurgeable = true;
        options.inPreferQualityOverSpeed = false;
        options.inMutable = false;

        // make a false load of the bitmap to get its dimensions
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(srcPath, options);

        // calculate factor to subsample the bitmap
        options.inSampleSize = calculateSampleFactor(options, reqWidth, reqHeight);

        // decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(srcPath, options);
    }

    /**
     * Calculates a proper value for options.inSampleSize in order to decode a Bitmap minimizing 
     * the memory overload and covering a target surface of reqWidth x reqHeight if the original
     * image is big enough. 
     *
     * @param options       Bitmap decoding options; options.outHeight and options.inHeight should
     *                      be set. 
     * @param reqWidth      Width of the surface where the Bitmap will be drawn on, in pixels.
     * @param reqHeight     Height of the surface where the Bitmap will be drawn on, in pixels.
     * @return The largest inSampleSize value that is a power of 2 and keeps both
     *                      height and width larger than reqWidth and reqHeight.
     */
    private static int calculateSampleFactor(Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // calculates the largest inSampleSize value (for smallest sample) that is a power of 2 and keeps both
            // height and width **larger** than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Rotate bitmap according to EXIF orientation. 
     * Cf. http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/ 
     * @param bitmap Bitmap to be rotated
     * @param storagePath Path to source file of bitmap. Needed for EXIF information.
     * @return correctly EXIF-rotated bitmap
     */
    public static Bitmap rotateImage(final Bitmap bitmap, final String storagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(storagePath);
            final int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            Matrix matrix = new Matrix();
            // 1: nothing to do

            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1.0f, 1.0f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1.0f, -1.0f);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.postRotate(-90);
                    matrix.postScale(1.0f, -1.0f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.postRotate(90);
                    matrix.postScale(1.0f, -1.0f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }

            // Rotate the bitmap
            final Bitmap resultBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (resultBitmap != bitmap) {
                bitmap.recycle();
            }
            return resultBitmap;
        } catch (Exception exception) {
            Timber.w("Could not rotate the image: %s", storagePath);
            return bitmap;
        }
    }

    private static float fixRawHSLValue(final float value, final float upperBound, final float scale) {
        return (value > upperBound) ? upperBound
                : (value < 0f) ? 0f
                : value * scale;
    }

    /**
     *  Convert HSL values to a RGB Color.
     *
     *  @param h Hue is specified as degrees in the range 0 - 360.
     *  @param s Saturation is specified as a percentage in the range 1 - 100.
     *  @param l Lumanance is specified as a percentage in the range 1 - 100.
     *  @param alpha  the alpha value between 0 - 1
     *  adapted from https://svn.codehaus.org/griffon/builders/gfxbuilder/tags/GFXBUILDER_0.2/
     *  gfxbuilder-core/src/main/com/camick/awt/HSLColor.java
     */
    public static int[] HSLtoRGB(final float h, final float s, final float l, final float alpha) {
        if (s < 0.0f || s > 100.0f) {
            Timber.w("Color parameter outside of expected range - Saturation");
        }

        if (l < 0.0f || l > 100.0f) {
            Timber.w("Color parameter outside of expected range - Luminance");
        }

        if (alpha < 0.0f || alpha > 1.0f) {
            Timber.w("Color parameter outside of expected range - Alpha");
        }

        //  Formula needs all values between 0 - 1.

        final float hr = (h % 360.0f) / 360f;
        final float sr = fixRawHSLValue(s, 100f, 1 / 100f);
        final float lr = fixRawHSLValue(s, 100f, 1 / 100f);

        final float q = (lr < 0.5)
                ? lr * (1 + sr)
                : (lr + sr) - (lr * sr);
        final float p = 2 * lr - q;
        final int r = Math.round(Math.max(0, HueToRGB(p, q, hr + (1.0f / 3.0f)) * 256));
        final int g = Math.round(Math.max(0, HueToRGB(p, q, hr) * 256));
        final int b = Math.round(Math.max(0, HueToRGB(p, q, hr - (1.0f / 3.0f)) * 256));

        return new int[]{r, g, b};
    }

    private static float HueToRGB(final float p, final float q, final float h) {
        final float hr = (h < 0) ? h + 1
                : (h > 1) ? h - 1
                : h;

        if (6 * hr < 1) {
            return p + ((q - p) * 6 * h);
        }
        if (2 * hr < 1) {
            return q;
        }
        if (3 * hr < 2) {
            return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
        }
        return p;
    }

    /**
     * Checks if file passed is an image
     * @param file
     * @return true/false
     */
    public static boolean isImage(File file) {
        final Uri selectedUri = Uri.fromFile(file);
        final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString().toLowerCase());
        final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        return (mimeType != null && mimeType.startsWith(MIME_PREFIX_IMAGE));
    }

    /**
     * calculates the RGB value based on a given account name.
     *
     * @param accountName The account name
     * @return corresponding RGB color
     * @throws UnsupportedEncodingException if the charset is not supported
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     */
    public static int[] calculateAvatarBackgroundRGB(String accountName)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // using adapted algorithm from /core/js/placeholder.js:50
        final String username = AccountUtils.getUsernameOfAccount(accountName);
        final byte[] seed = username.getBytes("UTF-8");
        final MessageDigest md = MessageDigest.getInstance("MD5");
        // Integer seedMd5Int = Math.abs(new String(Hex.encodeHex(seedMd5)).hashCode());
        final Integer seedMd5Int = String.format(Locale.ROOT, "%032x",
                new BigInteger(1, md.digest(seed))).hashCode();

        final double maxRange = Integer.MAX_VALUE;
        final float hue = (float) (seedMd5Int / maxRange * 360);

        return BitmapUtils.HSLtoRGB(hue, 90.0f, 65.0f, 1.0f);
    }

    /**
     * Returns a new circular bitmap drawable by creating it from a bitmap, setting initial target density based on
     * the display metrics of the resources.
     *
     * @param resources the resources for initial target density
     * @param bitmap the original bitmap
     * @return the circular bitmap
     */
    public static RoundedBitmapDrawable bitmapToCircularBitmapDrawable(Resources resources, Bitmap bitmap) {
        RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(resources, bitmap);
        roundedBitmap.setCircular(true);
        return roundedBitmap;
    }
}
