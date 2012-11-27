/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

/**
 *
 * @author crumb
 */
public class Util {

    public static double battleFieldWidth;
    public static double battleFieldHeight;
    public static double runnableWidth;
    public static double runnableHeight;
    public static double tankWidth;
    public static double tankHeight;
    public static double gunCoolingRate;

    public static void init(double battleFieldWidth, double battleFieldHeight, double tankWidth, double tankHeight, double gunCoolingRate) {
        Util.battleFieldWidth = battleFieldWidth;
        Util.battleFieldHeight = battleFieldHeight;
        Util.tankWidth = tankWidth;
        Util.tankHeight = tankWidth;
        Util.gunCoolingRate = gunCoolingRate;
        Util.runnableWidth = battleFieldWidth - tankWidth;
        Util.runnableHeight = battleFieldHeight - tankHeight;
    }
    public static long NOW;
    public static double calcX(double radians, double distance) {
        return Math.sin(radians) * distance;
    }

    public static double calcY(double radians, double distance) {
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
        if (power > 3.0) {
            return 0.0;
        }
        return power;
    }

    public static long gunTurnSpeed(double degree) {
        return (long) Math.ceil(degree / 20);
    }

    public static double turnSpeed(double velocity) {
        return 10 - 0.75 * Math.abs(velocity);
    }

    public static double calcRoughRunTime(double distance, double velocity) {
        velocity = (velocity == 0.0) ? 0.0000000000001 : 1;
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
        xmin = (xmin<Util.tankWidth)?Util.tankWidth:xmin;
        double xmax = center.x + extent;
        xmax = (xmax>Util.runnableWidth)?Util.runnableWidth:xmax;
        double ymin = center.x - extent;
        ymin = (ymin<Util.tankHeight)?Util.tankHeight:ymin;
        double ymax = center.x + extent;
        ymax = (ymax>Util.runnableHeight)?Util.runnableHeight:ymax;

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
}
