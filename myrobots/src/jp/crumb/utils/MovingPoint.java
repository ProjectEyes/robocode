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
}