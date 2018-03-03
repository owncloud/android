package devliving.online.cvscanner;

import android.graphics.RectF;

import devliving.online.cvscanner.util.CVProcessor;
import online.devliving.mobilevisionpipeline.FrameGraphic;
import online.devliving.mobilevisionpipeline.GraphicOverlay;
import online.devliving.mobilevisionpipeline.Util;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 8/13/17.
 */

public class BorderFrameGraphic extends FrameGraphic {
    boolean isForPassport = false;

    public BorderFrameGraphic(GraphicOverlay overlay, boolean forPassport) {
        super(overlay);
        isForPassport = forPassport;
    }

    @Override
    protected RectF getFrameRect(float canvasWidth, float canvasHeight) {
        RectF rect;
        float padding = 32;

        if(isForPassport){
            float frameHeight;
            float frameWidth;

            if(Util.isPortraitMode(mOverlay.getContext())){
                frameWidth = canvasWidth - (2 * padding);
                frameHeight = frameWidth * CVProcessor.PASSPORT_ASPECT_RATIO;
            }
            else{
                frameHeight = canvasHeight - (2 * padding);
                frameWidth = frameHeight/CVProcessor.PASSPORT_ASPECT_RATIO;
            }

            rect = new RectF(padding, padding, frameWidth, frameHeight);

            float cx = canvasWidth/2.0f;
            float cy = canvasHeight/2.0f;
            float dx = cx - rect.centerX();
            float dy = cy - rect.centerY();
            rect.offset(dx, dy);
        }
        else{
            rect = new RectF(padding, padding, canvasWidth  - padding, canvasHeight - padding);
        }
        return rect;
    }
}
