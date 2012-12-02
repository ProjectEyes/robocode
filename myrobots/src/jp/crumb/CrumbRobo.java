/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jp.crumb.utils.DeltaMovingPoint;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Point;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;
import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.Condition;
import robocode.CustomEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.SkippedTurnEvent;
import robocode.StatusEvent;

/**
 *
 * @author crumb
 */

abstract class CrumbRobo extends AdvancedRobot {
    protected static final double MOVE_COMPLETE_THRESHOLD = 1.0;

    protected static final int MODE_NORMAL = 1;
    protected static final int MODE_RADAR_LOCKON = 2;
    

    protected int MAX_CALC = 5;
    protected int SCAN_STALE = 9;
    protected int SYSTEM_BUG_TICKS = 30;
      
    // Current informations
    protected DeltaMovingPoint my = new DeltaMovingPoint();
    protected DeltaMovingPoint nextMy = new DeltaMovingPoint();
    
    protected double curTurnRemaining;
    protected double curTurnRemainingRadians;       
    protected double curGunTurnRemaining;        
    protected double curGunTurnRemainingRadians;            
    protected double curRadarTurnRemaining;            
    protected double curRadarTurnRemainingRadians;
    
    protected double energy;
    protected double curGunHeadingRadians;
    protected double curGunHeading;
    protected double curRadarHeadingRadians;
    protected double curRadarHeading;
    protected double curDistanceRemaining;
    protected double gunHeat;
    protected int others;
    
    protected double prevRadarHeadingRadians;
    // For auto move
    protected Point destination;

    protected static Map<String, Enemy> enemyMap = new HashMap<>();
    protected Map<String, Enemy> nextEnemyMap = new HashMap<>();
    protected Map<String, List<MovingPoint> > enemyPatternMap = new HashMap<>();

    protected boolean PATTERN = false;
    
    protected void cbMoving() {}
    protected void cbFiring() {}
    protected void cbThinking() {}
    protected void cbFirst() {}
    protected void cbRadarTurnComplete() {}
    protected void cbCompleteMove() {}
    protected void cbHitByBullet(HitByBulletEvent e) {}

    protected void prospectNext(Enemy enemy) {
        enemy.prospectNext(my);
    }
    protected void prospectNext(Enemy enemy,int interval) {
        for (int i = 1; i <= interval; i++) {
            enemy.prospectNext(my);
        }
    }

    protected Map.Entry<TimedPoint,BulletInfo> calcBulletSrc(Bullet bullet) {
        Point dst = new Point(bullet.getX(),bullet.getY());
        double bulletRadians = bullet.getHeadingRadians();
        
        Map.Entry<TimedPoint,BulletInfo> cur = null;
        double curDiffDistance = 0;
        double curDiffRadians = 0;
        for(Map.Entry<TimedPoint,BulletInfo> e : bulletList.entrySet() ) {
            TimedPoint src = e.getKey();
            double radians = src.calcRadians(dst);
            double diffRadians = Math.abs(Util.calcTurnRadians(radians, bulletRadians));
            double diffDistance = Math.abs((my.time - src.time)*bullet.getVelocity() - src.calcDistance(dst));
            if ( cur == null || diffRadians < curDiffRadians &&  diffDistance < curDiffDistance ) {
                cur = e;
                curDiffRadians = diffRadians;
                curDiffDistance = diffDistance;
            }
        }
        if ( cur != null ) {
            bulletList.remove(cur.getKey());
            return cur;
        }
        Logger.log("Unknown bullet: ");
        return null;
    }
    protected void cbBulletMissed(BulletMissedEvent e) {
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        calcBulletSrc(bullet);
        Logger.gun1("MISS: %s",dst);
    }
    
