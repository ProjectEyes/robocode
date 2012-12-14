/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.adv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jp.crumb.CrumbRobot;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.Score;
import jp.crumb.utils.TimedPoint;
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
abstract public class AdvCrumbRobot<T extends AdbCrumbContext> extends CrumbRobot<T> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(255,255,255), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(200,255,100));
    }

    protected static final long REACT_PATTERN_TERM = 20;
    protected static final double REACT_PATTERN_SCORE_ESTIMATE_LIMIT = 200;
    protected static final double REACT_PATTERN_SCORE_ESTIMATE_MIN = 20;

    protected static final long SIMPLE_PATTERN_TERM_MAX =1000;
    protected static final long SIMPLE_PATTERN_TERM_MIN = 15;

    protected static final double SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT = 50;
    protected static final double SIMPLE_PATTERN_SCORE_ESTIMATE_MIN = 50;
    protected static final double SHOT_SCORE_ESTIMATE_LIMIT = 5;
    protected static final double SHOT_SCORE_ESTIMATE_MIN = 5;
    protected static final double AIM_SCORE_ESTIMATE_LIMIT = 7;
    protected static final double AIM_SCORE_ESTIMATE_MIN = 7;
//    protected static final double PATTERN_SCORE_ESTIMATE_LIMIT = 10;
//    protected static final double SHOT_SCORE_ESTIMATE_LIMIT = 10;
//    protected static final double AIM_SCORE_ESTIMATE_LIMIT = 10;

    protected static final double PERFECT_SCORE = 100; // Distance
    
