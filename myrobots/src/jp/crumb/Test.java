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

    public Robot(AdvancedRobot my, ScannedRobotEvent e) {
        this.time = e.getTime();
//      this.my = my;
//      this.e = e;
        this.bearing = e.getBearing() + my.getHeading();
        this.bearingRadians = e.getBearingRadians() + my.getHeadingRadians();
        this.name = e.getName();
        this.distance = e.getDistance();
        this.x = Util.calcX(bearingRadians, distance) + my.getX();
        this.y = Util.calcY(bearingRadians, distance) + my.getY();
        
        this.heading = e.getHeading();
        this.headingRadians = e.getHeadingRadians();
        this.velocity = e.getVelocity();
    }

    public Robot() {
        this.name = "";
    }
    long time;
    String name;
    double distance;
    double bearing;
    double bearingRadians;
    double heading;
    double headingRadians;
    double velocity;
    Point acc = new Point(0, 0);
//  ScannedRobotEvent e;
//  AdvancedRobot my;
    double prospectX;
    double prospectY;

    public Point inertia(double interval, Point base) {
        double dist = velocity * interval;
        return new Point(
                Util.calcX(headingRadians, dist),
                Util.calcY(headingRadians, dist)).add(base);

    }

    public Point inertia(double interval) {
        Point ret = inertia(interval, this);
        ret.limit(Util.battleFieldWidth, Util.battleFieldHeight);
        return ret;
    }

    public Point prospect(double interval, Point base) {
        Point ret = inertia(interval, base);
        ret.x -= acc.x * interval;
        ret.y -= acc.y * interval;
        return ret;
    }

    public Point prospect(double interval) {
        Point ret = prospect(interval, this);
        ret.limit(Util.battleFieldWidth, Util.battleFieldHeight);
        return ret;
    }
    //            System.out.println("D:("+(prevR.x+delta.x-r.x) +" , "+ (prevR.y+delta.y-r.y)+")");
