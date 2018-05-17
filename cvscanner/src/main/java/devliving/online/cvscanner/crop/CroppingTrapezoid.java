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

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import static devliving.online.cvscanner.crop.HighLightView.GROW_BOTTOM_EDGE;
import static devliving.online.cvscanner.crop.HighLightView.GROW_LEFT_EDGE;
import static devliving.online.cvscanner.crop.HighLightView.GROW_NONE;
import static devliving.online.cvscanner.crop.HighLightView.GROW_RIGHT_EDGE;
import static devliving.online.cvscanner.crop.HighLightView.GROW_TOP_EDGE;
import static devliving.online.cvscanner.crop.HighLightView.MOVE;

/**
 * Created by renard on 26/02/15.
 */
public class CroppingTrapezoid {

    private final static String LOG_TAG = CroppingTrapezoid.class.getSimpleName();
    private final float[] mPoints = new float[8];
    private final float[] mMappedPoints = new float[8];
    private final Rect mImageRect;

    public CroppingTrapezoid(RectF cropRect, Rect imageRect) {
        mImageRect = new Rect(imageRect);
        mPoints[0] = cropRect.left;
        mPoints[1] = cropRect.top;
        mPoints[2] = cropRect.right;
        mPoints[3] = cropRect.top;
        mPoints[4] = cropRect.right;
        mPoints[5] = cropRect.bottom;
        mPoints[6] = cropRect.left;
        mPoints[7] = cropRect.bottom;
    }


    public Rect getBoundingRect() {
        return getBoundingRect(mPoints);
    }

    private float[] getBoundingPoints(Matrix matrix) {
        final Rect boundingRect = getBoundingRect(mPoints);
        float[] points = new float[4];
        points[0] = boundingRect.left;
        points[1] = boundingRect.top;
        points[2] = boundingRect.right;
        points[3] = boundingRect.bottom;
        matrix.mapPoints(points);
        return points;
    }

    public Rect getPerspectiveCorrectedBoundingRect() {
        float[] p = mMappedPoints;
        System.arraycopy(mPoints, 0, p, 0, 8);
        float w1 = p[2] - p[0];
        float w2 = p[4] - p[6];
        float h1 = p[7] - p[1];
        float h2 = p[5] - p[3];
        float diffH = w1 - w2;
        float diffV = h1 - h2;
        if (diffH < 0) {
            float scale = (Math.abs(diffH) + w2) / w2;
            h1 *= (scale);
            h2 *= (scale);
            //move top up
            p[1] = p[7] - h1;
            p[3] = p[5] - h2;
        } else {
            float scale = (diffH + w1) / w1;
            h1 *= scale;
            h2 *= scale;
            //move bottom down
            p[7] = p[1] + h1;
            p[5] = p[3] + h2;
        }
        if (diffV < 0) {
            float scale = (Math.abs(diffV) + h2) / h2;
            w1 *= scale;
            w2 *= scale;
            //move left to left
            p[0] = p[2] - w1;
            p[6] = p[4] - w2;
        } else {
            float scale = (diffV + h1) / h1;
            w1 *= scale;
            w2 *= scale;
            //move right to right
            p[2] = p[0] + w1;
            p[4] = p[6] + w2;
        }
        return getBoundingRect(mMappedPoints);
    }

    public Rect getBoundingRect(Matrix matrix) {
        float[] points = getBoundingPoints(matrix);
        int left = (int) Math.min(points[0], points[2]);
        int right = (int) Math.max(points[0], points[2]);
        int top = (int) Math.min(points[1], points[3]);
        int bottom = (int) Math.max(points[1], points[3]);

        return new Rect(left, top, right, bottom);
    }

    public float[] getScreenPoints(Matrix matrix) {
        matrix.mapPoints(mMappedPoints, mPoints);
        return mMappedPoints;
    }

    public float[] getPoints() {
        return mPoints;
    }

    public Point getTopLeft() {
        return new Point((int) mPoints[0], (int) mPoints[1]);
    }

    public Point getTopRight() {
        return new Point((int) mPoints[2], (int) mPoints[3]);
    }

    public Point getBottomRight() {
        return new Point((int) mPoints[4], (int) mPoints[5]);
    }

    public Point getBottomLeft() {
        return new Point((int) mPoints[6], (int) mPoints[7]);
    }

    // moves the cropping trapezoid by (dx, dy) in image space.
    public void moveBy(float dx, float dy) {
        final Rect boundingRect = getBoundingRect();
        dx = capDx(dx, boundingRect);
        dy = capDy(dy, boundingRect);

        for (int i = 0; i < 8; i += 2) {
            mPoints[i] += dx;
            mPoints[i + 1] += dy;
        }
        capPoints(GROW_NONE);
    }

    private float capDy(float dy, Rect boundingRect) {
        if ((boundingRect.bottom + dy) >= mImageRect.bottom) {
            dy = mImageRect.bottom-boundingRect.bottom;
        } else if ((boundingRect.top + dy) <= mImageRect.top) {
            dy = mImageRect.top-boundingRect.top;
        }
        return dy;
    }

