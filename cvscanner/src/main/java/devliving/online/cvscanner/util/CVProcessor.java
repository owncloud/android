package devliving.online.cvscanner.util;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfFloat4;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Mehedi on 9/20/16.
 */
public class CVProcessor {
    final static String TAG = "CV-PROCESSOR";
    final static int FIXED_HEIGHT = 800;
    private final static double COLOR_GAIN = 1.5;       // contrast
    private final static double COLOR_BIAS = 0;         // bright
    private final static int COLOR_THRESH = 110;        // threshold

    public final static float PASSPORT_ASPECT_RATIO = 3.465f/4.921f;

    public static Mat buildMatFromYUV(byte[] nv21Data, int width, int height){
        Mat yuv = new Mat(height + (height/2), width, CvType.CV_8UC1);
        yuv.put(0, 0, nv21Data);

        Mat rgba = new Mat();
        Imgproc.cvtColor(yuv, rgba, Imgproc.COLOR_YUV2RGBA_NV21, CvType.CV_8UC4);

        return rgba;
    }

    public static Rect detectBorder(Mat original){
        Mat src = original.clone();
        Log.d(TAG, "1 original: " + src.toString());

        Imgproc.GaussianBlur(src, src, new Size(3, 3), 0);
        Log.d(TAG, "2.1 --> Gaussian blur done\n blur: " + src.toString());

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
        Log.d(TAG, "2.2 --> Grayscaling done\n gray: " + src.toString());

        Mat sobelX = new Mat();
        Mat sobelY = new Mat();

        Imgproc.Sobel(src, sobelX, CvType.CV_32FC1, 2, 0, 5, 1, 0);
        Log.d(TAG, "3.1 --> Sobel done.\n X: " + sobelX.toString());
        Imgproc.Sobel(src, sobelY, CvType.CV_32FC1, 0, 2, 5, 1, 0);
        Log.d(TAG, "3.2 --> Sobel done.\n Y: " + sobelY.toString());

        Mat sum_img = new Mat();
        Core.addWeighted(sobelX, 0.5, sobelY, 0.5, 0.5, sum_img);
        //Core.add(sobelX, sobelY, sum_img);
        Log.d(TAG, "4 --> Addition done. sum: " + sum_img.toString());

        sobelX.release();
        sobelY.release();

        Mat gray = new Mat();
        Core.normalize(sum_img, gray, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
        Log.d(TAG, "5 --> Normalization done. gray: " + gray.toString());
        sum_img.release();

        Mat row_proj = new Mat();
        Mat col_proj = new Mat();
        Core.reduce(gray, row_proj, 1, Core.REDUCE_AVG, CvType.CV_8UC1);
        Log.d(TAG, "6.1 --> Reduce done. row: " + row_proj.toString());

        Core.reduce(gray, col_proj, 0, Core.REDUCE_AVG, CvType.CV_8UC1);
        Log.d(TAG, "6.2 --> Reduce done. col: " + col_proj.toString());
        gray.release();

        Imgproc.Sobel(row_proj, row_proj, CvType.CV_8UC1, 0, 2);
        Log.d(TAG, "7.1 --> Sobel done. row: " + row_proj.toString());

        Imgproc.Sobel(col_proj, col_proj, CvType.CV_8UC1, 2, 0);
        Log.d(TAG, "7.2 --> Sobel done. col: " + col_proj.toString());

        Rect result = new Rect();

        int half_pos = (int) (row_proj.total()/2);
        Mat row_sub = new Mat(row_proj, new Range(0, half_pos), new Range(0, 1));
        Log.d(TAG, "8.1 --> Copy sub matrix done. row: " + row_sub.toString());
        result.y = (int) Core.minMaxLoc(row_sub).maxLoc.y;
        Log.d(TAG, "8.2 --> Minmax done. Y: " + result.y);
        row_sub.release();
        Mat row_sub2 = new Mat(row_proj, new Range(half_pos, (int) row_proj.total()), new Range(0, 1));
        Log.d(TAG, "8.3 --> Copy sub matrix done. row: " + row_sub2.toString());
        result.height = (int) (Core.minMaxLoc(row_sub2).maxLoc.y + half_pos - result.y);
        Log.d(TAG, "8.4 --> Minmax done. Height: " + result.height);
        row_sub2.release();

        half_pos = (int) (col_proj.total()/2);
        Mat col_sub = new Mat(col_proj, new Range(0, 1), new Range(0, half_pos));
        Log.d(TAG, "9.1 --> Copy sub matrix done. col: " + col_sub.toString());
        result.x = (int) Core.minMaxLoc(col_sub).maxLoc.x;
        Log.d(TAG, "9.2 --> Minmax done. X: " + result.x);
        col_sub.release();
        Mat col_sub2 = new Mat(col_proj, new Range(0, 1), new Range(half_pos, (int) col_proj.total()));
        Log.d(TAG, "9.3 --> Copy sub matrix done. col: " + col_sub2.toString());
        result.width = (int) (Core.minMaxLoc(col_sub2).maxLoc.x + half_pos - result.x);
        Log.d(TAG, "9.4 --> Minmax done. Width: " + result.width);
        col_sub2.release();

        row_proj.release();
        col_proj.release();
        src.release();

        return result;
    }

    public static double getScaleRatio(Size srcSize){
        return srcSize.height/FIXED_HEIGHT;
    }

    public static List<MatOfPoint> findContours(Mat src){
        Mat img = src.clone();

        //find contours
        double ratio = getScaleRatio(img.size());
        int width = (int) (img.size().width / ratio);
        int height = (int) (img.size().height / ratio);
        Size newSize = new Size(width, height);
        Mat resizedImg = new Mat(newSize, CvType.CV_8UC4);
        Imgproc.resize(img, resizedImg, newSize);
        img.release();

        Imgproc.medianBlur(resizedImg, resizedImg, 7);

        Mat cannedImg = new Mat(newSize, CvType.CV_8UC1);
        Imgproc.Canny(resizedImg, cannedImg, 70, 200, 3, true);
        resizedImg.release();

        Imgproc.threshold(cannedImg, cannedImg, 70, 255, Imgproc.THRESH_OTSU);

        Mat dilatedImg = new Mat(newSize, CvType.CV_8UC1);
        Mat morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(cannedImg, dilatedImg, morph, new Point(-1, -1), 2, 1, new Scalar(1));
        cannedImg.release();
        morph.release();

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();
        dilatedImg.release();

        Log.d(TAG, "contours found: " + contours.size());

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
            }
        });

        return contours;
    }

    public static List<MatOfPoint> findContoursForMRZ(Mat src){
        Mat img = src.clone();
        src.release();
        double ratio = getScaleRatio(img.size());
        int width = (int) (img.size().width / ratio);
        int height = (int) (img.size().height / ratio);
        Size newSize = new Size(width, height);
        Mat resizedImg = new Mat(newSize, CvType.CV_8UC4);
        Imgproc.resize(img, resizedImg, newSize);

        Mat gray = new Mat();
        Imgproc.cvtColor(resizedImg, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(gray, gray, 3);
        //Imgproc.blur(gray, gray, new Size(3, 3));

        Mat morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13, 5));
        Mat dilatedImg = new Mat();
        Imgproc.morphologyEx(gray, dilatedImg, Imgproc.MORPH_BLACKHAT, morph);
        gray.release();

        Mat gradX = new Mat();
        Imgproc.Sobel(dilatedImg, gradX, CvType.CV_32F, 1, 0);
        dilatedImg.release();
        Core.convertScaleAbs(gradX, gradX, 1, 0);
        Core.MinMaxLocResult minMax = Core.minMaxLoc(gradX);
        Core.convertScaleAbs(gradX, gradX, (255/(minMax.maxVal - minMax.minVal)),
                - ((minMax.minVal * 255) / (minMax.maxVal - minMax.minVal)));
        Imgproc.morphologyEx(gradX, gradX, Imgproc.MORPH_CLOSE, morph);

        Mat thresh = new Mat();
        Imgproc.threshold(gradX, thresh, 0, 255, Imgproc.THRESH_OTSU);
        gradX.release();
        morph.release();

        morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 21));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, morph);
        Imgproc.erode(thresh, thresh, new Mat(), new Point(-1, -1), 4);
        morph.release();

        int col = (int) resizedImg.size().width;
        int p = (int) (resizedImg.size().width * 0.05);
        int row = (int) resizedImg.size().height;
        for(int i = 0; i < row; i++)
        {
            for(int j = 0; j < p; j++){
                thresh.put(i, j, 0);
                thresh.put(i, col-j, 0);
            }
        }

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        Log.d(TAG, "contours found: " + contours.size());

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
            }
        });

        return contours;
    }

    public static List<MatOfPoint> findContoursAfterClosing(Mat src){
        Mat img = src.clone();

        //find contours
        double ratio = getScaleRatio(img.size());
        int width = (int) (img.size().width / ratio);
        int height = (int) (img.size().height / ratio);
        Size newSize = new Size(width, height);
        Mat resizedImg = new Mat(newSize, CvType.CV_8UC4);
        Imgproc.resize(img, resizedImg, newSize);
        img.release();

        Imgproc.medianBlur(resizedImg, resizedImg, 5);

        Mat cannedImg = new Mat(newSize, CvType.CV_8UC1);
        Imgproc.Canny(resizedImg, cannedImg, 70, 200, 3, true);
        resizedImg.release();

        Imgproc.threshold(cannedImg, cannedImg, 70, 255, Imgproc.THRESH_OTSU);

        Mat dilatedImg = new Mat(newSize, CvType.CV_8UC1);
        Mat morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(cannedImg, dilatedImg, morph, new Point(-1, -1), 2, 1, new Scalar(1));
        cannedImg.release();
        morph.release();

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        Log.d(TAG, "contours found: " + contours.size());

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
            }
        });

        Rect box = Imgproc.boundingRect(contours.get(0));
        Imgproc.line(dilatedImg, box.tl(), new Point(box.br().x, box.tl().y), new Scalar(255, 255, 255), 2);

        contours = new ArrayList<>();
        hierarchy = new Mat();
        Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();
        dilatedImg.release();

        Log.d(TAG, "contours found: " + contours.size());

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
            }
        });

        return contours;
    }

    static public Quadrilateral getQuadForPassport(Mat img, double frameWidth, double frameHeight){
        final double requiredCoverageRatio = 0.60;
        double ratio = getScaleRatio(img.size());
        double width = img.size().width / ratio;
        double height = img.size().height / ratio;

        if(frameHeight == 0 || frameWidth == 0){
            frameWidth = width;
            frameHeight = height;
        }
        else{
            frameWidth = frameWidth/ratio;
            frameHeight = frameHeight/ratio;
        }

        Size newSize = new Size(width, height);
        Mat resizedImg = new Mat(newSize, CvType.CV_8UC4);
        Imgproc.resize(img, resizedImg, newSize);

        Imgproc.medianBlur(resizedImg, resizedImg, 13);

        Mat cannedImg = new Mat(newSize, CvType.CV_8UC1);
        Imgproc.Canny(resizedImg, cannedImg, 70, 200, 3, true);
        resizedImg.release();

        Mat morphR = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(5, 5));

        Imgproc.morphologyEx(cannedImg, cannedImg, Imgproc.MORPH_CLOSE, morphR, new Point(-1, -1), 1);

        MatOfFloat4 lines = new MatOfFloat4();
        Imgproc.HoughLinesP(cannedImg, lines, 1, Math.PI/180, 30, 30, 150);

        if(lines.rows() >= 3) {
            ArrayList<Line> hLines = new ArrayList<>();
            ArrayList<Line> vLines = new ArrayList<>();

            for (int i = 0; i < lines.rows(); i++) {
                double[] vec = lines.get(i, 0);
                Line l = new Line(vec[0], vec[1], vec[2], vec[3]);
                if(l.isNearHorizontal()) hLines.add(l);
                else if(l.isNearVertical()) vLines.add(l);
            }

            if(hLines.size() >= 2 && vLines.size() >= 2){
                Collections.sort(hLines, new Comparator<Line>() {
                    @Override
                    public int compare(Line o1, Line o2) {
                        return (int) Math.ceil(o1.start.y - o2.start.y);
                    }
                });

                Collections.sort(vLines, new Comparator<Line>() {
                    @Override
                    public int compare(Line o1, Line o2) {
                        return (int) Math.ceil(o1.start.x - o2.start.x);
                    }
                });

                List<Line> nhLines = Line.joinSegments(hLines);

                List<Line> nvLines = Line.joinSegments(vLines);

                if((nvLines.size() > 1 && nhLines.size() > 0) || (nvLines.size() > 0 && nhLines.size() > 1)){
                    Collections.sort(nhLines, new Comparator<Line>() {
                        @Override
                        public int compare(Line o1, Line o2) {
                            return (int) Math.ceil(o2.length() - o1.length());
                        }
                    });

                    Collections.sort(nvLines, new Comparator<Line>() {
                        @Override
                        public int compare(Line o1, Line o2) {
                            return (int) Math.ceil(o2.length() - o1.length());
                        }
                    });

                    Line left = null, right = null, bottom = null, top = null;

                    for(Line l:nvLines){
                        if(l.length()/frameHeight < requiredCoverageRatio || (left != null && right != null)) break;

                        if(left == null && l.isInleft(width)){
                            left = l;
                            continue;
                        }

                        if(right == null && !l.isInleft(width)) right = l;
                    }

                    for(Line l:nhLines){
                        if(l.length()/frameWidth < requiredCoverageRatio || (top != null && bottom != null)) break;

                        if(bottom == null && l.isInBottom(height)){
                            bottom = l;
                            continue;
                        }

                        if(top == null && !l.isInBottom(height)) top = l;
                    }

                    Point[] foundPoints = null;

                    if((left != null && right != null) && (bottom != null || top != null)){
                        Point vLeft = bottom != null? bottom.intersect(left):top.intersect(left);
                        Point vRight = bottom != null? bottom.intersect(right):top.intersect(right);
                        Log.d(TAG, "got the edges");
                        if(vLeft != null && vRight != null) {
                            double pwidth = new Line(vLeft, vRight).length();
                            double pHeight =  pwidth/PASSPORT_ASPECT_RATIO;

                            Point tLeft = getPointOnLine(vLeft, left.end, pHeight);
                            Point tRight = getPointOnLine(vRight, right.end, pHeight);

                            foundPoints = new Point[]{vLeft, vRight, tLeft, tRight};
                        }
                    }
                    else if((top != null && bottom != null) && (left != null || right != null)){
                        Point vTop = left != null? left.intersect(top):right.intersect(top);
                        Point vBottom = left != null? left.intersect(bottom):right.intersect(bottom);
                        Log.d(TAG, "got the edges");
                        if(vTop != null && vBottom != null) {
                            double pHeight = new Line(vTop, vBottom).length();
                            double pWidth = pHeight * PASSPORT_ASPECT_RATIO;

                            Point tTop = getPointOnLine(vTop, top.end, pWidth);
                            Point tBottom = getPointOnLine(vBottom, bottom.end, pWidth);

                            foundPoints = new Point[]{tTop, tBottom, vTop, vBottom};
                        }
                    }

                    if(foundPoints != null){
                        Point[] sPoints = sortPoints(foundPoints);

                        if(isInside(sPoints, newSize)
                                && isLargeEnough(sPoints, new Size(frameWidth, frameHeight), requiredCoverageRatio)){
                            return new Quadrilateral(null, sPoints);
                        }
                        else Log.d(TAG, "Not inside");
                    }
                }
            }
        }

        return null;
    }

    static public Point getPointOnLine(Point origin, Point another, double distance){
        double dFactor = distance / new Line(origin, another).length();
        double X = ((1 - dFactor) * origin.x) + (dFactor * another.x);
        double Y = ((1 - dFactor) * origin.y) + (dFactor * another.y);
        return new Point(X, Y);
    }

    static public Quadrilateral getQuadrilateral(List<MatOfPoint> contours, Size srcSize){
        double ratio = getScaleRatio(srcSize);
        int height = Double.valueOf(srcSize.height / ratio).intValue();
        int width = Double.valueOf(srcSize.width / ratio).intValue();
        Size size = new Size(width,height);

        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

            Point[] points = approx.toArray();
            Log.d("SCANNER", "approx size: " + points.length);

            // select biggest 4 angles polygon
            if (points.length == 4) {
                Point[] foundPoints = sortPoints(points);

                if (isInside(foundPoints, size) && isLargeEnough(foundPoints, size, 0.25)) {
                    return new Quadrilateral( c , foundPoints );
                }
                else{
                    //showToast(context, "Try getting closer to the ID");
                    Log.d("SCANNER", "Not inside defined area");
                }
            }
        }

        //showToast(context, "Make sure the ID is on a contrasting background");
        return null;
    }

    static public Quadrilateral getQuadForPassport(List<MatOfPoint> contours, Size srcSize, int frameSize){
        final int requiredAspectRatio = 5;
        final float requiredCoverageRatio = 0.80f;

        MatOfPoint rectContour = null;
        Point[] foundPoints = null;

        double ratio = getScaleRatio(srcSize);
        int width = Double.valueOf(srcSize.width / ratio).intValue();
        int frameWidth = Double.valueOf(frameSize / ratio).intValue();

        for(MatOfPoint c:contours){
            Rect bRect = Imgproc.boundingRect(c);
            float aspectRatio = bRect.width / (float)bRect.height;
            float coverageRatio = frameSize != 0? bRect.width/(float)frameWidth:bRect.width/(float)width;

            Log.d(TAG, "AR: " + aspectRatio + ", CR: " + coverageRatio + ", frameWidth: " + frameWidth);

            if(aspectRatio > requiredAspectRatio && coverageRatio > requiredCoverageRatio){
                MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
                double peri = Imgproc.arcLength(c2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

                Point[] points = approx.toArray();
                Log.d("SCANNER", "approx size: " + points.length);

                // select biggest 4 angles polygon
                if (points.length == 4){
                    rectContour = c;
                    foundPoints = CVProcessor.sortPoints(points);
                    break;
                }
                else if(points.length == 2){
                    if(rectContour == null){
                        rectContour = c;
                        foundPoints = points;
                    }
                    else{
                        //try to merge
                        RotatedRect box1 = Imgproc.minAreaRect(new MatOfPoint2f(c.toArray()));
                        RotatedRect box2 = Imgproc.minAreaRect(new MatOfPoint2f(rectContour.toArray()));

                        float ar = (float) (box1.size.width/box2.size.width);
                        if(box1.size.width > 0 && box2.size.width > 0 && 0.5 < ar && ar < 2.0) {
                            if (Math.abs(box1.angle - box2.angle) <= 0.1 ||
                                    Math.abs(Math.PI - (box1.angle - box2.angle)) <= 0.1) {
                                double minAngle = Math.min(box1.angle, box2.angle);
                                double relX = box1.center.x - box2.center.x;
                                double rely = box1.center.y - box2.center.y;
                                double distance = Math.abs((rely * Math.cos(minAngle)) - (relX * Math.sin(minAngle)));
                                if(distance < (1.5 * (box1.size.height + box2.size.height))){
                                    Point[] allPoints = Arrays.copyOf(foundPoints, 4);

                                    System.arraycopy(points, 0, allPoints, 2, 2);
                                    Log.d("SCANNER", "after merge approx size: " + allPoints.length);
                                    if (allPoints.length == 4){
                                        foundPoints = CVProcessor.sortPoints(allPoints);
                                        rectContour = new MatOfPoint(foundPoints);
                                        break;
                                    }
                                }
                            }
                        }

                        rectContour = null;
                        foundPoints = null;
                    }
                }
            }
        }

        if(foundPoints != null && foundPoints.length == 4){
            Point lowerLeft = foundPoints[3];
            Point lowerRight = foundPoints[2];
            Point topLeft = foundPoints[0];
            double w = Math.sqrt(Math.pow(lowerRight.x - lowerLeft.x, 2) + Math.pow(lowerRight.y - lowerLeft.y, 2));
            double h = Math.sqrt(Math.pow(topLeft.x - lowerLeft.x, 2) + Math.pow(topLeft.y - lowerLeft.y, 2));
            int px = (int) ((lowerLeft.x + w) * 0.03);
            int py = (int) ((lowerLeft.y + h) * 0.03);
            lowerLeft.x = lowerLeft.x - px;
            lowerLeft.y = lowerLeft.y + py;

            px = (int) ((lowerRight.x + w) * 0.03);
            py = (int) ((lowerRight.y + h) * 0.03);
            lowerRight.x = lowerRight.x + px;
            lowerRight.y = lowerRight.y + py;

            float pRatio = 3.465f/4.921f;
            w = Math.sqrt(Math.pow(lowerRight.x - lowerLeft.x, 2) + Math.pow(lowerRight.y - lowerLeft.y, 2));

            h = pRatio * w;
            h = h - (h * 0.04);

            foundPoints[1] = new Point(lowerRight.x, lowerRight.y - h);
            foundPoints[0] = new Point(lowerLeft.x, lowerLeft.y - h);

            return new Quadrilateral(rectContour, foundPoints);
        }

        return null;
    }

    public static Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    public static boolean isInsideBaseArea(Point[] rp, Size size) {

        int width = Double.valueOf(size.width).intValue();
        int height = Double.valueOf(size.height).intValue();
        int baseMeasure = height/4;

        int bottomPos = height-baseMeasure;
        int topPos = baseMeasure;
        int leftPos = width/2-baseMeasure;
        int rightPos = width/2+baseMeasure;

        return (
                rp[0].x <= leftPos && rp[0].y <= topPos
                        && rp[1].x >= rightPos && rp[1].y <= topPos
                        && rp[2].x >= rightPos && rp[2].y >= bottomPos
                        && rp[3].x <= leftPos && rp[3].y >= bottomPos

        );
    }

    public static boolean isInside(Point[] points, Size size){
        int width = Double.valueOf(size.width).intValue();
        int height = Double.valueOf(size.height).intValue();

        boolean isInside =  points[0].x >= 0 && points[0].y >= 0
                && points[1].x <= width && points[1].y >= 0
                && points[2].x <= width && points[2].y <= height
                && points[3].x >= 0 && points[3].y <= height;

        Log.d(TAG, "w: " + width + ", h: " + height + "\nPoints: " + points[0] + ", " + points[1] + ", " + points[2] + ", " + points[3] + ", result: " + isInside);
        return isInside;
    }

    public  static boolean isLargeEnough(Point[] points, Size size, double ratio){
        double contentWidth = Math.max(new Line(points[0], points[1]).length(), new Line(points[3], points[2]).length());
        double contentHeight = Math.max(new Line(points[0], points[3]).length(), new Line(points[1], points[2]).length());

        double widthRatio = contentWidth/size.width;
        double heightRatio = contentHeight/size.height;

        Log.d(TAG, "ratio: wr-"+ widthRatio + ", hr-" + heightRatio +", w: " + size.width + ", h: " + size.height + ", cw: " + contentWidth + ", ch: " + contentHeight);

        return widthRatio >= ratio && heightRatio >= ratio;
    }

    public static Point[] getUpscaledPoints(Point[] points, double scaleFactor){
        Point[] rescaledPoints = new Point[4];

        for ( int i=0; i<4 ; i++ ) {
            int x = Double.valueOf(points[i].x*scaleFactor).intValue();
            int y = Double.valueOf(points[i].y*scaleFactor).intValue();
            rescaledPoints[i] = new Point(x, y);
        }

        return rescaledPoints;
    }

    /**
     *
     * @param src - actual image
     * @param pts - points scaled up with respect to actual image
     * @return
     */
    public static Mat fourPointTransform( Mat src , Point[] pts ) {
        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }

    public static Mat adjustBirghtnessAndContrast(Mat src, double clipPercentage){
        int histSize = 256;
        double alpha, beta;
        double minGray, maxGray;

        Mat gray = null;
        if(src.type() == CvType.CV_8UC1){
            gray = src.clone();
        }
        else{
            gray = new Mat();
            Imgproc.cvtColor(src, gray, src.type() == CvType.CV_8UC3? Imgproc.COLOR_RGB2GRAY:Imgproc.COLOR_RGBA2GRAY);
        }

        if(clipPercentage == 0) {
            Core.MinMaxLocResult minMaxGray = Core.minMaxLoc(gray);
            minGray = minMaxGray.minVal;
            maxGray = minMaxGray.maxVal;
        }
        else{
            Mat hist = new Mat();
            MatOfInt size = new MatOfInt(histSize);
            MatOfInt channels = new MatOfInt(0);
            MatOfFloat ranges = new MatOfFloat(0, 256);
            Imgproc.calcHist(Arrays.asList(gray), channels, new Mat(), hist, size, ranges, false);
            gray.release();

            double[] accumulator = new double[histSize];

            accumulator[0] = hist.get(0, 0)[0];
            for(int i = 1; i < histSize; i++){
                accumulator[i] = accumulator[i - 1] + hist.get(i, 0)[0];
            }

            hist.release();

            double max = accumulator[accumulator.length - 1];
            clipPercentage = (clipPercentage * (max/100.0));
            clipPercentage = clipPercentage / 2.0f;

            minGray = 0;
            while (minGray < histSize && accumulator[(int) minGray] < clipPercentage){
                minGray++;
            }

            maxGray = histSize - 1;
            while (maxGray >= 0 && accumulator[(int) maxGray] >= (max - clipPercentage)){
                maxGray--;
            }
        }

        double inputRange = maxGray - minGray;
        alpha = (histSize - 1)/inputRange;
        beta = -minGray * alpha;

        Mat result = new Mat();
        src.convertTo(result, -1, alpha, beta);

        if(result.type() == CvType.CV_8UC4){
            Core.mixChannels(Arrays.asList(src), Arrays.asList(result), new MatOfInt(3, 3));
        }

        return result;
    }

    public static Mat sharpenImage(Mat src){
        Mat sharped = new Mat();
        Imgproc.GaussianBlur(src, sharped, new Size(0, 0), 3);
        Core.addWeighted(src, 1.5, sharped, -0.5, 0, sharped);

        return sharped;
    }

    public static class Quadrilateral {
        public MatOfPoint contour;
        public Point[] points;

        public Quadrilateral(MatOfPoint contour, Point[] points) {
            this.contour = contour;
            this.points = points;
        }
    }
}
