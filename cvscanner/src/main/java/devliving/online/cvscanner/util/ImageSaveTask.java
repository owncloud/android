package devliving.online.cvscanner.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.io.IOException;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 8/30/17.
 */

public class ImageSaveTask extends AsyncTask<Void, Void, String> {
    Bitmap image;
    int rotation;
    Point[] points;
    Context mContext;
    SaveCallback mCallback;

    public ImageSaveTask(Context context, Bitmap image, int rotation, Point[] points, SaveCallback callback) {
        this.image = image;
        this.rotation = rotation;
        this.points = points;
        this.mContext = context;
        this.mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        mCallback.onSaveTaskStarted();
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p/>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected String doInBackground(Void... params) {
        Size imageSize = new Size(image.getWidth(), image.getHeight());
        Mat imageMat = new Mat(imageSize, CvType.CV_8UC4);
        Utils.bitmapToMat(image, imageMat);

        image.recycle();

        Mat croppedImage = CVProcessor.fourPointTransform(imageMat, points);
        imageMat.release();

        Mat enhancedImage = CVProcessor.adjustBirghtnessAndContrast(croppedImage, 1);
        croppedImage.release();

        enhancedImage = CVProcessor.sharpenImage(enhancedImage);

        String imagePath = null;
        try {
            imagePath = Util.saveImage(mContext,
                    "IMG_CVScanner_" + System.currentTimeMillis(), enhancedImage, false);
            enhancedImage.release();
            Util.setExifRotation(mContext, Util.getUriFromPath(imagePath), rotation);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imagePath;
    }

    @Override
    protected void onPostExecute(String path) {
        if(path!= null) mCallback.onSaved(path);
        else mCallback.onSaveFailed(new Exception("could not save image"));
    }

    public interface SaveCallback{
        void onSaveTaskStarted();
        void onSaved(String savedPath);
        void onSaveFailed(Exception error);
    }
}