    protected void cbBulletHit(BulletHitEvent e){
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        String victim = bullet.getVictim();

        double range = 0.0;
        double aimDistance = 0.0;
        TimedPoint src = null;
        BulletInfo info = null;
        Map.Entry<TimedPoint,BulletInfo> entry = calcBulletSrc(bullet);
        if ( entry != null ){
            src = entry.getKey();
            info = entry.getValue();
            aimDistance = entry.getValue().distance;
            range = src.calcDistance(dst);
        }
        // Judge by chance 
        if ( info != null && info.target.equals(victim) && Math.abs(aimDistance - range) < new Point(Util.tankWidth,0).calcDistance(new Point(0,Util.tankHeight)) ) {
            Enemy target = enemyMap.get(victim);
            if ( target != null ) {
                target.getAimType().updateHitRange(range);
            }
            Logger.gun1("HIT: %s : %2.2f(%2.2f)  %s => %s",victim,aimDistance,range,src,dst);
        }else{
            if ( info != null ) {
                Logger.gun1("HIT (by chance): %s(%s): %2.2f(%2.2f)  %s => %s",victim,info.target,aimDistance,range,src,dst);
            }else{
                Logger.gun1("HIT (by chance): %s: %2.2f(%2.2f)  %s => %s",victim,aimDistance,range,src,dst);
            }
        }
    }
    protected void cbBulletHitBullet(BulletHitBulletEvent e){
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        Logger.gun1("INTERCEPT: %s",dst);
        Map.Entry<TimedPoint,BulletInfo> entry = calcBulletSrc(bullet);
        if ( entry != null ){
            BulletInfo info = entry.getValue();
            Enemy target = enemyMap.get(info.target);
            target.getAimType().revartAimRange(info.distance);
        }
    }
    static class BulletInfo {
        String target;
        double distance;
        double radians;
        public BulletInfo(String target, double distance,double radians) {
            this.target = target;
            this.distance = distance;
            this.radians = radians;
        }
    }
    private Map<TimedPoint,BulletInfo> bulletList = new HashMap<>();
    public void fire(double power, double distance,String name ) {
        Logger.gun2("FIRE( power => bearing): ( %2.2f ) => %2.2f",power,curGunHeading);
        this.paint(getGraphics());
        Enemy enemy = enemyMap.get(name);
        if ( enemy != null ) {
            enemy.getAimType().updateAimRange(distance);
        }
        TimedPoint src = new TimedPoint(my,my.time);
        bulletList.put(src,new BulletInfo(name, distance,curGunHeadingRadians));
        super.fire(power);
    }
    
    
    protected void cbStatus(StatusEvent e){}

    protected void cbHitWall(HitWallEvent e) {
        Logger.crash("CLASH WALL: %s : %f",my,e.getBearing());
    }
    protected void cbHitRobot(HitRobotEvent e) {
        Logger.crash("CLASH (%s): %s : %f",e.getName(),my,e.getBearing());
    }


    private void current() {
        Util.NOW = getTime();
        if ( Util.NOW == my.time ) {
            return;
        }
        prevRadarHeadingRadians = curRadarHeadingRadians;   
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

        DeltaMovingPoint prevMy = my;
        my = new DeltaMovingPoint();
        my.time = Util.NOW;
        my.velocity = getVelocity();
        my.x = getX();
        my.y = getY();
        my.heading = getHeading();
        my.headingRadians = getHeadingRadians();
        my.setPrev(prevMy);
        
        nextMy = new DeltaMovingPoint(my);
        nextMy.prospectNext();
        
    }    
        
