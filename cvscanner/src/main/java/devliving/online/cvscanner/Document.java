package devliving.online.cvscanner;

import com.google.android.gms.vision.Frame;

import org.opencv.core.Point;

import devliving.online.cvscanner.util.CVProcessor;

/**
 * Created by Mehedi on 10/15/16.
 *
 * Holds the actual image data. Quad point are also scaled with respect to actual image.
 */
public class Document {
    Frame image;
    CVProcessor.Quadrilateral detectedQuad;

    public Document(Frame image, CVProcessor.Quadrilateral detectedQuad) {
        this.image = image;
        this.detectedQuad = detectedQuad;
    }

    public Frame getImage() {
        return image;
    }

    public void setImage(Frame image) {
        this.image = image;
    }

    public CVProcessor.Quadrilateral getDetectedQuad() {
        return detectedQuad;
    }

    public void setDetectedQuad(CVProcessor.Quadrilateral detectedQuad) {
        this.detectedQuad = detectedQuad;
    }

    public int getMaxArea(){
        Point tl = detectedQuad.points[0];
        Point tr = detectedQuad.points[1];
        Point br = detectedQuad.points[2];
        Point bl = detectedQuad.points[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);

        return Double.valueOf(dw).intValue() * Double.valueOf(dh).intValue();
    }
}
