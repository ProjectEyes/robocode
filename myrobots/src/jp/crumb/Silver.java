package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import robocode.*;


/**
 * Silver - a robot by (your name here)
 */
public class Silver extends AdvancedRobot {
static class Util {

    static double battleFieldWidth;
    static double battleFieldHeight;
    static double runnableWidth;
    static double runnableHeight;
    static double tankWidth;
    static double tankHeight;    
    static double gunCoolingRate;
    
    public static void init(double battleFieldWidth, double battleFieldHeight,double tankWidth, double tankHeight, double gunCoolingRate) {
        Util.battleFieldWidth = battleFieldWidth;
        Util.battleFieldHeight = battleFieldHeight;
        Util.tankWidth = tankWidth;
        Util.tankHeight = tankWidth;
        Util.gunCoolingRate = gunCoolingRate;
        Util.runnableWidth = battleFieldWidth - tankWidth;
        Util.runnableHeight =battleFieldHeight - tankHeight;
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
        if ( y2 == y1 ) {
            return (x2 >x1)?90:-90;
        }else if (y2 > y1) {
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
    public static double turnSpeed(double velocity){
        return 10 - 0.75 * Math.abs(velocity);
    }
    public static double calcRoughRunTime(double distance,double velocity) {
        velocity=(velocity==0.0)?0.0000000000001:1;
        return distance/velocity;
    }
    public static double getRandom(double min,double max){
        double range = max - min;
        if ( range < 0 ) {
            return 0;
        }
        return Math.random() * range + min;
    }
    static final double BRAKING_ACC = 2;
    public static double getBrakingDistance(double velocity) {
        double v = Math.abs(velocity);
        double d = 0;
        for ( ;v > 0; ) {
            d += v;
            v -= BRAKING_ACC;
        }
        return d;
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
    
    @Override
    public String toString(){
        return String.format("(%2.2f,%2.2f)", x,y);
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
    static final int RADAR_LOCKON_RANGE = 400;
    static final int MODE_NORMAL = 1;
    static final int MODE_RADAR_LOCKON = 2;
    int mode = MODE_NORMAL;
    String lockon = "";
    int TOWARDS = 1;
    int TURN = 0;
    long now;
    double energy;
    double velocity;
    double curX;
    double curY;
    double curHeadingRadians;
    double curHeading;
    double curGunHeadingRadians;
    double curGunHeading;
    double curRadarHeadingRadians;
    double curRadarHeading;
    double gunHeat;

    double curTurnRemaining;
    double curTurnRemainingRadians;       
    double curGunTurnRemaining;        
    double curGunTurnRemainingRadians;            
    double curRadarTurnRemaining;            
    double curRadarTurnRemainingRadians;
    
    void log(String format, Object... args){
        System.out.println(String.format("%3d : ",now) + String.format(format, args) );
    }
    void current() {
        long n = getTime();
        if ( now == n ) {
            return;
        }
        now = n;
        energy = getEnergy();
        velocity = getVelocity();
        curX = getX();
        curY = getY();
        curHeadingRadians = getHeadingRadians();
        curHeading = getHeading();
        curGunHeadingRadians = getGunHeadingRadians();
        curGunHeading = getGunHeading();
        curRadarHeadingRadians = getRadarHeadingRadians();
        curRadarHeading = getRadarHeading();
        gunHeat = getGunHeat();
        
        curTurnRemaining = getTurnRemaining();
        curTurnRemainingRadians = getTurnRemainingRadians();
        curGunTurnRemaining = getGunTurnRemaining();
        curGunTurnRemainingRadians = getGunTurnRemainingRadians();
        curRadarTurnRemaining = getRadarTurnRemaining();
        curRadarTurnRemainingRadians = getRadarTurnRemainingRadians();
    }

    @Override
    public void execute() {
        super.execute();
    }

    Condition eachTickTimer = new Condition("eachTickTimer",10) {
        @Override
        public boolean test() {
            current();
            return true;
        }
    };
    
    Condition radarTimer = new Condition("radarTimer",10) {
        private long prev = 0;
        @Override
        public boolean test() {
            long now = getTime();
            if ( mode == MODE_NORMAL && prev + RADAR_INTERVAL < now) {
                prev = now;
                setTurnRadarRight(405);
                return true;
            } else {
                return false;
            }
        }
    };
//    RadarTurnCompleteCondition radarTurnCompleteCondition = new RadarTurnCompleteCondition(this);
//    TurnCompleteCondition turnCompleteCondition = new TurnCompleteCondition(this);
//    MoveCompleteCondition moveCompleteCondition = new MoveCompleteCondition(this);
//    GunTurnCompleteCondition gunTurnCompleteCondition = new GunTurnCompleteCondition(this);

    void initEventPriority(){
	this.setEventPriority("ScannedRobotEvent",10);
	this.setEventPriority("HitRobotEvent",10);
	this.setEventPriority("HitWallEvent",10);
	this.setEventPriority("HitByBulletEvent",10);
	this.setEventPriority("BulletHitEvent",10);
	this.setEventPriority("BulletHitBulletEvent",10);
	this.setEventPriority("BulletMissedEvent",10);
	this.setEventPriority("RobotDeathEvent",10);
	this.setEventPriority("CustomEvent",10);
//	this.setEventPriority("SkippedTurnEvent",10);
//	this.setEventPriority("WinEvent",10);
//	this.setEventPriority("DeathEvent",10);
     }
    
    @Override
    public void run() {
        setColors(new Color(255, 255, 255), new Color(0, 0, 0), new Color(200, 200, 50)); // body,gun,radar
        energy = getEnergy();
        Util.init(
                getBattleFieldWidth(),
                getBattleFieldHeight(),
                getWidth(),
                getHeight(),
                getGunCoolingRate()
                );
        initEventPriority();
        current();
        //     setMaxVelocity(8);
//     setMaxTurnRate(10);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        
        addCustomEvent(this.radarTimer);
        addCustomEvent(this.eachTickTimer);

//        addCustomEvent(new RadarTurnCompleteCondition(this));
//        addCustomEvent(new TurnCompleteCondition(this));
//        addCustomEvent(new MoveCompleteCondition(this));
//        addCustomEvent(new GunTurnCompleteCondition(this));

//        while (true) {
//            System.out.println(this.getAllEvents().size());
//            ahead(100);
//            back(100);
//        }
        setDestination(new Point(200,200));
        execute();
    }
    Point dst;
    void setDestination(Point dst){
        this.dst = dst;
    }
    void goPoint(){
        if ( this.dst == null ) {
            return;
        }
        double bearing = Util.calcT(curX, curY, dst.x, dst.y);
        double distance = Util.calcD(curX, curY, dst.x, dst.y);
        
        if ( distance < 1 ) { // complete
            setAhead(0);
            setTurnRight(0);
            return;
        }
        if ( distance < (Util.getBrakingDistance(velocity)+velocity) ) {
            myOnNearingDestination();
        }
        double aheadTurn = calcAbsTurn(bearing);
        double backTurn  = calcAbsTurn(bearing-180);
        double runTime = Util.calcRoughRunTime(distance,velocity);
        
        double turn = 0;
        
        if ( Math.abs(aheadTurn) < Math.abs(backTurn)) { // ahead
            turn = aheadTurn;
            double turnTime = Math.abs(aheadTurn/Util.turnSpeed(velocity));
        }else { // back
            turn = backTurn;
            distance *= -1; // back
        }
        double turnTime = Math.abs(turn/Util.turnSpeed(velocity));
        if ( runTime <= turnTime ) {
            distance = 0;
        }
        setAhead(distance);
        setTurnRight(turn);
        
        checkObstracle(turn);
    }
    
    void checkObstracle(double turn){
        for (Map.Entry<String, Robot> e : robotMap.entrySet()) {
            Robot r = e.getValue();
        }                
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
    static final int SCAN_STALE = 7;
    void lockOnRadar(Robot lockOnTarget) {
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
    
    double selectPowerFromDistance(double distance) {
        if (distance < SHOT_RANGE) {
            return 3;
        } else if (distance < LOCKON_RANGE) {
            return 2;
        } else if (distance < CHASE_RANGE) {
            return 1;
        } else if (distance < RADAR_LOCKON_RANGE) {
            return 0.3;
        } else {
            return 0.0;
        }
    }
    
    Point lockOnPoint = null;
    void lockOn(Robot lockOnTarget) {
        if (lockOnTarget == null) {
            return;
        }
        double dist = lockOnTarget.distance;
        double power = selectPowerFromDistance(dist);
        double gunTurn = 0;
        double allTime = dist / Util.bultSpeed(power);
        
        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            Point prospect = lockOnTarget.prospect(allTime);
            dist = Util.calcD(curX, curY, prospect.x, prospect.y);
            power = this.selectPowerFromDistance(dist);
            double bultTime = dist / Util.bultSpeed(power);
            gunTurn = calcAbsGunTurn(Util.calcT(curX, curY, prospect.x, prospect.y));
            double gunTurnTime = Util.gunTurnSpeed(gunTurn);
//            System.out.println("ALL: " + bultTime + " : "+  gunTurnTime  + " : "+  allTime);
            if (Math.abs(allTime - (bultTime + gunTurnTime)) < 1) {
//                System.out.println("@@@: " + allTime);
                lockOnPoint = prospect;
                break;
            }
            allTime = bultTime + gunTurnTime;
        }
        setTurnGunRight(gunTurn);

        log("GUN (%2.2f) %2.2f",power,gunTurn);
        if ( power > 0 ) {
            if ( Math.abs(gunTurn) < 0.8 ) {
                fire(power);
            }
        }
    }
    Map<String, Robot> robotMap = new HashMap<>();
    long lastThinking = 0;

    static final double PREPARE_LOCK_TIME=6;
    
    void thinking() {
        if (now - lastThinking < THINKING_INTERVAL) {
            return;
        }
        lastThinking = now;
        Robot lockOnTarget = null;
        for (Map.Entry<String, Robot> e : robotMap.entrySet()) {
            Robot r = e.getValue();
            if (lockOnTarget == null || lockOnTarget.distance > r.distance) {
                if ( (now - r.time) < SCAN_STALE )
                lockOnTarget = r;
            }
        }
        if ( lockOnTarget != null ) {
            lockon = lockOnTarget.name;
             if ( lockOnTarget.distance < RADAR_LOCKON_RANGE && (gunHeat/Util.gunCoolingRate) < PREPARE_LOCK_TIME ) {
                 mode = MODE_RADAR_LOCKON;
             }else{
                 mode = MODE_NORMAL;
                 lockOn(lockOnTarget);        
             }
        }
        System.out.println("MODE: " + mode);
    }


    @Override
    public void fire(double p) {
        if (gunHeat <= 0) {
            System.out.println("FIRE : " + "(" + p + ") => " + gunHeat);
            super.fire(p);
            energy -= p;
        }
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
//        setAhead(AHEAD_FOREVER * TOWARDS);
        execute();
    }
    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        current();
        robotMap.remove(e.getName());
        thinking();
        execute();
    }

    void myOnRadarTurnComplete() {
        log("myRadarTurnComplete");
        if ( mode == MODE_RADAR_LOCKON ) {
            Robot lockOnTarget = robotMap.get(lockon);
            lockOnRadar(lockOnTarget);
        }
    }
    
    Point randomPoint(){
        int LIMIT = 200;
        int MUST  = 100;
        double xmin = curX - LIMIT;
        xmin = (xmin<Util.tankWidth)?Util.tankWidth:xmin;
        double xmax = curX + LIMIT;
        xmax = (xmax>Util.runnableWidth)?Util.runnableWidth:xmax;
        double ymin = curX - LIMIT;
        ymin = (ymin<Util.tankHeight)?Util.tankHeight:ymin;
        double ymax = curX + LIMIT;
        ymax = (ymax>Util.runnableHeight)?Util.runnableHeight:ymax;

        return new Point(
                Util.getRandom(xmin, xmax),
                Util.getRandom(ymin, ymax)
                );
    }
    void myOnNearingDestination(){
        log("myOnNearingDestination");        
        setDestination(randomPoint());        
    }
    
    void myOnCompleteMove() {
        log("myOnCompleteMove");        
        setDestination(randomPoint());
    }


    void myOnScannedRobot(ScannedRobotEvent e) {
        Robot r = calcAbsRobot(e);
        log("%15s : %s",r.name,r);
        Robot prevR = robotMap.get(r.name);
        if (prevR != null) {
            long deltaTime = r.time - prevR.time;
            if (deltaTime > 0 && deltaTime < LIMIT_ACC_INTERVAL) {
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
    }
    
    @Override
    public void onCustomEvent(CustomEvent e) {
        current();
        if (e.getCondition() instanceof RadarTurnCompleteCondition) {
        } else if (e.getCondition() instanceof GunTurnCompleteCondition) {
//      System.out.println(now + " : " + e.getCondition().getName());
//      removeCustomEvent(e.getCondition());
            // thinking();
            // lockOn();
        } else if (e.getCondition() instanceof TurnCompleteCondition) {
        } else if (e.getCondition() instanceof MoveCompleteCondition) {
        } else if (e.getCondition().equals(this.eachTickTimer) ) {
System.out.println("------"+now+"------");
            this.setInterruptible(true);
            for ( ScannedRobotEvent sre: this.getScannedRobotEvents() ) {
                this.myOnScannedRobot(sre);
            }
            thinking();
            if ( this.curRadarTurnRemaining == 0.0){
                myOnRadarTurnComplete();
            }
            if ( this.curTurnRemaining == 0.0 && this.velocity == 0.0 ){
                myOnCompleteMove();
            }
            this.goPoint();
            this.onPaint(getGraphics());

        }
        execute();
    }

    private static void drawRound(Graphics2D g, double x, double y, double r) {
        g.drawRoundRect((int) (x - r / 2), (int) (y - r / 2), (int) r, (int) r, (int) r + 2, (int) r + 2);
    }

    @Override
    public void onPaint(Graphics2D g) {
        log("ONPAINT");
        double x = getX();
        double y = getY();
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0, 255, 0));
        drawRound(g, x, y, SHOT_RANGE * 2);
        g.setColor(new Color(100, 255, 100));
        drawRound(g, x, y, LOCKON_RANGE * 2);
        g.setColor(new Color(200, 255, 200));
        drawRound(g, x, y, RADAR_LOCKON_RANGE * 2);
        if (mode == MODE_NORMAL) {
            g.setColor(new Color(0, 255, 0));
        } else {
            g.setColor(new Color(255, 0, 0));
        }
        g.drawString(String.format("( %2.2f , %2.2f )", x , y), (int) x - 20, (int) y- 45);
        g.drawString(String.format("targ: %s", lockon), (int) x - 20, (int) y- 55);
        g.drawString(String.format("heat: %2.2f", getGunHeat()), (int) x - 20, (int) y- 65);
        g.drawString(String.format("velo: %2.1f", getVelocity()), (int) x - 20, (int) y- 75);

        g.setColor(new Color(0, 255, 255));
        for (Map.Entry<String, Robot> e : robotMap.entrySet()) {
            Robot r = e.getValue();
            drawRound(g, r.x, r.y, 35);            
            g.drawString(String.format("( %2.2f , %2.2f )", r.x , r.y), (int) r.x - 20, (int) r.y- 45);
            g.drawString(String.format("dist: %2.2f", r.distance), (int) r.x - 20, (int) r.y - 55);
            g.drawString(String.format("velo: %2.2f", r.velocity), (int) r.x - 20, (int) r.y - 65);
        }
        g.setStroke(new BasicStroke(4.0f));
        if ( dst != null ) {
            g.setColor(new Color(0, 255, 0));
            drawRound(g,dst.x,dst.y,10);
        }
        Robot r = robotMap.get(lockon);
        if (r != null) {
            if ( lockOnPoint != null ) {
                g.setColor(new Color(0, 0, 255));
                drawRound(g, lockOnPoint.x, lockOnPoint.y, 35);                    
            }
//            g.setColor(new Color(0, 0, 255));
//            Point i = r.inertia(now - r.time);
//            drawRound(g, i.x, i.y, 40);
//            g.setColor(new Color(255, 0, 255));
//            Point p = r.prospect(now - r.time);
//            drawRound(g, p.x, p.y, 40);
            g.setColor(new Color(255, 0, 0));
//      drawRound(g,r.prospect.x,r.prospect.y,10);

        }
    }
}