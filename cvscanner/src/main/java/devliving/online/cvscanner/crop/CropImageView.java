/*
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package devliving.online.cvscanner.crop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by renard on 13/11/14.
 */
public class CropImageView extends ImageViewTouchBase {
    public interface CropImageViewHost{
        boolean isBusy();
    }

    private HighLightView mCropHighlightView = null;
    private boolean mIsMoving = false;
    private float mLastX, mLastY;
    private int mMotionEdge;

    private CropImageViewHost mHost;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mBitmapDisplayed.getBitmap() != null && mCropHighlightView != null) {
            mCropHighlightView.getMatrix().set(getImageMatrix());
            centerBasedOnHighlightView(mCropHighlightView);
        }
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(context instanceof CropImageViewHost) mHost = (CropImageViewHost) context;
    }

    @Override
    protected void zoomTo(float scale, float centerX, float centerY) {
        super.zoomTo(scale, centerX, centerY);
        if (mCropHighlightView != null) {
            mCropHighlightView.getMatrix().set(getImageMatrix());
        }
    }

    @Override
    protected void zoomIn() {
        super.zoomIn();
        if (mCropHighlightView != null) {
            mCropHighlightView.getMatrix().set(getImageMatrix());
        }
    }

    @Override
    protected void zoomOut() {
        super.zoomOut();
        if (mCropHighlightView != null) {
            mCropHighlightView.getMatrix().set(getImageMatrix());
        }
    }

    @Override
    protected void postTranslate(float deltaX, float deltaY) {
        super.postTranslate(deltaX, deltaY);
        if (mCropHighlightView != null) {
            mCropHighlightView.getMatrix().postTranslate(deltaX, deltaY);
        }
    }

    public void setHost(CropImageViewHost mHost) {
        this.mHost = mHost;
    }

    private float[] mapPointToImageSpace(float x, float y) {
        float[] p = new float[2];
        Matrix m = getImageViewMatrix();
        Matrix m2 = new Matrix();
        m.invert(m2);
        p[0] = x;
        p[1] = y;
        m2.mapPoints(p);
        return p;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mHost.isBusy()) {
            return false;
        }
        if (mCropHighlightView == null) {
            return false;
        }

        float[] mappedPoint = mapPointToImageSpace(event.getX(), event.getY());


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int edge = mCropHighlightView.getHit(mappedPoint[0], mappedPoint[1], getScale());
                if (edge != CropHighlightView.GROW_NONE) {
                    mMotionEdge = edge;
                    mIsMoving = true;
                    mLastX = mappedPoint[0];
                    mLastY = mappedPoint[1];
                    break;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsMoving) {
                    centerBasedOnHighlightView(mCropHighlightView);
                }
                mIsMoving = false;
                center(true, true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsMoving) {
                    mCropHighlightView.handleMotion(mMotionEdge, mappedPoint[0] - mLastX, mappedPoint[1] - mLastY);
                    mLastX = mappedPoint[0];
                    mLastY = mappedPoint[1];
                    ensureVisible(mCropHighlightView);
                    invalidate();
                }
                // if we're not zoomed then there's no point in even allowing
                // the user to move the image around. This call to center puts
                // it back to the normalized location (with false meaning don't
                // animate).
                if (getScale() == 1F) {
                    center(true, true);
                }
                break;
        }
        return true;
    }

    @Override
    public void onZoomFinished() {
        if(mCropHighlightView != null){
            ensureVisible(mCropHighlightView);
        }
    }

    // Pan the displayed image to make sure the cropping rectangle is visible.
    private void ensureVisible(HighLightView hv) {
        Rect r = hv.getDrawRect();

        int panDeltaX1 = Math.max(0, mLeft - r.left);
        int panDeltaX2 = Math.min(0, mRight - r.right);

        int panDeltaY1 = Math.max(0, mTop - r.top);
        int panDeltaY2 = Math.min(0, mBottom - r.bottom);

        int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
        int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

        if (panDeltaX != 0 || panDeltaY != 0) {
            panBy(panDeltaX, panDeltaY);
        }
    }

    // If the cropping rectangle's size changed significantly, change the
    // view's center and scale according to the cropping rectangle.
    private void centerBasedOnHighlightView(HighLightView hv) {
        Rect drawRect = hv.getDrawRect();

        float width = drawRect.width();
        float height = drawRect.height();

        float thisWidth = getWidth();
        float thisHeight = getHeight();

        float z1 = thisWidth / width * .6F;
        float z2 = thisHeight / height * .6F;

        float zoom = Math.min(z1, z2);
        zoom = zoom * this.getScale();
        zoom = Math.max(1F, zoom);
        if ((Math.abs(zoom - getScale()) / zoom) > .1) {
            float[] coordinates = new float[]{hv.centerX(), hv.centerY()};
            getImageMatrix().mapPoints(coordinates);
            zoomTo(zoom, coordinates[0], coordinates[1], 300F);
        }

        ensureVisible(hv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInEditMode() && mCropHighlightView != null) {
            mCropHighlightView.draw(canvas);
        }
    }


    public void add(HighLightView hv) {
        mCropHighlightView = hv;
        invalidate();
    }

    public void setMaxZoom(int maxZoom) {
        mMaxZoom = maxZoom;
    }

    public void resetMaxZoom() {
        mMaxZoom = maxZoom();
    }
}
