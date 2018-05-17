package devliving.online.cvscanner;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 8/16/17.
 */

public class DocumentProcessor extends FocusingProcessor<Document> {
    public DocumentProcessor(Detector<Document> detector, Tracker<Document> tracker) {
        super(detector, tracker);
    }

    @Override
    public int selectFocus(Detector.Detections<Document> detections) {
        SparseArray<Document> detectedItems;
        if((detectedItems = detections.getDetectedItems()).size() == 0) {
            throw new IllegalArgumentException("No documents for selectFocus.");
        } else {
            int itemKey = detectedItems.keyAt(0);
            int itemArea = detectedItems.valueAt(0).getMaxArea();

            for(int index = 1; index < detectedItems.size(); ++index) {
                int itemKey2 = detectedItems.keyAt(index);
                int itemArea2;
                if((itemArea2 = detectedItems.valueAt(index).getMaxArea()) > itemArea) {
                    itemKey = itemKey2;
                    itemArea = itemArea2;
                }
            }

            return itemKey;
        }
    }
}
