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
        delta.headingRadians = Util.calcTurnRadians(prev.headingRadians,headingRadians);
        delta.velocity = velocity - prev.velocity;
    }
    
    public void setDelta(MovingPoint delta) {
        this.delta = delta;
    }
    public boolean inertia(long deltaTime) {
        RobotPoint backup = new RobotPoint(this);
        super.inertia(deltaTime);
        if ( isLimit()) {
            this.set(backup);
            return false;
        }
        return true;
    }
    public boolean prospectNext() {
        if ( delta == null ) {
            return inertia(1);
        }else {
            RobotPoint backup = new RobotPoint(this);
            if (delta.headingRadians != 0) {
                double deltaHeadingRadians = delta.headingRadians / delta.time;
                if ( Math.abs(deltaHeadingRadians)>Util.turnSpeedRadians(velocity) ) {
                    deltaHeadingRadians = Util.turnSpeedRadians(velocity);
                    if ( delta.headingRadians < 0 ) {
                        deltaHeadingRadians *= -1;
                    }
                }
                headingRadians += deltaHeadingRadians;
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
            add(deltaByTurn);
            time +=1;
//@@@
//            if ( isLimit()) {
//                this.set(backup);
//                return false;
//            }
            return true;
        }
    }

}