    private void initEventPriority(){
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
    
    
    protected void setDestination(Point dst){
        Logger.move_log("DST: %s", dst);
        this.destination = dst;
    }
    
    
    private void goPoint(){
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

    
    protected double calcAbsTurn(double absDegree) {
        return Util.calcTurn(my.heading,absDegree);
    }

    
    
    private double calcAbsGunTurnDiff(double diffDegree){
        long time = 0;
        double turnRemaining = curTurnRemaining;
        double sumGunTurn = 0.0;
        
        for (int i = 0 ;i < 100;i++){
            double nextTurn     = 0;
            double nextGunTurn  = 0;
            time++;
            if ( turnRemaining != 0.0 ) {
                nextTurn = Util.turnSpeed(my.velocity)*(Math.abs(turnRemaining)/turnRemaining);
                if ( Math.abs(nextTurn) > Math.abs(turnRemaining) ) {
                    nextTurn = turnRemaining;
                }
                turnRemaining -= nextTurn;
            }
//System.out.println(diffDegree + " t:" + nextTurn + " g:" + nextGunTurn);            
            diffDegree -= nextTurn;
            if ( diffDegree != 0.0 ) {
                nextGunTurn = Util.gunTurnSpeed()*(Math.abs(diffDegree)/diffDegree);
                if ( Math.abs(nextGunTurn) >  Math.abs(diffDegree) ) {
                    nextGunTurn = diffDegree;
                }
                diffDegree -= nextGunTurn;
                sumGunTurn += nextGunTurn;
            }
            if ( diffDegree == 0.0 ) {
                break;
            }
        }
        return sumGunTurn;
    }
    
    
    private double calcAbsRadarTurnDiff(double diffDegree){
        long time = 0;
        double turnRemaining = curTurnRemaining;
        double gunTurnRemaining = curGunTurnRemaining;

        double sumRadarTurn = 0.0;
        
        for (int i = 0 ;i < 100;i++){
            double nextTurn     = 0;
            double nextGunTurn  = 0;
            double nextRadarTurn= 0;
            time++;
            if ( turnRemaining != 0.0 ) {
                nextTurn = Util.turnSpeed(my.velocity)*(Math.abs(turnRemaining)/turnRemaining);
                if ( Math.abs(nextTurn) > Math.abs(turnRemaining) ) {
                    nextTurn = turnRemaining;
                }
                turnRemaining -= nextTurn;
            }

            if ( gunTurnRemaining != 0.0 ) {
                nextGunTurn = Util.gunTurnSpeed()*(Math.abs(gunTurnRemaining)/gunTurnRemaining);
                if ( Math.abs(nextGunTurn) >  Math.abs(gunTurnRemaining) ) {
                    nextGunTurn = gunTurnRemaining;
                }
                gunTurnRemaining -= nextGunTurn;
                nextGunTurn += nextTurn;
            }
//System.out.println(diffDegree + " t:" + nextTurn + " g:" + nextGunTurn);            
            diffDegree -= nextGunTurn;
            if ( diffDegree != 0.0 ) {
                nextRadarTurn = Util.radarTurnSpeed()*(Math.abs(diffDegree)/diffDegree);
                if ( Math.abs(nextRadarTurn) >  Math.abs(diffDegree) ) {
                    nextRadarTurn = diffDegree;
                }
                diffDegree -= nextRadarTurn;
                sumRadarTurn += nextRadarTurn;
            }
            if ( diffDegree == 0.0 ) {
                break;
            }
        }
        return sumRadarTurn;
    }

    protected double calcAbsGunTurn(double absDegree) {
        double diffDegree1 = (absDegree - curGunHeading) % 360;
        if (diffDegree1 > 180) {
            diffDegree1 = diffDegree1 - 360;
        } else if (diffDegree1 < -180) {
            diffDegree1 = diffDegree1 + 360;
        }
        double diffDegree2;
        if ( diffDegree1 < 0 ) {
            diffDegree2 = 360+diffDegree1;
        }else{
            diffDegree2 = diffDegree1-360;
        }
        double realDegree1 = calcAbsGunTurnDiff(diffDegree1);
        double realDegree2 = calcAbsGunTurnDiff(diffDegree2);
        Logger.gun3("CALC: %2.2f => %2.2f : 1 = %2.2f(%2.2f) : 2 = %2.2f(%2.2f)",curGunHeading,absDegree,diffDegree1,realDegree1,diffDegree2,realDegree2);
        if ( Math.abs(realDegree2) < Math.abs(realDegree1) ) {
            return realDegree2;
        }
        return realDegree1;
    }
    
    protected double calcAbsRadarTurn(double absDegree) {
        double diffDegree1 = (absDegree - curRadarHeading) % 360;
        if (diffDegree1 > 180) {
            diffDegree1 = diffDegree1 - 360;
        } else if (diffDegree1 < -180) {
            diffDegree1 = diffDegree1 + 360;
        }
        double diffDegree2;
        if ( diffDegree1 < 0 ) {
            diffDegree2 = 360+diffDegree1;
        }else{
            diffDegree2 = diffDegree1-360;
        }
        double realDegree1 = calcAbsRadarTurnDiff(diffDegree1);
        double realDegree2 = calcAbsRadarTurnDiff(diffDegree2);
        Logger.radar3("CALC: %2.2f => %2.2f : 1 = %2.2f(%2.2f) : 2 = %2.2f(%2.2f)",curRadarHeading,absDegree,diffDegree1,realDegree1,diffDegree2,realDegree2);
        if ( Math.abs(realDegree2) < Math.abs(realDegree1) ) {
            return realDegree2;
        }
        return realDegree1;
    }

    protected Enemy calcAbsRobot(ScannedRobotEvent e) {
        return new Enemy(my, e);
    }    
  

    private void nextEnemy(){
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            prospectNext(r);
        }
    }
    private void scannedRobot(ScannedRobotEvent e) {
        Enemy r = calcAbsRobot(e);
        Logger.scan("%15s : %s : %d",r.name,r,r.time);
        Enemy nextEnemy = nextEnemyMap.get(r.name);
        Enemy prevR = enemyMap.get(r.name);

        if ( prevR != null ) {
            if ( r.time != prevR.time && (r.time-prevR.time) < SCAN_STALE || (nextEnemy != null && nextEnemy.calcDistance(r) < 20 ) ){
                r.setPrev(prevR);
            }
            r.setAimType(prevR.getAimType());
        }
        
        enemyMap.put(r.name, r);
        
        if ( ! enemyPatternMap.containsKey(r.name)) {
            enemyPatternMap.put(r.name,new ArrayList());
        }
        
        if ( PATTERN ) {
            List<MovingPoint> patternList = enemyPatternMap.get(r.name);
            if ( patternList.size() == 0 ) {
                MovingPoint first = new MovingPoint();
                first.time = r.time;
                patternList.add(first);
            }else{
                MovingPoint lastPattern = patternList.get(patternList.size()-1);
                long deltaTime = r.time - lastPattern.time;
                for (int i = 1 ; i <= deltaTime ; i++ ) {
                    MovingPoint p = (MovingPoint)new MovingPoint(r).diff(nextEnemy).quot(deltaTime);
                    p.time = lastPattern.time + i;
                    patternList.add(p);
                }
            }
        }
        nextEnemyMap.put(r.name, new Enemy(r));
    }
    private void robotDeath(RobotDeathEvent e) {
        // enemyMap.remove(e.getName());
        nextEnemyMap.remove(e.getName());
        enemyPatternMap.remove(e.getName());
    }

    
    @Override
    public void setTurnGunRight(double degrees) {
        Logger.gun2("TURN: %2.2f : %2.2f => %2.2f",curGunHeading,curGunTurnRemaining,degrees);
        super.setTurnGunRight(degrees);
        curGunTurnRemaining = degrees;
        curGunTurnRemainingRadians = Math.toRadians(degrees);
    }

    @Override
    public void setTurnRight(double degrees) {
        super.setTurnRight(degrees);
        curTurnRemaining = degrees;
        curTurnRemainingRadians = Math.toRadians(degrees);
    }

    @Override
    public void setTurnRadarRight(double degrees) {
        Logger.radar2("TURN: %2.2f : %2.2f => %2.2f",curRadarHeading,curRadarTurnRemaining,degrees);
        super.setTurnRadarRight(degrees);
        curRadarTurnRemaining = degrees;
        curRadarTurnRemainingRadians = Math.toRadians(degrees);
    }

    @Override
    public void setAhead(double distance) {
        super.setAhead(distance);
        curDistanceRemaining = distance;
    }
    
    private Condition eachTickTimer = new Condition("eachTickTimer",10) {
        @Override
        public boolean test() {
            return true;
        }
    };
    private Condition firstTickTimer = new Condition("firstTickTimer",90) {
        @Override
        public boolean test() {
            return true;
        }
    };
    
    @Override
    public void run() {
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
//        setAdjustGunForRobotTurn(true);
//        setAdjustRadarForGunTurn(true);
        
        addCustomEvent(this.firstTickTimer);
        addCustomEvent(this.eachTickTimer);
        execute();
    }
    private void forSystemBug(){

        List<String> stales = new ArrayList<>();
        if ( nextEnemyMap.size() == 0 ) {
            return;
        }
        boolean allStale = true;
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( my.time - r.time < SYSTEM_BUG_TICKS) {
                allStale = false;
            }else{
                stales.add(e.getKey());
            }
        }
        if ( allStale ){
            Logger.log("ALL STALE !!!!!!!!!!! : "); 
            execute();
        }
        for ( String name : stales ) {
            Logger.log("STALE: %s",stales);            
            nextEnemyMap.remove(name);
        }
    }
    

    
    @Override
    public void onCustomEvent(CustomEvent event) {
        forSystemBug();
        this.setInterruptible(true);
        current();
        if (event.getCondition().equals(this.firstTickTimer) ) {
            removeCustomEvent(firstTickTimer);
            cbFirst();
            execute();
            return;
        }
        if (event.getCondition().equals(this.eachTickTimer) ) {
            for ( RobotDeathEvent e: this.getRobotDeathEvents() ) {
                this.robotDeath(e);
            }
            for ( HitRobotEvent e: this.getHitRobotEvents() ) {
                this.cbHitRobot(e);
            }
            for ( HitWallEvent e: this.getHitWallEvents() ) {
                this.cbHitWall(e);
            }
            for ( HitByBulletEvent e: this.getHitByBulletEvents() ) {
                this.cbHitByBullet(e);
            }
            for ( BulletHitBulletEvent e: this.getBulletHitBulletEvents() ) {
                this.cbBulletHitBullet(e);
            }
            for ( BulletHitEvent e: this.getBulletHitEvents() ) {
                this.cbBulletHit(e);
            }
            for ( BulletMissedEvent e: this.getBulletMissedEvents() ) {
                this.cbBulletMissed(e);
            }
            for ( StatusEvent e: this.getStatusEvents() ) {
                this.cbStatus(e);
            }
            if ( this.curTurnRemaining == 0.0 && my.velocity == 0.0 ){
                this.cbCompleteMove();
            }
            if ( this.curRadarTurnRemaining == 0.0){
                this.cbRadarTurnComplete();
            }
            
            for ( ScannedRobotEvent e: this.getScannedRobotEvents() ) {
                this.scannedRobot(e);
            }
            this.nextEnemy();
            this.cbMoving();
            this.goPoint();
            this.cbFiring();
            this.cbThinking();
        }
        this.paint(getGraphics());
        execute();
    }
    