    private float capDx(float dx, Rect boundingRect) {
        if ((boundingRect.right + dx) >= mImageRect.right) {
            dx = mImageRect.right-boundingRect.right;
        } else if (mImageRect.left >= (boundingRect.left + dx)) {
            dx = mImageRect.left-boundingRect.left;
        }
        return dx;
    }


    // Grows the cropping trapezoid by (dx, dy) in image space.
    public void growBy(int edge, float dx, float dy) {
//        final Rect boundingRect = getBoundingRect();
//        dx = capDx(dx, boundingRect);
//        dy = capDy(dy, boundingRect);


        if ((GROW_LEFT_EDGE | GROW_TOP_EDGE) == edge) {
            mPoints[0] += dx;
            mPoints[1] += dy;
            capPoints(edge);
            return;
        }
        if ((GROW_RIGHT_EDGE | GROW_TOP_EDGE) == edge) {
            mPoints[2] += dx;
            mPoints[3] += dy;
            capPoints(edge);
            return;
        }
        if ((GROW_RIGHT_EDGE | GROW_BOTTOM_EDGE) == edge) {
            mPoints[4] += dx;
            mPoints[5] += dy;
            capPoints(edge);
            return;
        }
        if ((GROW_LEFT_EDGE | GROW_BOTTOM_EDGE) == edge) {
            mPoints[6] += dx;
            mPoints[7] += dy;
            capPoints(edge);
            return;
        }
        if ((GROW_LEFT_EDGE) == edge) {
            mPoints[0] += dx;
            mPoints[1] += dy;
            mPoints[6] += dx;
            mPoints[7] += dy;
        }
        if ((GROW_RIGHT_EDGE) == edge) {
            mPoints[2] += dx;
            mPoints[3] += dy;
            mPoints[4] += dx;
            mPoints[5] += dy;
        }
        if ((GROW_TOP_EDGE) == edge) {
            mPoints[0] += dx;
            mPoints[1] += dy;
            mPoints[2] += dx;
            mPoints[3] += dy;
        }
        if ((GROW_BOTTOM_EDGE) == edge) {
            mPoints[4] += dx;
            mPoints[5] += dy;
            mPoints[6] += dx;
            mPoints[7] += dy;
        }


        capPoints(edge);
    }


    public int getHit(float x, float y, float hysteresis) {
        int retval = GROW_NONE;

        final boolean touchesTopLeft = calculateDistanceToPoint(mPoints[0], mPoints[1], x, y) <= hysteresis;
        if (touchesTopLeft) {
            Log.i(LOG_TAG, "top left");
            return GROW_LEFT_EDGE | GROW_TOP_EDGE;
        }
        final boolean touchesTopRight = calculateDistanceToPoint(mPoints[2], mPoints[3], x, y) <= hysteresis;
        if (touchesTopRight) {
            Log.i(LOG_TAG, "top right");
            return GROW_RIGHT_EDGE | GROW_TOP_EDGE;
        }
        final boolean touchesBottomRight = calculateDistanceToPoint(mPoints[4], mPoints[5], x, y) <= hysteresis;
        if (touchesBottomRight) {
            Log.i(LOG_TAG, "bottom right");
            return GROW_RIGHT_EDGE | GROW_BOTTOM_EDGE;
        }
        final boolean touchesBottomLeft = calculateDistanceToPoint(mPoints[6], mPoints[7], x, y) <= hysteresis;
        if (touchesBottomLeft) {
            Log.i(LOG_TAG, "bottom left");
            return GROW_LEFT_EDGE | GROW_BOTTOM_EDGE;
        }

        final double topDistance = calculateDistanceToLine(mPoints[0], mPoints[1], mPoints[2], mPoints[3], x, y);
        if (topDistance <= hysteresis) {
            Log.i(LOG_TAG, "top");
            retval |= GROW_TOP_EDGE;
        }
        final double rightDistance = calculateDistanceToLine(mPoints[2], mPoints[3], mPoints[4], mPoints[5], x, y);
        if (rightDistance <= hysteresis) {
            Log.i(LOG_TAG, "right");
            retval |= GROW_RIGHT_EDGE;
        }
        final double bottomDistance = calculateDistanceToLine(mPoints[6], mPoints[7], mPoints[4], mPoints[5], x, y);
        if (bottomDistance <= hysteresis) {
            Log.i(LOG_TAG, "bottom");
            retval |= GROW_BOTTOM_EDGE;
        }
        final double leftDistance = calculateDistanceToLine(mPoints[0], mPoints[1], mPoints[6], mPoints[7], x, y);
        if (leftDistance <= hysteresis) {
            Log.i(LOG_TAG, "left");
            retval |= GROW_LEFT_EDGE;
        }

        // Not near any edge but maybe inside the trapezoid
        if (retval == GROW_NONE) {
            //check if it is inside the trapezoid
            float xCross = calculateXCrossing(mPoints[2], mPoints[3], mPoints[4], mPoints[5], x, y);
            if (xCross < (x - hysteresis)) {
                //point is outside to right of trapezoid
                return retval;
            }
            xCross = calculateXCrossing(mPoints[0], mPoints[1], mPoints[6], mPoints[7], x, y);
            if (xCross > (x + hysteresis)) {
                //point is outside to left of trapezoid
                return retval;
            }
            float yCross = calculateYCrossing(mPoints[0], mPoints[1], mPoints[2], mPoints[3], x, y);
            if (yCross > (y + hysteresis)) {
                //point is outside to the top of trapezoid
                return retval;
            }
            yCross = calculateYCrossing(mPoints[4], mPoints[5], mPoints[6], mPoints[7], x, y);
            if (yCross < (y - hysteresis)) {
                //point is outside to the bottom of trapezoid
                return retval;
            }
            Log.i(LOG_TAG, "move");
            retval = MOVE;
        }

        return retval;
    }

