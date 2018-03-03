/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package devliving.online.cvscanner.crop;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import devliving.online.cvscanner.R;

// This class is used by CropImage to display a highlighted cropping mTrapezoid
// overlayed with the image. There are two coordinate spaces in use. One is
// image, another is screen. computeLayout() uses mMatrix to map from image
// space to screen space.
class CropHighlightView implements HighLightView {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = CropHighlightView.class.getSimpleName();
    private View mContext; // The View displaying the image.

    /* used during onDraw */
    private final Rect mViewDrawingRect = new Rect();
    private final Rect mLeftRect = new Rect();
    private final Rect mRightRect = new Rect();
    private final Rect mTopRect = new Rect();
    private final Rect mBottomRect = new Rect();
    private final RectF mPathBounds = new RectF();
    private final Rect mPathBoundsRounded = new Rect();
    private final Rect mCanvasCLipRect = new Rect();

    private final CroppingTrapezoid mTrapezoid;
    boolean mIsFocused;
    boolean mHidden = false;

    private Rect mDrawRect; // in screen space
    private Matrix mMatrix;

    private final Paint mFocusPaint = new Paint();
    private final Paint mOutlinePaint = new Paint();
    private final int mCropCornerHandleRadius;
    private final int mCropEdgeHandleRadius;
    private final float mHysteresis;


    public CropHighlightView(ImageView ctx, Rect imageRect, RectF cropRect) {
        mContext = ctx;
        final int progressColor = mContext.getResources().getColor(R.color.box_border);
        mCropCornerHandleRadius = mContext.getResources().getDimensionPixelSize(R.dimen.crop_handle_corner_radius);
        mCropEdgeHandleRadius = mContext.getResources().getDimensionPixelSize(R.dimen.crop_handle_edge_radius);
        mHysteresis = mContext.getResources().getDimensionPixelSize(R.dimen.crop_hit_hysteresis);
        final int edgeWidth = mContext.getResources().getDimensionPixelSize(R.dimen.crop_edge_width);
        mMatrix = new Matrix(ctx.getImageMatrix());
        Log.i(LOG_TAG, "image = " + imageRect.toString() + " crop = " + cropRect.toString());
        mTrapezoid = new CroppingTrapezoid(cropRect, imageRect);

        mDrawRect = computeLayout();

        mFocusPaint.setARGB(125, 50, 50, 50);
        mFocusPaint.setStyle(Paint.Style.FILL);

        mOutlinePaint.setARGB(0xFF, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor));
        mOutlinePaint.setStrokeWidth(edgeWidth);
        mOutlinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mOutlinePaint.setAntiAlias(true);
    }


    public void setFocus(boolean f) {
        mIsFocused = f;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mHidden) {
            return;
        }
        mDrawRect = computeLayout();
        drawEdges(canvas);

    }

    private void drawEdges(Canvas canvas) {
        final float[] p = mTrapezoid.getScreenPoints(getMatrix());
        Path path = new Path();
        path.moveTo((int) p[0], (int) p[1]);
        path.lineTo((int) p[2], (int) p[3]);
        path.lineTo((int) p[4], (int) p[5]);
        path.lineTo((int) p[6], (int) p[7]);
        path.close();
        path.computeBounds(mPathBounds, false);
        mPathBounds.round(mPathBoundsRounded);
        canvas.getClipBounds(mCanvasCLipRect);

        mContext.getDrawingRect(mViewDrawingRect);
        mTopRect.set(0, 0, mViewDrawingRect.right, getDrawRect().top);
        mRightRect.set(0, getDrawRect().top, getDrawRect().left, getDrawRect().bottom);
        mLeftRect.set(getDrawRect().right, getDrawRect().top, mViewDrawingRect.right, getDrawRect().bottom);
        mBottomRect.set(0, getDrawRect().bottom, mViewDrawingRect.right, mViewDrawingRect.bottom);

        canvas.drawRect(mTopRect, mFocusPaint);
        canvas.drawRect(mRightRect, mFocusPaint);
        canvas.drawRect(mLeftRect, mFocusPaint);
        canvas.drawRect(mBottomRect, mFocusPaint);
        if (mCanvasCLipRect.contains(mPathBoundsRounded)) {
            canvas.save();
            canvas.clipRect(getDrawRect());
            path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
            canvas.drawPath(path, mFocusPaint);
            canvas.restore();
        }
        canvas.drawLine(p[0], p[1], p[2], p[3], mOutlinePaint);
        canvas.drawLine(p[2], p[3], p[4], p[5], mOutlinePaint);
        canvas.drawLine(p[4], p[5], p[6], p[7], mOutlinePaint);
        canvas.drawLine(p[0], p[1], p[6], p[7], mOutlinePaint);

        canvas.drawCircle(p[0], p[1], mCropCornerHandleRadius, mOutlinePaint);
        canvas.drawCircle(p[2], p[3], mCropCornerHandleRadius, mOutlinePaint);
        canvas.drawCircle(p[4], p[5], mCropCornerHandleRadius, mOutlinePaint);
        canvas.drawCircle(p[6], p[7], mCropCornerHandleRadius, mOutlinePaint);

        float x = (p[0] + p[2]) / 2;
        float y = (p[1] + p[3]) / 2;
        canvas.drawCircle(x, y, mCropEdgeHandleRadius, mOutlinePaint);
        x = (p[2] + p[4]) / 2;
        y = (p[3] + p[5]) / 2;
        canvas.drawCircle(x, y, mCropEdgeHandleRadius, mOutlinePaint);
        x = (p[4] + p[6]) / 2;
        y = (p[5] + p[7]) / 2;
        canvas.drawCircle(x, y, mCropEdgeHandleRadius, mOutlinePaint);
        x = (p[0] + p[6]) / 2;
        y = (p[1] + p[7]) / 2;
        canvas.drawCircle(x, y, mCropEdgeHandleRadius, mOutlinePaint);


    }


    // Determines which edges are hit by touching at (x, y).
    public int getHit(float x, float y, float scale) {
        // convert hysteresis to imagespace
        final float hysteresis = mHysteresis / scale;
        return mTrapezoid.getHit(x, y, hysteresis);
    }


    // Handles motion (dx, dy) in screen space.
    // The "edge" parameter specifies which edges the user is dragging.
    @Override
    public void handleMotion(int edge, float dx, float dy) {
        if (edge == GROW_NONE) {
            return;
        } else if (edge == MOVE) {
            mTrapezoid.moveBy(dx, dy);
        } else {
            mTrapezoid.growBy(edge, dx, dy);
        }
        mDrawRect = computeLayout();
    }

    /**
     * @return cropping rectangle in image space.
     */
    public Rect getCropRect() {
        return mTrapezoid.getBoundingRect();
    }

    public float[] getTrapezoid() {
        return mTrapezoid.getPoints();
    }

    public Rect getPerspectiveCorrectedBoundingRect() {
        return mTrapezoid.getPerspectiveCorrectedBoundingRect();
    }

    // Maps the cropping rectangle from image space to screen space.
    private Rect computeLayout() {
        return mTrapezoid.getBoundingRect(getMatrix());
    }

    @Override
    public Matrix getMatrix() {
        return mMatrix;
    }

    @Override
    public Rect getDrawRect() {
        return mDrawRect;
    }

    @Override
    public float centerY() {
        return getCropRect().centerY();
    }

    @Override
    public float centerX() {
        return getCropRect().centerX();
    }
}
