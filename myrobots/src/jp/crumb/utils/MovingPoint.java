/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import jp.crumb.utils.Point;
import jp.crumb.utils.Util;

/**
 *
 * @author crumb
 */
public class MovingPoint extends Point {
    public long time;
    public double heading;
    public double headingRadians;
    public double velocity;

    public MovingPoint() {
    }

    public MovingPoint(MovingPoint in) {
        super(in);
        this.time = in.time;
        this.heading = in.heading;
        this.headingRadians = in.headingRadians;
        this.velocity = in.velocity;
        if ( in.delta != null ) {
            this.delta = new MovingPoint(in.delta);
        }
    }
    
    public MovingPoint delta;
    
    public void setPrev(MovingPoint prev) {
        delta = new MovingPoint();
        delta.x = x - prev.x;
        delta.y = y - prev.y;
        delta.time = time - prev.time;
        delta.heading = heading - prev.heading;
        delta.headingRadians = headingRadians - prev.headingRadians;
        delta.velocity = velocity - prev.velocity;
    }

    public void inertia(double interval) {
        double dist = velocity * interval;
        this.add(Util.calcPoint(headingRadians, dist));
    }


    public void prospectNext() {
        if ( delta == null ) {
            return;
        }
        if (delta.heading != 0) {
            double turnSpeed = Util.turnSpeed(velocity);
            double turnRadians = Math.toRadians(turnSpeed);
            heading += turnSpeed;
            headingRadians += turnRadians;
        }
        double deltaXbyTurn = Util.calcX(headingRadians, velocity);
        double deltaYbyTurn = Util.calcY(headingRadians, velocity);
        velocity += delta.velocity / delta.time;
        velocity = (velocity > 8) ? 8 : velocity;
        velocity = (velocity < -8) ? -8 : velocity;
        x += deltaXbyTurn;
        y += deltaYbyTurn;
    }

    public void prospect(double interval) {
        if ( delta == null ) {
            inertia(interval);
        } else {
            for (int i = 1; i <= interval; i++) {
                prospectNext();
            }
        }
        limit(Util.runnableWidth, Util.runnableHeight);
    }
}