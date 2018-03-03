package devliving.online.cvscanner;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import devliving.online.cvscanner.util.CVProcessor;
import online.devliving.mobilevisionpipeline.Util;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 12/23/16.
 */

public class PassportDetector extends Detector<Document> {
    Util.FrameSizeProvider frameSizeProvider = null;
    final boolean isMRZBasedDetection = false;

    public PassportDetector(Util.FrameSizeProvider sizeProvider){
        super();
        this.frameSizeProvider = sizeProvider;
    }

    @Override
    public SparseArray<Document> detect(Frame frame) {
        SparseArray<Document> detections = new SparseArray<>();
        Document doc = detectDocument(frame);

        if(doc != null) detections.append(frame.getMetadata().getId(), doc);

        return detections;
    }

    public String saveBitmapJPG(Bitmap img, String imageName) {
        File dir = new File(Environment.getExternalStorageDirectory(), "/" + "CVScannerSample" + "/");
        dir.mkdirs();

        File file = new File(dir, imageName);
        FileOutputStream fOut;
        try {
            if(!file.exists())
            {
                file.createNewFile();
            }
            fOut = new FileOutputStream(file);
            img.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    Document detectDocument(Frame frame){
        Size imageSize = new Size(frame.getMetadata().getWidth(), frame.getMetadata().getHeight());
        Mat src = new Mat();
        Utils.bitmapToMat(frame.getBitmap(), src);

        int shiftX = 0, shiftY = 0;

        if(frameSizeProvider != null){
            int frameWidth = frameSizeProvider.frameWidth();
            int frameHeight = frameSizeProvider.frameHeight();

            shiftX = Double.valueOf((imageSize.width - frameHeight)/2.0).intValue();
            shiftY = Double.valueOf((imageSize.height - frameWidth)/2.0).intValue();
            Rect rect = new Rect(shiftX, shiftY,
                    frameHeight, frameWidth);
            Mat cropped = new Mat(src, rect).clone();
            src.release();
            src = cropped;

            imageSize = new Size(src.cols(), src.rows());
        }

        CVProcessor.Quadrilateral quad = null;

        if(isMRZBasedDetection){
            List<MatOfPoint> contours = CVProcessor.findContoursForMRZ(src);

            if(!contours.isEmpty()){
                quad = CVProcessor.getQuadForPassport(contours, imageSize, frameSizeProvider != null? frameSizeProvider.frameWidth():0);
            }
        }
        else{
            quad = CVProcessor.getQuadForPassport(src, frameSizeProvider != null? frameSizeProvider.frameWidth():0,
                    frameSizeProvider != null? frameSizeProvider.frameHeight():0);
        }

        src.release();

        if(quad != null && quad.points != null){
            quad.points = CVProcessor.getUpscaledPoints(quad.points, CVProcessor.getScaleRatio(imageSize));

            //shift back to old coordinates
            for(int i = 0; i < quad.points.length; i++){
                quad.points[i] = shiftPointToOld(quad.points[i], shiftX, shiftY);
            }

            return new Document(frame, quad);
        }

        return null;
    }

    Point shiftPointToOld(Point point, int sx, int sy){
        point.x = point.x + sx;
        point.y = point.y + sy;

        return point;
    }
}