    protected static void drawRound(Graphics2D g, double x, double y, double r) {
        g.drawRoundRect((int) (x - r / 2), (int) (y - r / 2), (int) r, (int) r, (int) r + 2, (int) r + 2);
    }

    protected static final float PAINT_OPACITY=0.5f;
    protected void paint(Graphics2D g) {
        drawRound(g, my.x, my.y, 400 * 2);
        drawRound(g, my.x, my.y, 600 * 2);
        float[] dash = new float[2];
        dash[0] = 0.1f;
        dash[1] = 0.1f;
        g.setStroke(new BasicStroke(1.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,1.0f,dash,0.0f));
        drawRound(g, my.x, my.y, 100 * 2);
        drawRound(g, my.x, my.y, 300 * 2);
        drawRound(g, my.x, my.y, 500 * 2);

        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0, 0.7f, 0, PAINT_OPACITY));
        
        double fieldMax = new Point(Util.battleFieldWidth,Util.battleFieldHeight).calcDistance(new Point());
        g.drawLine((int)my.x,(int)my.y,(int)(Math.sin(curGunHeadingRadians)*fieldMax+my.x),(int)(Math.cos(curGunHeadingRadians)*fieldMax+my.y));

        double deltaRadians = Util.calcTurnRadians(curRadarHeadingRadians,prevRadarHeadingRadians)/10;
        if ( deltaRadians != 0.0 ) {
            int[] xs = new int[3];
            int[] ys = new int[3];
            xs[0] = (int)my.x;
            ys[0] = (int)my.y;
            double radians  = curRadarHeadingRadians;
            for(int i = 1 ; i < 10; i++) {
                xs[1] = (int)(Math.sin(radians)*fieldMax+my.x);
                ys[1] = (int)(Math.cos(radians)*fieldMax+my.y);
                radians += deltaRadians;
                xs[2] = (int)(Math.sin(radians)*fieldMax+my.x);
                ys[2] = (int)(Math.cos(radians)*fieldMax+my.y);
                g.setColor(new Color( i*0.03f,i*0.03f,1.0f, 0.1f));
                Polygon triangle = new Polygon(xs,ys,3);
                g.fill(triangle);
            }
        }else{
            g.setColor(new Color( 0.03f,0.03f,1.0f, 0.1f));
            g.drawLine((int)my.x,(int)my.y,(int)(Math.sin(curRadarHeadingRadians)*fieldMax+my.x),(int)(Math.cos(curRadarHeadingRadians)*fieldMax+my.y));
        }
        
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0,1.0f,0,PAINT_OPACITY));
        g.drawString(String.format("( %2.2f , %2.2f )", my.x , my.y), (int) my.x - 20, (int) my.y- 55);
        g.drawString(String.format("heat: %2.2f", getGunHeat()), (int) my.x - 20, (int) my.y- 65);
        g.drawString(String.format("velo: %2.1f", getVelocity()), (int) my.x - 20, (int) my.y- 75);
        if ( destination != null ) {
            g.setStroke(new BasicStroke(4.0f));
            g.setColor(new Color(0, 1.0f, 0,PAINT_OPACITY));
            drawRound(g,destination.x,destination.y,10);
        }

        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0, 1.0f, 1.0f,PAINT_OPACITY));
        for (Map.Entry<String, Enemy> e : enemyMap.entrySet()) {
            Enemy r = e.getValue();
            drawRound(g, r.x, r.y, 35);            
            g.drawString(String.format("%s : %s", r.name , r), (int) r.x - 20, (int) r.y- 30);
            g.drawString(String.format("hit: %d / %d  : %2.2f / %2.2f", r.getAimType().hit,r.getAimType().aim,r.getAimType().hitrange,r.getAimType().aimrange), (int) r.x - 20, (int) r.y - 40);
            Enemy next = nextEnemyMap.get(r.name);
            if ( next != null ) {
//                g.drawString(String.format("( %2.2f , %2.2f )", next.x , next.y), (int) r.x - 20, (int) r.y- 45);
                g.drawString(String.format("dist(degr): %2.2f(%2.2f)", next.distance,next.bearing), (int) r.x - 20, (int) r.y - 50);
                g.drawString(String.format("head(velo): %2.2f(%2.2f)", next.heading,next.velocity), (int) r.x - 20, (int) r.y - 60);
                g.setColor(new Color(0.2f, 1.0f, 0.7f,PAINT_OPACITY));

                //g.setColor(new Color(0.4f, 0.7f, 1.0f,PAINT_OPACITY));
                drawRound(g, next.x, next.y, 35);            
                Enemy enemy = new Enemy(next);
                for ( int i = 1 ; i < 20; i++) {
                    prospectNext(enemy);
                    drawRound(g,enemy.x,enemy.y,2);
                }
            }
        }

        for ( Map.Entry<TimedPoint,BulletInfo> e : bulletList.entrySet() ) {
            TimedPoint src = e.getKey();
            BulletInfo info = e.getValue();
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(1.0f, 0, 0,PAINT_OPACITY));
            g.drawLine((int)src.x,(int)src.y,(int)(Math.sin(info.radians)*fieldMax+src.x),(int)(Math.cos(info.radians)*fieldMax+src.y));
            g.setStroke(new BasicStroke(4.0f));
            Point dst = Util.calcPoint(info.radians, info.distance).add(src);
            drawRound(g, dst.x, dst.y, 5);
        }
        
//            g.setColor(new Color(0, 0, 255));
//            Point i = r.inertia(my.time - r.time);
//            drawRound(g, i.x, i.y, 40);
//            g.setColor(new Color(255, 0, 255));
//            Point p = r.prospect(my.time - r.time);
//            drawRound(g, p.x, p.y, 40);
//            for( MovingPoint p : enemyPatternMap.get(lockon) ) {
//                System.out.println(p);
//            }
//        }
    }

    private void dumpLog(){
        List<Enemy> enemyArray = new ArrayList(enemyMap.values());
        Collections.sort(enemyArray,new Comparator<Enemy>(){
            @Override
            public int compare(Enemy o1, Enemy o2) {
                return o1.getAimType().compareTo(o2.getAimType());
            }});
        for (Enemy enemy :  enemyArray ) {
            Logger.log("=== %s (%2.2f%%)===",enemy.name,enemy.getAimType().getHitRate()*100);
            Logger.log("aim : %2.2f(%d)",enemy.getAimType().aimrange,enemy.getAimType().aim);
            Logger.log("hit : %2.2f(%d)",enemy.getAimType().hitrange,enemy.getAimType().hit);
        }        
    }
    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        dumpLog();
    }
    
    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        dumpLog();
    }
    
}

