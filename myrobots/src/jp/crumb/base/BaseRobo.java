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
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
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
    protected static final double MOVE_COMPLETE_THRESHOLD = 1.0;

    protected static final int MAX_CALC = 5;
    protected static final int SCAN_STALE = 9;
    protected static final int SYSTEM_BUG_TICKS = 30;
      
    protected Logger logger = new Logger();
    protected T ctx = createContext(null);
    
   
    protected static Set<String> teammate = new HashSet<>();
    protected static boolean isLeader = false;
    protected static String leader = null;
    // Current informations
    private static Map<String, Enemy> enemyMap = new HashMap<>();
    private Map<String,BulletInfo> bulletList = new HashMap<>();
    protected static Map<String, List<MovingPoint> > enemyPatternMap = new HashMap<>();

    protected static String name;
    
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
    }        

    private Map.Entry<String,BulletInfo> calcBulletSrc(Bullet bullet) {
        Point dst = new Point(bullet.getX(),bullet.getY());
        double bulletRadians = bullet.getHeadingRadians();
        
        Map.Entry<String,BulletInfo> cur = null;
        double curDiffDistance = 0;
        double curDiffRadians = 0;
        for(Map.Entry<String,BulletInfo> e : bulletList.entrySet() ) {
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
            broadcastMessage(new ImpactEvent(cur.getKey()));
            impactBullet(cur.getKey());
            return cur;
        }
        logger.log("Unknown bullet: ");
        return null;
    }
    private void bulletMissed(BulletMissedEvent e) {
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        calcBulletSrc(bullet);
        logger.gun1("MISS: %s",dst);
    }
    
    private void bulletHit(BulletHitEvent e){
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        String victim = bullet.getVictim();

        double range = 0.0;
        double aimDistance = 0.0;
        BulletInfo info = null;
        Map.Entry<String,BulletInfo> entry = calcBulletSrc(bullet);
        if ( entry != null ){
            info = entry.getValue();
            aimDistance = entry.getValue().distance;
            range = info.src.calcDistance(dst);
        }
        // Judge by chance 
        if ( info != null && info.target.equals(victim) && Math.abs(aimDistance - range) < new Point(Util.tankWidth,0).calcDistance(new Point(0,Util.tankHeight)) ) {
            Enemy target = enemyMap.get(victim);
            if ( target != null ) {
                target.getAimType().updateHitRange(range);
            }
            logger.gun1("HIT: %s : %2.2f(%2.2f)  %s => %s",victim,aimDistance,range,info.src,dst);
        }else{
            if ( info != null ) {
                logger.gun1("HIT (by chance): %s(%s): %2.2f(%2.2f)  %s => %s",victim,info.target,aimDistance,range,info.src,dst);
            }else{
                logger.gun1("HIT (by chance): %s: %2.2f(%2.2f)  %s => %s",victim,aimDistance,range,info.src,dst);
            }
        }
    }
    private void bulletHitBullet(BulletHitBulletEvent e){
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        logger.gun1("INTERCEPT: %s",dst);
        Map.Entry<String,BulletInfo> entry = calcBulletSrc(bullet);
        if ( entry != null ){
            BulletInfo info = entry.getValue();
            Enemy target = enemyMap.get(info.target);
            target.getAimType().revartAimRange(info.distance);
        }
    }
    
    private void setBulletInfo(BulletInfo bulletInfo) {
        bulletList.put(bulletInfo.bulletName,bulletInfo);
        ctx.nextBulletList.put(bulletInfo.bulletName,new BulletInfo(bulletInfo));
    }
    private void impactBullet(String key){
        ctx.nextBulletList.remove(key);
        bulletList.remove(key);
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
        }else if (event instanceof FireEvent ) {
            FireEvent ev = (FireEvent)event;
            setBulletInfo(ev.bulletInfo);
        }else if (event instanceof ImpactEvent ) {
            ImpactEvent ev = (ImpactEvent)event;
            impactBullet(ev.key);
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
    private void scannedRobot(Enemy r) {
        logger.scan("%15s : %s : %d",r.name,r,r.time);
        Enemy prevR = enemyMap.get(r.name);
        if ( prevR != null && prevR.time == r.time ) {
            return;
        }
        Enemy nextEnemy =  getEnemy(r.name);

        if ( prevR != null ) {
//            if ( r.time != prevR.time && (r.time-prevR.time) < SCAN_STALE || (nextEnemy != null && nextEnemy.calcDistance(r) < 20 ) ){
            if ( r.time != prevR.time && (r.time-prevR.time) < SCAN_STALE ) {
                r.setPrev(prevR);
            }
            r.setAimType(prevR.getAimType());
        }
        
        enemyMap.put(r.name, r);
        
        if ( ! enemyPatternMap.containsKey(r.name)) {
            enemyPatternMap.put(r.name,new ArrayList());
        }
        

        List<MovingPoint> patternList = enemyPatternMap.get(r.name);
        if ( patternList.isEmpty() || nextEnemy == null ) { // first or stale
            MovingPoint first = new MovingPoint();
            first.time = r.time;
            patternList.add(first);
        }else{
            MovingPoint lastPattern = patternList.get(patternList.size()-1);
            long deltaTime = r.time - lastPattern.time;
            for (int i = 1 ; i <= deltaTime ; i++ ) {
                MovingPoint p = new MovingPoint(r).diff(nextEnemy).quot(deltaTime);
                p.time = lastPattern.time + i;
                patternList.add(p);
            }
        }

        ctx.nextEnemyMap.put(r.name, new Enemy(r));
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
        
        
//        double nextBearing = ctx.nextMy.calcDegree(ctx.destination);
//        double nextDistance = ctx.nextMy.calcDistance(ctx.destination);
//        Pair<Double,Integer> nextTurn = this.calcAbsTurn(nextBearing);
//        if ( Math.abs(nextTurn.first) > Math.abs(turnDegree) || Math.abs(nextDistance) > Math.abs(distance) ) {
//            distance = 0;
//        }
        
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
//        nextMy.prospectNext();
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
//        if ( true ) {
//            return null;
//        }
        return curContext;
    }
    protected final void fire(double power, double distance,String targetName ) {
        if ( ctx.gunHeat != 0 ) {
            return;
        }
        logger.gun2("FIRE( power => bearing): ( %2.2f ) => %2.2f",power,ctx.curGunHeading);
        this.paint(getGraphics());
        Enemy enemy = enemyMap.get(targetName);
        if ( enemy != null ) {
            enemy.getAimType().updateAimRange(distance);
        }
        MovingPoint src = new MovingPoint(
                ctx.my.x , ctx.my.y , ctx.my.time,
                ctx.curGunHeading , ctx.curGunHeadingRadians,
                Util.bultSpeed(power)
        );
        BulletInfo bulletInfo = new BulletInfo(this.name,targetName,distance,src);
        setBulletInfo(bulletInfo);
        broadcastMessage(new FireEvent(bulletInfo));
        super.fire(power);
    }
    
    
    public boolean isStale(Enemy e) {
         if ( (ctx.my.time - e.time) > SCAN_STALE ) {
             return false;
         }
         return true;
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
    protected void cbHitByBullet(HitByBulletEvent e) {}

    protected void prospectNextEnemy(Enemy enemy) {
        enemy.prospectNext(ctx.my);
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

        if ( ctx.nextEnemyMap.isEmpty() ) {
            return;
        }
        boolean allStale = true;
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
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
        g.drawRoundRect((int) (x - r / 2), (int) (y - r / 2), (int) r, (int) r, (int) r + 2, (int) r + 2);
    }

    protected static final float PAINT_OPACITY=0.5f;
    protected void paint(Graphics2D g) {
        this.drawRound(g, ctx.my.x, ctx.my.y, 400 * 2);
        drawRound(g, ctx.my.x, ctx.my.y, 600 * 2);
        float[] dash = new float[2];
        dash[0] = 0.1f;
        dash[1] = 0.1f;
        g.setStroke(new BasicStroke(1.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,1.0f,dash,0.0f));
        drawRound(g, ctx.my.x, ctx.my.y, 100 * 2);
        drawRound(g, ctx.my.x, ctx.my.y, 300 * 2);
        drawRound(g, ctx.my.x, ctx.my.y, 500 * 2);

        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0, 0.7f, 0, PAINT_OPACITY));
        
        double fieldMax = new Point(Util.battleFieldWidth,Util.battleFieldHeight).calcDistance(new Point());
        g.drawLine((int)ctx.my.x,(int)ctx.my.y,(int)(Math.sin(ctx.curGunHeadingRadians)*fieldMax+ctx.my.x),(int)(Math.cos(ctx.curGunHeadingRadians)*fieldMax+ctx.my.y));

        double deltaRadians = Util.calcTurnRadians(ctx.curRadarHeadingRadians,ctx.prevRadarHeadingRadians)/10;
        if ( deltaRadians != 0.0 ) {
            int[] xs = new int[3];
            int[] ys = new int[3];
            xs[0] = (int)ctx.my.x;
            ys[0] = (int)ctx.my.y;
            double radians  = ctx.curRadarHeadingRadians;
            for(int i = 1 ; i < 10; i++) {
                xs[1] = (int)(Math.sin(radians)*fieldMax+ctx.my.x);
                ys[1] = (int)(Math.cos(radians)*fieldMax+ctx.my.y);
                radians += deltaRadians;
                xs[2] = (int)(Math.sin(radians)*fieldMax+ctx.my.x);
                ys[2] = (int)(Math.cos(radians)*fieldMax+ctx.my.y);
                g.setColor(new Color( i*0.03f,i*0.03f,1.0f, 0.1f));
                Polygon triangle = new Polygon(xs,ys,3);
                g.fill(triangle);
            }
        }else{
            g.setColor(new Color( 0.03f,0.03f,1.0f, 0.1f));
            g.drawLine((int)ctx.my.x,(int)ctx.my.y,(int)(Math.sin(ctx.curRadarHeadingRadians)*fieldMax+ctx.my.x),(int)(Math.cos(ctx.curRadarHeadingRadians)*fieldMax+ctx.my.y));
        }
        
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0,1.0f,0,PAINT_OPACITY));
        g.drawString(String.format("( %2.2f , %2.2f )", ctx.my.x , ctx.my.y), (int) ctx.my.x - 20, (int) ctx.my.y- 55);
        g.drawString(String.format("heat: %2.2f", getGunHeat()), (int) ctx.my.x - 20, (int) ctx.my.y- 65);
        g.drawString(String.format("velo: %2.1f", getVelocity()), (int) ctx.my.x - 20, (int) ctx.my.y- 75);
        MyPoint mypoint = new MyPoint(ctx.nextMy);
        T curCtx = null;
        for ( int i = 0 ; i < 20; i++) {
            drawRound(g,mypoint.x,mypoint.y,2);
            curCtx = prospectNextMy(mypoint,curCtx);
        }
        g.setStroke(new BasicStroke(4.0f));
        g.setColor(new Color(0, 1.0f, 0,PAINT_OPACITY));
        if ( ctx.destination != null ) {
            drawRound(g,ctx.destination.x,ctx.destination.y,10);
        }

        g.setStroke(new BasicStroke(1.0f));
        for (Map.Entry<String, Enemy> e : enemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( teammate.contains(r.name ) ) {
                g.setColor(new Color(0, 1.0f, 0,PAINT_OPACITY));
            }else {
                g.setColor(new Color(0, 1.0f, 1.0f,PAINT_OPACITY));
            }
            drawRound(g, r.x, r.y, 35);            
            g.drawString(String.format("%s : %s", r.name , r), (int) r.x - 20, (int) r.y- 30);
            g.drawString(String.format("hit: %d / %d  : %2.2f / %2.2f", r.getAimType().hit,r.getAimType().aim,r.getAimType().hitrange,r.getAimType().aimrange), (int) r.x - 20, (int) r.y - 40);
            Enemy next = getEnemy(r.name);
            if ( next != null ) {
//                g.drawString(String.format("( %2.2f , %2.2f )", next.x , next.y), (int) r.x - 20, (int) r.y- 45);
                g.drawString(String.format("dist(degr): %2.2f(%2.2f)", next.distance,next.bearing), (int) r.x - 20, (int) r.y - 50);
                g.drawString(String.format("head(velo): %2.2f(%2.2f)", next.heading,next.velocity), (int) r.x - 20, (int) r.y - 60);
                g.setColor(new Color(0.2f, 1.0f, 0.7f,PAINT_OPACITY));

                //g.setColor(new Color(0.4f, 0.7f, 1.0f,PAINT_OPACITY));
                drawRound(g, next.x, next.y, 35);            
                Enemy enemy = new Enemy(next);
                for ( int i = 1 ; i < 20; i++) {
                    prospectNextEnemy(enemy);
                    drawRound(g,enemy.x,enemy.y,2);
                }
            }
        }

        for ( Map.Entry<String,BulletInfo> e : bulletList.entrySet() ) {
            BulletInfo info = e.getValue();
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(1.0f, 0, 0,PAINT_OPACITY));
            g.drawLine((int)info.src.x,(int)info.src.y,(int)(Math.sin(info.src.headingRadians)*fieldMax+info.src.x),(int)(Math.cos(info.src.headingRadians)*fieldMax+info.src.y));
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

    private void dumpLog(){
        List<Enemy> enemyArray = new ArrayList(enemyMap.values());
        Collections.sort(enemyArray,new Comparator<Enemy>(){
            @Override
            public int compare(Enemy o1, Enemy o2) {
                return o1.getAimType().compareTo(o2.getAimType());
            }});
        for (Enemy enemy :  enemyArray ) {
            logger.log("=== %s (%2.2f%%)===",enemy.name,enemy.getAimType().getHitRate()*100);
            logger.log("aim : %2.2f(%d)",enemy.getAimType().aimrange,enemy.getAimType().aim);
            logger.log("hit : %2.2f(%d)",enemy.getAimType().hitrange,enemy.getAimType().hit);
        }        
    }
    
}