//    protected static final double ENEMY_BULLET_DIFF_THRESHOLD = Math.PI/6; // more than 30 degrees
//    protected static final double BULLET_DIFF_THRESHOLD = Math.PI/6; // more than 30 degrees

    protected static final long   DEFAULT_ENEMY_BULLET_PROSPECT_TIME = 20;
    protected static final double DEFAULT_ENEMY_BULLET_WEIGHT = 1000;
    protected static final double DEFAULT_ENEMY_BULLET_DIM = 4;
    protected static final double DEFAULT_ENEMY_BULLET_DISTANCE = 800;


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
    protected Map<String,Map<Long,Score>> simplePatternScoreMap = new HashMap<>();
    protected Map<String,TreeMap<Long,Pair<Score,TimedPoint>>> reactPatternScoreMap = new HashMap<>();

    protected List<MoveType> initialShotTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>();
        MoveType moveType = new MoveType(MoveType.TYPE_PINPOINT);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA_FIRST);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA_CENTER);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCELERATION_FIRST);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCELERATION_CENTER);
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
        moveType = new MoveType(MoveType.TYPE_ACCELERATION_FIRST);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCELERATION_CENTER);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_SIMPLE_PATTERN_FIRST);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_SIMPLE_PATTERN_CENTER);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_FIRST);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }
    @Override
    protected MoveType getAimType(String name) {
        if ( aimTypeMap.get(name) == null ) {
            return new MoveType(MoveType.TYPE_ACCELERATION_FIRST);
        }
        return MoveType.getByScore(aimTypeMap.get(name));
    }
    protected MoveType getAimType(String targetName,int type){
        for ( MoveType aimType : aimTypeMap.get(targetName) ){
            if ( aimType.type == type ) {
                return aimType;
            }
        }
        return null;
    }

    @Override
    protected AdbCrumbContext createContext(AdbCrumbContext in) {
        if ( in == null ) {
            return new AdbCrumbContext();
        }
        return new AdbCrumbContext(in);
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
            if ( info.distance < ENEMY_BULLET_DISTANCE || (ctx.others==ctx.enemies) )  {
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
    protected Point calcDiffHeadingVecter(RobotPoint enemy){
        double enemyHeadingRadians = (enemy.velocity>0)?enemy.headingRadians:(enemy.headingRadians-Math.PI);
        double enemyDiffHeadingRadians = Util.calcTurnRadians(ctx.my.calcRadians(enemy),enemyHeadingRadians);
        return Util.calcPoint(enemyDiffHeadingRadians,enemy.velocity);
    }

    @Override
    protected boolean prospectNextRobotReactPattern(RobotPoint robot,long term,Object context){
        if ( context == null ) { // Not firing
            return robot.prospectNext(term);
        }
        Score s = (Score)context;
        if ( s.name.isEmpty() ) {
            TreeMap<Long,Pair<Score,TimedPoint>> reacts = reactPatternScoreMap.get(robot.name);
            if ( reacts == null ) {
                return robot.prospectNext(term);
            }
            Point v = calcDiffHeadingVecter(robot);
            double vecR = new Point(0, 0).calcDegree(v);
            List<Score> scores = new ArrayList<>();
            for (Long shotTime : reacts.keySet()) {
                Pair<Score, TimedPoint> pair = reacts.get(shotTime);
                TimedPoint vecter = pair.second;
                if (v.calcDistance(vecter) < 3.0) {
                    scores.add(pair.first);
                }
            }
            Score vest = Score.getByScore(scores);
            if ( vest == null ) {
                s.score = 0.0;
            }else{
                s.name = vest.name;
                s.score = vest.score;
                s.scoreCount = vest.scoreCount;
            }
        }
        if ( s.score == 0.0 ) {
            return robot.prospectNext(term);
        }
//logger.log("(%d/%d) %s: %2.2f => %d",robot.time,robot.timeStamp,s.name,s.score,robot.time-Integer.valueOf(s.name));


        for ( long l = 1; l <= term; l++) {
            long absLogTime = l+Integer.valueOf(s.name);
            RobotPoint logEnemy = logEnemy(robot.name,absLogTime);
            if ( logEnemy != null && logEnemy.delta != null) {
                robot.setDelta(logEnemy.delta);
                robot.prospectNext(1);
                if ( isPaint ) {
                    getGraphics().setColor(Color.BLACK);
                    drawRound(getGraphics(),logEnemy.x,logEnemy.y,10);
                    getGraphics().setColor(Color.WHITE);
                    drawRound(getGraphics(),robot.x,robot.y,10);
                }
                logger.prospect4("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
            }else{
                robot.prospectNext(1);
            }
            s.name = String.valueOf(Integer.valueOf(s.name) + 1);
        }
////                logger.log("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
//        }
        return true;
    }    
    
    @Override
    protected boolean prospectNextRobotSimplePattern(RobotPoint robot,long term,Object context){
        Map<Long,Score> scores = simplePatternScoreMap.get(robot.name);
        if ( scores == null || scores.size() <= term) {
            return robot.prospectNext(term);
        }
        Score s = Score.getByScore(scores.values());
        for ( long l = 1; l <= term; l++) {
            long absLogTime = l+robot.time-Integer.valueOf(s.name);
            RobotPoint logEnemy = logEnemy(robot.name,absLogTime);
            if ( logEnemy != null && logEnemy.delta != null) {
                logger.prospect4("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
//                logger.log("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
                robot.setDelta(logEnemy.delta);
                robot.add(logEnemy.delta);
                if ( isPaint ) {
                    getGraphics().setColor(Color.GRAY);
                    drawRound(getGraphics(),logEnemy.x,logEnemy.y,10);
                    getGraphics().setColor(Color.LIGHT_GRAY);
                    drawRound(getGraphics(),robot.x,robot.y,10);
                }
            }else{
                robot.prospectNext(1);
            }
        }
        return true;
    }
    
    protected void evalReactPattern(Enemy prevEnemy,Enemy constEnemy){
        TreeMap<Long,Pair<Score,TimedPoint>> reacts = reactPatternScoreMap.get(constEnemy.name);
        if ( reacts == null ) {
            return;
        }

        long deltaTime = constEnemy.time - prevEnemy.time;
        for ( Long shotTime : reacts.keySet() ) {
            Pair<Score,TimedPoint> pair = reacts.get(shotTime);
            Score s = pair.first;
            TimedPoint vecter = pair.second;

            for ( Map.Entry<Long,Pair<Score,TimedPoint>> p : reacts.tailMap(shotTime,false).entrySet() ) {
                long pastShotTime = p.getKey();
                long fromShotTime = constEnemy.time - pastShotTime;
                if ( fromShotTime <= REACT_PATTERN_TERM && vecter.calcDistance(p.getValue().second) < 3.0 ) {
                    long absTime = shotTime+fromShotTime + 1;
                    RobotPoint logEnemy = logEnemy(constEnemy.name, absTime);
                    if ( logEnemy == null || logEnemy.delta == null) {
                        continue;
                    }
                    RobotPoint prospectEnemy = new RobotPoint(prevEnemy);
                    prospectEnemy.setDelta(logEnemy.delta);
                    prospectEnemy.prospectNext(deltaTime);
                    double d = prospectEnemy.calcDistance(constEnemy);
                    s.updateScore(Util.fieldFullDistance-d,REACT_PATTERN_SCORE_ESTIMATE_LIMIT,REACT_PATTERN_SCORE_ESTIMATE_MIN);
                    logger.prospect4("REACT(%02d): %2.2f(%2.2f)",shotTime,s.score,d);

                    //double pastR = new Point(0,0).calcDegree(p.getValue().second);
                    //double vecR = new Point(0,0).calcDegree(vecter);
                    // logger.log("REACT: %2.2f => %2.2f = %d/%d(%d):(%2.2f/%2.2f):%2.2f",d,s.score,shotTime,pastShotTime,fromShotTime,vecR,pastR,vecter.calcDistance(p.getValue().second));
                }


                //  logger.log("REACT(%02d):%2.2f => %2.2f = %2.2f",i,d,d,s.score);
                
            }
        }
    }
    
    protected void evalSimplePattern(Enemy prevEnemy,Enemy constEnemy){
        Map<Long,Score> scores = simplePatternScoreMap.get(constEnemy.name);
        long deltaTime = constEnemy.time - prevEnemy.time;
        for ( long i = SIMPLE_PATTERN_TERM_MIN ; i <= SIMPLE_PATTERN_TERM_MAX; i++) {
            long absTime = constEnemy.time - i;
            RobotPoint logEnemy = logEnemy(constEnemy.name, absTime);
            if ( logEnemy == null || logEnemy.delta == null) {
                continue;
            }
            RobotPoint prospectEnemy = new RobotPoint(prevEnemy);
            prospectEnemy.setDelta(logEnemy.delta);
            prospectEnemy.prospectNext(deltaTime);
            double d = prospectEnemy.calcDistance(constEnemy);
            Score s = scores.get(i);
            s.updateScore(Util.fieldFullDistance-d,SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT,SIMPLE_PATTERN_TERM_MIN);
            logger.prospect4("REACT(%02d): %2.2f(%2.2f)",absTime,s.score,d);
            //logger.log("SIMPLE(%02d):%2.2f => %2.2f = %2.2f",i,d,d,s.score);
        }
    }
    @Override
    protected Enemy cbScannedRobot(Enemy enemy) {
        Enemy prevEnemy = enemyMap.get(enemy.name);
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
        if ( ! simplePatternScoreMap.containsKey(enemy.name)) {
            Map<Long,Score> scores = new HashMap<>();
            for ( long i = SIMPLE_PATTERN_TERM_MIN ; i <= SIMPLE_PATTERN_TERM_MAX; i++) {
                scores.put(i,new Score(String.valueOf(i)));
            }
            simplePatternScoreMap.put(enemy.name,scores);
        }

        if ( prevEnemy != null ) {
            // Aimed
            if ( (prevEnemy.energy - enemy.energy) >= 0.1 && (prevEnemy.energy - enemy.energy) <= 3 ) {
                enemyBullet(prevEnemy,enemy);
            }
            evalSimplePattern(prevEnemy, constEnemy);
            evalReactPattern(prevEnemy, constEnemy);
        }
        return constEnemy;
    }


    protected void reEvalShot(BulletInfo info) {
        RobotPoint prevMy = logMy(info.src.timeStamp);
        if ( prevMy == null ) {
            return ; // TODO: use nearest data ?
        }
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
            // double closestDistance = Util.fieldFullDistance;
            // double bulletDistance = 0;
            RobotPoint prevTarget = target;

            for (long i = info.src.timeStamp + 1; i <= ctx.my.time; i++) {
                target = logEnemy(info.targetName, i);
                if (target == null) {
                    // No data
                    prospectNextRobot(new RobotPoint(prevTarget), moveType, 1);
                    target = prevTarget;
                }
                prevTarget = target;

                bulletPoint.inertia(1);

                double d = bulletPoint.calcDistance(target);
                if (d < closest) {
                    closest = d;
                    // closestDistance = bulletDistance;
                    if (d < (Util.tankWidth / 2)) { // hit
                        closest = 0.0;
                        break;
                    }
                }
                // bulletDistance += info.src.velocity;
            }
            // Closest will nearly equals right angle with the bullet line.
            // ndouble diffRadians = closest / closestDistance; // So use sin
            // diffRadians = (Math.abs(diffRadians) < BULLET_DIFF_THRESHOLD) ? diffRadians : BULLET_DIFF_THRESHOLD;
            //moveType.updateScore(Math.PI / 2 - diffRadians,AIM_SCORE_ESTIMATE_LIMIT,AIM_SCORE_ESTIMATE_MIN);a
            moveType.updateScore(PERFECT_SCORE-closest);
            logger.prospect1("AIMTYPE(x%02x)  %2.2f(%2.2f)", moveType.type,moveType.score,closest );
        }
        broadcastMessage(new AimTypeEvent(info.targetName, aimTypeList));
    }
    @Override
    protected void doFire(double power, double distance, String targetName,int type) {
        if ( ! reactPatternScoreMap.containsKey(targetName)) {
            TreeMap<Long,Pair<Score,TimedPoint>> scores = new TreeMap<>();
            reactPatternScoreMap.put(targetName,scores);
        }
        Map<Long,Pair<Score,TimedPoint>> scores = reactPatternScoreMap.get(targetName);
        Enemy enemy = enemyMap.get(targetName);
        // Create enemy vecter
        Point v = calcDiffHeadingVecter(enemy);
        TimedPoint vecter = new TimedPoint();
        vecter.set(v);
        vecter.time = enemy.time;
        vecter.timeStamp = enemy.timeStamp;
        scores.put(enemy.time,new Pair<>(new Score(String.valueOf(enemy.time)),vecter));
        super.doFire(power, distance, targetName,type);
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
            BulletInfo info = entry.getValue();
            if ( bullet.getVictim().equals(info.targetName) ) {
                getAimType(info.targetName, info.type).updateHit();
                reEvalShot(info);
            }
        }
        return entry;
    }
    @Override
    protected Map.Entry<String, BulletInfo> cbBulletMissed(BulletMissedEvent e) {
        Map.Entry<String,BulletInfo> entry = super.cbBulletMissed(e);
        if ( entry != null ) {
            BulletInfo info = entry.getValue();
            reEvalShot(info);
        }
        return entry;
    }

    @Override
    protected Map.Entry<String,BulletInfo> cbBulletHitBullet(BulletHitBulletEvent e){
        Map.Entry<String,BulletInfo> entry = super.cbBulletHitBullet(e);
        Bullet bullet = e.getHitBullet();
        Map.Entry<String,BulletInfo> eEntry = Util.calcBulletSrc(ctx.my.time,bullet,enemyBulletList);
        if ( eEntry == null ) {
            logger.fire1("Unknown bullet hit: ");
        }else{
            BulletInfo info = entry.getValue();
            getAimType(info.targetName, info.type).revartAim();
            impactEnemyBulletInfo(entry.getKey());
        }
        return entry;
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
            long deltaTime = ctx.my.time-info.src.timeStamp;
            RobotPoint prevMy = logMy(info.src.timeStamp);
            if ( prevMy == null ) {
                return ; // TODO: use nearest data ?
            }

            double distance = info.src.calcDistance(prevMy);
            List<MoveType> shotTypeList = shotTypeMap.get(enemyName);
            for ( MoveType moveType : shotTypeList ) {
                double tankWidthRadians = Math.asin((Util.tankWidth/2)/distance);
                Pair<Long,Double> shot = calcShot(moveType,prevMy,info.src,bulletVelocity,deltaTime);
                Point bulletPoint = Util.calcPoint(shot.second, shot.first*bulletVelocity);
                double d = bulletPoint.calcDistance(ctx.my);
                moveType.updateScore(PERFECT_SCORE-d,SHOT_SCORE_ESTIMATE_LIMIT,SHOT_SCORE_ESTIMATE_MIN);
//                double shotRadians = 
//                double diffRadians = Util.calcTurnRadians(bulletRadians,shotRadians);
//                diffRadians = (Math.abs(diffRadians)<ENEMY_BULLET_DIFF_THRESHOLD)?diffRadians:ENEMY_BULLET_DIFF_THRESHOLD;
//                double correctedRadians = Math.abs(diffRadians) - Math.abs(tankWidthRadians);
//                correctedRadians = (correctedRadians<0)?0:correctedRadians;
//                moveType.updateScore(Math.PI/2-correctedRadians,SHOT_SCORE_ESTIMATE_LIMIT,SHOT_SCORE_ESTIMATE_MIN);
                logger.prospect1("SHOTTYPE(x%02x)  %2.2f(%2.2f)", moveType.type,moveType.score,d );
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
            if ( prevMy == null ) {
                prevMy = ctx.my; // No data
            }
            double distance = src.calcDistance(prevMy);
            MoveType shotType =MoveType.getByScore(shotTypeMap.get(enemy.name));
            double bulletVelocity = Util.bultSpeed(prev.energy-enemy.energy);
            logger.fire1("ENEMY(fire): x%02x : %s(%2.2f)", shotType.type,Util.bultPower(bulletVelocity), Util.bultSpeed(prev.energy-enemy.energy));
            double radians = calcShot(shotType,prevMy,src,bulletVelocity).second; //

            src.headingRadians = radians;
            src.heading  = Math.toDegrees(radians);
            src.velocity = bulletVelocity;
            BulletInfo bulletInfo = new BulletInfo(enemy.name,name,distance,src,shotType.type);
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
                if (info.distance < ENEMY_BULLET_DISTANCE || (ctx.others==ctx.enemies)  ){
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

    @Override
    protected void dumpLog(){
        // g.drawString(String.format("hit: %d / %d  : %2.2f / %2.2f", r.getAimType().hitCount, r.getAimType().aimCount, r.getAimType().hitTime, r.getAimType().aimTime), (int) r.x - 20, (int) r.y - 40);
        // TODO: merge to AIMING
        for ( Map.Entry<String,List<MoveType>> e : aimTypeMap.entrySet() ) {
            String enemyName = e.getKey();
            Logger.log("=== %s ===",enemyName);
            long hitCount = 0;
            long aimCount = 0;
            for ( MoveType aimType : e.getValue() ) {
                hitCount+= aimType.hitCount;
                aimCount+= aimType.aimCount;
            }
            Logger.log("ALL : %2.2f %% (%d/%d)",(double)hitCount/(double)aimCount*100.0,hitCount,aimCount);
            for ( MoveType aimType : e.getValue() ) {            
                Logger.log("x%02x : %2.2f %% (%d/%d) : %2.2f",aimType.type,aimType.avrage()*100.0,aimType.hitCount,aimType.aimCount,aimType.score);
            }
        }
        
    }
}