    private Rect getBoundingRect(float[] p) {
        //return new Rect(Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom));

        int left = (int) Math.min(p[0], p[6]);
        int right = (int) Math.max(p[2], p[4]);
        int top = (int) Math.min(p[1], p[3]);
        int bottom = (int) Math.max(p[5], p[7]);

        return new Rect(left, top, right, bottom);
    }

    private void capPoints(final int edge) {
        mPoints[0] = Math.max(0, mPoints[0]); //left
        mPoints[1] = Math.max(0, mPoints[1]); //top
        mPoints[2] = Math.min(mImageRect.right, mPoints[2]); //right
        mPoints[3] = Math.max(0, mPoints[3]); //top
        mPoints[4] = Math.min(mImageRect.right, mPoints[4]); //right
        mPoints[5] = Math.min(mImageRect.bottom, mPoints[5]); //bottom
        mPoints[6] = Math.max(0, mPoints[6]); //left
        mPoints[7] = Math.min(mImageRect.bottom, mPoints[7]); //bottom
        //lef top always to left of right top
        final float cap = 50f;
        final float leftBound = Math.max(mPoints[0], mPoints[6]) + cap;
        final float rightBound = Math.min(mPoints[2], mPoints[4]) - cap;
        final float topBound = Math.max(mPoints[1], mPoints[3]) + cap;
        final float bottomBound = Math.min(mPoints[7], mPoints[5]) - cap;

        if ((GROW_LEFT_EDGE & edge) > 0) {
            mPoints[0] = Math.min(mPoints[0], rightBound);
            mPoints[6] = Math.min(mPoints[6], rightBound);
        }
        if ((GROW_RIGHT_EDGE & edge) > 0) {
            mPoints[2] = Math.max(mPoints[2], leftBound);
            mPoints[4] = Math.max(mPoints[4], leftBound);
        }
        if ((GROW_TOP_EDGE & edge) > 0) {
            mPoints[1] = Math.min(mPoints[1], bottomBound);
            mPoints[3] = Math.min(mPoints[3], bottomBound);
        }
        if ((GROW_BOTTOM_EDGE & edge) > 0) {
            mPoints[7] = Math.max(mPoints[7], topBound);
            mPoints[5] = Math.max(mPoints[5], topBound);
        }


    }

    private double calculateDistanceToLine(float x1, float y1, float x2, float y2, float x, float y) {
        if ((x > x1 && x < x2) || (y > y1 && y < y2)) {
            if (x1 == x2) {
                return Math.abs(x1 - x);
            }
            float mTop = getRiseOfLine(x1, y1, x2, y2);
            float nTop = getYCrossingOfLine(x1, y1, mTop);
            if (mTop == 0) {
                return Math.abs(nTop - y);
            }
            float mCross = -1 / mTop;
            float nCross = getYCrossingOfLine(x, y, mCross);
            float xCross = (nTop - nCross) / (mCross - mTop);
            float yCross = mCross * xCross + nCross;
            return calculateDistanceToPoint(xCross, yCross, x, y);
        } else {
            return Double.MAX_VALUE;
        }
    }

    private double calculateDistanceToPoint(float x1, float y1, float x, float y) {
        return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
    }

    private float calculateXCrossing(float x1, float y1, float x2, float y2, float x, float y) {
        if (x1 == x2) {
            return x1;
        }
        float mTop = getRiseOfLine(x1, y1, x2, y2);
        float nTop = getYCrossingOfLine(x1, y1, mTop);
        float xCross = getXOnLine(y, mTop, nTop);
        return xCross;
    }

    private float getXOnLine(float y, float mTop, float nTop) {
        return (y - nTop) / mTop;
    }

    private float calculateYCrossing(float x1, float y1, float x2, float y2, float x, float y) {
        if (y1 == y2) {
            return y1;
        }
        float mTop = getRiseOfLine(x1, y1, x2, y2);
        float nTop = getYCrossingOfLine(x1, y1, mTop);
        float xCross = getXOnLine(y, mTop, nTop);
        return mTop * xCross + nTop;
    }

    private float getYCrossingOfLine(float x1, float y1, float mTop) {
        return y1 - mTop * x1;
    }

    private float getRiseOfLine(float x1, float y1, float x2, float y2) {
        return (y1 - y2) / (x1 - x2);
    }

}
