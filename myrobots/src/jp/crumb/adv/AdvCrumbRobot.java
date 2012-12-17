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
import jp.crumb.ProspectContext;
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

    protected static final long REACT_PATTERN_TERM = 16;
    protected static final double REACT_PATTERN_SCORE_ESTIMATE_LIMIT = 3;
    protected static final double REACT_PATTERN_SCORE_ESTIMATE_MIN = 1;

    protected static final long SIMPLE_PATTERN_TERM_MAX =2000;
    protected static final long SIMPLE_PATTERN_TERM_MIN = 15;

    protected static final double SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT = 7;
    protected static final double SIMPLE_PATTERN_SCORE_ESTIMATE_MIN = 1;
    protected static final double SHOT_SCORE_ESTIMATE_LIMIT = 5;
    protected static final double SHOT_SCORE_ESTIMATE_MIN = 5;
    protected static final double AIM_SCORE_ESTIMATE_LIMIT = 7;
    protected static final double AIM_SCORE_ESTIMATE_MIN = 7;

//    protected static final double PATTERN_SCORE_ESTIMATE_LIMIT = 10;
//    protected static final double SHOT_SCORE_ESTIMATE_LIMIT = 10;
//    protected static final double AIM_SCORE_ESTIMATE_LIMIT = 10;

    
//    protected static final double ENEMY_BULLET_DIFF_THRESHOLD = Math.PI/6; // more than 30 degrees
//    protected static final double BULLET_DIFF_THRESHOLD = Math.PI/6; // more than 30 degrees

    protected static final double DEFAULT_CORNER_WEIGHT = -10;
    protected static final double DEFAULT_CORNER_DIM = 1;
    protected static final long   DEFAULT_ENEMY_BULLET_PROSPECT_TIME = 20;
    protected static final double DEFAULT_ENEMY_BULLET_WEIGHT = 1000;
    protected static final double DEFAULT_ENEMY_BULLET_DIM = 4;
    protected static final double DEFAULT_ENEMY_BULLET_DISTANCE = 800;


    // For move
    protected  double CORNER_WEIGHT          = DEFAULT_CORNER_WEIGHT;
    protected  double CORNER_DIM             = DEFAULT_CORNER_DIM;
    protected  long   ENEMY_BULLET_PROSPECT_TIME   = DEFAULT_ENEMY_BULLET_PROSPECT_TIME;
    protected  double ENEMY_BULLET_WEIGHT          = DEFAULT_ENEMY_BULLET_WEIGHT;
    protected  double ENEMY_BULLET_DIM             = DEFAULT_ENEMY_BULLET_DIM;
    protected  double ENEMY_BULLET_DISTANCE        = DEFAULT_ENEMY_BULLET_DISTANCE;


    private Map<String,BulletInfo> enemyBulletList = new HashMap<>(15,0.3f);
    protected static Map<String,List<MoveType>> shotTypeMap = new HashMap<>(15,0.3f);
    protected static Map<String,List<MoveType>> aimTypeMap = new HashMap(15,0.3f);

    protected Map<Long,RobotPoint> myLog = new HashMap<>(2500);
    protected static Map<String, Map<Long,Enemy> > enemyLog = new HashMap<>(15,0.3f);
    protected Map<String,Map<Long,Score>> simplePatternScoreMap = new HashMap<>(15,0.3f);

    protected static final double REACT_LOG_VALID_DISTANCE =100;
    protected static class DistancePoint extends Point {
        public double distance;
        public boolean isNearly(DistancePoint in ) {
            return calcDdiff(in) < 30.0;
        }
        public double calcDdiff(DistancePoint in){
            return calcDistance(in) * Math.abs(distance - in.distance);
        }
    }
    protected DistancePoint calcDiffHeadingVecter(RobotPoint enemy){
        double enemyHeadingRadians = (enemy.velocity>0)?enemy.headingRadians:(enemy.headingRadians-Math.PI);
        double enemyDiffHeadingRadians = Util.calcTurnRadians(ctx.my.calcRadians(enemy),enemyHeadingRadians);
        DistancePoint ret = new DistancePoint();
        double vectorVelocity = enemy.velocity * 2 + ((enemy.velocity>0)?2:-2); // v * 2 + 2 (correct towards)
        ret.set(Util.calcPoint(enemyDiffHeadingRadians,vectorVelocity));
        ret.distance = ctx.my.calcDistance(enemy);
        return ret;
    }

    protected Map<String,TreeMap<Long,Pair<Score,DistancePoint>>> reactPatternScoreMap = new HashMap<>(15,0.3f);

    protected List<MoveType> initialShotTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>(10);
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
        List<MoveType> moveTypeList = new ArrayList<>(10);
        MoveType moveType = new MoveType(MoveType.TYPE_PINPOINT);
        moveTypeList.add(moveType);
