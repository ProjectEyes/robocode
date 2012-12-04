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
    public double heading;
    public double headingRadians;
    public double velocity;

    public MovingPoint() {
    }

    public MovingPoint(double x,double y,long time,double heading, double headingRadians, double velocity) {
        this.x = x;
        this.y = y;
        this.time = time;
        this.heading = heading;
        this.headingRadians = headingRadians;
        this.velocity = velocity;
    }


    public MovingPoint(MovingPoint in) {
        this.set(in);

    }

    public void set(MovingPoint in) {
        super.set(in);
        this.heading = in.heading;
        this.headingRadians = in.headingRadians;
        this.velocity = in.velocity;
    }
    
    public void inertia(double interval) {
        double dist = velocity * interval;
        this.add(Util.calcPoint(headingRadians, dist));
    }
    public boolean islimit() {
        if (    x < Util.runnableMinX ||
                x > Util.runnableMaxX ||
                y < Util.runnableMinY ||
                y > Util.runnableMaxY) {
            return true;
        }
        return false;
    }
}