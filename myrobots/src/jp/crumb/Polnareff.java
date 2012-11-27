package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;
import robocode.*;


/**
 * Silver - a robot by (your name here)
 */
public class Polnareff extends AdvancedRobot {
    static final double RANGE_SHOT = 70;
    static final double RANGE_LOCKON = 150;
    static final double RANGE_CHASE = 250;
    static final double RANGE_RADAR_LOCKON = 400;
    static final int MODE_NORMAL = 1;
    static final int MODE_RADAR_LOCKON = 2;
    int mode = MODE_NORMAL;
    String lockon = "";

    
    MovingPoint my = new MovingPoint();
    MovingPoint nextMy = new MovingPoint();
    
    double curTurnRemaining;
    double curTurnRemainingRadians;       
    double curGunTurnRemaining;        
    double curGunTurnRemainingRadians;            
    double curRadarTurnRemaining;            
    double curRadarTurnRemainingRadians;
    
    double energy;
    double curGunHeadingRadians;
    double curGunHeading;
    double curRadarHeadingRadians;
    double curRadarHeading;
    double curDistanceRemaining;
    double gunHeat;
    int others;
    Point destination;
    
    
    
    
    void current() {
        this.setInterruptible(true);
        Util.NOW = getTime();
        if ( Util.NOW == my.time ) {
            return;
        }
        
        energy = getEnergy();
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
        curDistanceRemaining = getDistanceRemaining();

        MovingPoint prevMy = my;
        my = new MovingPoint();
        my.time = Util.NOW;
        my.velocity = getVelocity();
        my.x = getX();
        my.y = getY();
        my.heading = getHeading();
        my.headingRadians = getHeadingRadians();
        my.setPrev(prevMy);
        
        nextMy = new MovingPoint(my);
        nextMy.prospectNext();
        
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
    }

    void setDestination(Point dst){
        this.destination = dst;
    }

    void checkMove(){
        double distance = my.calcDistance(destination);
        if ( distance < (Util.getBrakingDistance(my.velocity)+my.velocity) ) {
            myOnNearingDestination();
        }
        // wall check
        // collision check
        // 
    }
    
