/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import java.util.HashMap;
import java.util.Map;
import jp.crumb.base.BulletInfo;
import robocode.Bullet;

/**
 *
 * @author crumb
 */
public class Util {

    public static double battleFieldWidth;
    public static double battleFieldHeight;
    public static double runnableMinX;
    public static double runnableMaxX;
    public static double runnableMinY;
    public static double runnableMaxY;
    public static double tankWidth;
    public static double tankHeight;
    public static double tankSize;
    public static double gunCoolingRate;
    public static double fieldFullDistance;

    public static void init(double battleFieldWidth, double battleFieldHeight, double tankWidth, double tankHeight, double gunCoolingRate) {
        Util.battleFieldWidth = battleFieldWidth;
        Util.battleFieldHeight = battleFieldHeight;
        Util.tankWidth = tankWidth;
        Util.tankHeight = tankWidth;
        Util.tankSize = new Point(Util.tankWidth,0).calcDistance(new Point(0,Util.tankHeight));
        Util.gunCoolingRate = gunCoolingRate;
        Util.runnableMinX = 17.9;
        Util.runnableMaxX = battleFieldWidth - 17.9;
        Util.runnableMinY = 17.9;
        Util.runnableMaxY = battleFieldHeight - 17.9;
        Util.fieldFullDistance = new Point(Util.battleFieldWidth,Util.battleFieldHeight).calcDistance(new Point());

    }
    public static long NOW;
    private static double calcX(double radians, double distance) {
        return Math.sin(radians) * distance;
    }

    private static double calcY(double radians, double distance) {
        return Math.cos(radians) * distance;
    }
    public static Point calcPoint(double radians, double distance) {
        return new Point(calcX(radians,distance),calcY(radians,distance));
    }

    public static double bultSpeed(double power) {
        return (20 - 3 * power);
    }

    public static double bultPower(double speed) {
        double power = (20 - speed) / 3;
        if (power > 3.0 ){
            return 0.0;
        }else if ( power < 0.0 ) {
            return 0.0;
        }
        return power;
    }

    public static double radarTurnSpeed() {
        return 45;
    }
    public static double gunTurnSpeed() {
        return 20;
    }

    public static double turnSpeed(double velocity) {
        return 10 - 0.75 * Math.abs(velocity);
//        return 10 - 0.95 * Math.abs(velocity);
    }

    public static double calcRoughRunTime(double distance, double velocity) {
        velocity = Math.abs(velocity);
        velocity = (velocity == 0.0) ? 0.0000000000001 : velocity;
        return distance / velocity;
    }

    public static double getRandom(double min, double max) {
        double range = max - min;
        if (range < 0) {
            return 0;
        }
        return Math.random() * range + min;
    }
    
    static final double BRAKING_ACC = 2;
    public static double getBrakingDistance(double velocity) {
        double v = Math.abs(velocity);
        double d = 0;
        for (; v > 0;) {
            d += v;
            v -= BRAKING_ACC;
        }
        return d;
    }
    
    public static Point getRandomPoint( Point center,double extent  ){
        double xmin = center.x - extent;
        xmin = (xmin<Util.runnableMinX)?Util.runnableMinX:xmin;
        double xmax = center.x + extent;
        xmax = (xmax>Util.runnableMaxX)?Util.runnableMaxX:xmax;
        double ymin = center.y - extent;
        ymin = (ymin<Util.runnableMinY)?Util.runnableMinY:ymin;
        double ymax = center.y + extent;
        ymax = (ymax>Util.runnableMaxY)?Util.runnableMaxY:ymax;

        return new Point(
                Util.getRandom(xmin, xmax),
                Util.getRandom(ymin, ymax)
                );
    }
    public static double calcPointToLineRange(Point base,Point target,double radians){
        return Math.abs(Math.sin(
                radians-base.calcRadians(target)
                ) * base.calcDistance(target));
    }
    public static double calcPointToLineDistance(Point base,Point target,double radians){
        return Math.abs(Math.cos(
                radians-base.calcRadians(target)
                ) * base.calcDistance(target));
    }
    
    public static Point getGrabity(Point base,Point target,double weight,double dim){
        double distance = base.calcDistance(target);
        distance = (distance<tankSize)?tankWidth:distance;
        double radians  = base.calcRadians(target);
        double force = weight/Math.pow(distance/10,dim)*10;
        return calcPoint(radians, force);
    }
    public static double calcTurn(double src,double dst){
        double diffDegree = (dst - src) % 360;
        if (diffDegree > 180) {
            diffDegree = diffDegree - 360;
        } else if (diffDegree < -180) {
            diffDegree = diffDegree + 360;
        }
        return diffDegree;
    }
    public static double calcTurnRadians(double src,double dst){
        double diffRadians = (dst - src) % (2*Math.PI);
        if (diffRadians > Math.PI) {
            diffRadians = diffRadians - 2*Math.PI;
        } else if (diffRadians < -Math.PI) {
            diffRadians = diffRadians + 2*Math.PI;
        }
        return diffRadians;
    }

    public static <K,V> HashMap<K,V> deepCopyHashMap(Map<K,V> in , Copy<V> copy){
        HashMap<K,V> ret = new HashMap<>();
        for ( Map.Entry<K,V> e : in.entrySet() ) {
            ret.put(e.getKey(), copy.copy(e.getValue()) );
        }
        return ret;
    }
    public static Map.Entry<String,BulletInfo> calcBulletSrc(long now,Bullet bullet,Map<String,BulletInfo> list ) {
        Point dst = new Point(bullet.getX(),bullet.getY());
        double bulletRadians = bullet.getHeadingRadians();
        
        Map.Entry<String,BulletInfo> cur = null;
        double curDiffDistance = 0;
        double curDiffRadians = 0;
        for(Map.Entry<String,BulletInfo> e : list.entrySet() ) {
            String key = e.getKey();
            BulletInfo bulletInfo = e.getValue();
            double radians = bulletInfo.src.calcRadians(dst);
            double diffRadians = Math.abs(Util.calcTurnRadians(radians, bulletRadians));
            double diffDistance = Math.abs((now - bulletInfo.src.time)*bullet.getVelocity() - bulletInfo.src.calcDistance(dst));
            if ( cur == null || diffRadians < curDiffRadians &&  diffDistance < curDiffDistance ) {
                cur = e;
                curDiffRadians = diffRadians;
                curDiffDistance = diffDistance;
            }
        }
        if ( cur != null ) {
            return cur;
        }
        return null;
    }
    
}
