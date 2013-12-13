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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jp.crumb.CrumbRobot;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.Copy;
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
    
    protected static final double DEFAULT_ANGLE_LIMIT = Math.PI/6;


    // For move
    protected double CORNER_WEIGHT          = DEFAULT_CORNER_WEIGHT;
    protected double CORNER_DIM             = DEFAULT_CORNER_DIM;
    protected long   ENEMY_BULLET_PROSPECT_TIME   = DEFAULT_ENEMY_BULLET_PROSPECT_TIME;
    protected double ENEMY_BULLET_WEIGHT          = DEFAULT_ENEMY_BULLET_WEIGHT;
    protected double ENEMY_BULLET_DIM             = DEFAULT_ENEMY_BULLET_DIM;
    protected double ENEMY_BULLET_DISTANCE        = DEFAULT_ENEMY_BULLET_DISTANCE;
    protected double ANGLE_LIMIT = DEFAULT_ANGLE_LIMIT;


    private Map<String,BulletInfo> enemyBulletList = new HashMap<>(15,0.95f);
    protected static Map<String,List<MoveType>> shotTypeMap = new HashMap<>(15,0.95f);
    protected static Map<String,List<MoveType>> aimTypeMap = new HashMap(15,0.95f);


    protected static final int  DEFAULT_LOG_ROUND = 6;
    protected static int LOG_ROUND = DEFAULT_LOG_ROUND;
    protected static int EVAL_SIMPLE_PATTERN_ROUND = 2;
    protected int logRound = 0;
//    protected Map<Long,RobotPoint> myLog = new HashMap<>(2000);
//    protected Map<String, Map<Long,Enemy> > enemyLog = new HashMap<>(15,0.95f);
    protected static Map<Integer,Map<Long,RobotPoint>> myLog = new HashMap<>(LOG_ROUND,0.95f);
    protected static Map<Integer,Map<String, Map<Long,Enemy>>> enemyLog  = new HashMap<>(LOG_ROUND,0.95f);
    protected static Map<Integer,Map<String, Long>> lastEnemy = new HashMap<>(LOG_ROUND,0.95f);

//    protected Map<String,Map<Long,Score>> simplePatternScoreMap = new HashMap<>(15,0.95f);
//    protected Map<String,TreeMap<Long,Pair<Score,DistancePoint>>> reactPatternScoreMap = new HashMap<>(15,0.95f);
    protected Map<String,Map<Integer,Map<Long,Score>>> simplePatternScoreMap = new HashMap<>(15,0.95f);
    protected static int K_SIMPLE = 5;
    protected Map<String,List<Score>> simplePatternBestScoreMap = new HashMap<>(15,0.95f);
    protected static Map<String,Map<Integer, TreeMap<Long,DistancePoint>>> reactPatternScoreMap = new HashMap<>(15,0.95f);
    protected static Map<String,Map<Integer, TreeMap<Long,AimLog>>> bulletLogMap = new HashMap<>(15,0.95f);

    Point ANTI_UNKNOWN = null;

    protected static class AimLog {
        List<Score> simple;
        Score       react;
        AimLog(List<Score> simple,Score react){
            if ( simple != null ) {
                this.simple = Util.deepCopyArrayList(simple, new Copy<Score>(){
                    @Override
                    public Score copy(Score e) {
                        return new Score(e);
                    }
                });
            }
            if ( react != null) {
                this.react = new Score(react);
            }
        }
    }
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
        // LOG_ROUND = DEFAULT_LOG_ROUND / allEnemies;
        logRound = getRoundNum() % LOG_ROUND;
        myLog.put(logRound,new HashMap<Long,RobotPoint>(2000,0.95f));
        enemyLog.put(logRound,new HashMap<String,Map<Long,Enemy>>(15,0.95f));
        lastEnemy.put(logRound,new HashMap<String,Long>(15,0.95f));
        for ( Map.Entry<String,Map<Integer,TreeMap<Long,DistancePoint>>> e : reactPatternScoreMap.entrySet() ){
            e.getValue().put(logRound,new TreeMap<Long,DistancePoint>());
        }
        for ( Map.Entry<String,Map<Integer,TreeMap<Long,AimLog>>> e : bulletLogMap.entrySet() ){
            e.getValue().put(logRound,new TreeMap<Long,AimLog>());
        }
    }

    protected List<MoveType> initialShotTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>(10);
        MoveType moveType = null;
        moveType = new MoveType(MoveType.TYPE_UNKNOWN);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_PINPOINT);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCELERATION);
        moveTypeList.add(moveType);
        return moveTypeList;
    }
    protected List<MoveType> initialAimTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>(10);
        MoveType moveType = null;