    static double MOVE_COMPLETE_THRESHOLD = 1.0;
    void goPoint(){
        if ( this.destination == null ) {
            return;
        }
        double bearing = my.calcDegree(destination);
        double distance = my.calcDistance(destination);
        
        if ( distance < MOVE_COMPLETE_THRESHOLD ) { 
            setAhead(0);
            setTurnRight(0);
            this.destination = null;
            return;
        }
        double aheadTurnDegree = calcAbsTurn(bearing);
        double backTurnDegree  = calcAbsTurn(bearing-180);
        double runTime = Util.calcRoughRunTime(distance,my.velocity);
        
        double turnDegree;
        
        if ( Math.abs(aheadTurnDegree) < Math.abs(backTurnDegree)) { // ahead
            turnDegree = aheadTurnDegree;
            double turnTime = Math.abs(aheadTurnDegree/Util.turnSpeed(my.velocity));
        }else { // back
            turnDegree = backTurnDegree;
            distance *= -1; // back
        }
        double turnTime = Math.abs(turnDegree/Util.turnSpeed(my.velocity));
        if ( runTime <= turnTime ) {
            distance = 0;
        }
        setAhead(distance);
        setTurnRight(turnDegree);
        
    }
    
    
    double calcAbsTurn(double n) {
        double target = (n - my.heading) % 360;
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

    Enemy calcAbsRobot(ScannedRobotEvent e) {
        return new Enemy(this, e);
    }

    static final int MAX_CALC = 5;
    static final int LOCKON_ANGLE = 20;
    static final int SCAN_STALE = 10;
    void lockOnRadar(Enemy lockOnTarget) {
        if (lockOnTarget == null || (my.time - lockOnTarget.time) > SCAN_STALE ) {
            mode = MODE_NORMAL;
            return;
        }
        double radarTurn = calcAbsRadarTurn(lockOnTarget.bearing);
        if ( radarTurn < 0 ) {
            radarTurn -= LOCKON_ANGLE;
        }else{
            radarTurn += LOCKON_ANGLE;
        }
        this.setTurnRadarRight(radarTurn);
        lockOn(lockOnTarget);      
    }
    
    double selectPowerFromDistance(double distance) {
        if (distance < RANGE_SHOT) {
            return 3;
        } else if (distance < RANGE_LOCKON) {
            return 2;
        } else if (distance < RANGE_CHASE) {
            return 1;
        } else if (distance < RANGE_RADAR_LOCKON) {
            return 0.3;
        } else {
            return 0.0;
        }
    }


    static final int MAX_HIT_TIME = 20; 
    Point aimPoint;
    Point firePoint;
    void firing(){
        if ( mode == MODE_RADAR_LOCKON && gunHeat == 0.0 ) {
            
            Enemy lockOnTarget = nextEnemyMap.get(lockon);
            if ( lockOnTarget != null && lockOnTarget.delta != null && lockOnTarget.delta.time == 1 ) { 
                double firePower = 0.0;
                MovingPoint prospectTarget = new MovingPoint(lockOnTarget);

                double maxPower = 0.0;
                double aimDistance = 0.0;
                for ( int i = 1; i <= MAX_HIT_TIME; i++ ) {
                    double d = Util.calcPointToLineRange(my,prospectTarget,curGunHeadingRadians);
                    if ( d < (Util.tankWidth/2) ) { // hit ?
                        double bultDistance = Util.calcPointToLineDistance(my,prospectTarget,curGunHeadingRadians);
                        double bultSpeed = bultDistance/i;
                        double power = Util.bultPower( bultSpeed );
                        if ( maxPower < power ) {
                            Logger.gun_log("POWER (%2.2f) => (%2.2f)", maxPower,power);
                            maxPower = power;
                            aimDistance = bultDistance;
                        }
                    }
                    prospectTarget.prospectNext();
                }
                if ( maxPower > 0 ) {
                    Logger.gun_log("FIRE : ( %2.2f )",maxPower);
                    mode = MODE_NORMAL;
                    firePoint = new Point(my);
                    aimPoint  = Util.calcPoint(curGunHeadingRadians, aimDistance).add(my);
                    Logger.scan_log("**%f  ,  %f  : %s",curGunHeadingRadians,aimDistance,aimPoint);
                    fire(maxPower);
                }
            }
        }
    }
    
    Point lockOnPoint = null;
    void lockOn(Enemy lockOnTarget) {
        if (lockOnTarget == null) {
            return;
        }
        
        double dist = lockOnTarget.distance;
        double power = selectPowerFromDistance(dist);
        double gunTurn = 0;
        double allTime = dist / Util.bultSpeed(power)+1; // 1 = gunturn
        
        
        MovingPoint prospectMy  = new MovingPoint(my);
        prospectMy.prospectNext();
        
        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            MovingPoint prospectTarget = new MovingPoint(lockOnTarget);
            prospectTarget.prospect(allTime);
            dist = nextMy.calcDistance(prospectTarget);
            power = this.selectPowerFromDistance(dist);
            double bultTime = dist / Util.bultSpeed(power);
            gunTurn = calcAbsGunTurn(nextMy.calcDegree(prospectTarget));
            double gunTurnTime = Util.gunTurnSpeed(gunTurn); 
            
            // Todo:
            if (Math.abs(allTime - (bultTime + gunTurnTime)) < 1) { 
                lockOnPoint = prospectTarget;
                break;
            }
            allTime = bultTime + gunTurnTime;
        }
        Logger.ctrl_log("GUN (%2.2f) %2.2f",curGunTurnRemaining,gunTurn);
        setTurnGunRight(gunTurn);
    }
    
    Map<String, Enemy> enemyMap = new HashMap<>();
    Map<String, Enemy> nextEnemyMap = new HashMap<>();

