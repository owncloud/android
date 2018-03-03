package devliving.online.cvscanner.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import devliving.online.cvscanner.CVScanner;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 8/20/17.
 */

public final class Util {
    private static final int SIZE_DEFAULT = 2048;
    private static final int SIZE_LIMIT = 4096;

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // Do nothing
        }
    }

    public static Uri createTempFile(Context context, String fileName, String fileExtension,  boolean useExternalStorage) throws IOException {
        File storageDir;

        if(useExternalStorage){
            storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
        else{
            storageDir = new File(context.getCacheDir(), "/CVScanner/");

            if(!storageDir.exists()) storageDir.mkdirs();
        }

        File image = File.createTempFile(fileName,
                fileExtension, storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        Uri currentPhotoUri = getUriForFile(context, image);
        Log.d("MAIN", "photo-uri: " + currentPhotoUri);

        return currentPhotoUri;
    }

    /**
     * Shareable FileProvider uri. The image must be in either 'context.getCacheDir() + "/CVScanner/"'
     * or 'context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)'
     * @param context
     * @param file
     * @return
     */
    public static Uri getUriForFile(Context context, File file){
        return CVFileProvider.getUriForFile(context,
                CVScanner.getFileproviderName(context),
                file);
    }

    public static Uri getUriFromPath(String path){
        File file = new File(path);
        return Uri.fromFile(file);
    }

    /**
     *
     * @param context
     * @param imageName without extension
     * @param img
     * @param useExternalStorage
     * @return
     * @throws IOException
     */
    public static String saveImage(Context context, String imageName, @NonNull Mat img, boolean useExternalStorage) throws IOException {
        String imagePath = null;

        File dir = null;
        if(useExternalStorage){
            dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
        else {
            dir = new File(context.getCacheDir(), "/CVScanner/");
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File imageFile = File.createTempFile(imageName, ".jpg", dir);

        Bitmap bitmap = Bitmap.createBitmap((int) img.size().width, (int) img.size().height, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bitmap);

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            fout.flush();

            imagePath = imageFile.getAbsolutePath();
        }
        finally {
            closeSilently(fout);
        }

        return imagePath;
    }

    public static int calculateBitmapSampleSize(Context context, Uri bitmapUri) throws IOException {

        BitmapFactory.Options options = decodeImageForSize(context, bitmapUri);

        int maxSize = getMaxImageSize();
        int sampleSize = 1;
        while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize << 1;
        }

        return sampleSize;
    }

    private static BitmapFactory.Options decodeImageForSize(Context context, @NonNull Uri imageUri) throws FileNotFoundException {
        InputStream is = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(is, null, options); // Just get image size
        } finally {
            Util.closeSilently(is);
        }

        return options;
    }

    public static Bitmap loadBitmapFromUri(Context context, int sampleSize, Uri uri) throws FileNotFoundException {
        InputStream is = null;
        Bitmap out = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;

        try {
            is = context.getContentResolver().openInputStream(uri);
            out = BitmapFactory.decodeStream(is, null, options);
        } finally {
            Util.closeSilently(is);
        }

        return out;
    }

    public static int calculateInSampleSize(Context context, Uri imageUri, int reqWidth,
                                            int reqHeight, boolean keepAspectRatio) throws FileNotFoundException {
        BitmapFactory.Options options = decodeImageForSize(context, imageUri);
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        final float aspectRatio = (float)height/width;
        int inSampleSize = 1;

        if (reqWidth > 0 && (keepAspectRatio || reqHeight > 0)) {
            if(keepAspectRatio)
            {
                reqHeight = Math.round(reqWidth * aspectRatio);
            }

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    /**
     * Tries to preserve aspect ratio
     * @param context
     * @param imageUri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap loadBitmapFromUri(Context context, @NonNull Uri imageUri, int reqWidth, int reqHeight) {
        InputStream imageStream = null;
        Bitmap image = null;
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(context, imageUri, reqWidth, reqHeight, true);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            imageStream = context.getContentResolver().openInputStream(imageUri);
            image = BitmapFactory.decodeStream(imageStream, null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            closeSilently(imageStream);
        }

        return image;
    }

    public static int getExifRotation(Context context, Uri imageUri) throws IOException {
        if (imageUri == null) return 0;
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            // We only recognize a subset of orientation tag values
            switch (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        }finally {
            closeSilently(inputStream);
        }
    }

    public static boolean setExifRotation(Context context, Uri imageUri, int rotation) throws IOException {
        if (imageUri == null) return false;

        InputStream destStream = null;
        try{
            destStream = context.getContentResolver().openInputStream(imageUri);

            ExifInterface exif = new ExifInterface(destStream);

            exif.setAttribute("UserComment", "Generated using CVScanner");

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            switch (rotation){
                case 1:
                    orientation = ExifInterface.ORIENTATION_ROTATE_90;
                    break;

                case 2:
                    orientation = ExifInterface.ORIENTATION_ROTATE_180;
                    break;

                case 3:
                    orientation = ExifInterface.ORIENTATION_ROTATE_270;
                    break;
            }
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
            exif.saveAttributes();
        }finally {
            closeSilently(destStream);
        }
        return true;
    }

    private static int getMaxImageSize() {
        int textureLimit = getMaxTextureSize();
        if (textureLimit == 0) {
            return SIZE_DEFAULT;
        } else {
            return Math.min(textureLimit, SIZE_LIMIT);
        }
    }

    private static int getMaxTextureSize() {
        // The OpenGL texture size is the maximum size that can be drawn in an ImageView
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        return maxSize[0];
    }
}
