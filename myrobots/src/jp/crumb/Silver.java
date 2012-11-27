package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public Point() {
    }
    public Point(Point in) {
        this.x = in.x;
        this.y = in.y;
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
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

static class Prospector extends Point {

        public Prospector() {
        }

        public Prospector(Prospector in) {
            super(in);
        }
    
}
static class Robot extends Prospector {
    long time;
    String name;
    double distance;
    double bearing;
    double bearingRadians;
    double heading;
    double headingRadians;
    double velocity;
    long deltaTime = 0;
    double deltaVelocity;
    double deltaHeading;
    double deltaHeadingRadians;
    double deltaX;
    double deltaY;

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
        public Robot(long time, String name, double x, double y,double distance, double bearing, double bearingRadians, double heading, double headingRadians, double velocity, double deltaVelocity, double deltaHeading, double deltaHeadingRadians, double deltaX, double deltaY) {
            this.time = time;
            this.name = name;
            this.x = x;
            this.y = y;        
            this.distance = distance;
            this.bearing = bearing;
            this.bearingRadians = bearingRadians;
            this.heading = heading;
            this.headingRadians = headingRadians;
            this.velocity = velocity;
            this.deltaVelocity = deltaVelocity;
            this.deltaHeading = deltaHeading;
            this.deltaHeadingRadians = deltaHeadingRadians;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

    public Robot(Robot in) {
        super(in);
        this.time = in.time;
        this.name = in.name;
        this.distance = in.distance;
        this.bearing = in.bearing;
        this.bearingRadians = in.bearingRadians;
        this.heading = in.heading;
        this.headingRadians = in.headingRadians;
        this.velocity = in.velocity;
        this.deltaVelocity = in.deltaVelocity;
        this.deltaHeading = in.deltaHeading;
        this.deltaHeadingRadians = in.deltaHeadingRadians;
        this.deltaX = in.deltaX;
        this.deltaY = in.deltaY;
    }

    public Robot() {
        this.name = "";
    }

    public void inertia(double interval ) {
        double dist = velocity * interval;
        this.add(
                new Point(
                Util.calcX(headingRadians, dist),
                Util.calcY(headingRadians, dist))
                );
    }

    public void setPrev(Robot prev) {
        deltaTime = time - prev.time;
        deltaVelocity = velocity - prev.velocity;
        deltaHeading = heading - prev.heading;
        deltaHeadingRadians = headingRadians - prev.headingRadians;
        deltaX = x - prev.x;
        deltaY = y - prev.y;
    }
    public void prospectNext(Robot base) {
        if (deltaHeading != 0) {
            double turnSpeed = Util.turnSpeed(base.velocity);
            double turnRadians = Math.toRadians(turnSpeed);
            base.heading += turnSpeed;
            base.headingRadians += turnRadians;
        }
        double turnX = Util.calcX(base.headingRadians, base.velocity);
        double turnY = Util.calcY(base.headingRadians, base.velocity);
        base.velocity += deltaVelocity/deltaTime;
        base.velocity = (base.velocity > 8) ? 8 : base.velocity;
        base.velocity = (base.velocity < -8) ? -8 : base.velocity;
        base.x += turnX;
        base.y += turnY;
    }
    public Point prospect(double interval) {
        // inertia & turn
        Robot ret = new Robot(this);
        if ( deltaTime == 0 ) {
            ret.inertia(interval);
        }else{
            for ( int i = 1 ; i <= interval; i++ ) {
                prospectNext(ret);
            }
        }
        ret.limit(Util.runnableWidth, Util.runnableHeight);
        return ret;
    }
    }
    static final long VALID_ACC_INTERVAL = 2;
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
    int others;

    double curTurnRemaining;
    double curTurnRemainingRadians;       
    double curGunTurnRemaining;        
    double curGunTurnRemainingRadians;            
    double curRadarTurnRemaining;            
    double curRadarTurnRemainingRadians;
    
    static final int LOGLV = 5;
    void log(String format, Object... args){
        System.out.println(String.format("%3d : ",now) + String.format(format, args) );
    }
    void trace(String format, Object... args){
        if ( LOGLV >= 10 ) {
            System.out.println(String.format("%3d : ",now) + String.format(format, args) );
        }
    }
    void current() {
        this.setInterruptible(true);
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
        others = getOthers();
        curTurnRemaining = getTurnRemaining();
        curTurnRemainingRadians = getTurnRemainingRadians();
        curGunTurnRemaining = getGunTurnRemaining();
        curGunTurnRemainingRadians = getGunTurnRemainingRadians();
        curRadarTurnRemaining = getRadarTurnRemaining();
        curRadarTurnRemainingRadians = getRadarTurnRemainingRadians();
    }


    Condition eachTickTimer = new Condition("eachTickTimer",10) {
        @Override
        public boolean test() {
            current();
            return true;
        }
    };
    Condition waitTimer = new Condition("waitTimer",10) {
        @Override
        public boolean test() {
            return false;
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



//
//    @Override
//    public void onScannedRobot(ScannedRobotEvent event) {
//        current();
//        System.out.println("**************");
//        myOnScannedRobot(event);
//        setInterruptible(true);
//    }
    
    
    @Override
    public void run() {
        setColors(new Color(255, 255, 255), new Color(0, 0, 0), new Color(200, 200, 50)); // body,gun,radar
        this.setBulletColor(new Color(200,255,100));
        energy = getEnergy();
        Util.init(
                getBattleFieldWidth(),
                getBattleFieldHeight(),
                getWidth(),
                getHeight(),
                getGunCoolingRate()
                );
        initEventPriority();
//     setMaxVelocity(8);
//     setMaxTurnRate(10);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        
        addCustomEvent(this.eachTickTimer);
//        setTurnRadarRight(4050);

//        waitFor(waitTimer);
//        while (true) {
//            System.out.println(this.getAllEvents().size());
//            ahead(100);
//            back(100);
//        }
//        setDestination(new Point(200,200));


    }
    Point dst;
    void setDestination(Point dst){
        this.dst = dst;
    }
    
    void antiCollision(){
        // Todo: yet
    }
    void goPoint(){
        antiCollision();
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
    static final int SCAN_STALE = 10;
    void lockOnRadar(Robot lockOnTarget) {
        if (lockOnTarget == null || (now - lockOnTarget.time) > SCAN_STALE ) {
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
    double isFire(){
        return 0;
    }
    
    boolean locking = false;
    Point lockOnPoint = null;
    void lockOn(Robot lockOnTarget) {
        if (lockOnTarget == null) {
            return;
        }
        double fire = isFire();
        // @@@
        
        double dist = lockOnTarget.distance;
        double power = selectPowerFromDistance(dist);
        double gunTurn = 0;
        double allTime = dist / Util.bultSpeed(power)+1; // 1 = next turn

        
        Robot my = new Robot(
                now, 
                "my",
                curX,
                curY,
                0, 
                0, 
                0,
                curHeading, 
                curHeadingRadians, 
                velocity, 
                0, 
                curTurnRemaining, 
                curTurnRemainingRadians, 
                0,
                0);
        my.prospectNext(my);
        
        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            Point prospect = lockOnTarget.prospect(allTime);
//            dist = Util.calcD(curX, curY, prospect.x, prospect.y);
            dist = Util.calcD(my.x, my.y, prospect.x, prospect.y);
            power = this.selectPowerFromDistance(dist);
            double bultTime = dist / Util.bultSpeed(power);
//            gunTurn = calcAbsGunTurn(Util.calcT(curX, curY, prospect.x, prospect.y));
            gunTurn = calcAbsGunTurn(Util.calcT(my.x, my.y, prospect.x, prospect.y));            
            double gunTurnTime = Util.gunTurnSpeed(gunTurn)+1; // 1 = next turn
            
            // Todo:
            if (Math.abs(allTime - (bultTime + gunTurnTime)) < 1) {
                lockOnPoint = prospect;
                break;
            }
            allTime = bultTime + gunTurnTime;
        }
        log("GUN (%2.2f) %2.2f",curGunTurnRemaining,gunTurn);
        setTurnGunRight(gunTurn);
//        if ( mode == MODE_RADAR_LOCKON ) {
//            locking = true;
//        }else{
//            locking = false;
//        }
        if ( power > 0 ) {
//            if ( locking && curGunTurnRemaining == 0.0 ) {
            if ( Math.abs(gunTurn) < 1.0 ) {
                fire(power);
            }
        }
        
    }
    
    Map<String, Robot> robotMap = new HashMap<>();

    static final double PREPARE_LOCK_TIME=6;
    
    void thinking() {
        Robot lockOnTarget = null;
        for (Map.Entry<String, Robot> e : robotMap.entrySet()) {
            Robot r = e.getValue();
            if (lockOnTarget == null || lockOnTarget.distance > r.distance) {
                if ( (now - r.time) < SCAN_STALE )
                lockOnTarget = r;
            }
        }
        mode = MODE_NORMAL;
        if ( lockOnTarget != null ) {
            lockon = lockOnTarget.name;
             if ( lockOnTarget.distance < RADAR_LOCKON_RANGE && (gunHeat/Util.gunCoolingRate) < PREPARE_LOCK_TIME ||
                     others == 1 ) {
                 mode = MODE_RADAR_LOCKON;
             }
        }
        if ( mode == MODE_NORMAL) {
            setTurnRadarRight(180);
            lockOn(lockOnTarget);
        }
        log("MODE: %d",mode);
    }

    Point aimPoint;
    Point firePoint;
    @Override
    public void fire(double p) {
        if (gunHeat <= 0) {
            log("FIRE : ( %2.2f )",p);
            mode = MODE_NORMAL;
            firePoint = new Point(curX,curY);
            aimPoint  = new Point(lockOnPoint);
            super.fire(p);
        }
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        current();
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
        trace("myRadarTurnComplete");
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
        trace("myOnNearingDestination");        
        setDestination(randomPoint());        
    }
    
    void myOnCompleteMove() {
        trace("myOnCompleteMove");        
        setDestination(randomPoint());
    }


    void myOnScannedRobot(ScannedRobotEvent e) {
        Robot r = calcAbsRobot(e);
        log("%15s : %s",r.name,r);
        Robot prevR = robotMap.get(r.name);
        if ( prevR != null && (r.time != prevR.time) && (r.time-prevR.time) < SCAN_STALE ) {
            r.setPrev(prevR);
        }
        robotMap.put(r.name, r);
    }
    
    
    @Override
    public void onCustomEvent(CustomEvent e) {
        current();
        if (e.getCondition().equals(this.eachTickTimer) ) {
System.out.println("------"+now+"------");
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
        }
        this.onPaint(getGraphics());
        execute();
    }

    private static void drawRound(Graphics2D g, double x, double y, double r) {
        g.drawRoundRect((int) (x - r / 2), (int) (y - r / 2), (int) r, (int) r, (int) r + 2, (int) r + 2);
    }

    @Override
    public void onPaint(Graphics2D g) {
        double x = getX();
        double y = getY();
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0, 255, 0));
        drawRound(g, x, y, SHOT_RANGE * 2);
        drawRound(g, x, y, LOCKON_RANGE * 2);
        drawRound(g, x, y, RADAR_LOCKON_RANGE * 2);
        g.drawLine((int)curX,(int)curY,(int)(Math.sin(curGunHeadingRadians)*1000+curX),(int)(Math.cos(curGunHeadingRadians)*1000+curY));

        
        if (mode == MODE_NORMAL) {
            g.setColor(new Color(0, 255, 0));
        } else {
            g.setColor(new Color(255, 0, 0));
        }
        g.drawString(String.format("( %2.2f , %2.2f )", x , y), (int) x - 20, (int) y- 45);
        g.drawString(String.format("targ: %s", lockon), (int) x - 20, (int) y- 55);
        g.drawString(String.format("heat: %2.2f", getGunHeat()), (int) x - 20, (int) y- 65);
        g.drawString(String.format("velo: %2.1f", getVelocity()), (int) x - 20, (int) y- 75);

        for (Map.Entry<String, Robot> e : robotMap.entrySet()) {
            g.setColor(new Color(0, 255, 255));
            Robot r = e.getValue();
            drawRound(g, r.x, r.y, 35);            
            g.drawString(String.format("( %2.2f , %2.2f )", r.x , r.y), (int) r.x - 20, (int) r.y- 45);
            g.drawString(String.format("dist: %2.2f", r.distance), (int) r.x - 20, (int) r.y - 55);
            g.drawString(String.format("velo: %2.2f", r.velocity), (int) r.x - 20, (int) r.y - 65);

            g.setColor(new Color(50, 255, 150));
            for ( int i = 1 ; i < 15; i++) {
                Point p = r.prospect(i);
                drawRound(g,p.x,p.y,5+1.5*i);
            }
        }
        g.setStroke(new BasicStroke(4.0f));
        if ( lockOnPoint != null ) {
            g.setColor(new Color(255, 0, 0));
            drawRound(g, lockOnPoint.x, lockOnPoint.y, 5);
        }
        if ( aimPoint != null && firePoint != null ) {
            g.setStroke(new BasicStroke(1.0f));
            g.drawLine((int)firePoint.x,(int)firePoint.y,(int)aimPoint.x,(int)aimPoint.y);
        }

        g.setStroke(new BasicStroke(4.0f));
        if ( dst != null ) {
            g.setColor(new Color(0, 255, 0));
            drawRound(g,dst.x,dst.y,10);
        }
        Robot r = robotMap.get(lockon);
        if (r != null) {
//            g.setColor(new Color(0, 0, 255));
//            Point i = r.inertia(now - r.time);
//            drawRound(g, i.x, i.y, 40);
//            g.setColor(new Color(255, 0, 255));
//            Point p = r.prospect(now - r.time);
//            drawRound(g, p.x, p.y, 40);


        }
        execute();
    }
}