    static final double PREPARE_LOCK_TIME=6;
    
    void thinking() {
        Enemy lockOnTarget = null;
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            if (lockOnTarget == null || lockOnTarget.distance > r.distance) {
                if ( (my.time - r.time) < SCAN_STALE ) {
                    lockOnTarget = r;
                }
            }
        }
        mode = MODE_NORMAL;
        if ( lockOnTarget != null ) {
            lockon = lockOnTarget.name;
             if ( lockOnTarget.distance < RANGE_RADAR_LOCKON && (gunHeat/Util.gunCoolingRate) < PREPARE_LOCK_TIME ||
                     others == 1 ) {
                 mode = MODE_RADAR_LOCKON;
             }
        }
        if ( mode == MODE_NORMAL) {
            setTurnRadarRight(180);
            lockOn(lockOnTarget);
        }
        Logger.ctrl_log("MODE: %d",mode);
    }
    
    void nextEnemy(){
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            r.prospectNext();
        }
    }


    void myOnRadarTurnComplete() {
        Logger.trace("myRadarTurnComplete");
        if ( mode == MODE_RADAR_LOCKON ) {
            Enemy lockOnTarget = nextEnemyMap.get(lockon);
            lockOnRadar(lockOnTarget);
        }
    }
    
    static final double RANDOM_MOVE_EXTENT = 200;
    void myOnNearingDestination(){
        Logger.trace("myOnNearingDestination");        
        setDestination(Util.getRandomPoint(my,RANDOM_MOVE_EXTENT));
    }
    
    void myOnCompleteMove() {
        Logger.trace("myOnCompleteMove");        
        setDestination(Util.getRandomPoint(my,RANDOM_MOVE_EXTENT));
    }

    void myOnScannedRobot(ScannedRobotEvent e) {
        Enemy r = calcAbsRobot(e);
        Logger.scan_log("%15s : %s",r.name,r);
        Enemy prevR = enemyMap.get(r.name);
        if ( prevR != null && (r.time != prevR.time) && (r.time-prevR.time) < SCAN_STALE ) {
            r.setPrev(prevR);
        }
        enemyMap.put(r.name, r);
        nextEnemyMap.put(r.name, new Enemy(r));
    }
    void myOnRobotDeath(RobotDeathEvent e) {
        enemyMap.remove(e.getName());
        nextEnemyMap.remove(e.getName());
    }
    
    
    void myOnHitByBullet(HitByBulletEvent e) {
    }

    void myOnBulletMissed(BulletMissedEvent e) {
    }

    void myOnBulletHit(BulletHitEvent e) {
    }

    
    void myOnHitWall(HitWallEvent e) {
    }

    void myOnHitRobot(HitRobotEvent e) {
    }

    void myOnBulletHitBullet(BulletHitBulletEvent e) {
    }
    
    
            
    @Override
    public void onCustomEvent(CustomEvent event) {
        current();
        if (event.getCondition().equals(this.eachTickTimer) ) {
            for ( RobotDeathEvent e: this.getRobotDeathEvents() ) {
                this.myOnRobotDeath(e);
            }
//            for ( HitRobotEvent e: this.getHitRobotEvents() ) {
//                this.myOnHitRobot(e);
//            }
//            for ( HitWallEvent e: this.getHitWallEvents() ) {
//                this.myOnHitWall(e);
//            }
//            for ( HitByBulletEvent e: this.getHitByBulletEvents() ) {
//                this.myOnHitByBullet(e);
//            }
//            for ( BulletHitBulletEvent e: this.getBulletHitBulletEvents() ) {
//                this.myOnBulletHitBullet(e);
//            }
//            for ( BulletHitEvent e: this.getBulletHitEvents() ) {
//                this.myOnBulletHit(e);
//            }
//            for ( BulletMissedEvent e: this.getBulletMissedEvents() ) {
//                this.myOnBulletMissed(e);
//            }
//            for ( StatusEvent e: this.getStatusEvents() ) {
//            }
            
            if ( this.curTurnRemaining == 0.0 && my.velocity == 0.0 ){
                this.myOnCompleteMove();
            }
            if ( this.curRadarTurnRemaining == 0.0){
                this.myOnRadarTurnComplete();
            }
            for ( ScannedRobotEvent e: this.getScannedRobotEvents() ) {
                this.myOnScannedRobot(e);
            }
            this.nextEnemy();
            this.checkMove();
            this.goPoint();
            this.firing();
            this.thinking();
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
        drawRound(g, x, y, RANGE_SHOT * 2);
        drawRound(g, x, y, RANGE_LOCKON * 2);
        drawRound(g, x, y, RANGE_CHASE * 2);
        drawRound(g, x, y, RANGE_RADAR_LOCKON * 2);
        g.drawLine((int)my.x,(int)my.y,(int)(Math.sin(curGunHeadingRadians)*1000+my.x),(int)(Math.cos(curGunHeadingRadians)*1000+my.y));

        
        if (mode == MODE_NORMAL) {
            g.setColor(new Color(0, 255, 0));
        } else {
            g.setColor(new Color(255, 0, 0));
        }
        g.drawString(String.format("( %2.2f , %2.2f )", x , y), (int) x - 20, (int) y- 45);
        g.drawString(String.format("targ: %s", lockon), (int) x - 20, (int) y- 55);
        g.drawString(String.format("heat: %2.2f", getGunHeat()), (int) x - 20, (int) y- 65);
        g.drawString(String.format("velo: %2.1f", getVelocity()), (int) x - 20, (int) y- 75);

        for (Map.Entry<String, Enemy> e : enemyMap.entrySet()) {
            g.setColor(new Color(100, 200, 255));
            Enemy r = e.getValue();
            drawRound(g, r.x, r.y, 35);            
            g.drawString(String.format("( %2.2f , %2.2f )", r.x , r.y), (int) r.x - 20, (int) r.y- 45);
            g.drawString(String.format("dist: %2.2f", r.distance), (int) r.x - 20, (int) r.y - 55);
            g.drawString(String.format("velo: %2.2f", r.velocity), (int) r.x - 20, (int) r.y - 65);

            g.setColor(new Color(50, 255, 150));
            MovingPoint enemy = new MovingPoint(r);
            for ( int i = 1 ; i < 20; i++) {
                enemy.prospectNext();
                drawRound(g,enemy.x,enemy.y,2);
            }
        }
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            g.setColor(new Color(0, 255, 255));
            Enemy r = e.getValue();
            drawRound(g, r.x, r.y, 35);            
        }        
        g.setStroke(new BasicStroke(4.0f));
        if ( lockOnPoint != null ) {
            g.setColor(new Color(255, 255, 0));
            drawRound(g, lockOnPoint.x, lockOnPoint.y, 5);
        }
        if ( aimPoint != null && firePoint != null ) {
            g.setColor(new Color(255, 0, 0));
            g.setStroke(new BasicStroke(1.0f));
            g.drawLine((int)firePoint.x,(int)firePoint.y,(int)aimPoint.x,(int)aimPoint.y);
            g.setStroke(new BasicStroke(4.0f));
            drawRound(g, aimPoint.x, aimPoint.y, 5);
        }

        g.setStroke(new BasicStroke(4.0f));
        if ( destination != null ) {
            g.setColor(new Color(0, 255, 0));
            drawRound(g,destination.x,destination.y,10);
        }
        Enemy r = enemyMap.get(lockon);
        if (r != null) {
//            g.setColor(new Color(0, 0, 255));
//            Point i = r.inertia(my.time - r.time);
//            drawRound(g, i.x, i.y, 40);
//            g.setColor(new Color(255, 0, 255));
//            Point p = r.prospect(my.time - r.time);
//            drawRound(g, p.x, p.y, 40);


        }
        execute();
    }
}