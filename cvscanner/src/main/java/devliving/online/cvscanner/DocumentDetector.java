package devliving.online.cvscanner;

import android.content.Context;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;

import java.util.List;

import devliving.online.cvscanner.util.CVProcessor;

/**
 * Created by user on 10/15/16.
 */
public class DocumentDetector extends Detector<Document> {

    Context mContext;

    public DocumentDetector(Context context){
        super();
        mContext = context;
    }

    @Override
    public SparseArray<Document> detect(Frame frame) {
        SparseArray<Document> detections = new SparseArray<>();
        if(frame.getBitmap() != null) {
            Document doc = detectDocument(frame);

            if (doc != null) detections.append(frame.getMetadata().getId(), doc);
        }

        return detections;
    }

    Document detectDocument(Frame frame){
        Size imageSize = new Size(frame.getMetadata().getWidth(), frame.getMetadata().getHeight());
        Mat src = new Mat();
        Utils.bitmapToMat(frame.getBitmap(), src);
        List<MatOfPoint> contours = CVProcessor.findContours(src);
        src.release();

        if(!contours.isEmpty()){
            CVProcessor.Quadrilateral quad = CVProcessor.getQuadrilateral(contours, imageSize);

            if(quad != null){
                quad.points = CVProcessor.getUpscaledPoints(quad.points, CVProcessor.getScaleRatio(imageSize));
                return new Document(frame, quad);
            }
        }

        return null;
    }
}