//        moveType = new MoveType(MoveType.TYPE_PINPOINT);
//        moveTypeList.add(moveType);
//        moveType = new MoveType(MoveType.TYPE_INERTIA);
//        moveTypeList.add(moveType);
//        moveType = new MoveType(MoveType.TYPE_ACCELERATION);
//        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_SIMPLE_PATTERN);
        moveTypeList.add(moveType);
        moveType.score = 100.0; // Initial type (will be overrided by first hit!!)
        moveType = new MoveType(MoveType.TYPE_REACT_PATTERN);
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

    Pair<TreeMap<Long,AimLog>,TreeMap<Long,DistancePoint>> initReactPattern(String enemyName){
        if ( ! reactPatternScoreMap.containsKey(enemyName)) {
            reactPatternScoreMap.put(enemyName,new HashMap<Integer,TreeMap<Long,DistancePoint>>(LOG_ROUND,0.95f));
            reactPatternScoreMap.get(enemyName).put(logRound,new TreeMap<Long,DistancePoint>());
        }
        if ( ! bulletLogMap.containsKey(enemyName)) {
            bulletLogMap.put(enemyName,new HashMap<Integer,TreeMap<Long,AimLog>>(LOG_ROUND,0.95f));
            bulletLogMap.get(enemyName).put(logRound,new TreeMap<Long,AimLog>());
        }
        return new Pair<>(bulletLogMap.get(enemyName).get(logRound) , reactPatternScoreMap.get(enemyName).get(logRound));
    }


    boolean isValidReactPattern(RobotPoint enemy){
        Map<Integer,TreeMap<Long,DistancePoint>> reactPatternScore = reactPatternScoreMap.get(enemy.name);
        if ( reactPatternScore == null ) {
            return false;
        }
        DistancePoint v = calcDiffHeadingVecter(enemy);
        for (Map.Entry<Integer,TreeMap<Long,DistancePoint>> e : reactPatternScore.entrySet() ) {
            int round = e.getKey();
            Map<Long,DistancePoint> reacts = e.getValue();
            for ( Long shotTime : reacts.keySet() ) {
                DistancePoint vector = reacts.get(shotTime);
                if (vector.isNearly(v)) {
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
                if ( ANTI_UNKNOWN != null ) {
                    setDestination(ANTI_UNKNOWN);
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
                        dst.diff(Util.getGrabity(ctx.my,bullet, ENEMY_BULLET_WEIGHT*info.threat,ENEMY_BULLET_DIM));
                        bullet.inertia(1);
                    }
                }
            }
        }
        // TODO: Right angle move (researching...)
        if ( ctx.enemies == 1 ) {
            Enemy lockOnTarget = ctx.nextEnemyMap.get(ctx.lockonTarget);
            if ( lockOnTarget != null ) {
                double heading = ctx.my.calcRadians(dst);
                double angle = ctx.my.calcRadians(lockOnTarget);
                double turn1 = Util.calcTurnRadians(heading, angle + Math.PI/2);
                double turn2 = Util.calcTurnRadians(heading, angle - Math.PI/2);
                double dist = ctx.my.calcDistance(dst);
                if ( Math.abs(turn1) < Math.abs(turn2) ) {
                    if ( Math.abs(turn1) > ANGLE_LIMIT ) {
                        if ( turn1 < 0 ) {
                            dst = Util.calcPoint(angle + Math.PI/2 + ANGLE_LIMIT , dist).add(ctx.my);
                        }else {
                            dst = Util.calcPoint(angle + Math.PI/2 - ANGLE_LIMIT , dist).add(ctx.my);
                        }
                    }
                }else{
                    if ( Math.abs(turn2) > ANGLE_LIMIT ) {
                        if ( turn2 < 0 ) {
                            dst = Util.calcPoint(angle - Math.PI/2 + ANGLE_LIMIT , dist).add(ctx.my);
                        }else {
                            dst = Util.calcPoint(angle - Math.PI/2 - ANGLE_LIMIT , dist).add(ctx.my);
                        }
                    }
                }
            }
        }
        return dst;
    }

    protected List<Point> replayAsPattern(RobotPoint robot,long term,Score score ){
        return replayAsPattern(robot,term,score,false);
    }
    protected List<Point> replayAsPattern(RobotPoint robot,long term,Score score, boolean limitError ){
//@@@
//        if ( score.round == logRound ) {
//            score.time = robot.time - score.time;
//        }else{
//            long lastLog = lastEnemy.get(score.round).get(robot.name);
//            score.time = robot.time - score.time + lastLog;
//        }
        RobotPoint baseLog = null;
        List<Point> ret = new ArrayList<>((int)term);
        int velocityTowards = 1;
        for ( long l = 1; l <= term; l++) {
            long absLogTime = score.time + l;
            RobotPoint logEnemy = logEnemy(score.round,robot.name,absLogTime);
            if ( logEnemy != null && logEnemy.delta != null) {
                if ( baseLog == null ) {
                    baseLog = logEnemy;
                    if ( robot.velocity * baseLog.velocity < 0 && robot.headingRadians * baseLog.headingRadians < 0 ) {
                        velocityTowards = -1;
                    }
                }
                MovingPoint delta = logEnemy.delta;
                if ( velocityTowards < 0 ) {
                    delta = new MovingPoint(delta).prod(-1);
                    delta.time = Math.abs(delta.time);
                }
                // TODO: more perform
                robot.setDelta(delta);
                robot.prospectNext();
                if ( limitError && robot.isLimit() ) {
                    return null;
                }
                ret.add(new Point(robot));
                logger.prospect4("%d: %d(%d) : %s %s",l,score.time,absLogTime,logEnemy.delta,robot);
                // logger.log("%d: %d(%d) : %s %s",l,score.time,absLogTime,logEnemy.delta,robot);
                if ( isPaint ) {
                    getGraphics().setStroke(new BasicStroke(1.0f));
                    if (  term > 1 ) {
                        if ( paintFlg == 1 ) {
                            if ( l == term ) {
                                getGraphics().setColor(Color.BLACK);
                                drawRound(getGraphics(), logEnemy.x, logEnemy.y, 2);
                            } else {
                                getGraphics().setColor(Color.GRAY);
                                drawRound(getGraphics(), logEnemy.x, logEnemy.y, 1);
                            }

                            if (l == term) {
                                getGraphics().setColor(Color.RED);
                                drawRound(getGraphics(), robot.x, robot.y, 7);
                            } else {
                                getGraphics().setColor(Color.GREEN);
                                drawRound(getGraphics(), robot.x, robot.y, 1);
                            }
                        }else if ( paintFlg == 2 ) {
                            if ( l == term ) {
                                getGraphics().setColor(Color.DARK_GRAY);
                                drawRound(getGraphics(), logEnemy.x, logEnemy.y, 2);
                            } else {
                                getGraphics().setColor(Color.LIGHT_GRAY);
                                drawRound(getGraphics(), logEnemy.x, logEnemy.y, 1);
                            }

                            if (l == term) {
                                getGraphics().setColor(Color.PINK);
                                drawRound(getGraphics(), robot.x, robot.y, 7);
                            } else {
                                getGraphics().setColor(Color.ORANGE);
                                drawRound(getGraphics(), robot.x, robot.y, 1);
                            }
                        }else {
//                            if ( l == term ) {
//                                getGraphics().setColor(Color.MAGENTA);
//                                drawRound(getGraphics(),robot.x,robot.y,4);
//                            }else {
//                                getGraphics().setColor(Color.CYAN);
//                                drawRound(getGraphics(),robot.x,robot.y,1);
//                            }
                        }
                    }
                }
                logger.prospect4("%d: (%d/%d) : %s",l,score.time,absLogTime,logEnemy.delta);
            }else{
                robot.prospectNext();
                ret.add(new Point(robot));
            }
        }
        return ret;        
    }

    protected Score getBestReact(RobotPoint robot,DistancePoint v){
        Map<Integer,TreeMap<Long,DistancePoint>> reactPatternScore = reactPatternScoreMap.get(robot.name);
        if ( reactPatternScore == null ) {// No reaction log
            return null;
        }
        double bestDiff = Util.fieldFullDistance;
        Score best = null;
        for (Map.Entry<Integer, TreeMap<Long, DistancePoint>> e : reactPatternScore.entrySet()) {
            int round = e.getKey();
            Map<Long, DistancePoint> reacts = e.getValue();
            for (Long shotTime : reacts.keySet()) {
                DistancePoint vector = reacts.get(shotTime);
                double diff = vector.calcDdiff(v);
                if (logRound == round) {
                    if (ctx.my.time <= shotTime + REACT_PATTERN_TERM) {
                        continue;
                    }
                } else {
                    long lastLog = lastEnemy.get(round).get(robot.name);
                    if (lastLog <= shotTime + REACT_PATTERN_TERM) {
                        continue;
                    }
                }
                if (bestDiff > diff && vector.isNearly(v)) {
                    logger.prospect4("D:%2.2f (%2.2f)%s D:%2.2f || (%2.2f)%s D:%2.2f",
                            diff,
                            v.x, robot, v.distance,
                            vector.x, logEnemy(round, robot.name, shotTime), vector.distance);
                    bestDiff = diff;
                    best = new Score(shotTime,logRound);
                }
            }
        }        
        return best;
    }
    
    @Override
    protected List<Point> prospectNextRobotReactPattern(RobotPoint robot,long term){
        DistancePoint v = calcDiffHeadingVecter(robot);
        Score best = getBestReact(robot,v);
        if (best != null) {
            return replayAsPattern(robot,term, best);
        }
        return prospectNextRobotAcceleration(robot, term);
    }    
    
    @Override
    protected List<Point> prospectNextRobotSimplePattern(RobotPoint robot,long term){
        List<Score> bests = simplePatternBestScoreMap.get(robot.name);
        if (bests == null || bests.size() == 0) {
            return prospectNextRobotAcceleration(robot, term);
        }
        List<Point> res = null;
        // TODO: k-nearlest
        RobotPoint cand = null;

        for ( Score best : bests) {
            cand = new RobotPoint(robot);
            res = replayAsPattern(cand, term,best,true);
            if ( res != null ) {
                break;
            }
        }
        if ( res == null ) {
            cand = new RobotPoint(robot);
            res = replayAsPattern(cand, term,bests.get(0));
        }
        robot.set(cand);
        return res;
    }

    
    protected void evalSimplePattern(Enemy prevEnemy,Enemy constEnemy){
//long nnano = 0;        
//long pnano = 0;        
//long fnano = 0;    
//long dnano = 0;        
//long enano = 0;
        TreeMap<Double,Score> bestScores = new TreeMap<>();
        long deltaTime = constEnemy.time - prevEnemy.time;
        for( Map.Entry<Integer,Map<String,Map<Long,Enemy>>> e : enemyLog.entrySet() ) {
            int round = e.getKey();
            if ( ! (round <= logRound && round > (logRound - EVAL_SIMPLE_PATTERN_ROUND) ||
                 round > logRound && round > LOG_ROUND + logRound - EVAL_SIMPLE_PATTERN_ROUND)  ) {
                continue;
            }

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
                        continue;
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
                for ( long i = 0 ; i < deltaTime ; i++ ) {
                    prospectEnemy.prospectNext();
                }
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
                
                if ( score.scoreCount >= SIMPLE_PATTERN_SCORE_ESTIMATE_LIMIT ) {
                    if ( bestScores.size() < K_SIMPLE ) {
                        bestScores.put(score.score,score);
                    } else {
                        if ( bestScores.firstKey() < score.score ) {
                            bestScores.remove(bestScores.firstKey());
                            bestScores.put(score.score,score);
                        }
                    }
                }
//                logger.prospect4("SIMPLE %d/%03d(%03d): %2.2f(%2.2f)",score.round,score.time,absTime,score.score,d);
//                logger.log("SIMPLE %d:%03d(%03d): %2.2f(%2.2f)",score.round,score.time,absTime,score.score,d);
            }
        }
        // Update best
        List bests = new ArrayList(bestScores.values());
        Collections.reverse(bests);
        simplePatternBestScoreMap.put(constEnemy.name,bests);