//            System.out.println("DD:("+(prevR.x+delta.x-r.x - prevR.acc.x*deltaTime ) +" , "+ (prevR.y+delta.y-r.y - prevR.acc.y*deltaTime)+")");
}
    static final long RADAR_INTERVAL = 8;
    static final long THINKING_INTERVAL = 1;
    static final long VALID_ACC_INTERVAL = 2;
    static final long LIMIT_ACC_INTERVAL = 10;
    static final int SHOT_RANGE = 70;
    static final int LOCKON_RANGE = 150;
    static final int CHASE_RANGE = 250;
    static final int RADER_LOCKON_RANGE = 1000;
    static final int MODE_NORMAL = 1;
    static final int MODE_RADAR_LOCKON = 2;
    int mode = MODE_NORMAL;
    String lockon = "";
    int TOWARDS = 1;
    int TURN = 0;
    long now;
    double energy;
    double curX;
    double curY;
    double curHeadingRadians;
    double curHeading;
    double curGunHeadingRadians;
    double curGunHeading;
    double curRadarHeadingRadians;
    double curRadarHeading;
    double gunHeat;

    void current() {
        now = getTime();
        energy = getEnergy();
        curX = getX();
        curY = getY();
        curHeadingRadians = getHeadingRadians();
        curHeading = getHeading();
        curGunHeadingRadians = getGunHeadingRadians();
        curGunHeading = getGunHeading();
        curRadarHeadingRadians = getRadarHeadingRadians();
        curRadarHeading = getRadarHeading();
        gunHeat = getGunHeat();

    }
    Condition radarTimer = new Condition("radarTimer") {
        private long prev = 0;

        @Override
        public boolean test() {
            long now = getTime();
            if ( mode == MODE_NORMAL && prev + RADAR_INTERVAL < now) {
                prev = now;
                setTurnRadarRight(360);
                return true;
            } else {
                return false;
            }
        }
    };
    Condition randomRunTimer = new Condition("randomRunTimer") {
        private long prev = 0;

        @Override
        public boolean test() {
            long now = getTime();

            if (prev == 0 || prev + Math.random() * 10 < now) {
                prev = now;
                return true;
            } else {
                return false;
            }
        }
    };

    @Override
    public void run() {
        setColors(new Color(255, 255, 255), new Color(0, 0, 0), new Color(200, 200, 50)); // body,gun,radar
        energy = getEnergy();
        Util.init(getBattleFieldWidth(), getBattleFieldHeight());
        current();
        //     setMaxVelocity(8);
//     setMaxTurnRate(10);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

//        addCustomEvent(randomRunTimer);
        addCustomEvent(new RadarTurnCompleteCondition(this));
        addCustomEvent(radarTimer);
////        addCustomEvent(new GunTurnCompleteCondition(this));

//        while (true) {
//            System.out.println(this.getAllEvents().size());
//            ahead(100);
//            back(100);
//        }
        execute();
    }

    double calcAbsTurn(double n) {
        double target = (n - curHeading) % 360;
        if (target > 180) {
            target = target - 360;
        } else if (target < -180) {
            target = target + 360;
        }
        return target;
    }

    double calcAbsGunTurn(double n) {
        double target = (n - curGunHeading) % 360;
        if (target > 180) {
            target = target - 360;
        } else if (target < -180) {
            target = target + 360;
        }
        return target;
    }
    double calcAbsRadarTurn(double n) {
        double target = (n - curRadarHeading) % 360;
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

    static final int MAX_CALC = 5;
    static final int LOCKON_ANGLE = 20;
    static final int SCAN_STALE = 6;
    public void lockOnRadar(Robot lockOnTarget) {
        if (lockOnTarget == null) {
            return;
        }
        if ( (now - lockOnTarget.time) > SCAN_STALE ) {
            mode = MODE_NORMAL;
            return;
        }
        double radarTurn = calcAbsRadarTurn(lockOnTarget.bearing);
//System.out.println("radar: " + lockOnTarget.bearing +" => "+ radarTurn);
        if ( radarTurn < 0 ) {
            radarTurn -= LOCKON_ANGLE;
        }else{
            radarTurn += LOCKON_ANGLE;
        }
        this.setTurnRadarRight(radarTurn);
        lockOn(lockOnTarget);        
    }
    public void lockOn(Robot lockOnTarget) {
        if (lockOnTarget == null) {
            return;
        }
        double power;
        if (lockOnTarget.distance < SHOT_RANGE) {
            power = 3;
        } else if (lockOnTarget.distance < LOCKON_RANGE) {
            power = 2;
        } else if (lockOnTarget.distance < CHASE_RANGE) {
            power = 1;
        } else {
            power = 0.3;
        }

        double dist = lockOnTarget.distance;
        double gunTurn = 0;
        double allTime = dist / Util.bultSpeed(power);
        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            Point p = lockOnTarget.prospect(allTime);
            double bultTime = Util.calcD(curX, curY, p.x, p.y) / Util.bultSpeed(power);
            gunTurn = calcAbsGunTurn(Util.calcT(curX, curY, p.x, p.y));
            double gunTurnTime = Util.gunTurnSpeed(gunTurn);
//            System.out.println("ALL: " + bultTime + " : "+  gunTurnTime  + " : "+  allTime);
            if (Math.abs(allTime - (bultTime + gunTurnTime)) < 1) {
//                System.out.println("@@@: " + allTime);
                break;
            }
            allTime = bultTime + gunTurnTime;
        }
        setTurnGunRight(gunTurn);
//    if ( power > 0 ) {
//        if ( Math.abs(turn) < 0.01 || power == 3.0 && Math.abs(turn) < 1 ) {
//            System.out.println(now + " : TURN : " + turn);
//            fire(power);
//        }else{
//        }
//    }
    }
    Map<String, Robot> robotMap = new HashMap<>();
    long lastThinking = 0;

    public void thinking() {
        if (now - lastThinking < THINKING_INTERVAL) {
            return;
        }
        lastThinking = now;
        Robot lockOnTarget = null;
        for (Map.Entry<String, Robot> e : robotMap.entrySet()) {
            Robot r = e.getValue();
            if (lockOnTarget == null || lockOnTarget.distance > r.distance) {
                lockOnTarget = r;
            }
        }
        if ( lockOnTarget != null ) {
            lockon = lockOnTarget.name;
             if ( lockOnTarget.distance < RADER_LOCKON_RANGE ) {
                 mode = MODE_RADAR_LOCKON;
                 lockOnRadar(lockOnTarget);
             }else{
                 mode = MODE_NORMAL;
                 lockOn(lockOnTarget);        
             }
        }
//        System.out.println("MODE: " + mode);
    }
    void onRadarTurnComplete(CustomEvent e) {
        Vector<ScannedRobotEvent> list = getScannedRobotEvents();
        for ( ScannedRobotEvent sre : list ) {
            onScannedRobot(sre);
        }
        if ( mode == MODE_RADAR_LOCKON ) {
            Robot lockOnTarget = robotMap.get(lockon);
            lockOnRadar(lockOnTarget);
        }
    }
    static final double RANDOM_REVERSE = 0.1;
    static final double AHEAD_FOREVER = 100000;
    static final double TURN_LEFT = 0.1;
    static final double TURN_RIGHT = 0.1;

    void onRandomRun(CustomEvent e) {
        if (Math.random() < RANDOM_REVERSE) {
            TOWARDS *= -1;
            setAhead(TOWARDS * AHEAD_FOREVER);
        }
        double turnRemain = this.getTurnRemaining();
        if (turnRemain < 0.1) {
            if (Math.random() < TURN_RIGHT) {
                this.setTurnRight(Math.random() * 180);
            }
            if (Math.random() < TURN_LEFT) {
                this.setTurnLeft(Math.random() * 180);
            }
        }
    }

    @Override
    public void fire(double p) {
        current();
        if (gunHeat <= 0) {
            System.out.println("FIRE : " + "(" + p + ") => " + gunHeat);
            super.fire(p);
            energy -= p;
        }
        execute();
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        current();
//        back(10);
        execute();
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        current();
        TOWARDS *= -1;
        setAhead(AHEAD_FOREVER * TOWARDS);
        execute();
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        current();
        Robot r = calcAbsRobot(e);
// System.out.println(r.name+ " : "  + "("+r.x+","+ r.y+")");
        Robot prevR = robotMap.get(r.name);
        if (prevR != null) {
            long deltaTime = r.time - prevR.time;
            if (deltaTime < LIMIT_ACC_INTERVAL) {
                Point inertia = prevR.inertia(deltaTime);
//            System.out.println("D:("+(inertia.x-r.x) +" , "+ (inertia.y-r.y)+")");
//            System.out.println("DD:("+(inertia.x-r.x - prevR.acc.x*deltaTime ) +" , "+ (inertia.y-r.y - prevR.acc.y*deltaTime)+")");
                r.acc = new Point(
                        (inertia.x - r.x) / deltaTime,
                        (inertia.y - r.y) / deltaTime);
            }
        }
        robotMap.put(r.name, r);
//    System.out.println(r.name+ " : "  + "("+r.x+","+ r.y+")");
        thinking();
        execute();
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        current();
        robotMap.remove(e.getName());
        thinking();
        execute();
    }

    @Override
    public void onCustomEvent(CustomEvent e) {
        current();
        if (e.getCondition().getName().equals("randomRunTimer")) {
            onRandomRun(e);
        } else if (e.getCondition() instanceof RadarTurnCompleteCondition) {

            onRadarTurnComplete(e);
        } else if (e.getCondition() instanceof GunTurnCompleteCondition) {
//      System.out.println(now + " : " + e.getCondition().getName());
//      removeCustomEvent(e.getCondition());
            // thinking();
            // lockOn();
        }
        execute();
    }

    private static void drawRound(Graphics2D g, double x, double y, double r) {
        g.drawRoundRect((int) (x - r / 2), (int) (y - r / 2), (int) r, (int) r, (int) r + 2, (int) r + 2);
    }

    @Override
    public void onPaint(Graphics2D g) {
        System.out.println("ONPAINT");
        double x = getX();
        double y = getY();
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0, 255, 0));
        drawRound(g, x, y, SHOT_RANGE * 2);
        g.setColor(new Color(100, 255, 100));
        drawRound(g, x, y, LOCKON_RANGE * 2);
        g.setColor(new Color(200, 255, 200));
        drawRound(g, x, y, RADER_LOCKON_RANGE * 2);
        if (mode == 1) {
            g.setColor(new Color(0, 255, 0));
        } else {
            g.setColor(new Color(255, 0, 0));
        }
        g.drawString(String.format("targ: %s", lockon), (int) x - 20, (int) y- 45);
        g.drawString(String.format("heat: %2.2f", getGunHeat()), (int) x - 20, (int) y- 55);

        g.setColor(new Color(0, 255, 255));
        for (Map.Entry<String, Robot> e : robotMap.entrySet()) {
            Robot r = e.getValue();
            g.drawString(String.format("dist: %2.2f", r.distance), (int) r.x - 20, (int) r.y - 45);
            g.drawString(String.format("velo: %2.2f", r.velocity), (int) r.x - 20, (int) r.y - 55);
        }
        Robot r = robotMap.get(lockon);
        if (r != null) {
            g.setStroke(new BasicStroke(4.0f));
            g.setColor(new Color(0, 0, 255));
            Point i = r.inertia(now - r.time);
            drawRound(g, i.x, i.y, 40);
            g.setColor(new Color(255, 0, 255));
            Point p = r.prospect(now - r.time);
            drawRound(g, p.x, p.y, 40);
            g.setColor(new Color(255, 0, 0));
//      drawRound(g,r.prospect.x,r.prospect.y,10);

        }
    }
}