//        moveType = new MoveType(MoveType.TYPE_INERTIA_FIRST);
//        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA_CENTER);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(moveType);
//        moveType = new MoveType(MoveType.TYPE_ACCELERATION_FIRST);
//        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCELERATION_CENTER);
        moveTypeList.add(moveType);
//        moveType = new MoveType(MoveType.TYPE_SIMPLE_PATTERN_FIRST);
//        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_SIMPLE_PATTERN_CENTER);
        moveTypeList.add(moveType);
//        moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_FIRST);
//        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }

    boolean isValidSimplePattern(RobotPoint target){
        boolean ret = true;
        Map<Long, Score> simple = simplePatternScoreMap.get(target.name);
        if (simple == null) {
            ret = false; // Data not found
        }
        return ret;
    }
    boolean isValidReactPattern(RobotPoint target){
        boolean ret = false;
        TreeMap<Long, Pair<Score, DistancePoint>> reacts = reactPatternScoreMap.get(target.name);
        if (reacts != null) {
            DistancePoint v = calcDiffHeadingVecter(target);
            for (Long shotTime : reacts.keySet()) {
                Pair<Score, DistancePoint> pair = reacts.get(shotTime);
                DistancePoint vecter = pair.second;
                if (vecter.isNearly(v)) {
                    ret = true; // Valid data found
                }
            }
        }
        return ret;
    }

    @Override
    protected MoveType getBestAimType(RobotPoint target) {
        MoveType ret = null;
        if (aimTypeMap.get(target.name) != null) {
            boolean simplePattern = isValidSimplePattern(target);
            boolean reactPattern = isValidReactPattern(target);
            List<MoveType> scores = new ArrayList(10);
            for (MoveType m : aimTypeMap.get(target.name)) {
                if (m.isTypeSimplePattern() && !simplePattern) {
                    continue;
                }
                if (m.isTypeReactPattern() && !reactPattern) {
                    continue;
                }
                scores.add(m);
            }
            ret = MoveType.getByScore(scores);
        }
        return ret;
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
        // corner
        if ( ctx.enemies > 1 ) { // Melee mode
            dst.diff(Util.getGrabity(ctx.my, new Point(Util.runnableMinX,Util.runnableMinY), CORNER_WEIGHT,CORNER_DIM));
            dst.diff(Util.getGrabity(ctx.my, new Point(Util.runnableMinX,Util.runnableMaxY), CORNER_WEIGHT,CORNER_DIM));
            dst.diff(Util.getGrabity(ctx.my, new Point(Util.runnableMaxX,Util.runnableMinY), CORNER_WEIGHT,CORNER_DIM));
            dst.diff(Util.getGrabity(ctx.my, new Point(Util.runnableMaxX,Util.runnableMaxY), CORNER_WEIGHT,CORNER_DIM));
        }
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

    @Override
    protected boolean prospectNextRobotReactPattern(RobotPoint robot,long term,ProspectContext context){
        if ( context == null ) { // Not firing time
            return robot.prospectNext(term);
        }
        if ( context.shotTarget == null ) {
            context.shotTarget = robot.name; // proced
            context.shotTime = 0; // invalid
            TreeMap<Long,Pair<Score,DistancePoint>> reacts = reactPatternScoreMap.get(robot.name);
            if ( reacts == null ) { // No reaction log
                return robot.prospectNext(term);
            }
            DistancePoint v = calcDiffHeadingVecter(robot);
            List<Score> scores = new ArrayList<>();
//            for (Long shotTime : reacts.keySet()) {
//                Pair<Score, DistancePoint> pair = reacts.get(shotTime);
//                DistancePoint vecter = pair.second;
//                if (vecter.isNearly(v) ){
//                    scores.add(pair.first);
//                }
//            }
//            Score best = Score.getByScore(scores);
            double bestDiff = Util.fieldFullDistance;
            Score best = null;
            for (Long shotTime : reacts.keySet()) {
                Pair<Score, DistancePoint> pair = reacts.get(shotTime);
                DistancePoint vecter = pair.second;
                double diff = vecter.calcDdiff(v);
                if ( bestDiff > diff && vecter.isNearly(v) ) {
                    bestDiff = diff;
                    best = pair.first;
                }
            }
            if ( best != null ) {
                context.shotTime = Long.parseLong(best.name);
                context.time = context.shotTime;
                context.baseTarget = robot;
                context.diff = bestDiff;
            }
        }
        if ( context.shotTime == 0 ) {
            return robot.prospectNext(term);
        }
//logger.log("(%d/%d) %s: %2.2f => %d",robot.time,robot.timeStamp,s.name,s.score,robot.time-Integer.valueOf(s.name));

        boolean first = true;
        Graphics2D g = getGraphics();
        for ( long l = 1; l <= term; l++) {
            long absLogTime = l+context.time;
            RobotPoint logEnemy = logEnemy(robot.name,absLogTime);
            if ( logEnemy != null && logEnemy.delta != null) {
                if ( context.baseLog == null ) {
                    context.baseLog = logEnemy;
                }
                MovingPoint delta = logEnemy.delta;
                if ( context.baseTarget.velocity * context.baseLog.velocity < 0 ) {
                    delta = new MovingPoint(delta).prod(-1);
                    delta.time *= -1;
                }
                robot.setDelta(delta);
                robot.prospectNext(1);
                if ( isPaint ) {
                    g.setStroke(new BasicStroke(1.0f));
                    g.setColor(Color.BLACK);
                    drawRound(g,logEnemy.x,logEnemy.y,10);
                    g.setColor(Color.WHITE);
                    drawRound(g,robot.x,robot.y,10);
                }
                logger.prospect4("%d: %s(%d/%d) : %s",l,context.time/context.shotTime,absLogTime,logEnemy.delta);
            }else{
                robot.prospectNext(1);
            }
            context.time += 1;
        }
////                logger.log("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
//        }
        return true;
    }    
    
    @Override
    protected boolean prospectNextRobotSimplePattern(RobotPoint robot,long term,ProspectContext context){
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
                robot.prospectNext(1);
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
        TreeMap<Long,Pair<Score,DistancePoint>> reacts = reactPatternScoreMap.get(constEnemy.name);
        if ( reacts == null ) {
            return;
        }

        long deltaTime = constEnemy.time - prevEnemy.time;
        for ( Long shotTime : reacts.keySet() ) {
            Pair<Score,DistancePoint> pair = reacts.get(shotTime);
            Score s = pair.first;
            DistancePoint vecter = pair.second;

            for ( Map.Entry<Long,Pair<Score,DistancePoint>> p : reacts.tailMap(shotTime,false).entrySet() ) {
                long pastShotTime = p.getKey();
                long fromShotTime = constEnemy.time - pastShotTime;
                if ( fromShotTime <= REACT_PATTERN_TERM && vecter.isNearly(p.getValue().second) ) {
                    long absTime = shotTime+fromShotTime + 1;
                    RobotPoint logEnemy = logEnemy(constEnemy.name, absTime);
                    if ( logEnemy == null || logEnemy.delta == null) {
                        continue;
                    }
                    RobotPoint prospectEnemy = new RobotPoint(prevEnemy);
                    prospectEnemy.setDelta(logEnemy.delta);
                    prospectEnemy.prospectNext(deltaTime);
                    double d = prospectEnemy.calcDistance(constEnemy);
                    s.updateScore(PERFECT_SCORE-d,REACT_PATTERN_SCORE_ESTIMATE_LIMIT,REACT_PATTERN_SCORE_ESTIMATE_MIN);
                   logger.prospect4("REACT(%02d/%02d/%02d): %2.2f(%2.2f)",fromShotTime,shotTime,pastShotTime,s.score,d);

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
            s.updateScore(PERFECT_SCORE-d,SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT,SIMPLE_PATTERN_TERM_MIN);
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
                enemyLog.put(constEnemy.name,new HashMap<Long,Enemy>(2000));
            }
            enemyLog.get(constEnemy.name).put(enemy.time,new Enemy(constEnemy));
        }

        if ( ! shotTypeMap.containsKey(constEnemy.name) ) {
            shotTypeMap.put(constEnemy.name,initialShotTypeList());
        }
        if ( ! aimTypeMap.containsKey(constEnemy.name) ) {
            aimTypeMap.put(constEnemy.name,initialAimTypeList());
        }
        if ( getAimType(enemy.name,MoveType.TYPE_SIMPLE_PATTERN_CENTER) != null ||
                getAimType(enemy.name,MoveType.TYPE_SIMPLE_PATTERN_FIRST) != null ) {
            if ( ! simplePatternScoreMap.containsKey(enemy.name)) {
                Map<Long,Score> scores = new HashMap<>((int)SIMPLE_PATTERN_TERM_MAX);
                for ( long i = SIMPLE_PATTERN_TERM_MIN ; i <= SIMPLE_PATTERN_TERM_MAX; i++) {
                    scores.put(i,new Score(String.valueOf(i)));
                }
                simplePatternScoreMap.put(enemy.name,scores);
            }
        }

        if ( prevEnemy != null ) {
            // Aimed
            if ( (prevEnemy.energy - enemy.energy) >= 0.1 && (prevEnemy.energy - enemy.energy) <= 3 ) {
                enemyBullet(prevEnemy,enemy);
            }
            if ( getAimType(enemy.name,MoveType.TYPE_SIMPLE_PATTERN_CENTER) != null ||
                 getAimType(enemy.name,MoveType.TYPE_SIMPLE_PATTERN_FIRST) != null ) {
                evalSimplePattern(prevEnemy, constEnemy);
            }
            if ( getAimType(enemy.name,MoveType.TYPE_REACT_PATTERN_CENTER) != null ||
                 getAimType(enemy.name,MoveType.TYPE_REACT_PATTERN_FIRST) != null ) {
                evalReactPattern(prevEnemy, constEnemy);
            }
        }
        return constEnemy;
    }


    protected void reEvalShot(BulletInfo info) {
        RobotPoint prevMy = logMy(info.src.timeStamp);
        if ( prevMy == null ) {
            return ; // TODO: use nearest data ?
        }

        RobotPoint target = logEnemy(info.targetName, info.src.timeStamp);
        if (target == null) {
            Logger.log("Cannot evaluate shot target log is NULL %d",info.src.timeStamp);
            return; // Cannot evaluate...
        }
if ( isPaint ) {
    getGraphics().setColor(Color.RED);
    getGraphics().setStroke(new BasicStroke(4.0f));
    drawRound(getGraphics(),target.x,target.y,10);
}

        boolean simplePattern = isValidSimplePattern(target);
        boolean reactPattern = isValidReactPattern(target);


        List<MoveType> aimTypeList = aimTypeMap.get(info.targetName);
        for (MoveType moveType : aimTypeList) {
            if (moveType.isTypeSimplePattern() && !simplePattern) {
                continue;
            }
            if (moveType.isTypeReactPattern() && !reactPattern) {
                continue;
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

            RobotPoint      reactView = null;
            ProspectContext reactCtx = null;

            if ( isPaint && moveType.isTypeReactPattern() ) {
                reactView = new RobotPoint(target);
                reactCtx = new ProspectContext();
            }
            for (long i = info.src.timeStamp + 1; i <= ctx.my.time; i++) {
                RobotPoint actualTarget = logEnemy(info.targetName, i);
                if (actualTarget == null) {
                    // No data
                    actualTarget = new RobotPoint(prevTarget);
                    prospectNextRobot(actualTarget, moveType, 1);
                }
                prevTarget = actualTarget;
                if ( isPaint && moveType.isTypeReactPattern() ) {
                    prospectNextRobot(reactView, moveType, 1,reactCtx);
                    getGraphics().setColor(Color.RED);
                    getGraphics().setStroke(new BasicStroke(1.0f));
                    drawRound(getGraphics(),actualTarget.x,actualTarget.y,10);
                    getGraphics().setColor(Color.GREEN);
                    drawRound(getGraphics(),reactView.x,reactView.y,10);
                    logger.log("DIFF:%2.2f",reactCtx.diff);
                }


                bulletPoint.inertia(1);

                double d = bulletPoint.calcDistance(actualTarget);
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
            moveType.updateScore(PERFECT_SCORE-closest,AIM_SCORE_ESTIMATE_LIMIT,AIM_SCORE_ESTIMATE_MIN);
            logger.prospect1("AIMTYPE(x%03x)  s:%2.2f d:%03.1f %2.2f %% (%d/%d)", moveType.type,moveType.score,closest,moveType.avrage()*100.0,moveType.hitCount,moveType.aimCount);
        }
        broadcastMessage(new AimTypeEvent(info.targetName, aimTypeList));
    }
    @Override
    protected void doFire(double power, double distance, String targetName,int type) {
        if ( getAimType(targetName,MoveType.TYPE_REACT_PATTERN_CENTER) != null ||
                getAimType(targetName,MoveType.TYPE_REACT_PATTERN_FIRST) != null ) {
            if ( ! reactPatternScoreMap.containsKey(targetName)) {
                TreeMap<Long,Pair<Score,DistancePoint>> scores = new TreeMap<>();
                reactPatternScoreMap.put(targetName,scores);
            }
            Map<Long,Pair<Score,DistancePoint>> scores = reactPatternScoreMap.get(targetName);
            Enemy enemy = enemyMap.get(targetName);
            // Create enemy vecter
            DistancePoint vecter = calcDiffHeadingVecter(enemy);
            scores.put(enemy.time,new Pair<>(new Score(String.valueOf(enemy.time)),vecter));
        }
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
        }
        if ( entry != null) {
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
                Point bulletPoint = Util.calcPoint(shot.second, shot.first*bulletVelocity).add(info.src);
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
    protected double powerLimit(double enemyEnergy, MoveType aimType) {
        double limit = ctx.my.energy / 10;
        limit = limit*aimType.score/PERFECT_SCORE;
        double need = Util.powerByDamage(enemyEnergy);
        if ( need < limit ) {
            limit = need;
        }
        return limit;
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
