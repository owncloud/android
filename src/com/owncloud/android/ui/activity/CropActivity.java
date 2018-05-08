package com.owncloud.android.ui.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.chrisbanes.photoview.PhotoView;
import com.owncloud.android.R;
import com.owncloud.android.callbacks.OpenCVCallback;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.QuadrilateralSelectionImageView;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Point;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CropActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = CropActivity.class.getName();

    private static final int MAX_HEIGHT = 500;
    private QuadrilateralSelectionImageView mSelectionImageView;
    private Button mCropButton;
    private Button mCancelButton;
    MaterialDialog mResultDialog;
    OpenCVCallback mOpenCVLoaderCallback;
    private Bitmap mBitmap = null;
    private Bitmap mResult = null;
    private byte[] scannedDocumentInByteArray = null;
    private File scannedDocumentFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        mOpenCVLoaderCallback = new OpenCVCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        break;
                    }

                    default: {
                        super.onManagerConnected(status);
                    }
                }
            }
        };
        initOpenCV();
        mSelectionImageView = (QuadrilateralSelectionImageView) findViewById(R.id.polygon_view);
        mCropButton = (Button) findViewById(R.id.crop_button);
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        Intent callingIntent = getIntent();
        Bundle scannedDocumentData = callingIntent.getExtras();
        final String scannedDocumentPath = (String) scannedDocumentData.get(FileDisplayActivity.SCANNED_DOCUMENT_IMAGE);
        if(scannedDocumentPath != null) {
            scannedDocumentFile = new File(scannedDocumentPath);
            mSelectionImageView.setImageURI(Uri.fromFile(scannedDocumentFile));
            scannedDocumentInByteArray = getByteArrayFromImage(Uri.fromFile(scannedDocumentFile));
        }
        Bitmap scannedDocument = BitmapFactory.decodeByteArray(scannedDocumentInByteArray,0,scannedDocumentInByteArray.length);
        mBitmap = scannedDocument;
        mSelectionImageView.setImageBitmap(getResizedBitmap(scannedDocument,MAX_HEIGHT));
        List<PointF> points = findPoints();
        if(points != null){
            mSelectionImageView.setPoints(points);
        } else{
            Log_OC.d(TAG,getResources().getString(R.string.no_contour_found));
        }
        mCropButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mResultDialog = new MaterialDialog.Builder(this)
                .title("Scan Result")
                .positiveText("Save")
                .negativeText("Cancel")
                .customView(R.layout.dialog_document_scan_result, false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        // TODO Saving
                        Intent croppedDocumentIntent = new Intent();
                        FileOutputStream outputScannedDocument = null;
                        try {
                            outputScannedDocument = new FileOutputStream(scannedDocumentFile);
                        } catch(FileNotFoundException exception){
                            Log_OC.d(TAG,exception.toString());
                        }
                        mResult.compress(Bitmap.CompressFormat.JPEG,100,outputScannedDocument);
                        croppedDocumentIntent.putExtra(FileDisplayActivity.CROPPED_SCANNED_DOCUMENT_IMAGE_PATH,scannedDocumentFile.getAbsolutePath());
                        setResult(RESULT_OK,croppedDocumentIntent);
                        finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                    }
                }).build();
    }

    private byte[] getByteArrayFromImage(Uri scannedDocumentUri){
        byte[] imageInByte = null;
        try{
            ContentResolver contentResolver = getBaseContext().getContentResolver();
            InputStream imageInputStream = contentResolver.openInputStream(scannedDocumentUri);
            Bitmap scannedDocument = BitmapFactory.decodeStream(imageInputStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            scannedDocument.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
            imageInByte = byteArrayOutputStream.toByteArray();
        } catch(FileNotFoundException exception){
            Log_OC.d(TAG,exception.toString());
        }
        return imageInByte;
    }

    @Override
    public void onResume() {
        super.onResume();
        initOpenCV();
    }

    /**
     * Attempt to load OpenCV via statically compiled libraries.  If they are not found, then load
     * using OpenCV Manager.
     */
    private void initOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log_OC.d(TAG,"Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mOpenCVLoaderCallback);
        } else {
            Log_OC.d(TAG,"OpenCV library found inside package. Using it!");
            mOpenCVLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * Resize a given bitmap to scale using the given height
     *
     * @return The resized bitmap
     */
    private Bitmap getResizedBitmap(Bitmap bitmap, int maxHeight) {
        double ratio = bitmap.getHeight() / (double) maxHeight;
        int width = (int) (bitmap.getWidth() / ratio);
        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false);
    }

    /**
     * Attempt to find the four corner points for the largest contour in the image.
     *
     * @return A list of points, or null if a valid rectangle cannot be found.
     */
    private List<PointF> findPoints() {
        List<PointF> result = null;

        Mat image = new Mat();
        Mat orig = new Mat();
        org.opencv.android.Utils.bitmapToMat(getResizedBitmap(mBitmap, MAX_HEIGHT), image);
        org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

        Mat edges = edgeDetection(image);
        MatOfPoint2f largest = findLargestContour(edges);

        if (largest != null) {
            Point[] points = sortPoints(largest.toArray());
            result = new ArrayList<>();
            result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));
            largest.release();
        } else {
            Log_OC.d(TAG,"Can't find rectangle!");
        }

        edges.release();
        image.release();
        orig.release();

        return result;
    }

    /**
     * Detect the edges in the given Mat
     * @param src A valid Mat object
     * @return A Mat processed to find edges
     */
    private Mat edgeDetection(Mat src) {
        Mat edges = new Mat();
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 75, 200);
        return edges;
    }

    /**
     * Find the largest 4 point contour in the given Mat.
     *
     * @param src A valid Mat
     * @return The largest contour as a Mat
     */
    private MatOfPoint2f findLargestContour(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Get the 5 largest contours
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double area1 = Imgproc.contourArea(o1);
                double area2 = Imgproc.contourArea(o2);
                return (int) (area2 - area1);
            }
        });
        if (contours.size() > 5) contours.subList(4, contours.size() - 1).clear();

        MatOfPoint2f largest = null;
        for (MatOfPoint contour : contours) {
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f c = new MatOfPoint2f();
            contour.convertTo(c, CvType.CV_32FC2);
            Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);

            if (approx.total() >= 4 && Imgproc.contourArea(contour) > 150) {
                // the contour has 4 points, it's valid
                largest = approx;
                break;
            }
        }

        return largest;
    }

    /**
     * Transform the coordinates on the given Mat to correct the perspective.
     *
     * @param src A valid Mat
     * @param points A list of coordinates from the given Mat to adjust the perspective
     * @return A perspective transformed Mat
     */
    private Mat perspectiveTransform(Mat src, List<PointF> points) {
        Point point1 = new Point(points.get(0).x, points.get(0).y);
        Point point2 = new Point(points.get(1).x, points.get(1).y);
        Point point3 = new Point(points.get(2).x, points.get(2).y);
        Point point4 = new Point(points.get(3).x, points.get(3).y);
        Point[] pts = {point1, point2, point3, point4};
        return fourPointTransform(src, sortPoints(pts));
    }

    /**
     * Apply a threshold to give the "scanned" look
     *
     * NOTE:
     * See the following link for more info http://docs.opencv.org/3.1.0/d7/d4d/tutorial_py_thresholding.html#gsc.tab=0
     * @param src A valid Mat
     * @return The processed Bitmap
     */
    private Bitmap applyThreshold(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

        // Some other approaches
//        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
//        Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        Imgproc.GaussianBlur(src, src, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        Bitmap bm = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(src, bm);

        return bm;
    }

    /**
     * Sort the points
     *
     * The order of the points after sorting:
     * 0------->1
     * ^        |
     * |        v
     * 3<-------2
     *
     * NOTE:
     * Based off of http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
     *
     * @param src The points to sort
     * @return An array of sorted points
     */
    private Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = {null, null, null, null};

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };
        Comparator<Point> differenceComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        result[0] = Collections.min(srcPoints, sumComparator);        // Upper left has the minimal sum
        result[2] = Collections.max(srcPoints, sumComparator);        // Lower right has the maximal sum
        result[1] = Collections.min(srcPoints, differenceComparator); // Upper right has the minimal difference
        result[3] = Collections.max(srcPoints, differenceComparator); // Lower left has the maximal difference

        return result;
    }

    /**
     * NOTE:
     * Based off of http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
     *
     * @param src
     * @param pts
     * @return
     */
    private Mat fourPointTransform(Mat src, Point[] pts) {
        double ratio = src.size().height / (double) MAX_HEIGHT;

        Point ul = pts[0];
        Point ur = pts[1];
        Point lr = pts[2];
        Point ll = pts[3];

        double widthA = Math.sqrt(Math.pow(lr.x - ll.x, 2) + Math.pow(lr.y - ll.y, 2));
        double widthB = Math.sqrt(Math.pow(ur.x - ul.x, 2) + Math.pow(ur.y - ul.y, 2));
        double maxWidth = Math.max(widthA, widthB) * ratio;

        double heightA = Math.sqrt(Math.pow(ur.x - lr.x, 2) + Math.pow(ur.y - lr.y, 2));
        double heightB = Math.sqrt(Math.pow(ul.x - ll.x, 2) + Math.pow(ul.y - ll.y, 2));
        double maxHeight = Math.max(heightA, heightB) * ratio;

        Mat resultMat = new Mat(Double.valueOf(maxHeight).intValue(), Double.valueOf(maxWidth).intValue(), CvType.CV_8UC4);

        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);
        srcMat.put(0, 0, ul.x * ratio, ul.y * ratio, ur.x * ratio, ur.y * ratio, lr.x * ratio, lr.y * ratio, ll.x * ratio, ll.y * ratio);
        dstMat.put(0, 0, 0.0, 0.0, maxWidth, 0.0, maxWidth, maxHeight, 0.0, maxHeight);

        Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Imgproc.warpPerspective(src, resultMat, M, resultMat.size());

        srcMat.release();
        dstMat.release();
        M.release();

        return resultMat;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.crop_button){
            List<PointF> points = mSelectionImageView.getPoints();
            if (mBitmap != null) {
                Mat orig = new Mat();
                org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

                Mat transformed = perspectiveTransform(orig, points);
                mResult = applyThreshold(transformed);

                if (mResultDialog.getCustomView() != null) {
                    PhotoView photoView = (PhotoView) mResultDialog.getCustomView().findViewById(R.id.cropped_document_image_view);
                    photoView.setImageBitmap(mResult);
                    mResultDialog.show();
                }

                orig.release();
                transformed.release();
            }
        }
        else if(v.getId() == R.id.cancel_button){
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