//        if ( bestScore != null ) {
//            logger.log("BEST %d:%03d: %2.2f",bestScore.round,bestScore.time,bestScore.score);
//        }
//logger.log("P: %2.2f %2.2f %2.2f %2.2f %2.2f",(double)nnano/1000000.0,(double)fnano/1000000.0,(double)pnano/1000000.0,(double)dnano/1000000.0,(double)enano/1000000.0);
    }

    int paintFlg = 0;
    @Override
    protected Enemy cbScannedRobot(Enemy enemy) {
        
        Enemy prevEnemy = enemyMap.get(enemy.name);
        Enemy curEnemy = super.cbScannedRobot(enemy);
        if ( curEnemy == null ) {
            return null;
        }
        updateLogEnemy(curEnemy);

        if (!shotTypeMap.containsKey(curEnemy.name)) {
            shotTypeMap.put(curEnemy.name, initialShotTypeList());
        }
        if (!aimTypeMap.containsKey(curEnemy.name)) {
            aimTypeMap.put(curEnemy.name, initialAimTypeList());
        }
        initSimplePattern(curEnemy.name);

        if (prevEnemy != null) {
            List<MoveType> aimTypeList = aimTypeMap.get(curEnemy.name);
            for (MoveType moveType : aimTypeList) {
                Map<Integer,TreeMap<Long,AimLog>> bulletLogs = bulletLogMap.get(curEnemy.name);
                if ( bulletLogs != null ) {
                    long EVAL_LIMIT = 30;
                    long EVAL_THRESHOLD = 10;
                    for(Map.Entry<Long,AimLog> bulletLog : bulletLogs.get(logRound).tailMap(ctx.my.time - EVAL_LIMIT).entrySet() ){
                        long shotTime = bulletLog.getKey();
                        long deltaTime = ctx.my.time - shotTime;
                        if ( deltaTime < EVAL_THRESHOLD ) {
                            continue;
                        }
                        AimLog aimLog = bulletLog.getValue();
                        RobotPoint log = logEnemy(curEnemy.name, shotTime);
                        if ( log != null ) {
                            RobotPoint evalLog = null;
                            if ( moveType.isTypePinPoint() ) {
                                evalLog = new RobotPoint(log);
                            }else if ( moveType.isTypeSimplePattern() ) {
                                if ( aimLog.simple == null ) {
                                    break; // SKIP
                                }
                                paintFlg = 1;
                                List<Point> res = null;
                                if ( ! aimLog.simple.isEmpty() ) {
                                    for ( Score best : aimLog.simple ) {
                                        evalLog = new RobotPoint(log);
                                        res = replayAsPattern(evalLog,deltaTime,best,true);
                                        if ( res != null ) {
                                            break;
                                        }
                                    }
                                    if ( res == null ) {
                                        evalLog = new RobotPoint(log);
                                        res = replayAsPattern(evalLog,deltaTime,aimLog.simple.get(0),true);
                                    }
                                }
                                paintFlg = 0;
                            }else if ( moveType.isTypeReactPattern() ) {
                                if ( aimLog.react == null ) {
                                    break; // SKIP
                                }
                                evalLog = new RobotPoint(log);
                                paintFlg = 2;
                                replayAsPattern(evalLog,deltaTime,aimLog.react);
                                paintFlg = 0;
                            }else{
                                evalLog = new RobotPoint(log);
                                prospectNextRobot(evalLog, moveType, deltaTime);
                            }
                            if ( evalLog != null) {
                                double d = curEnemy.calcDistance(evalLog);
                                moveType.updateScore((PERFECT_SCORE-d)*deltaTime/EVAL_LIMIT,AIM_SCORE_ESTIMATE_LIMIT);
                                // logger.log("EACH-AIM(x%03x)  s:%2.2f d:%03.1f T:%d(%d) %s", moveType.type,moveType.score,d,shotTime,ctx.my.time - shotTime,evalLog);
                            }
//@@@
//                            break;
                        }
                    }
                }
            }
            curEnemy.heat = prevEnemy.heat - Util.gunCoolingRate * (curEnemy.time - prevEnemy.time);
            // Aimed (gunCoolingRate : It'll run before prospectNextTurn() will decrement heat)
            if ( (prevEnemy.energy - curEnemy.energy) > 0 && (prevEnemy.energy - curEnemy.energy) <= 3 && curEnemy.heat <= Util.gunCoolingRate ) {
                enemyBullet(prevEnemy, curEnemy);
                curEnemy.heat = Util.bultHeat(prevEnemy.energy - curEnemy.energy);
            }
            evalSimplePattern(prevEnemy, curEnemy);
        }
        return curEnemy;
    }

    @Override
    protected void doFire(double power, double distance, RobotPoint target,int type) {
        Pair<TreeMap<Long,AimLog>,TreeMap<Long,DistancePoint>> pair = initReactPattern(target.name);
        Map<Long,AimLog> bulletLogs = pair.first;
        Map<Long,DistancePoint> reactScores = pair.second;
        // Create enemy vecter
        DistancePoint vecter = calcDiffHeadingVecter(target);
        reactScores.put(target.time,vecter);
        // BulletLog
        bulletLogs.put(target.time,new AimLog(
                simplePatternBestScoreMap.get(target.name),
                getBestReact(target,vecter)
                ));
        for ( MoveType moveType : aimTypeMap.get(target.name) ) {
            logger.prospect1("AIMTYPE(x%03x)  s:%2.2f %2.2f %% (%d/%d)", moveType.type,moveType.score,moveType.avrage()*100.0,moveType.hitCount,moveType.aimCount);
        }

        super.doFire(power, distance, target,type);
    }
    @Override
    protected Map.Entry<String, BulletInfo> cbBulletHit(BulletHitEvent e) {
        Map.Entry<String,BulletInfo> entry = super.cbBulletHit(e);
        Bullet bullet = e.getBullet();
        if ( entry != null ) {
            BulletInfo info = entry.getValue();
            if ( bullet.getVictim().equals(info.targetName) ) {
                getAimType(info.targetName, info.type).updateHit();
            }
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
                logger.fire1("Unknown bullet hit: (probably teammate or my bullet) : %s : %s [%2.2f] %2.2f" , entry.getKey() , tEntry.getKey() , e.getBullet().getPower(),distance);
            }else{
                logger.fire1("Unknown bullet hit: (probably teammate or my bullet) ");
            }
        }
        if ( entry != null ) {
            BulletInfo info = entry.getValue();
            getAimType(info.targetName, info.type).revartAim();
        }
        if ( eEntry != null) {
            BulletInfo info = eEntry.getValue();
            impactEnemyBulletInfo(eEntry.getKey());
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
            return; // Maybe Skip turns
        }

        double distance = info.src.calcDistance(prevMy);
        long maxTerm = (long)Math.ceil(distance/(bulletVelocity-8));
        List<MoveType> shotTypeList = shotTypeMap.get(enemyName);
        double closest = Util.fieldFullDistance;
        MoveType shotUnknownType = null;
        for (MoveType moveType : shotTypeList) {
            if ( moveType.isTypeUnknown() ) {
                shotUnknownType = moveType;
                continue;
            }
            double tankWidthRadians = Math.asin((Util.tankWidth / 2) / distance);
            Pair<Long, Double> shot = calcShot(moveType, prevMy, info.src, bulletVelocity, maxTerm);
            Point aimPoint = Util.calcPoint(shot.second, shot.first * bulletVelocity).add(info.src);
            Point bulletPoint = Util.calcPoint(bullet.getHeadingRadians(), shot.first * bulletVelocity).add(info.src);
            double d = aimPoint.calcDistance(bulletPoint);
            moveType.updateScore(PERFECT_SCORE - d, SHOT_SCORE_ESTIMATE_LIMIT);
            if ( closest > d ) {
                closest = d;
            }
            logger.prospect1("SHOTTYPE(x%02x)  %2.2f(%2.2f)", moveType.type, moveType.score, d);
        }
        if ( shotUnknownType != null ) {
            if ( closest < Util.tankSize ) {
                closest = Util.tankSize;
            }else {
                closest = 0;
            }
            shotUnknownType.updateScore(PERFECT_SCORE - closest, SHOT_SCORE_ESTIMATE_LIMIT);
            logger.prospect1("SHOTTYPE(x%02x)  %2.2f(%2.2f)", shotUnknownType.type, shotUnknownType.score, closest);

        }
        broadcastMessage(new ShotTypeEvent(enemyName, shotTypeList));

        recalcEnemyBullet(enemyName);
    }
    protected void recalcEnemyBullet(String enemyName) {
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
                BulletInfo bulletInfo = calcEnemyBullet(enemyName,binfo.src,Util.bultPower(binfo.src.velocity),shotType);
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
        if ( ctx.enemies > 1 ) {
            double myDistance = enemy.calcDistance(ctx.my);
            long count = 0;
            for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
                double d = enemy.calcDistance(e.getValue());
                if ( d > Util.tankWidth && (d * 1.5) < myDistance ) {
                    count++;
                    if ( count > ctx.enemies / 3 ) {
                        return;
                    }
                }
            }
        }
        MoveType aimType = MoveType.getByScore(aimTypeMap.get(prev.name));
        Enemy cpPrev = new Enemy(prev);
        prospectNextRobot(cpPrev, aimType,1); // Next turn of past scan.
        MovingPoint src = new MovingPoint(cpPrev);
        double power = prev.energy-enemy.energy;
        MoveType shotType =MoveType.getByScore(shotTypeMap.get(enemy.name));
        BulletInfo bulletInfo = calcEnemyBullet(enemy.name,src,power,shotType);
        addEnemyBulletInfo(bulletInfo);
        if ( ctx.enemies == 1 && shotType.isTypeUnknown() ) {
            double distance = ctx.my.calcDistance(src);
            double term = distance/Util.bultSpeed(power);
            double forwardDistance = 0;
            double forwardVelocity = ctx.my.velocity;
            double backDistance = 0;
            double backVelocity = ctx.my.velocity;
            for ( int i = 0 ; i < term;i++ ) {
                forwardDistance += forwardVelocity;
                backDistance    += backVelocity;
                if ( forwardVelocity >= 0 ) {
                    forwardVelocity = (forwardVelocity>8)?8:forwardVelocity+1;
                }else{
                    forwardVelocity += 2;
                }
                if ( backVelocity > 0 ) {
                    backVelocity -= 2;
                }else{
                    backVelocity = (backVelocity<-8)?-8:backVelocity-1;
                }
            }
            double angle = ctx.my.calcRadians(enemy);
            double turn1 = Util.calcTurnRadians(ctx.my.headingRadians, angle + Math.PI/2);
            double turn2 = Util.calcTurnRadians(ctx.my.headingRadians, angle - Math.PI/2);
            double rightAngle = angle - Math.PI/2;
            if ( Math.abs(turn1) < Math.abs(turn2) ) {
                rightAngle = angle + Math.PI/2;
            }
            // TODO: more effectively !!

            double dstDistance = Math.random()*(forwardDistance-backDistance) - (forwardDistance - backDistance / 2);
            ANTI_UNKNOWN = adjustMoveAngle(ctx.my, rightAngle, dstDistance);
            if ( ANTI_UNKNOWN == null ) {
                ANTI_UNKNOWN = adjustMoveAngle(ctx.my, rightAngle, dstDistance*-1);
            }
        }else{
            ANTI_UNKNOWN = null;
        }
    }
    Point adjustMoveAngle(Point src, double angle, double distance ) {
        Point p = Util.calcPoint(angle,distance).add(src);
        if ( ! p.isLimit() ) {
            return p;
        }
        double tmp1Angle = angle;
        double tmp2Angle = angle;
        for (int i = 0; i < 4 ; i++) {
            tmp1Angle += Math.PI/12;
            p = Util.calcPoint(tmp1Angle,distance).add(src);
            if ( ! p.isLimit() ) {
                return p;
            }
            tmp2Angle -= Math.PI/12;
            p = Util.calcPoint(tmp2Angle,distance).add(src);
            if ( ! p.isLimit() ) {
                return p;
            }
        }
        return null;
    }

    BulletInfo calcEnemyBullet(String enemyName,MovingPoint src,double power,MoveType shotType) {
        RobotPoint prevMy = logMy(src.time-1); // Detect enemy-firing after one turn from actual.
        if ( prevMy == null ) {
            prevMy = ctx.my; // No data
        }
        double distance = src.calcDistance(prevMy);
        double bulletVelocity = Util.bultSpeed(power);
        logger.fire1("ENEMY(fire): x%02x : %s(%2.2f)", shotType.type,power, bulletVelocity);
        long maxTerm = (long)Math.ceil(distance/(bulletVelocity-8));
        double radians = calcShot(shotType,prevMy,src,bulletVelocity,maxTerm).second; //

        src.headingRadians = radians;
        src.velocity = bulletVelocity;
        double threat = power;
        if ( shotType.isTypeUnknown() ) {
            threat = 0;
        }
        return  new BulletInfo(enemyName,name,distance,src,shotType.type,threat);        
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
    protected BulletInfo removeEnemyBulletInfo(String key){
        logger.fire2("IMPACT(enemy): %s",key);
        ctx.nextEnemyBulletList.remove(key);
        return enemyBulletList.remove(key);
    }

    @Override
    protected void cbUnprospectiveNextTurn(){
        super.cbUnprospectiveNextTurn();
        List<String> rmEnemyBullet = new ArrayList<>();
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            if ( e.getValue().src.isOutOfField() ) {
                rmEnemyBullet.add(e.getKey());
            }else{
                BulletInfo info = enemyBulletList.get(e.getKey());
                double distMy = info.src.calcDistance(ctx.my);
                double distB  = info.src.calcDistance(e.getValue().src);
                if ( distB > distMy + Util.tankSize/2 ) {
                    rmEnemyBullet.add(e.getKey());
                }
            }
        }
        for( String n : rmEnemyBullet ) {
            BulletInfo info = removeEnemyBulletInfo(n);
            if ( ctx.others == 1 ) {
                MoveType shotType =MoveType.getByScore(shotTypeMap.get(info.owner));
                shotType.updateScore(PERFECT_SCORE, SHOT_SCORE_ESTIMATE_LIMIT*7); // 1/7 weight (profitable ratio)
                logger.prospect1("SHOTTYPE(x%02x)  %2.2f(%2.2f/7)", shotType.type, shotType.score, PERFECT_SCORE);
            }
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
        super.dumpLog();
        // g.drawString(String.format("hit: %d / %d  : %2.2f / %2.2f", r.getAimType().hitCount, r.getAimType().aimCount, r.getAimType().hitTime, r.getAimType().aimTime), (int) r.x - 20, (int) r.y - 40);
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
