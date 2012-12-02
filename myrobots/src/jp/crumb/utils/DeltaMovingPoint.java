/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;


/**
 *
 * @author crumb
 */
public class DeltaMovingPoint extends MovingPoint {
    public DeltaMovingPoint() {
    }

    public DeltaMovingPoint(DeltaMovingPoint in) {
        this.set(in);
    }
    public void set(DeltaMovingPoint in) {
        super.set(in);
        if ( in.delta != null ) {
            this.delta = new MovingPoint(in.delta);
        }
    }
    
    public MovingPoint delta;
    
    public void setPrev(DeltaMovingPoint prev) {
        delta = new DeltaMovingPoint();
        delta.x = x - prev.x;
        delta.y = y - prev.y;
        delta.time = time - prev.time;
        delta.heading = heading - prev.heading;
        delta.headingRadians = headingRadians - prev.headingRadians;
        delta.velocity = velocity - prev.velocity;
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
    
    
    public void prospectNext() {
        DeltaMovingPoint backup = new DeltaMovingPoint(this);
        if ( delta == null ) {
            inertia(1);
        }else {
            if (delta.heading != 0) {
                double turnSpeed = Util.turnSpeed(velocity);
                double turnRadians = Math.toRadians(turnSpeed);
                heading += turnSpeed;
                headingRadians += turnRadians;
            }
            Point deltaByTurn = Util.calcPoint(headingRadians, velocity);
            velocity += delta.velocity / delta.time;
            velocity = (velocity > 8) ? 8 : velocity;
            velocity = (velocity < -8) ? -8 : velocity;
            x += deltaByTurn.x;
            y += deltaByTurn.y;
        }
        if ( islimit()) {
            this.set(backup);
        }
    }

}