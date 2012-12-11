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
    static public final int ROLE_DROID   = 1;
    static public final int ROLE_ROBOT   = 2;
    static public final int ROLE_LEADER  = 3;


    public String name = "";
    public double distance;
    public double bearing;
    public double bearingRadians;
    public double energy;
    public boolean scanned;
    public int role;
    public int aimType = MoveType.TYPE_ACCURATE1;
//
    public Map<Integer,MoveType> aimTypeMap = new HashMap();

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
        this.scanned = true;
        this.role   = 0;
        this.aimType = aimType; // TODO: move to static map
        this.changeAimType(this.aimType);
    }
    public Enemy(MovingPoint my, ScannedRobotEvent e) {
        this(my, e,MoveType.TYPE_ACCURATE1);
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
        this.scanned = in.scanned;
        this.role = in.role;
        this.aimType = in.aimType;
        this.aimTypeMap = new HashMap(in.aimTypeMap);
    }

    public Enemy() {
        changeAimType(MoveType.TYPE_ACCURATE1);
    }
    public MoveType getAimType(int type) {
        return this.aimTypeMap.get(type);
    }
    public MoveType getAimType() {
        return this.getAimType(this.aimType);
    }
    public void setAimType(MoveType type) {
        this.aimTypeMap.put(aimType, new MoveType(type));
    }
    // TODO: remove aimtype
    public void changeAimType(int type) {
        this.aimType = type;
        if ( getAimType(type) == null ) {
            this.aimTypeMap.put(type, new MoveType(type));
        }
    }
    public void calcPosition(MovingPoint base) {
        this.bearing = base.calcDegree(this);
        this.bearingRadians = Math.toRadians(heading);
        this.distance = base.calcDistance(this);
    }
    public boolean prospectNext(MovingPoint base) {
        if ( this.energy == 0 ) {
            return false;
        }
        boolean ret = super.prospectNext();
        calcPosition(base);
        return ret;
    }

}
