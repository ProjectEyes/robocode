/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import java.util.HashMap;
import java.util.Map;
import robocode.ScannedRobotEvent;


/**
 *
 * @author crumb
 */
public class Enemy extends RobotPoint {
    static public final int ROLE_DROID   = 1;
    static public final int ROLE_ROBOT   = 2;
    static public final int ROLE_LEADER  = 3;


    public boolean scanned;
    public int role;

    public Enemy(MovingPoint my, ScannedRobotEvent e) {
        super(); // default constractor
        this.time = e.getTime();
        this.name = e.getName();
        double bearingRadians = (e.getBearingRadians() + my.headingRadians)%(Math.PI*2);
        double distance = e.getDistance();
        Point d =Util.calcPoint(bearingRadians, distance).add(my);
        this.x = d.x;
        this.y = d.y;
        this.heading = e.getHeading();
        this.headingRadians = e.getHeadingRadians();
        this.velocity = e.getVelocity();
        this.energy = e.getEnergy();
        this.scanned = true;
        this.role   = 0;
    }
    public Enemy(Enemy in ) {
        this.set(in);
    }

    public void set(Enemy in) {
        super.set(in);
        this.scanned = in.scanned;
        this.role = in.role;
    }

    public Enemy() {
    }
    @Override
    public boolean prospectNext() {
        if ( this.energy == 0 ) {
            return false;
        }
        return super.prospectNext();
    }

}
