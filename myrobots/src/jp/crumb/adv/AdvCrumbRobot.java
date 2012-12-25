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
//    protected static final double REACT_PATTERN_SCORE_ESTIMATE_LIMIT = 3;
//    protected static final double REACT_PATTERN_SCORE_ESTIMATE_MIN = 1;

//    protected static final long SIMPLE_PATTERN_TERM_MAX =1500;
    protected static final long SIMPLE_PATTERN_TERM_MIN = 15;

    protected static final double SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT = 7;
    protected static final double SHOT_SCORE_ESTIMATE_LIMIT = 1;
    protected static final double AIM_SCORE_ESTIMATE_LIMIT = 1;

//    protected static final double PATTERN_SCORE_ESTIMATE_LIMIT = 10;
//    protected static final double SHOT_SCORE_ESTIMATE_LIMIT = 10;
//    protected static final double AIM_SCORE_ESTIMATE_LIMIT = 10;

    
//    protected static final double ENEMY_BULLET_DIFF_THRESHOLD = Math.PI/6; // more than 30 degrees
//    protected static final double BULLET_DIFF_THRESHOLD = Math.PI/6; // more than 30 degrees
    
    protected static final double DEFAULT_CORNER_WEIGHT = 200;
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


    private Map<String,BulletInfo> enemyBulletList = new HashMap<>(15,0.95f);
    protected static Map<String,List<MoveType>> shotTypeMap = new HashMap<>(15,0.95f);
    protected static Map<String,List<MoveType>> aimTypeMap = new HashMap(15,0.95f);


    protected static final int  DEFAULT_LOG_ROUND = 6;
    protected static int  LOG_ROUND = DEFAULT_LOG_ROUND;
    protected int logRound = 0;
//    protected Map<Long,RobotPoint> myLog = new HashMap<>(2000);
//    protected Map<String, Map<Long,Enemy> > enemyLog = new HashMap<>(15,0.95f);
    protected static Map<Integer,Map<Long,RobotPoint>> myLog = new HashMap<>(LOG_ROUND,0.95f);
    protected static Map<Integer,Map<String, Map<Long,Enemy>>> enemyLog  = new HashMap<>(LOG_ROUND,0.95f);
    protected static Map<Integer,Map<String, Long>> lastEnemy = new HashMap<>(LOG_ROUND,0.95f);

//    protected Map<String,Map<Long,Score>> simplePatternScoreMap = new HashMap<>(15,0.95f);
//    protected Map<String,TreeMap<Long,Pair<Score,DistancePoint>>> reactPatternScoreMap = new HashMap<>(15,0.95f);
    protected Map<String,Map<Integer,Map<Long,Score>>> simplePatternScoreMap = new HashMap<>(15,0.95f);
    protected Map<String,Score> simplePatternBestScoreMap = new HashMap<>(15,0.95f);
    protected static Map<String,Map<Integer,Map<Long,Pair<Score,DistancePoint>>>> reactPatternScoreMap = new HashMap<>(15,0.95f);

