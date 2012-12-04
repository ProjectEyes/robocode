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
public class Enemy extends DeltaMovingPoint {
    static public final int AIM_TYPE_INERTIA    = 1;
    static public final int AIM_TYPE_ACCERARATE = 2;
    static public final int AIM_TYPE_DIFF_ERROR = 3;
    
    public String name = "";
    public double distance;
    public double bearing;
    public double bearingRadians;
    public double energy;

    public int aimType = AIM_TYPE_ACCERARATE;
    public Map<Integer,AimType> aimTypeMap = new HashMap();

    public Enemy(MovingPoint my, ScannedRobotEvent e,int aimType) {
        super(); // default constractor
        this.time = e.getTime();
        this.bearing = (e.getBearing() + my.heading)%(360);
        this.bearingRadians = (e.getBearingRadians() + my.headingRadians)%(Math.PI*2);
        this.name = e.getName();
        this.distance = e.getDistance();
        Point d =Util.calcPoint(bearingRadians, distance).add(my);
        this.x = d.x;
        this.y = d.y;
        this.heading = e.getHeading();
        this.headingRadians = e.getHeadingRadians();
        this.velocity = e.getVelocity();
        this.energy = e.getEnergy();
        this.aimType = aimType;
        this.changeAimType(this.aimType);
    }
    public Enemy(MovingPoint my, ScannedRobotEvent e) {
        this(my, e,AIM_TYPE_ACCERARATE);
    }

    public Enemy(Enemy in ) {
        this.set(in);
    }

    public void set(Enemy in) {
        super.set(in);
        this.name = in.name;
        this.distance = in.distance;
        this.bearing = in.bearing;
        this.bearingRadians = in.bearingRadians;
        this.energy = in.energy;
        this.aimType = in.aimType;
        this.aimTypeMap = new HashMap(in.aimTypeMap);
    }

    public Enemy() {
        changeAimType(AIM_TYPE_ACCERARATE);
    }
    public AimType getAimType(int type) {
        return this.aimTypeMap.get(type);
    }
    public AimType getAimType() {
        return this.getAimType(this.aimType);
    }
    public void setAimType(AimType type) {
        this.aimTypeMap.put(aimType, new AimType(type));
    }
    public void changeAimType(int type) {
        this.aimType = type;
        if ( getAimType(type) == null ) {
            this.aimTypeMap.put(type, new AimType());
        }
    }
    public void calcPosition(MovingPoint base) {
        this.bearing = base.calcDegree(this);
        this.bearingRadians = Math.toRadians(heading);
        this.distance = base.calcDistance(this);
    }
    public void prospectNext(MovingPoint base) {
        if ( this.energy == 0 ) {
            return;
        }
        super.prospectNext();
        calcPosition(base);
    }

}
