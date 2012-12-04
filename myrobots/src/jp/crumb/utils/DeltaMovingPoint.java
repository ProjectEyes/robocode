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
    
    public void setPrev(MovingPoint prev) {
        delta = new MovingPoint();
        delta.x = x - prev.x;
        delta.y = y - prev.y;
        delta.time = time - prev.time;
        delta.heading = heading - prev.heading;
        delta.headingRadians = headingRadians - prev.headingRadians;
        delta.velocity = velocity - prev.velocity;
    }
    public void setDelta(MovingPoint delta) {
        this.delta = delta;
    }    
    public void prospectNext() {
        DeltaMovingPoint backup = new DeltaMovingPoint(this);
        if ( delta == null ) {
            inertia(1);
        }else {
            if (delta.heading != 0) {
                double deltaHeading = delta.heading / delta.time;
                deltaHeading = (Math.abs(deltaHeading)<Util.turnSpeed(velocity))?deltaHeading:Util.turnSpeed(velocity)*Math.abs(deltaHeading)/deltaHeading;
                double deltaRadians = Math.toRadians(deltaHeading);
                heading += deltaHeading;
                headingRadians += deltaRadians;
            }
            
            Point deltaByTurn = Util.calcPoint(headingRadians, velocity);
            double deltaVelocity = delta.velocity / delta.time;
            if ( deltaVelocity > 1 ) {
                deltaVelocity = 1;
            }else if (deltaVelocity < -2 ) {
                deltaVelocity = -2;
            }
            velocity += deltaVelocity;
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