package devliving.online.cvscanner;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;

import online.devliving.mobilevisionpipeline.GraphicOverlay;

/**
 * Created by user on 10/15/16.
 */
public class DocumentTracker extends Tracker<Document> {
    GraphicOverlay<DocumentGraphic> mOverlay;
    DocumentGraphic mGraphic;
    DocumentDetectionListener mListener;

    public DocumentTracker(GraphicOverlay<DocumentGraphic> mOverlay, DocumentGraphic mGraphic, DocumentDetectionListener mListener) {
        this.mOverlay = mOverlay;
        this.mGraphic = mGraphic;
        this.mListener = mListener;
    }

    @Override
    public void onNewItem(int i, Document document) {
        mGraphic.setId(i);

        if(mListener != null){
            mListener.onDocumentDetected(document);
        }
    }

    @Override
    public void onUpdate(Detector.Detections<Document> detections, Document document) {
        mOverlay.add(mGraphic);
        mGraphic.update(document);
    }

    @Override
    public void onMissing(Detector.Detections<Document> detections) {
        mOverlay.remove(mGraphic);
    }

    @Override
    public void onDone() {
        mOverlay.remove(mGraphic);
    }

    public interface DocumentDetectionListener{
        void onDocumentDetected(Document document);
    }
}
