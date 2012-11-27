package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import robocode.*;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html


/**
 * Silver - a robot by (your name here)
 */
public class Test extends AdvancedRobot {
static class Util {

    static double battleFieldWidth;
    static double battleFieldHeight;

    public static void init(double battleFieldWidth, double battleFieldHeight) {
        Util.battleFieldWidth = battleFieldWidth;
        Util.battleFieldHeight = battleFieldHeight;
    }

    public static double calcX(double radians, double distance) {
        return Math.sin(radians) * distance;
    }

    public static double calcY(double radians, double distance) {
        return Math.cos(radians) * distance;
    }

    public static double calcD(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static double calcT(double x1, double y1, double x2, double y2) {
        if (y2 > y1) {
            return Math.toDegrees(Math.atan((x1 - x2) / (y1 - y2)));
        } else {
            return Math.toDegrees(Math.atan((x1 - x2) / (y1 - y2))) - 180;
        }
    }

    public static double bultSpeed(double power) {
        return (20 - 3 * power);
    }

    public static long gunTurnSpeed(double degree) {
        return (long) Math.ceil(degree / 20);
    }
}

static class Point {

    double x;
    double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point() {
    }

    public Point add(Point p) {
        this.x += p.x;
        this.y += p.y;
        return this;
    }

    public void limit(double limitX, double limitY) {
        if (x < 0) {
            x = 0;
        } else if (x > limitX) {
            x = limitX;
        }
        if (y < 0) {
            y = 0;
        } else if (y > limitY) {
            y = limitY;
        }
    }
}

static class Robot extends Point {
    String name;
    double distance;
    public Robot(AdvancedRobot my, ScannedRobotEvent e) {
        double bearingRadians = e.getBearingRadians() + my.getHeadingRadians();
        this.name = e.getName();
        this.distance = e.getDistance();
        this.x = Util.calcX(bearingRadians, distance) + my.getX();
        this.y = Util.calcY(bearingRadians, distance) + my.getY();
    }

    public Robot() {
        this.name = "";
    }
 
}
 
    Condition eachTickTimer = new Condition("eachTickTimer",10) {
        @Override
        public boolean test() {
            return true;
        }
    };


    @Override
    public void run() {
        setColors(new Color(255, 255, 255), new Color(0, 0, 0), new Color(200, 200, 50)); // body,gun,radar
        Util.init(getBattleFieldWidth(), getBattleFieldHeight());
        //     setMaxVelocity(8);
//     setMaxTurnRate(10);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

//        this.setTurnRadarRight(10000000);
        execute();

        addCustomEvent(this.eachTickTimer);
        this.turnRight(calcAbsTurn(90));
//        while(true) {
//            ahead(300);
//            back(300);
//        }
        setMaxVelocity(6);
        setTurnRight(100000000);
        while (true) {
            ahead(100000000);
        }
    }

    double calcAbsTurn(double n) {
        double target = (n - getHeading()) % 360;
        if (target > 180) {
            target = target - 360;
        } else if (target < -180) {
            target = target + 360;
        }
        return target;
    }


    Robot calcAbsRobot(ScannedRobotEvent e) {
        return new Robot(this, e);
    }

    Robot enemy = null;
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        enemy = calcAbsRobot(e);
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        back(300);
    }
    @Override
    public void onCustomEvent(CustomEvent e) {
        if (e.getCondition().equals(this.eachTickTimer) ) {
//            System.err.println(getTime() + " : v:" + getVelocity() + " (" + getX() + " , " + getY() +")");
//            if ( getTime() == 20 ) {
//                setTurnRight(90);
//                execute();
//            }
            if ( (getTime() % 100) == 0 ) {
                int max = (int)(Math.random()*5+4);
                setMaxVelocity(max);
                System.out.println(getTime() + " : v:" + getVelocity() + " (" + max +")");                
                execute();
            }
        }
    }

    
}