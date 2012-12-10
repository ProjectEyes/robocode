/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.base;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jp.crumb.utils.DeltaMovingPoint;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.MyPoint;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;
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
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.TeamRobot;


/**
 *
 * @author crumb
 */
abstract public class BaseRobo<T extends BaseContext> extends TeamRobot {
    protected static final boolean isPaint = true; // TODO: apply to crumbRobo
    protected Logger logger = new Logger();

    protected static final double MOVE_COMPLETE_THRESHOLD = 1.0;
    protected static final double ENEMY_BULLET_UNKNOWN_THRESHOLD = Math.PI/4; // more than 45 degrees

    protected static final int MAX_CALC = 5;
    protected static final int SCAN_STALE = 9;
    protected static final int SYSTEM_BUG_TICKS = 30;
      
    protected T ctx = createContext(null);
    
   
    protected static Set<String> teammate = new HashSet<>();
    protected static boolean isLeader = false;
    protected static String leader = null;
    protected static String name;

    // Current informations
    protected static Map<String, Enemy> enemyMap = new HashMap<>();
    private Map<String,BulletInfo> bulletList = new HashMap<>();
    private Map<String,BulletInfo> enemyBulletList = new HashMap<>();
    protected List<MyPoint> myLog = new ArrayList<>();
    
    
    protected static Map<String, List<Enemy> > enemyLog = new HashMap<>();

    protected static Map<String,List<MoveType>> shotTypeMap = new HashMap<>();



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

    private void current() {
        Util.NOW = getTime();
        if ( Util.NOW == ctx.my.time ) {
            return;
        }
        ctx.prevRadarHeadingRadians = ctx.curRadarHeadingRadians;   
        ctx.energy = getEnergy();
        ctx.curGunHeadingRadians = getGunHeadingRadians();
        ctx.curGunHeading = getGunHeading();
        ctx.curRadarHeadingRadians = getRadarHeadingRadians();
        ctx.curRadarHeading = getRadarHeading();
        
        ctx.gunHeat = getGunHeat();
        ctx.others = getOthers();
        ctx.enemies = ctx.others; // will be decl by each TeammateInfoEvent

        ctx.curTurnRemaining = getTurnRemaining();
        ctx.curTurnRemainingRadians = getTurnRemainingRadians();
        ctx.curGunTurnRemaining = getGunTurnRemaining();
        ctx.curGunTurnRemainingRadians = getGunTurnRemainingRadians();
        ctx.curRadarTurnRemaining = getRadarTurnRemaining();
        ctx.curRadarTurnRemainingRadians = getRadarTurnRemainingRadians();
        ctx.curDistanceRemaining = getDistanceRemaining();

        MyPoint prevMy = ctx.my;
        ctx.my = new MyPoint();
        ctx.my.time = Util.NOW;
        ctx.my.velocity = getVelocity();
        ctx.my.x = getX();
        ctx.my.y = getY();
        ctx.my.heading = getHeading();
        ctx.my.headingRadians = getHeadingRadians();
        ctx.my.setPrev(prevMy);
        ctx.nextMy = new MyPoint(ctx.my);
        prospectNextMy(ctx.nextMy,null);

        myLog.add(ctx.my);
    }
    protected MyPoint prevMy(long prev) {
        long size = myLog.size();
        if ( size > prev )  {
            return myLog.get((int)size-1-(int)prev);
        }
        return null;
    }

