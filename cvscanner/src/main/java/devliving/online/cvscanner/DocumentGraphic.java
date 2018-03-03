package devliving.online.cvscanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.util.Log;

import online.devliving.mobilevisionpipeline.GraphicOverlay;

/**
 * Created by user on 10/15/16.
 */
public class DocumentGraphic extends GraphicOverlay.Graphic {
    int Id;
    Document scannedDoc;
    Paint borderPaint, bodyPaint;

    int borderColor = Color.parseColor("#41fa97"), fillColor = Color.parseColor("#69fbad");

    public DocumentGraphic(GraphicOverlay overlay, Document doc) {
        super(overlay);
        scannedDoc = doc;

        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        borderPaint.setStrokeWidth(12);

        bodyPaint = new Paint();
        bodyPaint.setColor(fillColor);
        bodyPaint.setAlpha(180);
        bodyPaint.setStyle(Paint.Style.FILL);
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    void update(Document doc){
        scannedDoc = doc;
        postInvalidate();
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        borderPaint.setColor(borderColor);
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
        bodyPaint.setColor(fillColor);
    }

    /**
     * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
     * convert to view coordinates for the graphics that are drawn:
     * <ol>
     * <li>{@link GraphicOverlay.Graphic#scaleX(float)} and {@link GraphicOverlay.Graphic#scaleY(float)} adjust the size of
     * the supplied value from the preview scale to the view scale.</li>
     * <li>{@link GraphicOverlay.Graphic#translateX(float)} and {@link GraphicOverlay.Graphic#translateY(float)} adjust the
     * coordinate from the preview's coordinate system to the view coordinate system.</li>
     * </ol>
     *
     * @param canvas drawing canvas
     */
    @Override
    public void draw(Canvas canvas) {
        //TODO fix the coordinates see http://zhengrui.github.io/android-coordinates.html

        if(scannedDoc != null && scannedDoc.detectedQuad != null){
            //boolean isPortrait = Util.isPortraitMode(mOverlay.getContext());
            Path path = new Path();

            /*
            Log.d("DOC-GRAPHIC", "IsPortrait? " + isPortrait);

            float tlX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[0].y):translateX((float) scannedDoc.detectedQuad.points[0].x);
            float tlY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[0].x):translateY((float) scannedDoc.detectedQuad.points[0].y);

            Log.d("DOC-GRAPHIC", "Top left: x: " + scannedDoc.detectedQuad.points[0].x + ", y: " + scannedDoc.detectedQuad.points[0].y
                    + " -> x: " + tlX + ", y: " + tlY);

            float blX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[1].y):translateX((float) scannedDoc.detectedQuad.points[1].x);
            float blY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[1].x):translateY((float) scannedDoc.detectedQuad.points[1].y);

            Log.d("DOC-GRAPHIC", "Bottom left: x: " + scannedDoc.detectedQuad.points[1].x + ", y: " + scannedDoc.detectedQuad.points[1].y
                    + " -> x: " + blX + ", y: " + blY);

            float brX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[2].y):translateX((float) scannedDoc.detectedQuad.points[2].x);
            float brY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[2].x):translateY((float) scannedDoc.detectedQuad.points[2].y);

            Log.d("DOC-GRAPHIC", "Bottom right: x: " + scannedDoc.detectedQuad.points[2].x + ", y: " + scannedDoc.detectedQuad.points[2].y
                    + " -> x: " + brX + ", y: " + brY);

            float trX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[3].y):translateX((float) scannedDoc.detectedQuad.points[3].x);
            float trY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[3].x):translateY((float) scannedDoc.detectedQuad.points[3].y);

            Log.d("DOC-GRAPHIC", "Top right: x: " + scannedDoc.detectedQuad.points[3].x + ", y: " + scannedDoc.detectedQuad.points[3].y
                    + " -> x: " + trX + ", y: " + trY);
            */
            int frameWidth = scannedDoc.getImage().getMetadata().getHeight();

            path.moveTo(((float)(frameWidth - scannedDoc.detectedQuad.points[0].y)), ((float)scannedDoc.detectedQuad.points[0].x));
            path.lineTo(((float)(frameWidth - scannedDoc.detectedQuad.points[1].y)), ((float)scannedDoc.detectedQuad.points[1].x));
            path.lineTo(((float)(frameWidth - scannedDoc.detectedQuad.points[2].y)), ((float)scannedDoc.detectedQuad.points[2].x));
            path.lineTo(((float)(frameWidth - scannedDoc.detectedQuad.points[3].y)), ((float)scannedDoc.detectedQuad.points[3].x));
            path.close();

            PathShape shape = new PathShape(path, scannedDoc.getImage().getMetadata().getHeight(), scannedDoc.getImage().getMetadata().getWidth());
            shape.resize(canvas.getWidth(), canvas.getHeight());

            shape.draw(canvas, bodyPaint);
            shape.draw(canvas, borderPaint);

            //canvas.drawPath(path, borderPaint);
            //canvas.drawPath(path, bodyPaint);

            Log.d("DOC-GRAPHIC", "DONE DRAWING");
        }
    }
}
