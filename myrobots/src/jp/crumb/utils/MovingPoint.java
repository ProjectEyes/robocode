/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;


/**
 *
 * @author crumb
 */
public class MovingPoint extends TimedPoint {
    public double headingRadians;
    public double velocity;

    public MovingPoint() {
    }

    public MovingPoint(double x,double y,long time,double headingRadians, double velocity) {
        this.timeStamp = time;
        this.x = x;
        this.y = y;
        this.time = time;
        this.headingRadians = headingRadians;
        this.velocity = velocity;
    }


    public MovingPoint(MovingPoint in) {
        this.set(in);
    }

    public MovingPoint diff(MovingPoint p) {
        super.diff(p);
        this.headingRadians -= p.headingRadians;
        this.velocity       -= p.velocity;
        return this;
    }
    public MovingPoint add(MovingPoint p) {
        super.add(p);
        this.headingRadians += p.headingRadians;
        this.velocity       += p.velocity;
        return this;
    }
    @Override
    public MovingPoint prod(double q) {
        super.prod(q);
        this.headingRadians *= q;
        this.velocity       *= q;
        return this;
    }    
    @Override
    public MovingPoint quot(double q) {
        super.quot(q);
        this.headingRadians /= q;
        this.velocity       /= q;
        return this;
    }    
    
    public void set(MovingPoint in) {
        super.set(in);
        this.headingRadians = in.headingRadians;
        this.velocity = in.velocity;
    }
    
    public MovingPoint inertia(double interval) {
        double dist = velocity * interval;
        this.add(Util.calcPoint(headingRadians, dist));
        time += Math.ceil(interval);
        return this;
    }
    public boolean isLimit() {
        if (    x < Util.runnableMinX ||
                x > Util.runnableMaxX ||
                y < Util.runnableMinY ||
                y > Util.runnableMaxY) {
            return true;
        }
        return false;
    }
    public boolean isOutOfField() {
        if (    x < 0 ||
                x > Util.battleFieldWidth ||
                y < 0 ||
                y > Util.battleFieldHeight) {
            return true;
        }
        return false;
    }
    @Override
    public String toString() {
        return String.format("t(%d/%d): d[%2.2f] dr[%2.2f] p(%2.2f,%2.2f) v:%2.2f", time,timeStamp,Math.toDegrees(headingRadians),headingRadians,x, y,velocity);
    }
}