//20
//289 : === jp.crumb.develop.Inertia* ===
//289 : ALL : 24.07 % (5754/23903)
//289 : x10 : 22.00 % (4403/20012) : 51.89
//289 : x202 : 34.72 % (1351/3891) : 55.73
//
//25
//282 : === jp.crumb.develop.Inertia* ===
//282 : ALL : 24.32 % (5734/23581)
//282 : x10 : 21.76 % (4021/18478) : 43.39
//282 : x202 : 33.57 % (1713/5103) : 60.81
//
//30
//381 : === jp.crumb.develop.Inertia* ===
//381 : ALL : 24.41 % (5755/23575)
//381 : x10 : 21.43 % (3733/17417) : 58.07
//381 : x202 : 32.84 % (2022/6158) : 55.67
//
//34
//401 : === jp.crumb.develop.Inertia* ===
//401 : ALL : 24.05 % (5764/23971)
//401 : x10 : 21.39 % (3662/17117) : 46.36
//401 : x202 : 30.67 % (2102/6854) : 56.15
//
//40
//246 : === jp.crumb.develop.Inertia* ===
//246 : ALL : 24.24 % (5782/23853)
//246 : x10 : 21.21 % (3396/16008) : 64.19
//246 : x202 : 30.41 % (2386/7845) : 47.92
//
//50
//382 : === jp.crumb.develop.Inertia* ===
//382 : ALL : 23.78 % (5778/24297)
//382 : x10 : 20.45 % (3111/15214) : 56.29
//382 : x202 : 29.36 % (2667/9083) : 63.42

    protected static final double REACT_DIFF_THRESHOLD = 30.0;
    protected static class DistancePoint extends Point {
        public double distance;
        public boolean isNearly(DistancePoint in ) {
            return calcDdiff(in) < REACT_DIFF_THRESHOLD;
        }
        public double calcDdiff(DistancePoint in){
            return this.calcDistance(in)*50 + Math.abs(distance - in.distance);
        }
    }
    protected DistancePoint calcDiffHeadingVecter(RobotPoint enemy){
        double enemyHeadingRadians = (enemy.velocity>0)?enemy.headingRadians:(enemy.headingRadians-Math.PI);
        double enemyDiffHeadingRadians = Util.calcTurnRadians(ctx.my.calcRadians(enemy),enemyHeadingRadians);
        DistancePoint ret = new DistancePoint();
        double vectorVelocity = 0.0;
        if ( enemy.velocity != 0 ) {
            vectorVelocity = (Math.abs(enemy.velocity)+1)*10;
        }
        ret.x = enemyDiffHeadingRadians;
        ret.y = vectorVelocity;
        ret.distance = ctx.my.calcDistance(enemy);
        
        return ret;
    }

    @Override
    protected void cbFirst() {
        super.cbFirst();
        LOG_ROUND = DEFAULT_LOG_ROUND / allEnemies;
        logRound = getRoundNum() % LOG_ROUND;
        myLog.put(logRound,new HashMap<Long,RobotPoint>(2000,0.95f));
        enemyLog.put(logRound,new HashMap<String,Map<Long,Enemy>>(15,0.95f));
        lastEnemy.put(logRound,new HashMap<String,Long>(15,0.95f));
        for ( Map.Entry<String,Map<Integer,Map<Long,Pair<Score,DistancePoint>>>> e : reactPatternScoreMap.entrySet() ){
            e.getValue().put(logRound,new HashMap<Long,Pair<Score,DistancePoint>>(2000,0.95f));
        }
    }

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
        moveType = new MoveType(MoveType.TYPE_RECENT_PATTERN_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }

    boolean isValidSimplePattern(RobotPoint target){
//        for ( Map.Entry<Integer,TreeMap<Long,Score>> e : getSimplePattern(target.name).entrySet() ) {
//            if ( e.getValue().size() > SIMPLE_PATTERN_TERM_MIN ) {
//                return true;
//            }
//        }
//        return false;
        return true;
    }
    void initSimplePattern(String enemyName){
        if ( ! simplePatternScoreMap.containsKey(enemyName) ) {
            simplePatternScoreMap.put(enemyName,new HashMap<Integer,Map<Long,Score>>(LOG_ROUND,0.95f));
            for ( int r = 0 ; r < LOG_ROUND; r++ ) {
                simplePatternScoreMap.get(enemyName).put(r,new HashMap<Long,Score>(2000,0.95f));
            }
        }
    }
    Map<Integer,Map<Long,Score>> getSimplePattern(String enemyName) {
        return simplePatternScoreMap.get(enemyName);
    }

    Map<Long,Pair<Score,DistancePoint>> initReactPattern(String enemyName){
        if ( ! reactPatternScoreMap.containsKey(enemyName)) {
            reactPatternScoreMap.put(enemyName,new HashMap<Integer,Map<Long,Pair<Score,DistancePoint>>>(LOG_ROUND,0.95f));
            reactPatternScoreMap.get(enemyName).put(logRound,new HashMap<Long,Pair<Score,DistancePoint>>(100,0.95f));
        }
        return reactPatternScoreMap.get(enemyName).get(logRound);
    }


    boolean isValidReactPattern(RobotPoint constEnemy){
        Map<Integer,Map<Long,Pair<Score,DistancePoint>>> reactPatternScore = reactPatternScoreMap.get(constEnemy.name);
        if ( reactPatternScore == null ) {
            return false;
        }
        DistancePoint v = calcDiffHeadingVecter(constEnemy);
        for (Map.Entry<Integer,Map<Long,Pair<Score,DistancePoint>>> e : reactPatternScore.entrySet() ) {
            int round = e.getKey();
            Map<Long,Pair<Score,DistancePoint>> reacts = e.getValue();
            for ( Long shotTime : reacts.keySet() ) {
                Pair<Score, DistancePoint> pair = reacts.get(shotTime);
                DistancePoint vecter = pair.second;
                if (vecter.isNearly(v)) {
                    return true;
                }
            }
        }
        return false;
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
        myLog.get(logRound).put(ctx.my.timeStamp,ctx.my);
    }
    protected RobotPoint logMy(int round,long absTime) {
        return myLog.get(round).get(absTime);
    }
    protected RobotPoint logMy(long absTime) {
        return logMy(logRound,absTime);
    }
    protected void updateLogEnemy(Enemy enemy){
        if ( ! enemyLog.get(logRound).containsKey(enemy.name)) {
            enemyLog.get(logRound).put(enemy.name,new HashMap<Long,Enemy>(2000,0.95f));
        }
        enemyLog.get(logRound).get(enemy.name).put(enemy.time,new Enemy(enemy));
        lastEnemy.get(logRound).put(enemy.name,enemy.time);
    }
    protected RobotPoint logEnemy(int round, String enemyName,long absTime) {
        Map<Long,Enemy> log = enemyLog.get(round).get(enemyName);
        if ( log != null ) {
            return log.get(absTime);
        }
        return null;
    }
    protected RobotPoint logEnemy(String name,long absTime) {
        return logEnemy(logRound,name,absTime);
    }


    @Override
    protected void cbMoving() {
        if ( ctx.enemies == 1 && ctx.lockonTarget != null ){
            if ( ! ctx.isFireMode(ctx.MODE_FIRE_MANUAL) ) {
                setFireMode(ctx.MODE_FIRE_AUTO);
                Enemy lockOnTarget = ctx.nextEnemyMap.get(ctx.lockonTarget);
                if ( lockOnTarget != null && lockOnTarget.energy < 0.1 && ctx.my.energy > 0.6 ) {
                    setFireMode(ctx.MODE_FIRE_CLOSE);
                    setDestination(lockOnTarget);
                    return;
                }
                if ( lockOnTarget != null ) {
                    double diffEnergy = (ctx.my.energy-lockOnTarget.energy) / (ctx.my.energy+lockOnTarget.energy)* 800;
                    if ( diffEnergy > 0 ) {
                        GT_WEIGHT = DEFAULT_GT_WEIGHT + diffEnergy;
                    }
                }
            }
        }
        super.cbMoving();
    }

    @Override
    protected Point movingBase() {
        Point dst = super.movingBase();
        // corner
        if ( ctx.enemies > 1 ) { // Melee mode
            dst.diff(Util.getGrabity(ctx.my, new Point(Util.runnableMaxX,Util.runnableMaxY).quot(2), CORNER_WEIGHT,CORNER_DIM));
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
    protected boolean prospectNextRobotRecentPattern(RobotPoint robot,long term,ProspectContext context){
        if ( context == null ) { // Not firing time
            return robot.prospectNext(term);
        }
        if ( context.shotTarget == null ) {
            context.shotTarget = robot.name; // proced
            context.shotTime = 0; // invalid

            Map<Integer,Map<Long,Pair<Score,DistancePoint>>> reactPatternScore = reactPatternScoreMap.get(robot.name);
            if ( reactPatternScore == null ) {// No reaction log
                return robot.prospectNext(term);
            }
            for (Map.Entry<Integer,Map<Long,Pair<Score,DistancePoint>>> e : reactPatternScore.entrySet() ) {
                int round = e.getKey();
                Map<Long,Pair<Score,DistancePoint>> reacts = e.getValue();
            }
        }
        return true;
    }
    @Override
    protected boolean prospectNextRobotReactPattern(RobotPoint robot,long term,ProspectContext context){
        if ( context == null ) { // Not firing time
            return robot.prospectNext(term);
        }
        if ( context.shotTarget == null ) {
            context.shotTarget = robot.name; // proced
            context.shotTime = 0; // invalid

            Map<Integer,Map<Long,Pair<Score,DistancePoint>>> reactPatternScore = reactPatternScoreMap.get(robot.name);
            if ( reactPatternScore == null ) {// No reaction log
                return robot.prospectNext(term);
            }
            DistancePoint v = calcDiffHeadingVecter(robot);
            double bestDiff = Util.fieldFullDistance;
            Score best = null;
            for (Map.Entry<Integer,Map<Long,Pair<Score,DistancePoint>>> e : reactPatternScore.entrySet() ) {
                int round = e.getKey();
                Map<Long,Pair<Score,DistancePoint>> reacts = e.getValue();
                for ( Long shotTime : reacts.keySet() ) {
                    Pair<Score, DistancePoint> pair = reacts.get(shotTime);
                    DistancePoint vecter = pair.second;
                    Score s = pair.first;
                    double diff = vecter.calcDdiff(v);
                    if ( logRound == round) {
                        if ( ctx.my.time <= s.time + REACT_PATTERN_TERM ){
                            continue;
                        }
                    }else {
                        long lastLog = lastEnemy.get(round).get(robot.name);
                        if ( lastLog <= s.time + REACT_PATTERN_TERM ) {
                            continue;
                        }
                    }
                    if ( bestDiff > diff && vecter.isNearly(v) ) {
                        logger.prospect4("D:%2.2f (%2.2f)%s D:%2.2f || (%2.2f)%s D:%2.2f", 
                                diff,
                                v.x,robot,v.distance,
                                vecter.x,logEnemy(round,robot.name,pair.first.time),vecter.distance);
                        bestDiff = diff;
                        best = pair.first;
                    }
                }
            }
            if ( best != null ) {
                context.round = best.round;
                context.shotTime = best.time;
                context.time = context.shotTime;
                context.baseTarget = robot;
                context.diff = bestDiff;
            }
        }
        if ( context.shotTime == 0 ) {
            return robot.prospectNext(term);
        }
//logger.log("BEST: %d:(%d/%d) %d = %2.2f",context.round,context.shotTime,context.baseTarget.time,robot.time-context.time,context.diff);
        for ( long l = 1; l <= term; l++) {
            long absLogTime = l+context.time;
            RobotPoint logEnemy = logEnemy(context.round,robot.name,absLogTime);
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
//logger.log("%2.2f : %s => %s  ** %s => %s",context.diff,logEnemy,logEnemy.delta,robot,robot.delta);
                if ( isPaint ) {
                    getGraphics().setStroke(new BasicStroke(1.0f));
                    getGraphics().setColor(Color.BLACK);
                    drawRound(getGraphics(),logEnemy.x,logEnemy.y,10);
                    getGraphics().setColor(Color.WHITE);
                    drawRound(getGraphics(),robot.x,robot.y,10);
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
        Score best = simplePatternBestScoreMap.get(robot.name);
        if ( best == null ) {
            return robot.prospectNext(term);
        }
        for ( long l = 1; l <= term; l++) {
            long absLogTime;
            if ( best.round == logRound ) {
                absLogTime = robot.time - best.time + l;
            }else{
                long lastLog = lastEnemy.get(best.round).get(robot.name);
                absLogTime = robot.time - best.time + lastLog + l;
            }
            RobotPoint logEnemy = logEnemy(best.round,robot.name,absLogTime);
            if ( logEnemy != null && logEnemy.delta != null) {
                logger.prospect4("%d: %d(%d) : %s",l,best.time,absLogTime,logEnemy.delta);
//                logger.log("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
                // TODO: more perform
                // Util.replayMove(robot, logEnemy);
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
//        long deltaTime = constEnemy.time - prevEnemy.time;
//        Map<Integer,TreeMap<Long,Pair<Score,DistancePoint>>> reactPatternScore = reactPatternScoreMap.get(constEnemy.name);
//        if ( reactPatternScore == null ) {
//            return;
//        }
//        for (Map.Entry<Integer,TreeMap<Long,Pair<Score,DistancePoint>>> e : reactPatternScore.entrySet() ) {
//            int round = e.getKey();
//            TreeMap<Long,Pair<Score,DistancePoint>> reacts = e.getValue();
//            for ( Long shotTime : reacts.keySet() ) {
//                Pair<Score,DistancePoint> pair = reacts.get(shotTime);
//                Score reactScore = pair.first;
//                DistancePoint vecter = pair.second;
//
//                aaa
//                for ( Map.Entry<Long,Pair<Score,DistancePoint>> p : reacts.tailMap(shotTime,false).entrySet() ) {
//                    long pastShotTime = p.getKey();
//                    long fromShotTime = constEnemy.time - pastShotTime;
//                    if ( fromShotTime <= REACT_PATTERN_TERM && vecter.isNearly(p.getValue().second) ) {
//                        long absTime = shotTime+fromShotTime + 1;
//                        RobotPoint logEnemy = logEnemy(reactScore.round,constEnemy.name, absTime);
//                        if ( logEnemy == null || logEnemy.delta == null) {
//                            continue;
//                        }
//                        RobotPoint prospectEnemy = new RobotPoint(prevEnemy);
//                        prospectEnemy.setDelta(logEnemy.delta);
//                        prospectEnemy.prospectNext(deltaTime);
//                        double d = prospectEnemy.calcDistance(constEnemy);
//                        reactScore.updateScore(PERFECT_SCORE-d,REACT_PATTERN_SCORE_ESTIMATE_LIMIT,REACT_PATTERN_SCORE_ESTIMATE_MIN);
//                        //logger.prospect4("REACT %d:(%02d/%02d/%02d): %2.2f(%2.2f)",reactScore.round,fromShotTime,shotTime,pastShotTime,reactScore.score,d);
//                        logger.log("REACT %d:(%02d/%02d/%02d): %2.2f(%2.2f)",reactScore.round,fromShotTime,shotTime,pastShotTime,reactScore.score,d);
//
//                        //double pastR = new Point(0,0).calcDegree(p.getValue().second);
//                        //double vecR = new Point(0,0).calcDegree(vecter);
//                        // logger.log("REACT: %2.2f => %2.2f = %d/%d(%d):(%2.2f/%2.2f):%2.2f",d,s.score,shotTime,pastShotTime,fromShotTime,vecR,pastR,vecter.calcDistance(p.getValue().second));
//                    }
//                    //  logger.log("REACT(%02d):%2.2f => %2.2f = %2.2f",i,d,d,s.score);
//                }
//            }
//        }
    }
    
    protected void evalSimplePattern(Enemy prevEnemy,Enemy constEnemy){
//long nnano = 0;        
//long pnano = 0;        
//long fnano = 0;    
//long dnano = 0;        
//long enano = 0;        
        Score bestScore = null;
        long deltaTime = constEnemy.time - prevEnemy.time;
        for( Map.Entry<Integer,Map<String,Map<Long,Enemy>>> e : enemyLog.entrySet() ) {
            int round = e.getKey();
            Map<Long,Enemy> logs = e.getValue().get(constEnemy.name);
            if ( logs == null ) {
                continue;
            }
            for ( Map.Entry<Long,Enemy> te : logs.entrySet() ) {
//long nano0 = System.nanoTime();                
//long nano1 = System.nanoTime();                
//nnano += nano1 - nano0;

                long absTime = te.getKey();
                Enemy logEnemy = te.getValue();
                if ( logEnemy.delta == null ) {
                    continue;
                }
                long scoreTime;
                Map<Long,Score> scores = getSimplePattern(constEnemy.name).get(round);
                if ( round == logRound ) {
                    scoreTime = constEnemy.time - absTime;
                    if ( scoreTime <= SIMPLE_PATTERN_TERM_MIN) {
                        continue;
                    }
                }else{
                    long lastLog = lastEnemy.get(round).get(constEnemy.name);
                    scoreTime = constEnemy.time - absTime + lastLog;
                    if ( scoreTime <= SIMPLE_PATTERN_TERM_MIN + constEnemy.time ) {
                        scores.remove(scoreTime);
                        continue;// TODO: remove
                    }
                }
//long nano2 = System.nanoTime();                
//fnano += nano2 - nano1;
                Score score = scores.get(scoreTime);
                if ( score == null ) {
                    score = new Score(scoreTime, round);
                    scores.put(scoreTime,score);
                }
//long nano3 = System.nanoTime();                 
//pnano += nano3 - nano2;
                RobotPoint prospectEnemy = new RobotPoint(prevEnemy);
                prospectEnemy.setDelta(logEnemy.delta);
                prospectEnemy.prospectNext(deltaTime);
                double d = prospectEnemy.calcDistance(constEnemy);

//long nano4 = System.nanoTime();                
//dnano += nano4 - nano3;
// TODO: more perform
//                MovingPoint prospectEnemy2 = new MovingPoint(prevEnemy);
//                Util.replayMove( prospectEnemy2,logEnemy);
//                double d = prospectEnemy2.calcDistance(constEnemy);

//long nano5 = System.nanoTime();                
//enano += nano5 - nano4;
                score.updateScore(PERFECT_SCORE-d,SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT);
                if ( bestScore == null || bestScore.score < score.score && score.scoreCount >= SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT ){
                    bestScore = score;
                }
//                logger.prospect4("SIMPLE %d/%03d(%03d): %2.2f(%2.2f)",score.round,score.time,absTime,score.score,d);
//                logger.log("SIMPLE %d:%03d(%03d): %2.2f(%2.2f)",score.round,score.time,absTime,score.score,d);
            }
        }
        // Update best
        simplePatternBestScoreMap.put(constEnemy.name, bestScore);
//        if ( bestScore != null ) {
//            logger.log("BEST %d:%03d: %2.2f",bestScore.round,bestScore.time,bestScore.score);
//        }

//logger.log("P: %2.2f %2.2f %2.2f %2.2f %2.2f",(double)nnano/1000000.0,(double)fnano/1000000.0,(double)pnano/1000000.0,(double)dnano/1000000.0,(double)enano/1000000.0);

    }
    @Override
    protected Enemy cbScannedRobot(Enemy enemy) {
        Enemy prevEnemy = enemyMap.get(enemy.name);
        Enemy constEnemy = super.cbScannedRobot(enemy);
        if ( constEnemy == null ) {
            return null;
        }
long start = System.nanoTime();
            updateLogEnemy(constEnemy);

            if ( ! shotTypeMap.containsKey(constEnemy.name) ) {
                shotTypeMap.put(constEnemy.name,initialShotTypeList());
            }
            if ( ! aimTypeMap.containsKey(constEnemy.name) ) {
                aimTypeMap.put(constEnemy.name,initialAimTypeList());
            }
            if ( getAimType(enemy.name,MoveType.TYPE_SIMPLE_PATTERN_CENTER) != null ||
                    getAimType(enemy.name,MoveType.TYPE_SIMPLE_PATTERN_FIRST) != null ) {
                initSimplePattern(enemy.name);
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
long end = System.nanoTime();
if ( end-start > 8000000 ) {
    logger.log("SCAN ADV : %2.2f",(double)(end-start)/1000000.0);
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
                    //logger.log("DIFF:%2.2f",reactCtx.diff);
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
            closest = (closest > Util.tankSize)?PERFECT_SCORE:closest;
            moveType.updateScore(PERFECT_SCORE-closest,AIM_SCORE_ESTIMATE_LIMIT);
            logger.prospect1("AIMTYPE(x%03x)  s:%2.2f d:%03.1f %2.2f %% (%d/%d)", moveType.type,moveType.score,closest,moveType.avrage()*100.0,moveType.hitCount,moveType.aimCount);
        }
        broadcastMessage(new AimTypeEvent(info.targetName, aimTypeList));
    }
    @Override
    protected void doFire(double power, double distance, String targetName,int type) {
        if ( getAimType(targetName,MoveType.TYPE_REACT_PATTERN_CENTER) != null ||
                getAimType(targetName,MoveType.TYPE_REACT_PATTERN_FIRST) != null ) {
            Map<Long,Pair<Score,DistancePoint>> reactScores = initReactPattern(targetName);
            Enemy enemy = enemyMap.get(targetName);
            // Create enemy vecter
            DistancePoint vecter = calcDiffHeadingVecter(enemy);
            reactScores.put(enemy.time,new Pair<>(new Score(enemy.time,logRound),vecter));
        }
        super.doFire(power, distance, targetName,type);
    }
    @Override
    protected Map.Entry<String, BulletInfo> cbBulletHit(BulletHitEvent e) {
        Map.Entry<String,BulletInfo> entry = super.cbBulletHit(e);
        Bullet bullet = e.getBullet();
//        Point dst = new Point(bullet.getX(),bullet.getY());
//        for ( Map.Entry<String,BulletInfo> ebi : enemyBulletList.entrySet() ) {
//            BulletInfo bulletInfo = ebi.getValue();
//            double diff = Util.calcPointToLineRange(bulletInfo.src, dst, bullet.getHeadingRadians());
//            if ( e.getTime() == bulletInfo.src.timeStamp && diff < Util.tankSize ) {
//                logger.fire2("CANCEL BULLET() %s : %s ", dst,bulletInfo.src);
//                impactEnemyBulletInfo(bulletInfo.bulletName);
//                break;
//            }
//        }
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
            Map.Entry<String,BulletInfo> tEntry = Util.calcBulletSrc(ctx.my.time,bullet,bulletList);
            if ( tEntry != null ) {
                double distance = new Point(e.getBullet().getX(),e.getBullet().getY()).calcDistance(new Point(bullet.getX(),bullet.getY()));
                logger.log("Unknown bullet hit: (probably teammate or my bullet) : %s : %s [%2.2f] %2.2f" , entry.getKey() , tEntry.getKey() , e.getBullet().getPower(),distance);
            }
            logger.fire1("Unknown bullet hit: (probably teammate or my bullet) ");
        }
        if ( entry != null) {
            BulletInfo info = entry.getValue();
            getAimType(info.targetName, info.type).revartAim();
            impactEnemyBulletInfo(entry.getKey());
            if( ctx.others == 1 ) {
                evalEnemyBullet(info,bullet);
            }
        }
        return entry;
    }
    protected void evalEnemyBullet(BulletInfo info,Bullet bullet) {
        double bulletVelocity = bullet.getVelocity();
        double bulletRadians = bullet.getHeadingRadians();
        String enemyName = bullet.getName();
        if ( isTeammate(enemyName) || name.equals(enemyName)) {
            return; // Not enemy bullet
        }
        long deltaTime = ctx.my.time - info.src.timeStamp;
        RobotPoint prevMy = logMy(info.src.timeStamp);
        if (prevMy == null) {
            return; // TODO: use nearest data ?
        }

        double distance = info.src.calcDistance(prevMy);
        List<MoveType> shotTypeList = shotTypeMap.get(enemyName);
        for (MoveType moveType : shotTypeList) {
            double tankWidthRadians = Math.asin((Util.tankWidth / 2) / distance);
            Pair<Long, Double> shot = calcShot(moveType, prevMy, info.src, bulletVelocity, deltaTime);
            Point bulletPoint = Util.calcPoint(shot.second, shot.first * bulletVelocity).add(info.src);
            double d = bulletPoint.calcDistance(ctx.my);
            moveType.updateScore(PERFECT_SCORE - d, SHOT_SCORE_ESTIMATE_LIMIT);
//                double shotRadians =
//                double diffRadians = Util.calcTurnRadians(bulletRadians,shotRadians);
//                diffRadians = (Math.abs(diffRadians)<ENEMY_BULLET_DIFF_THRESHOLD)?diffRadians:ENEMY_BULLET_DIFF_THRESHOLD;
//                double correctedRadians = Math.abs(diffRadians) - Math.abs(tankWidthRadians);
//                correctedRadians = (correctedRadians<0)?0:correctedRadians;
//                moveType.updateScore(Math.PI/2-correctedRadians,SHOT_SCORE_ESTIMATE_LIMIT,SHOT_SCORE_ESTIMATE_MIN);
            logger.prospect1("SHOTTYPE(x%02x)  %2.2f(%2.2f)", moveType.type, moveType.score, d);
        }
        broadcastMessage(new ShotTypeEvent(enemyName, shotTypeList));
        // recalc bullet
        List<Map.Entry<String,BulletInfo>> enemyBullets = new ArrayList<>(5);
        for( Map.Entry<String,BulletInfo> e: enemyBulletList.entrySet() ) {
            if ( e.getValue().owner.equals(enemyName) ) {
                enemyBullets.add(e);
            }
        }

        MoveType shotType =MoveType.getByScore(shotTypeMap.get(enemyName));

        for( Map.Entry<String,BulletInfo> e: enemyBullets ) {
            BulletInfo binfo = e.getValue();
            if ( binfo.type != shotType.type ) {
                // update bullet
                removeEnemyBulletInfo(e.getKey());
                BulletInfo bulletInfo = calcEnemyBullet(enemyName,binfo.src,Util.bultPower(binfo.src.velocity));
                BulletInfo nextBulletInfo = new BulletInfo(bulletInfo);
                nextBulletInfo.src.inertia(ctx.my.time - binfo.src.time);
                addEnemyBulletInfo(bulletInfo,nextBulletInfo);
            }
        }
    }

    @Override
    protected void cbHitByBullet(HitByBulletEvent e) {
        long time = e.getTime();
        Bullet bullet = e.getBullet();
        Map.Entry<String,BulletInfo> entry = Util.calcBulletSrc(ctx.my.time,bullet,enemyBulletList);
        if ( entry != null ) {
            BulletInfo info = entry.getValue();
            impactEnemyBulletInfo(entry.getKey());

            evalEnemyBullet(info,bullet);
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

        double power = prev.energy-enemy.energy;
        BulletInfo bulletInfo = calcEnemyBullet(enemy.name,src,power);
        addEnemyBulletInfo(bulletInfo);
    }
    BulletInfo calcEnemyBullet(String enemyName,MovingPoint src,double power) {
        RobotPoint prevMy = logMy(src.time-1); // Detect enemy-firing after one turn from actual.
        if ( prevMy == null ) {
            prevMy = ctx.my; // No data
        }
        double distance = src.calcDistance(prevMy);
        MoveType shotType =MoveType.getByScore(shotTypeMap.get(enemyName));
        double bulletVelocity = Util.bultSpeed(power);
        logger.fire1("ENEMY(fire): x%02x : %s(%2.2f)", shotType.type,Util.bultPower(bulletVelocity), bulletVelocity);
        double radians = calcShot(shotType,prevMy,src,bulletVelocity).second; //

        src.headingRadians = radians;
        src.velocity = bulletVelocity;
        return  new BulletInfo(enemyName,name,distance,src,shotType.type);
    }

    private void impactEnemyBulletInfo(String key){
        broadcastMessage(new CancelEnemyBalletEvent(key));
        removeEnemyBulletInfo(key);
    }
    protected void addEnemyBulletInfo(BulletInfo bulletInfo,BulletInfo nextBulletInfo) {
        enemyBulletList.put(bulletInfo.bulletName,bulletInfo);
        ctx.nextEnemyBulletList.put(bulletInfo.bulletName,nextBulletInfo);

    }
    protected void addEnemyBulletInfo(BulletInfo bulletInfo) {
        logger.fire2("SHOT(enemy): %s",bulletInfo.bulletName);
        addEnemyBulletInfo(bulletInfo,new BulletInfo(bulletInfo));
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
        limit = (limit > 4)?4:limit; // TODO:
        limit = limit*aimType.score/PERFECT_SCORE;
        double need = Util.powerByDamage(enemyEnergy);
        if ( need < limit ) {
            limit = need;
        }
        if ( limit < 0.02 && ctx.my.energy < 0.2 && enemyEnergy != 0.0 ) { // last 1 shot limitter
            return 0.0;
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
