/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.ace;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.crumb.CrumbRobot;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.Util;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.HitByBulletEvent;
import robocode.MessageEvent;


/**
 *
 * @author crumb
 */
abstract public class AceRobot<T extends AceContext> extends CrumbRobot<T> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(255,255,255), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(200,255,100));
    }

    protected static final double SHOT_SCORE_ESTIMATE_LIMIT = 10;
    protected static final double AIM_SCORE_ESTIMATE_LIMIT = 10;

    protected static final double ENEMY_BULLET_DIFF_THRESHOLD = Math.PI/4; // more than 45 degrees
    protected static final double BULLET_DIFF_THRESHOLD = Math.PI/4; // more than 45 degrees

    protected static final long   DEFAULT_ENEMY_BULLET_PROSPECT_TIME = 20;
    protected static final double DEFAULT_ENEMY_BULLET_WEIGHT = 1000;
    protected static final double DEFAULT_ENEMY_BULLET_DIM = 4;
    protected static final double DEFAULT_ENEMY_BULLET_DISTANCE = 400;


    // For move
    protected  long   ENEMY_BULLET_PROSPECT_TIME   = DEFAULT_ENEMY_BULLET_PROSPECT_TIME;
    protected  double ENEMY_BULLET_WEIGHT          = DEFAULT_ENEMY_BULLET_WEIGHT;
    protected  double ENEMY_BULLET_DIM             = DEFAULT_ENEMY_BULLET_DIM;
    protected  double ENEMY_BULLET_DISTANCE        = DEFAULT_ENEMY_BULLET_DISTANCE;


    private Map<String,BulletInfo> enemyBulletList = new HashMap<>();
    protected static Map<String,List<MoveType>> shotTypeMap = new HashMap<>();
    protected static Map<String,List<MoveType>> aimTypeMap = new HashMap();

    protected Map<Long,RobotPoint> myLog = new HashMap<>();
    protected static Map<String, Map<Long,Enemy> > enemyLog = new HashMap<>();

    protected List<MoveType> initialShotTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>();
        MoveType moveType = new MoveType(MoveType.TYPE_PINPOINT);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA_FIRST);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA_CENTER);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCURATE_FIRST);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCURATE_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }
    protected List<MoveType> initialAimTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>();
        MoveType moveType = new MoveType(MoveType.TYPE_PINPOINT);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA_FIRST);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA_CENTER);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCURATE_FIRST);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCURATE_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }
    @Override
    protected MoveType getAimType(String name) {
        if ( aimTypeMap.get(name) == null ) {
            return new MoveType(MoveType.TYPE_ACCURATE_FIRST);
        }
        return MoveType.getByScore(aimTypeMap.get(name));
    }

    @Override
    protected AceContext createContext(AceContext in) {
        if ( in == null ) {
            return new AceContext();
        }
        return new AceContext(in);
    }

    @Override
    protected void updateCurrent() {
        super.updateCurrent();
        myLog.put(ctx.my.timeStamp,ctx.my);
    }
    protected RobotPoint logMy(long absTime) {
        return myLog.get(absTime);
    }
    protected RobotPoint logEnemy(String name,long absTime) {
        Map<Long,Enemy> log = enemyLog.get(name);
        if ( log != null ) {
            return log.get(absTime);
        }
        return null;
    }

    @Override
    protected Point movingBase() {
        Point dst = super.movingBase();
        // Enemy Bullet
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            if ( info.distance < ENEMY_BULLET_DISTANCE || ctx.enemies == 1 )  {
                if ( ! info.owner.equals(name) ){
                    MovingPoint bullet = new MovingPoint(info.src);
                    for ( int i = 0 ; i < ENEMY_BULLET_PROSPECT_TIME;i++) {
                        dst.diff(Util.getGrabity(ctx.my,bullet, ENEMY_BULLET_WEIGHT,ENEMY_BULLET_DIM));
                        bullet.inertia(1);
                    }
                }
            }
        }
        return dst;
    }

    @Override
    protected Enemy cbScannedRobot(Enemy enemy) {
        Enemy prevR = enemyMap.get(enemy.name);
        Enemy constEnemy = super.cbScannedRobot(enemy);
        if ( constEnemy == null ) {
            return null;
        }
        if ( ! isTeammate(constEnemy.name)) {
            if ( ! enemyLog.containsKey(constEnemy.name)) {
                enemyLog.put(constEnemy.name,new HashMap<Long,Enemy>());
            }
            enemyLog.get(constEnemy.name).put(enemy.time,new Enemy(constEnemy));
        }

        if ( ! shotTypeMap.containsKey(constEnemy.name) ) {
            shotTypeMap.put(constEnemy.name,initialShotTypeList());
        }
        if ( ! aimTypeMap.containsKey(constEnemy.name) ) {
            aimTypeMap.put(constEnemy.name,initialAimTypeList());
        }
        if ( prevR != null ) {
            // Aimed
            if ( (prevR.energy - enemy.energy) >= 0.1 && (prevR.energy - enemy.energy) <= 3 ) {
                enemyBullet(prevR,enemy);
            }
        }
        return constEnemy;
    }


    protected void reEvalShot(BulletInfo info) {
        // TODO: AIMING estimate
        RobotPoint prevMy = logMy(info.src.timeStamp);
        List<MoveType> aimTypeList = aimTypeMap.get(info.targetName);
        for (MoveType moveType : aimTypeList) {
            RobotPoint target = logEnemy(info.targetName, info.src.timeStamp);
            if (target == null) {
                Logger.log("Cannot evaluate shot target log is NULL %d",info.src.timeStamp);
                return; // Cannot evaluate...
            }
            Pair<Long, Double> shot = calcShot(moveType, target, prevMy, info.src.velocity);
            MovingPoint bulletPoint = new MovingPoint(info.src);
            bulletPoint.headingRadians = shot.second;
            bulletPoint.heading = Math.toDegrees(shot.second);
            // Validate on history
            double closest = Util.fieldFullDistance;
            double closestDistance = Util.fieldFullDistance;
            double bulletDistance = 0;
            RobotPoint prevTarget = target;
            for (long i = info.src.timeStamp + 1; i <= ctx.my.time; i++) {
                target = logEnemy(info.targetName, i);
                if (target == null) {
                    // No data
                    prospectNextRobot(prevTarget, moveType, 1);
                    target = prevTarget;
                }
                prevTarget = target;

                bulletPoint.inertia(1);

                double d = bulletPoint.calcDistance(target);
                if (d < (Util.tankWidth / 2)) { // hit
                    closest = 0.0;
                    closestDistance = bulletDistance;
                    break;
                }
                if (d < closest) {
                    closest = d;
                    closestDistance = bulletDistance;
                }
                bulletDistance += info.src.velocity;
            }
            // Closest will nearly equals right angle with the bullet line.
            double diffRadians = closest / closestDistance; // So use sin
            diffRadians = (Math.abs(diffRadians) < BULLET_DIFF_THRESHOLD) ? diffRadians : BULLET_DIFF_THRESHOLD;
            moveType.updateScore(Math.PI / 2 - diffRadians,AIM_SCORE_ESTIMATE_LIMIT);
            logger.prospect2("AIMTYPE(x%02x)  degree: %2.2f => %2.2f = %2.2f", moveType.type, closest, Math.toDegrees(diffRadians), Math.toDegrees(moveType.score));
        }
        broadcastMessage(new AimTypeEvent(info.targetName, aimTypeList));
    }

    @Override
    protected Map.Entry<String, BulletInfo> cbBulletHit(BulletHitEvent e) {
        Map.Entry<String,BulletInfo> entry = super.cbBulletHit(e);
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        for ( Map.Entry<String,BulletInfo> ebi : enemyBulletList.entrySet() ) {
            BulletInfo bulletInfo = ebi.getValue();
            if ( e.getTime() == bulletInfo.src.timeStamp && bulletInfo.src.calcDegree(dst) < Util.tankSize ) {
                logger.fire2("CANCEL BULLET() %s : %s ", dst,bulletInfo.src);
                impactEnemyBulletInfo(bulletInfo.bulletName);
                break;
            }
        }
        if ( entry != null ) {
            if ( e.getBullet().getVictim().equals(entry.getValue().targetName) ) {
                reEvalShot(entry.getValue());
            }
        }
        return entry;
    }
    @Override
    protected Map.Entry<String, BulletInfo> cbBulletMissed(BulletMissedEvent e) {
        Map.Entry<String,BulletInfo> entry = super.cbBulletMissed(e);
        if ( entry != null ) {
            reEvalShot(entry.getValue());
        }
        return entry;
    }

    @Override
    protected void cbBulletHitBullet(BulletHitBulletEvent e){
        super.cbBulletHitBullet(e);
        Bullet bullet = e.getHitBullet();
        Map.Entry<String,BulletInfo> entry = Util.calcBulletSrc(ctx.my.time,bullet,enemyBulletList);
        if ( entry == null ) {
            logger.fire1("Unknown bullet hit: ");
        }else{
            impactEnemyBulletInfo(entry.getKey());
        }
    }
    @Override
    protected void cbHitByBullet(HitByBulletEvent e) {
        long time = e.getTime();
        Bullet bullet = e.getBullet();
        double bulletVelocity = e.getVelocity();
        double bulletRadians = e.getHeadingRadians();
        String enemyName = e.getName();
        Map.Entry<String,BulletInfo> entry = Util.calcBulletSrc(ctx.my.time,bullet,enemyBulletList);
        if ( entry != null ) {
            BulletInfo info = entry.getValue();
            impactEnemyBulletInfo(entry.getKey());
        // TODO: hit by bullet
            long deltaTime = ctx.my.time-info.src.timeStamp;
            RobotPoint prevMy = logMy(info.src.timeStamp);
            double distance = info.src.calcDistance(prevMy);

            List<MoveType> shotTypeList = shotTypeMap.get(enemyName);
            for ( MoveType moveType : shotTypeList ) {
                double tankWidthRadians = Math.asin((Util.tankWidth/2)/distance);
                double shotRadians = calcShot(moveType,prevMy,info.src,bulletVelocity,deltaTime).second;
                double diffRadians = Util.calcTurnRadians(bulletRadians,shotRadians);
                diffRadians = (Math.abs(diffRadians)<ENEMY_BULLET_DIFF_THRESHOLD)?diffRadians:ENEMY_BULLET_DIFF_THRESHOLD;
                double correctedRadians = Math.abs(diffRadians) - Math.abs(tankWidthRadians);
                correctedRadians = (correctedRadians<0)?0:correctedRadians;

                moveType.updateScore(Math.PI/2-correctedRadians,SHOT_SCORE_ESTIMATE_LIMIT);
                logger.prospect3("SHOTTYPE(x%02x)  degree: %2.2f => %2.2f = %2.2f",moveType.type,Math.toDegrees(diffRadians), Math.toDegrees(correctedRadians),Math.toDegrees(moveType.score));
            }
            broadcastMessage(new ShotTypeEvent(enemyName, shotTypeList));

        }
    }

    protected void enemyBullet(Enemy prev,Enemy enemy){
        MoveType aimType = MoveType.getByScore(aimTypeMap.get(prev.name));
        Enemy cpPrev = new Enemy(prev);
        prospectNextRobot(cpPrev, aimType,1);
        // Detect collsion
        for ( Map.Entry<String,BulletInfo> entry : enemyBulletList.entrySet() ) {
            BulletInfo info =  entry.getValue();
            if ( info.src.timeStamp == cpPrev.time && info.src.calcDistance(cpPrev) <= Util.tankSize * 1.5 ) {
                logger.fire4("ENEMY(collision): %s = %s dist(%2.2f)", info.src,cpPrev , info.src.calcDistance(cpPrev));
                removeEnemyBulletInfo(entry.getKey());
                return;// Maybe collision
                // TODO: Cannot judge collision with teammate & myself.Maybe should treat only myself and receive it from teammate
                //
            }
        }
        // Detect wall
        for ( long i = prev.time; i < enemy.time; i++  ) {
            boolean isMove = prospectNextRobot(cpPrev, aimType,1);
            if ( ! isMove ) {
                logger.fire4("ENEMY(wall): %s ", cpPrev);
                return; // Maybe hit wall
            }
        }
        // Bullet src
        cpPrev = new Enemy(prev);
        prospectNextRobot(cpPrev, aimType,1);
        MovingPoint src = cpPrev;

        if ( ! isTeammate(enemy.name) ) {
            RobotPoint prevMy = logMy(src.time-1); // Detect enemy-firing after one turn from actual.
            double distance = src.calcDistance(prevMy);
            MoveType shotType =MoveType.getByScore(shotTypeMap.get(enemy.name));
            double bulletVelocity = Util.bultSpeed(prev.energy-enemy.energy);
            logger.fire1("ENEMY(fire): x%02x : %s(%2.2f)", shotType.type,Util.bultPower(bulletVelocity), Util.bultSpeed(prev.energy-enemy.energy));
            double radians = calcShot(shotType,prevMy,src,bulletVelocity).second; //

            src.headingRadians = radians;
            src.heading  = Math.toDegrees(radians);
            src.velocity = bulletVelocity;
            BulletInfo bulletInfo = new BulletInfo(enemy.name,name,distance,src);
            addEnemyBulletInfo(bulletInfo);
        }
    }


    private void impactEnemyBulletInfo(String key){
        broadcastMessage(new CancelEnemyBalletEvent(key));
        removeEnemyBulletInfo(key);
    }
    protected void addEnemyBulletInfo(BulletInfo bulletInfo) {
        logger.fire2("SHOT(enemy): %s",bulletInfo.bulletName);
        enemyBulletList.put(bulletInfo.bulletName,bulletInfo);
        ctx.nextEnemyBulletList.put(bulletInfo.bulletName,new BulletInfo(bulletInfo));
    }
    protected void removeEnemyBulletInfo(String key){
        logger.fire2("IMPACT(enemy): %s",key);
        enemyBulletList.remove(key);
        ctx.nextEnemyBulletList.remove(key);
    }

    @Override
    protected void cbUnprospectiveNextTurn(){
        super.cbUnprospectiveNextTurn();
        List<String> rmEnemyBullet = new ArrayList<>();
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            if ( e.getValue().src.isOutOfField() ) {
                rmEnemyBullet.add(e.getKey());
            }
        }
        for( String n : rmEnemyBullet ) {
            removeEnemyBulletInfo(n);
        }
    }
    @Override
    protected void cbProspectNextTurn(){
        super.cbProspectNextTurn();
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            info.src.inertia(1);
        }
    }
    @Override
    protected void cbExtMessage(MessageEvent e) {
        super.cbExtMessage(e);
        Serializable event = e.getMessage();
        if (event instanceof CancelEnemyBalletEvent ) {
            CancelEnemyBalletEvent ev = (CancelEnemyBalletEvent)event;
            removeEnemyBulletInfo(ev.key);
        }else if (event instanceof ShotTypeEvent ) {
            ShotTypeEvent ev = (ShotTypeEvent)event;
            shotTypeMap.put(ev.name,ev.shotTypeList);
        }else if (event instanceof AimTypeEvent ) {
            AimTypeEvent ev = (AimTypeEvent)event;
            aimTypeMap.put(ev.name,ev.aimTypeList);
        }
    }

    @Override
    protected void paint(Graphics2D g) {
        if ( isPaint) {
            super.paint(g);
//            for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
//                Enemy enemy = e.getValue();
//                g.setColor(new Color(0.3f, 0.5f, 1.0f,PAINT_OPACITY));
//                Enemy next = new Enemy(enemy);
//                for (int i = 1; i < 20; i++) {
//                    MoveType aimType = MoveType.getByScore(aimTypeMap.get(next.name));
//                    prospectNextRobot(next, aimType,1);
//                    drawRound(g, next.x, next.y, 2);
//                }
//            }
            // Bullet
            g.setColor(new Color(1.0f, 0.5f, 0.5f, PAINT_OPACITY));
            for (Map.Entry<String, BulletInfo> e : enemyBulletList.entrySet()) {
                BulletInfo info = e.getValue();
                g.setStroke(new BasicStroke(1.0f));
                g.drawLine((int) info.src.x, (int) info.src.y, (int) (Math.sin(info.src.headingRadians) * Util.fieldFullDistance + info.src.x), (int) (Math.cos(info.src.headingRadians) * Util.fieldFullDistance + info.src.y));
                g.setStroke(new BasicStroke(4.0f));
                Point dst = Util.calcPoint(info.src.headingRadians, info.distance).add(info.src);
                drawRound(g, dst.x, dst.y, 5);
            }
            g.setColor(new Color(0, 0, 0, PAINT_OPACITY));
            g.setStroke(new BasicStroke(1.0f));
            for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
                BulletInfo info = e.getValue();
                if (info.distance < ENEMY_BULLET_DISTANCE || ctx.enemies == 1) {
                    if (!info.owner.equals(name)) {
                        MovingPoint bullet = new MovingPoint(info.src);
                        for (int i = 0; i < ENEMY_BULLET_PROSPECT_TIME; i++) {
                            drawRound(g, bullet.x, bullet.y, 5);
                            bullet.inertia(1);
                        }
                    }
                }
            }
        }
    }
}
