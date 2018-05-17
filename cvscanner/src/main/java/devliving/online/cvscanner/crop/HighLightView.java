package devliving.online.cvscanner.crop;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * Created by renard on 07/06/15.
 */
public interface HighLightView {
    int GROW_NONE = 0;
    int GROW_LEFT_EDGE = (1 << 1);
    int GROW_RIGHT_EDGE = (1 << 2);
    int GROW_TOP_EDGE = (1 << 3);
    int GROW_BOTTOM_EDGE = (1 << 4);
    int MOVE = (1 << 5);


    /**
     * @return Matrix that converts between image and screen space.
     */
    Matrix getMatrix();

    /**
     *
     * @return Drawing rect in screen space.
     */
    Rect getDrawRect();

    /**
     * @return vertical center in image space.
     */
    float centerY();

    /**
     * @return horizontal center in image space.
     */
    float centerX();


    int getHit(float x, float y, float scale);

    void handleMotion(int motionEdge, float dx, float dy);

    void draw(Canvas canvas);
}
