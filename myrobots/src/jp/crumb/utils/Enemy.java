/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Util;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;


/**
 *
 * @author crumb
 */
public class Enemy extends MovingPoint {

    public String name = "";
    public double distance;
    public double bearing;
    public double bearingRadians;
    public double energy;


    public Enemy(AdvancedRobot my, ScannedRobotEvent e) {
        super(); // default constractor
        this.time = e.getTime();
        this.bearing = e.getBearing() + my.getHeading();
        this.bearingRadians = e.getBearingRadians() + my.getHeadingRadians();
        this.name = e.getName();
        this.distance = e.getDistance();
        this.x = Util.calcX(bearingRadians, distance) + my.getX();
        this.y = Util.calcY(bearingRadians, distance) + my.getY();
        this.heading = e.getHeading();
        this.headingRadians = e.getHeadingRadians();
        this.velocity = e.getVelocity();
        this.energy = e.getEnergy();
    }

    public Enemy(Enemy in ) {
        super(in);
        this.name = in.name;
        this.distance = in.distance;
        this.bearing = in.bearing;
        this.bearingRadians = in.bearingRadians;
        this.energy = in.energy;
    }

    public Enemy() {
    }

}