    private Map.Entry<String,BulletInfo> calcBulletSrc(Bullet bullet,Map<String,BulletInfo> list ) {
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
            double diffDistance = Math.abs((ctx.my.time - bulletInfo.src.time)*bullet.getVelocity() - bulletInfo.src.calcDistance(dst));
            if ( cur == null || diffRadians < curDiffRadians &&  diffDistance < curDiffDistance ) {
                cur = e;
                curDiffRadians = diffRadians;
                curDiffDistance = diffDistance;
            }
        }
        if ( cur != null ) {
            return cur;
        }
        logger.log("Unknown bullet: ");
        return null;
    }
    private void bulletMissed(BulletMissedEvent e) {
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        Map.Entry<String,BulletInfo> entry = calcBulletSrc(bullet,bulletList);
        if ( entry != null ) {
            broadcastMessage(new BulletImpactEvent(entry.getKey()));
            impactBullet(entry.getKey());
        }

        logger.gun1("MISS: %s",dst);
    }
    
    private void bulletHit(BulletHitEvent e){
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        String victim = bullet.getVictim();

        double range = 0.0;
        double aimDistance = 0.0;
        BulletInfo info = null;
        Map.Entry<String,BulletInfo> entry = calcBulletSrc(bullet,bulletList);
        if ( entry != null ) {
            impactBullet(entry.getKey());
            info = entry.getValue();
            aimDistance = entry.getValue().distance;
            range = info.src.calcDistance(dst);
        }
        for ( Map.Entry<String,BulletInfo> ebi : enemyBulletList.entrySet() ) {
            BulletInfo bulletInfo = ebi.getValue();
            if ( e.getTime() == bulletInfo.src.time && bulletInfo.src.calcDegree(dst) < Util.tankSize ) {
                logger.fire3("CANCEL BULLET() %s : %s ", dst,bulletInfo.src);
                removeEnemyBulletInfo(bulletInfo.bulletName);
                broadcastMessage(new CancelEnemyBalletEvent(bulletInfo.bulletName));
                break;
            }
        }

        // Judge by chance
        if ( info != null && info.target.equals(victim) && Math.abs(aimDistance - range) < Util.tankSize) {
            Enemy target = enemyMap.get(victim);
            if ( target != null ) {
                target.getAimType().updateHit(range);
            }
            logger.gun1("HIT: %s : %2.2f(%2.2f)  %s => %s",victim,aimDistance,range,info.src,dst);
        }else{
            if ( info != null ) {
                logger.gun1("HIT (by chance): %s(%s): %2.2f(%2.2f)  %s => %s",victim,info.target,aimDistance,range,info.src,dst);
            }else{
                logger.gun1("HIT (by chance): %s: %2.2f(%2.2f)  %s => %s",victim,aimDistance,range,"NULL",dst);
            }
        }
    }


    private void bulletHitBullet(BulletHitBulletEvent e){
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        logger.gun1("INTERCEPT: %s",dst);
        Map.Entry<String,BulletInfo> entry = calcBulletSrc(bullet,bulletList);
        if ( entry != null ) {
            impactBullet(entry.getKey());
            BulletInfo info = entry.getValue();
            Enemy target = enemyMap.get(info.target);
            target.getAimType().revartAim(info.distance/info.src.velocity);
        }
    }
    
    private void setEnemyBulletInfo(BulletInfo bulletInfo) {
        enemyBulletList.put(bulletInfo.bulletName,bulletInfo);
        ctx.nextEnemyBulletList.put(bulletInfo.bulletName,new BulletInfo(bulletInfo));
    }
    private void removeEnemyBulletInfo(String key){
        ctx.nextEnemyBulletList.remove(key);
        enemyBulletList.remove(key);
    }
    private void setBulletInfo(BulletInfo bulletInfo) {
        bulletList.put(bulletInfo.bulletName,bulletInfo);
        ctx.nextBulletList.put(bulletInfo.bulletName,new BulletInfo(bulletInfo));
    }
    private void impactBullet(String key){
        ctx.nextBulletList.remove(key);
        bulletList.remove(key);
    }
    protected void cbHitByBullet(HitByBulletEvent e) {
        long time = e.getTime();
        Bullet bullet = e.getBullet();
        double bulletVelocity = e.getVelocity();
        double bulletRadians = e.getHeadingRadians();
        String enemyName = e.getName();
        Map.Entry<String,BulletInfo> entry = calcBulletSrc(bullet,enemyBulletList);
        if ( entry != null ) {
            BulletInfo info = entry.getValue();
            removeEnemyBulletInfo(entry.getKey());
        // TODO: hit by bullet
            long deltaTime = ctx.my.time-info.src.time;
            MyPoint prevMy = prevMy(deltaTime);
            double distance = info.src.calcDistance(prevMy);
            DeltaMovingPoint my = new DeltaMovingPoint(prevMy);
            for ( MoveType moveType : shotTypeMap.get(enemyName) ) {
                double tankWidthRadians = Math.asin(Util.tankWidth/distance);
                
                if ( moveType.type == MoveType.TYPE_UNKNOWN ) {
                }else{
                    double diffRadians = Util.calcTurnRadians(bulletRadians,calcEnemyBulletRadians(prevMy,bulletVelocity,info.src,moveType.type,deltaTime));
                    double correctedRadians = Math.abs(diffRadians) - Math.abs(tankWidthRadians);
                    correctedRadians = (correctedRadians<0)?0:correctedRadians;
                    moveType.updateScore(Math.PI/2-correctedRadians);
// TODO: scoring by distance
System.out.println(moveType.type + " : " + Math.toDegrees(diffRadians) + " => " + Math.toDegrees(correctedRadians) + " = " + Math.toDegrees(moveType.score));
                }
            }

        }
    }

    private void sendMyInfo(){
        Enemy my = new Enemy();
        my.time = ctx.my.time +1;
        my.name = name;
        my.x = ctx.nextMy.x;
        my.y = ctx.nextMy.y;
        my.heading = ctx.nextMy.heading;
        my.headingRadians = ctx.nextMy.headingRadians;
        my.velocity = ctx.nextMy.velocity;
        my.energy = ctx.energy;
        broadcastMessage(new TeammateInfoEvent(my,isLeader));
    }
    private void dispatchMessage(MessageEvent e) {
        Serializable event = e.getMessage();
        if (event instanceof ScanEnemyEvent ) {
            ScanEnemyEvent ev = (ScanEnemyEvent)event;
            Enemy enemy = ev.e;
            enemy.calcPosition(ctx.my);
            scannedRobot(enemy);
        }else if (event instanceof TeammateInfoEvent ) {
            ctx.enemies--;
            TeammateInfoEvent ev = (TeammateInfoEvent)event;
            if ( ev.isLeader ) {
                leader = ev.e.name;
            }
            Enemy enemy = ev.e;
            enemy.calcPosition(ctx.my);
            scannedRobot(enemy);
        }else if (event instanceof BulletEvent ) {
            BulletEvent ev = (BulletEvent)event;
            setBulletInfo(ev.bulletInfo);
        }else if (event instanceof BulletImpactEvent ) {
            BulletImpactEvent ev = (BulletImpactEvent)event;
            impactBullet(ev.key);
        }else if (event instanceof CancelEnemyBalletEvent ) {
            CancelEnemyBalletEvent ev = (CancelEnemyBalletEvent)event;
            removeEnemyBulletInfo(ev.key);
        }else{
            cbExtMessage(e);
        }
    }

    private void goPoint(){
        Pair<Double,Double> go = calcGoPoint();
        if ( go == null ) {
            return;
        }
        setAhead(go.first);
        setTurnRight(go.second);
    }
    private void scannedRobot(ScannedRobotEvent e) {
        Enemy r = calcAbsRobot(e);
        if ( ! isTeammate(r.name)) {
            Enemy next = new Enemy(r);
            prospectNextEnemy(next);
            next.time++;
            this.broadcastMessage(new ScanEnemyEvent(next));
        }
        scannedRobot(r);
    }


    private void enemyPattern(){
        
    }
    
    
    
    
    protected void scannedRobot(Enemy r) {
        logger.scan("%15s : %s : %d",r.name,r,r.time);
        Enemy prevR = enemyMap.get(r.name);
        if ( prevR == null )  { // The first time
            if ( r.energy > 120 ) {
                r.role = Enemy.ROLE_LEADER;
            }else if ( r.energy > 100 ) {
                r.role = Enemy.ROLE_DROID;
            }else {
                r.role = Enemy.ROLE_ROBOT;
            }
        }
        if ( prevR != null && prevR.time >= r.time ) {
            return;
        }

        if ( ! shotTypeMap.containsKey(r.name) ) {
            // TODO: scanned
            List<MoveType> shotTypeList = new ArrayList<>();
            MoveType moveType = new MoveType(MoveType.TYPE_UNKNOWN);
            moveType.score = -1; // 
            shotTypeList.add(new MoveType(moveType));
            moveType = new MoveType(MoveType.TYPE_PINPOINT);
            shotTypeList.add(new MoveType(moveType));
            moveType = new MoveType(MoveType.TYPE_INERTIA);
            shotTypeList.add(new MoveType(moveType));
            moveType = new MoveType(MoveType.TYPE_ACCURATE);
            moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
            shotTypeList.add(new MoveType(moveType));
            shotTypeMap.put(r.name,shotTypeList);
        }
        if ( ! enemyLog.containsKey(r.name)) {
            enemyLog.put(r.name,new ArrayList());
        }

        if ( prevR != null ) {
            // Aimed
            if ( (prevR.energy - r.energy) >= 0.1 && (prevR.energy - r.energy) <= 3 ) {
                enemyBullet(prevR,r);
            }
            // Prev info
//            if ( r.time != prevR.time && (r.time-prevR.time) < SCAN_STALE || (nextEnemy != null && nextEnemy.calcDistance(r) < 20 ) ){
            if ( r.time != prevR.time && (r.time-prevR.time) < SCAN_STALE ) {
                r.setPrev(prevR);
            }
            r.setAimType(prevR.getAimType());
        }

        // TODO: seperate teammateMap
        enemyMap.put(r.name, r);
        ctx.nextEnemyMap.put(r.name, new Enemy(r));
        if ( ! isTeammate(r.name)) {
            enemyLog.get(r.name).add(new Enemy(r));
        }

//        int PAST = 40;
//            pattarnAvgMap.put("I", new HashMap<String,Pair<Long,Double>>());
//            pattarnAvgMap.put("A", new HashMap<String,Pair<Long,Double>>());
//            for ( int i = 1;i<=PAST;i++) {
//                pattarnAvgMap.put(String.valueOf(i), new HashMap<String,Pair<Long,Double>>());
//                pattarnPastMap.put(String.valueOf(i), new HashMap<String,Pair<Long,Double>>());
//            }
//        }
//        if ( ! pattarnAvgMap.get("I").containsKey(r.name) )  {
//            pattarnAvgMap.get("I").put(r.name, new Pair<>(0L,0.0));
//        }
//        if ( ! pattarnAvgMap.get("A").containsKey(r.name) )  {
//            pattarnAvgMap.get("A").put(r.name, new Pair<>(0L,0.0));
//        }
//
//        for ( int i = 1;i<=PAST;i++) {
//            if ( ! pattarnAvgMap.get(String.valueOf(i)).containsKey(r.name)) {
//                pattarnAvgMap.get(String.valueOf(i)).put(r.name, new Pair<>(0L,0.0));
//            }
//            if ( ! pattarnPastMap.get(String.valueOf(i)).containsKey(r.name)) {
//                pattarnPastMap.get(String.valueOf(i)).put(r.name, new Pair<>(0L,0.0));
//            }
//        }
//
//
//        if ( prevR != null ) {
//            List<MovingPoint> patternList = enemyPatternMap.get(r.name);
//            MovingPoint pt = new MovingPoint(r).diff(prevR);
//            patternList.add(0,pt);
//            // @@@
//            logger.LOGLV = Logger.LOGLV_SCAN;
//            {
//                Enemy rr = new Enemy(r);
//                Enemy p = new Enemy(prevR);
//                p.inertia(1);
//                rr.diff(p);
//                Pair<Long,Double> pp = pattarnAvgMap.get("I").get(r.name);
//                double distance = rr.calcDistance(new Point());
//                pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                logger.log("I : %2.2f / %d = %2.2f (%2.2f)",pp.second,pp.first,pp.second/pp.first,distance);
//            }
//            {
//                Enemy rr = new Enemy(r);
//                Enemy p = new Enemy(prevR);
//                p.prospectNext();
//                rr.diff(p);
//                Pair<Long,Double> pp = pattarnAvgMap.get("A").get(r.name);
//                double distance = rr.calcDistance(new Point());
//                pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                logger.log("A : %2.2f / %d = %2.2f (%2.2f)",pp.second,pp.first,pp.second/pp.first,distance);
//            }
//            {
//                for ( int N = 2; N <=PAST;N++) {
//                    if ( N < patternList.size() ) {
//                        MovingPoint diff = new MovingPoint();
//                        for ( int i = 2; i <=N; i++ ) {
//                            diff.add(patternList.get(i));
//                            diff.time = patternList.get(i).time;
//                        }
//                        diff.quot(N-1);
//
//                        Enemy rr = new Enemy(r);
//                        Enemy p = new Enemy(prevR);
//                        long diffTime = rr.time-diff.time;
//                        if ( diffTime > PAST) {
//                            break;
//                        }
//                        diff.time = 1;
//                        // logger.log("Diff(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f (%d)",N,diff.x,diff.y,diff.calcDistance(new Point()),diff.heading,diff.time - past.time);
//                        p.setDelta(diff);
//                        p.prospectNext();
//                        rr.diff(p);
//                        Pair<Long,Double> pp = pattarnAvgMap.get(String.valueOf(diffTime)).get(r.name);
//                        double distance = rr.calcDistance(new Point());
//                        pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                        logger.log("A%2d : %2.2f / %d = %2.2f (%2.2f)",diffTime,pp.second,pp.first,pp.second/pp.first,distance);
//                        // logger.log("Avrage(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f",N,rr.x,rr.y,rr.calcDistance(new Point()),rr.heading);
//
//                    }
//                }
//            }
//            {
//                for ( int N = 2; N <=PAST;N++) {
//                    if ( N < patternList.size() ) {
//
//                        MovingPoint diff = new MovingPoint(patternList.get(N));
//                        Enemy rr = new Enemy(r);
//                        Enemy p = new Enemy(prevR);
//                        long diffTime = rr.time-diff.time;
//                        if ( diffTime > PAST) {
//                            break;
//                        }
//                        diff.time = 1;
//                        // diff.diff(past).quot(diffTime);
//                        // logger.log("Diff(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f (%d)",N,diff.x,diff.y,diff.calcDistance(new Point()),diff.heading,diff.time - past.time);
//                        p.setDelta(diff);
//                        p.prospectNext();
//                        rr.diff(p);
//                        Pair<Long,Double> pp = pattarnPastMap.get(String.valueOf(diffTime)).get(r.name);
//                        double distance = rr.calcDistance(new Point());
//                        pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                        logger.log("P%2d : %2.2f / %d = %2.2f (%2.2f)",diffTime,pp.second,pp.first,pp.second/pp.first,distance);
//                        // logger.log("Avrage(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f",N,rr.x,rr.y,rr.calcDistance(new Point()),rr.heading);
//
//                    }
//                }
//            }
//        }
    }
    private void cbRobotDeath(RobotDeathEvent e) {
        // ctx.nextEnemyMap.remove(e.getName());
        Enemy enemy = getEnemy(e.getName());
        if ( enemy != null ) {
            enemy.time = 0;
        }
    }    
        
    protected final void setDestination(Point dst){
        logger.move_log("DST: %s", dst);
        this.ctx.destination = dst;
    }
    private void prospectNext(){
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            prospectNextEnemy(r);
        }
        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            info.src.inertia(1);
        }
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            info.src.inertia(1);
        }

    }    
    private void clearEnemyBullet(){
        List<String> rmEnemyBullet = new ArrayList<>();
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            if ( e.getValue().src.isLimit() ) {
                rmEnemyBullet.add(e.getKey());
            }
        }
        for( String n : rmEnemyBullet ) {
            removeEnemyBulletInfo(n);
        }
    }
    protected final Pair<Double,Double> calcGoPoint(){
        if ( ctx.destination == null ) {
            return null;
        }
        double bearing = ctx.my.calcDegree(ctx.destination);
        double distance = ctx.my.calcDistance(ctx.destination);

        double runTime = Util.calcRoughRunTime(distance,ctx.my.velocity);

        Pair<Double,Integer> turn = ctx.calcAbsTurn(bearing);
        double turnDegree = turn.first;
        distance *= turn.second;
        
        double turnTime = Math.abs(turnDegree/Util.turnSpeed(ctx.my.velocity));
        if ( runTime <= turnTime ) {
            distance = 0;
        }
        if ( Math.abs(distance) < MOVE_COMPLETE_THRESHOLD ) { 
            distance = 0.0;
        }
        return new Pair<>(distance,turnDegree);
    }
    
    protected final BaseContext defalutCreateContext(BaseContext in) {
        if ( in == null ) {
            return new BaseContext();
        }else{
            return new BaseContext(in);
        }
    }

    protected final T prospectNextMy(MyPoint nextMy,T curContext) {
        if ( ctx.my.time == 1 ) {
            return null;
        }
        T backupContext = ctx;
        if ( curContext == null ) {
            curContext =  createContext(ctx);
        }
        ctx = curContext;
        ctx.nextMy = nextMy;
        this.prospectNext();
        this.cbMoving();

        Pair<Double,Double> go = this.calcGoPoint();
        ctx.my = new MyPoint(nextMy);
        if ( go != null ) {
//        this.cbThinking();
//        
            MovingPoint delta = new MovingPoint();
            delta.time = 1;
            delta.heading = go.second;
            delta.headingRadians = Math.toRadians(go.second);
            delta.velocity = go.first;
            nextMy.setDelta(delta);
            nextMy.prospectNext();
        }
        nextMy.time++;
        ctx = backupContext;
//        nextMy.prospectNext();
//        if ( true ) {
//            return null;
//        }
        return curContext;
    }

    protected void enemyBullet(Enemy prev,Enemy enemy){

        Enemy cpPrev = new Enemy(prev);
        prospectNextEnemy(cpPrev);
        cpPrev.time ++;
        // Detect collsion
        for ( Map.Entry<String,BulletInfo> entry : enemyBulletList.entrySet() ) {
            BulletInfo info =  entry.getValue();
            if ( info.src.time == cpPrev.time && info.src.calcDistance(cpPrev) <= Util.tankSize * 1.5 ) {
                logger.fire4("ENEMY(collision): %s = %s dist(%2.2f)", info.src,cpPrev , info.src.calcDistance(cpPrev));
                removeEnemyBulletInfo(entry.getKey());
                return;// Maybe collision
                // TODO: teammate & myself
            }
        }
        // Detect wall
        for ( long i = prev.time; i < enemy.time; i++  ) {
            boolean isMove = prospectNextEnemy(cpPrev);
            if ( ! isMove ) {
                logger.fire4("ENEMY(wall): %s ", cpPrev);
                return; // Maybe hit wall
            }
        }
        // Bullet src
        cpPrev = new Enemy(prev);
        prospectNextEnemy(cpPrev);
        cpPrev.time++;
        MovingPoint src = cpPrev;

        if ( ! isTeammate(enemy.name) ) {
            MyPoint prevMy = prevMy(ctx.my.time-src.time+1);
            DeltaMovingPoint my = new DeltaMovingPoint(prevMy);
            double distance = src.calcDistance(my);
            MoveType shotType =getMoveType(shotTypeMap.get(enemy.name));
            double bulletVelocity = Util.bultSpeed(prev.energy-enemy.energy);
logger.LOGLV = Logger.LOGLV_FIRE3;
logger.fire3("ENEMY(fire): %d : %s(%2.2f)", shotType.type,Util.bultPower(bulletVelocity), Util.bultSpeed(prev.energy-enemy.energy));
//TODO: shot
            double radians = calcEnemyBulletRadians(my,bulletVelocity,src,shotType.type,0); //

            src.headingRadians = radians;
            src.heading  = Math.toDegrees(radians);
            src.velocity = bulletVelocity;
            BulletInfo bulletInfo = new BulletInfo(enemy.name,my,distance,src);
            setEnemyBulletInfo(bulletInfo);
        }
    }
    private MoveType getMoveType(List<MoveType> list) {
        MoveType ret = null;
        double score = Double.NEGATIVE_INFINITY;
        for( MoveType  moveType : list ) {
            if ( moveType.score > score ) {
                score =moveType.score;
                ret = moveType;
            }
        }
        return ret;
    }
    protected double calcEnemyBulletRadians(DeltaMovingPoint myAsEnemy,double velocity,Point src,int shotType,long deltaTime){
System.out.println(myAsEnemy.time);
        double distance = src.calcDistance(myAsEnemy);
        double ret = 0;
        if ( shotType == MoveType.TYPE_UNKNOWN ) {
        }else if ( shotType == MoveType.TYPE_PINPOINT ) {
            ret = src.calcRadians(myAsEnemy);
        }else if ( shotType == MoveType.TYPE_INERTIA1 ) {
            // ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(velocity/distance)))
            //  - Calc only fixed time ( hitted )
            //  - for prospecting time ( shoting ) =>  with updating distance each ticks.
            for ( int i = 1 ; i <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(velocity/distance))) ; i++ ) {
                DeltaMovingPoint cpMyAsEnemy = new DeltaMovingPoint(myAsEnemy);
                cpMyAsEnemy.inertia(1);
                distance = src.calcDistance(cpMyAsEnemy);
                ret = src.calcRadians(cpMyAsEnemy);
                if ( distance - Math.abs(velocity*i) < (Util.tankWidth/2) ) { // hit ?
logger.log("INERTIA1 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(ret),distance,Math.abs(velocity*i),distance - Math.abs(velocity*i)); 
                    break;
                }
            }
        }else if ( shotType == MoveType.TYPE_INERTIA2 ) {
            for ( int i = 1 ; i <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(velocity/distance))) ; i++ ) {
                DeltaMovingPoint cpMyAsEnemy = new DeltaMovingPoint(myAsEnemy);
                cpMyAsEnemy.inertia(1);
                distance = src.calcDistance(cpMyAsEnemy);
                ret = src.calcRadians(cpMyAsEnemy);
                if ( distance - Math.abs(velocity*i) < 0 ) { // hit ?
logger.log("INERTIA2 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(ret),distance,Math.abs(velocity*i),distance - Math.abs(velocity*i)); 
                    break;
                }
            }
        }else if ( shotType == MoveType.TYPE_ACCURATE1 ) {
            for ( int i = 1 ; i <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(velocity/distance))) ; i++ ) {
                DeltaMovingPoint cpMyAsEnemy = new DeltaMovingPoint(myAsEnemy);
                distance = src.calcDistance(cpMyAsEnemy);
                ret = src.calcRadians(cpMyAsEnemy);
                if ( distance - Math.abs(velocity*i) < 0 ) { // hit ?
logger.log("ACCURATE1 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(ret),distance,Math.abs(velocity*i),distance - Math.abs(velocity*i)); 
                    break;
                }
            }
        }else if ( shotType == MoveType.TYPE_ACCURATE2 ) {
            for ( int i = 1 ; i <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(velocity/distance))) ; i++ ) {
                DeltaMovingPoint cpMyAsEnemy = new DeltaMovingPoint(myAsEnemy);
// TODO : @@@ PAINT
                cpMyAsEnemy.prospectNext();
getGraphics().setColor(new Color(255,255,255));
drawRound(getGraphics(),cpMyAsEnemy.x,cpMyAsEnemy.y,2);

                distance = src.calcDistance(cpMyAsEnemy);
                ret = src.calcRadians(cpMyAsEnemy);
                if ( distance - Math.abs(velocity*i) < 0 ) { // hit ?
logger.log("ACCURATE2 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(ret),distance,Math.abs(velocity*i),distance - Math.abs(velocity*i)); 
                    break;
                }
            }
//        }else if ( shotType == MoveType.TYPE_ACCURATE1 ) {
//            // Recursive calculating
//            double distance = src.calcDistance(myAsEnemy);
//            for (int i = 0 ; i < MAX_CALC ; i++ ) {
//                DeltaMovingPoint cpMyAsEnemy = new DeltaMovingPoint(myAsEnemy);
//                for ( int j = 0 ; j < Math.abs(distance/velocity); j++ ) {
//                }
//                distance = src.calcDistance(cpMyAsEnemy);
//                ret = src.calcRadians(cpMyAsEnemy);
//                double d = Util.calcPointToLineRange(src,cpMyAsEnemy,ret);
//                if ( d < (Util.tankWidth/2) ) { // hit ?
//                    break;
//                }
//            }
        }


        return ret;
    }


    protected final void fire(double power, double distance,String targetName ) {
        if ( ctx.gunHeat != 0 ) {
            return;
        }
        logger.fire1("FIRE( power => bearing): ( %2.2f ) => %2.2f",power,ctx.curGunHeading);
        this.paint(getGraphics());
        double bulletVelocity = Util.bultSpeed(power);
        MovingPoint src = new MovingPoint(
                ctx.my.x , ctx.my.y , ctx.my.time,
                ctx.curGunHeading , ctx.curGunHeadingRadians,
                bulletVelocity
        );
        Enemy enemy = enemyMap.get(targetName);
        if ( enemy != null ) {
            enemy.getAimType().updateAim(distance/bulletVelocity);
        }
        BulletInfo bulletInfo = new BulletInfo(this.name,new DeltaMovingPoint(enemy),distance,src);
        setBulletInfo(bulletInfo);
        broadcastMessage(new BulletEvent(bulletInfo));
        super.fire(power);
    }
    
    public boolean isStale(Enemy e) {
         if ( e == null || (ctx.my.time - e.time) > SCAN_STALE ) {
             return true;
         }
         return false;
    }

    public Enemy getEnemy(String name) {
        Enemy ret = ctx.nextEnemyMap.get(name);
        if ( isStale(ret) ) {
            return null;
        }
        return ret;
    }
    
    abstract protected T createContext(T in);
    protected void cbThinking() {}
    protected void cbMoving() {}
    protected void cbUnprospectiveMoving() {}
    protected void cbGun() {}
    protected void cbRadar() {}
    protected void cbFiring() {}
    protected void cbFirst() {}

    protected boolean prospectNextEnemy(Enemy enemy) {
        return enemy.prospectNext(ctx.my);
    }
    protected void prospectNextEnemy(Enemy enemy,int interval) {
        for (int i = 1; i <= interval; i++) {
            prospectNextEnemy(enemy);
        }
    }

    protected void cbStatus(StatusEvent e){}

    protected void cbHitWall(HitWallEvent e) {
        logger.crash("CLASH WALL: %s : %f",ctx.my,e.getBearing());
    }
    protected void cbHitRobot(HitRobotEvent e) {
        logger.crash("CLASH (%s): %s : %f",e.getName(),ctx.my,e.getBearing());
    }
    protected void cbExtMessage(MessageEvent e) {
        
    }

    protected Enemy calcAbsRobot(ScannedRobotEvent e) {
        return new Enemy(ctx.my, e);
    }    


    
    
    
  


    private void forSystemBug(){
        if ( enemyMap.isEmpty() ) {
            return;
        }
        boolean allStale = true;
        for (Map.Entry<String, Enemy> e : enemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( ctx.my.time - r.time < SYSTEM_BUG_TICKS) {
                allStale = false;
                break;
            }
        }
        if ( allStale ){
            logger.log("ALL STALE !!!!!!!!!!! : "); 
            execute();
        }
    }
    
    @Override
    public void run() {
        Util.init(
                getBattleFieldWidth(),
                getBattleFieldHeight(),
                getWidth(),
                getHeight(),
                getGunCoolingRate()
                );
        initEventPriority();
        name = getName();
        String [] array =this.getTeammates();
        if ( array != null ) {
            teammate.addAll(Arrays.asList(array));
            if ( getEnergy() == 200 ) {
                isLeader = true;
                leader = name;
            }
        }
        addCustomEvent(this.firstTickTimer);
        addCustomEvent(this.eachTickTimer);
        execute();
    }
  @Override
    public boolean isTeammate(String name) {
        return teammate.contains(name);
    }

    @Override
    public void broadcastMessage(Serializable e ){
        try {
            super.broadcastMessage(e);
        } catch (IOException ex) {
            logger.log("Send message error %s", ex.getMessage() );
        }
    }
    @Override
    public void sendMessage(String name,Serializable e ){
        try {
            super.sendMessage(name,e);
        } catch (IOException ex) {
            logger.log("Send message error %s", ex.getMessage() );
        }
    }

    @Override
    public void setAhead(double distance) {
        super.setAhead(distance);
        ctx.curDistanceRemaining = distance;
    }
    
    @Override
    public void setTurnRight(double degrees) {
        super.setTurnRight(degrees);
        ctx.curTurnRemaining = degrees;
        ctx.curTurnRemainingRadians = Math.toRadians(degrees);
    }

    @Override
    public void setTurnGunRight(double degrees) {
        logger.gun3("TURN: %2.2f : %2.2f => %2.2f",ctx.curGunHeading,ctx.curGunTurnRemaining,degrees);
        super.setTurnGunRight(degrees);
        ctx.curGunTurnRemaining = degrees;
        ctx.curGunTurnRemainingRadians = Math.toRadians(degrees);
    }

    @Override
    public void setTurnRadarRight(double degrees) {
        logger.radar3("TURN: %2.2f : %2.2f => %2.2f",ctx.curRadarHeading,ctx.curRadarTurnRemaining,degrees);
        super.setTurnRadarRight(degrees);
        ctx.curRadarTurnRemaining = degrees;
        ctx.curRadarTurnRemainingRadians = Math.toRadians(degrees);
    }
    
    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        dumpLog();
    }
    
    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        dumpLog();
    }
    
    @Override
    public void onCustomEvent(CustomEvent event) {
        forSystemBug();
        this.setInterruptible(true);
        current();
        if (event.getCondition().equals(this.firstTickTimer) ) {
            this.removeCustomEvent(firstTickTimer);
            cbFirst();
            execute();
            return;
        }
        if (event.getCondition().equals(this.eachTickTimer) ) {
            sendMyInfo();
            
            for ( ScannedRobotEvent e: this.getScannedRobotEvents() ) {
                this.scannedRobot(e);
            }
            for ( MessageEvent e : this.getMessageEvents() ) {
                this.dispatchMessage(e);
            }
            for ( BulletHitBulletEvent e: this.getBulletHitBulletEvents() ) {
                this.bulletHitBullet(e);
            }
            for ( BulletHitEvent e: this.getBulletHitEvents() ) {
                this.bulletHit(e);
            }
            for ( BulletMissedEvent e: this.getBulletMissedEvents() ) {
                this.bulletMissed(e);
            }
            for ( RobotDeathEvent e: this.getRobotDeathEvents() ) {
                this.cbRobotDeath(e);
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
            for ( StatusEvent e: this.getStatusEvents() ) {
                this.cbStatus(e);
            }
            this.prospectNext();
            this.clearEnemyBullet();
            this.cbThinking();
            this.cbMoving();
            this.goPoint();
            this.cbUnprospectiveMoving();
            this.cbGun();
            this.cbRadar();
            this.cbFiring();
        }
        this.paint(getGraphics());
        execute();
    }
    
    protected static void drawRound(Graphics2D g, double x, double y, double r) {
        if (isPaint) {
            g.drawRoundRect((int) (x - r / 2), (int) (y - r / 2), (int) r, (int) r, (int) r + 2, (int) r + 2);
        }
    }

    protected static final float PAINT_OPACITY=0.5f;
    protected void paint(Graphics2D g) {
        if (isPaint) {
            this.drawRound(g, ctx.my.x, ctx.my.y, 400 * 2);
            drawRound(g, ctx.my.x, ctx.my.y, 600 * 2);
            float[] dash = new float[2];
            dash[0] = 0.1f;
            dash[1] = 0.1f;
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, dash, 0.0f));
            drawRound(g, ctx.my.x, ctx.my.y, 100 * 2);
            drawRound(g, ctx.my.x, ctx.my.y, 300 * 2);
            drawRound(g, ctx.my.x, ctx.my.y, 500 * 2);

            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(0, 0.7f, 0, PAINT_OPACITY));

            double fieldMax = new Point(Util.battleFieldWidth, Util.battleFieldHeight).calcDistance(new Point());
            g.drawLine((int) ctx.my.x, (int) ctx.my.y, (int) (Math.sin(ctx.curGunHeadingRadians) * fieldMax + ctx.my.x), (int) (Math.cos(ctx.curGunHeadingRadians) * fieldMax + ctx.my.y));

            double deltaRadians = Util.calcTurnRadians(ctx.curRadarHeadingRadians, ctx.prevRadarHeadingRadians) / 10;
            if (deltaRadians != 0.0) {
                int[] xs = new int[3];
                int[] ys = new int[3];
                xs[0] = (int) ctx.my.x;
                ys[0] = (int) ctx.my.y;
                double radians = ctx.curRadarHeadingRadians;
                for (int i = 1; i < 10; i++) {
                    xs[1] = (int) (Math.sin(radians) * fieldMax + ctx.my.x);
                    ys[1] = (int) (Math.cos(radians) * fieldMax + ctx.my.y);
                    radians += deltaRadians;
                    xs[2] = (int) (Math.sin(radians) * fieldMax + ctx.my.x);
                    ys[2] = (int) (Math.cos(radians) * fieldMax + ctx.my.y);
                    g.setColor(new Color(i * 0.03f, i * 0.03f, 1.0f, 0.1f));
                    Polygon triangle = new Polygon(xs, ys, 3);
                    g.fill(triangle);
                }
            } else {
                g.setColor(new Color(0.03f, 0.03f, 1.0f, 0.1f));
                g.drawLine((int) ctx.my.x, (int) ctx.my.y, (int) (Math.sin(ctx.curRadarHeadingRadians) * fieldMax + ctx.my.x), (int) (Math.cos(ctx.curRadarHeadingRadians) * fieldMax + ctx.my.y));
            }

            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(0, 1.0f, 0, PAINT_OPACITY));
            g.drawString(String.format("( %2.2f , %2.2f )", ctx.my.x, ctx.my.y), (int) ctx.my.x - 20, (int) ctx.my.y - 55);
            g.drawString(String.format("heat: %2.2f", getGunHeat()), (int) ctx.my.x - 20, (int) ctx.my.y - 65);
            g.drawString(String.format("velo: %2.1f", getVelocity()), (int) ctx.my.x - 20, (int) ctx.my.y - 75);
            MyPoint mypoint = new MyPoint(ctx.nextMy);
            T curCtx = null;
            for (int i = 0; i < 20; i++) {
                drawRound(g, mypoint.x, mypoint.y, 2);
                curCtx = prospectNextMy(mypoint, curCtx);
            }
            g.setStroke(new BasicStroke(4.0f));
            g.setColor(new Color(0, 1.0f, 0, PAINT_OPACITY));
            if (ctx.destination != null) {
                drawRound(g, ctx.destination.x, ctx.destination.y, 10);
            }

            g.setStroke(new BasicStroke(1.0f));
            for (Map.Entry<String, Enemy> e : enemyMap.entrySet()) {
                Enemy r = e.getValue();
                if (teammate.contains(r.name)) {
                    g.setColor(new Color(0, 1.0f, 0, PAINT_OPACITY));
                } else {
                    g.setColor(new Color(0, 1.0f, 1.0f, PAINT_OPACITY));
                }
                drawRound(g, r.x, r.y, 35);
                g.drawString(String.format("%s : %s", r.name, r), (int) r.x - 20, (int) r.y - 30);
                g.drawString(String.format("hit: %d / %d  : %2.2f / %2.2f", r.getAimType().hitCount, r.getAimType().aimCount, r.getAimType().hitTime, r.getAimType().aimTime), (int) r.x - 20, (int) r.y - 40);
                Enemy next = getEnemy(r.name);
                if (next != null) {
//                g.drawString(String.format("( %2.2f , %2.2f )", next.x , next.y), (int) r.x - 20, (int) r.y- 45);
                    g.drawString(String.format("dist(degr): %2.2f(%2.2f)", next.distance, next.bearing), (int) r.x - 20, (int) r.y - 50);
                    g.drawString(String.format("head(velo): %2.2f(%2.2f)", next.heading, next.velocity), (int) r.x - 20, (int) r.y - 60);
                    g.setColor(new Color(0.2f, 1.0f, 0.7f, PAINT_OPACITY));

                    //g.setColor(new Color(0.4f, 0.7f, 1.0f,PAINT_OPACITY));
                    drawRound(g, next.x, next.y, 35);
                    Enemy enemy = new Enemy(next);
                    for (int i = 1; i < 20; i++) {
                        prospectNextEnemy(enemy);
                        drawRound(g, enemy.x, enemy.y, 2);
                    }
                }
            }
            for (Map.Entry<String, BulletInfo> e : bulletList.entrySet()) {
                BulletInfo info = e.getValue();
                g.setStroke(new BasicStroke(1.0f));
                g.setColor(new Color(0, 1.0f, 0, PAINT_OPACITY));
                g.drawLine((int) info.src.x, (int) info.src.y, (int) (Math.sin(info.src.headingRadians) * fieldMax + info.src.x), (int) (Math.cos(info.src.headingRadians) * fieldMax + info.src.y));
                g.setStroke(new BasicStroke(4.0f));
                Point dst = Util.calcPoint(info.src.headingRadians, info.distance).add(info.src);
                drawRound(g, dst.x, dst.y, 5);
            }
            for (Map.Entry<String, BulletInfo> e : enemyBulletList.entrySet()) {
                BulletInfo info = e.getValue();
                g.setStroke(new BasicStroke(1.0f));
                g.setColor(new Color(1.0f, 0.5f, 0.5f, PAINT_OPACITY));
                g.drawLine((int) info.src.x, (int) info.src.y, (int) (Math.sin(info.src.headingRadians) * fieldMax + info.src.x), (int) (Math.cos(info.src.headingRadians) * fieldMax + info.src.y));
                g.setStroke(new BasicStroke(4.0f));
                Point dst = Util.calcPoint(info.src.headingRadians, info.distance).add(info.src);
                drawRound(g, dst.x, dst.y, 5);
            }

//            g.setColor(new Color(0, 0, 255));
//            Point i = r.inertia(ctx.my.time - r.time);
//            drawRound(g, i.x, i.y, 40);
//            g.setColor(new Color(255, 0, 255));
//            Point p = r.prospect(ctx.my.time - r.time);
//            drawRound(g, p.x, p.y, 40);
//            for( MovingPoint p : ctx.enemyPatternMap.get(lockon) ) {
//                System.out.println(p);
//            }
//        }
        }
    }

    private void dumpLog(){
        List<Enemy> enemyArray = new ArrayList(enemyMap.values());
        Collections.sort(enemyArray,new Comparator<Enemy>(){
            @Override
            public int compare(Enemy o1, Enemy o2) {
                return o1.getAimType().compareTo(o2.getAimType());
            }});
        for (Enemy enemy :  enemyArray ) {
            logger.log("=== %s (%2.2f%%)===",enemy.name,enemy.getAimType().getHitRate()*100);
            logger.log("aim : %2.2f(%d)",enemy.getAimType().aimTime,enemy.getAimType().aimCount);
            logger.log("hit : %2.2f(%d)",enemy.getAimType().hitTime,enemy.getAimType().hitCount);
        }        
    }
    
}

