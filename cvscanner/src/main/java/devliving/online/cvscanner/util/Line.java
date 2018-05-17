package devliving.online.cvscanner.util;

import org.opencv.core.Point;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 1/25/17.
 */

public class Line{
    final float INIFINITE_SLOPE = Float.MAX_VALUE;
    final float THRESHOLD = 0.1f;

    public Point start, end;
    double slope = INIFINITE_SLOPE;

    public Line(Point start, Point end){
        assert start != null && end != null;

        this.start = start;
        this.end = end;
            /*if(start.x < end.x || (start.x == end.x && start.y < end.y)) {
                this.start = start;
                this.end = end;
            }
            else{
                this.start = end;
                this.end = start;
            }*/

        findSlope();
    }

    public Line(double x1, double y1, double x2, double y2){
        this(new Point(x1, y1), new Point(x2, y2));
    }

    private void findSlope(){
        if(start.x == end.x) return;

        slope = Math.abs((end.y - start.y)/(end.x - start.x));
    }

    public double length(){
        return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
    }

    public boolean isInleft(double width){
        return Math.max(start.x, end.x) < width/2.0;
    }

    public boolean isInBottom(double height){
        return Math.max(start.y, end.y) > height/2.0;
    }


    public Point intersect(Line line){
        double denominator = (start.x - end.x)*(line.start.y - line.end.y) - (line.start.x - line.end.x)*(start.y - end.y);
        if(denominator > THRESHOLD){
            double x = (((start.x*end.y - start.y*end.x)*(line.start.x - line.end.x)) - ((line.start.x*line.end.y - line.start.y*line.end.x)*(start.x - end.x)))/denominator;
            double y = (((start.x*end.y - start.y*end.x)*(line.start.y - line.end.y)) - ((line.start.x*line.end.y - line.start.y*line.end.x)*(start.y - end.y)))/denominator;

            return new Point(x, y);
        }
        return null;
    }

    public Line merge(Line line){
        final int DIFF_THRESHOLD = 40;
        final double DIFF_SLOPE = 0.176;

        if(isNearHorizontal() && line.isNearHorizontal()){
            Line fLine = this;
            Line sLine = line;
            if(start.x > line.start.x){
                fLine = line;
                sLine = this;
            }
            //double slopeDiff = Math.abs(fLine.slope - sLine.slope);
            double yDiff = Math.abs(Math.min(Math.min(fLine.start.y - sLine.start.y, fLine.start.y - sLine.end.y), Math.min(fLine.end.y - sLine.end.y, fLine.end.y - sLine.start.y)));

            if(yDiff < DIFF_THRESHOLD){
                //Log.d("SCANNER", "MERGING: yDiff: " + yDiff + ", line 1: -> start: " + fLine.start + " end: " + fLine.end
                  //      + ", line 2: -> start: " + sLine.start + " end: " + sLine.end);
                return merge(Arrays.asList(fLine.start, fLine.end, sLine.start, sLine.end), true);
            }
            //Log.d("SCANNER", "NOT MERGING: yDiff: " + yDiff + ", line 1: -> start: " + fLine.start + " end: " + fLine.end
              //      + ", line 2: -> start: " + sLine.start + " end: " + sLine.end);
        }
        else if(isNearVertical() && line.isNearVertical()){
            Line fLine = this;
            Line sLine = line;
            if(start.y > line.start.y){
                fLine = line;
                sLine = this;
            }
            //double slopeDiff = Math.abs(fLine.slope - sLine.slope);
            double xDiff = Math.abs(Math.min(Math.min(fLine.start.x - sLine.start.x, fLine.start.x - sLine.end.x), Math.min(fLine.end.x - sLine.end.x, fLine.end.x - sLine.start.x)));
            if(xDiff < DIFF_THRESHOLD){
                //Log.d("SCANNER", "MERGING: xDiff: " + xDiff + ", line 1: -> start: " + fLine.start + " end: " + fLine.end
                       // + ", line 2: -> start: " + sLine.start + " end: " + sLine.end);
                return merge(Arrays.asList(fLine.start, fLine.end, sLine.start, sLine.end), false);
            }
            //Log.d("SCANNER", "NOT MERGING: xDiff: " + xDiff + ", line 1: -> start: " + fLine.start + " end: " + fLine.end
              //      + ", line 2: -> start: " + sLine.start + " end: " + sLine.end);
        }
        return null;
    }

    private Line merge(List<Point> points, final boolean isHorizontal){
        Collections.sort(points, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return (int) (isHorizontal? o1.x - o2.x:o1.y - o2.y);
            }
        });

        return new Line(points.get(0), points.get(points.size()-1));
    }

    public boolean isNearVertical(){
        return slope == INIFINITE_SLOPE || slope > 5.671;
        //return Math.abs(start.x - end.x) < 10;
    }

    public boolean isNearHorizontal(){
        return slope < 0.176;
        //return Math.abs(start.y - end.y) < 10;
    }

    public static List<Line> joinSegments(List<Line> segments){
        Deque<Line> stack = new ArrayDeque();
        stack.push(segments.get(0));

        for(int i = 1; i < segments.size(); i++){
            Line second = segments.get(i);
            Line first = stack.peek();

            Line merged = first.merge(second);
            if(merged != null){
                stack.pop();
                stack.push(merged);
            }
            else stack.push(second);
        }

        return new ArrayList<>(stack);
    }
}