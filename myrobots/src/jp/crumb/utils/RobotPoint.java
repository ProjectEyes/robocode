/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;


/**
 *
 * @author crumb
 */
public class RobotPoint extends MovingPoint {
    public String name = "";
    public double energy;
    public MovingPoint delta;

    public RobotPoint() {
    }

    public RobotPoint(RobotPoint in) {
        this.set(in);
    }
    public void set(RobotPoint in) {
        super.set(in);
        this.name = in.name;
        this.energy = in.energy;
        if ( in.delta != null ) {
            this.delta = new MovingPoint(in.delta);
        }
    }
    
    
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
    public boolean prospectNext() {
        RobotPoint backup = new RobotPoint(this);
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
            
            // Point deltaByTurn = Util.calcPoint(headingRadians, velocity); Move to after of velocity
            double deltaVelocity = delta.velocity / delta.time;
            if ( velocity > 0 ) {
                if ( deltaVelocity > 1 ) {
                    deltaVelocity = 1;
                }else if (deltaVelocity < -2 ) {
                    deltaVelocity = -2;
                }
            }else {
                if ( deltaVelocity < -1 ) {
                    deltaVelocity = -1;
                }else if (deltaVelocity > 2 ) {
                    deltaVelocity = 2;
                }
            }
            velocity += deltaVelocity;
            velocity = (velocity > 8) ? 8 : velocity;
            velocity = (velocity < -8) ? -8 : velocity;
            Point deltaByTurn = Util.calcPoint(headingRadians, velocity);
            x += deltaByTurn.x;
            y += deltaByTurn.y;
        }
        if ( isLimit()) {
            this.set(backup);
            return false;
        }
        return true;
    }

}