/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.adv;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import jp.crumb.sample.*;
import jp.crumb.adv.PatternContext;
import jp.crumb.adv.PatternRobot;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.Score;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;



/**
 *
 * @author crumb
 */
public class ReactPatternLeader extends PatternRobot<PatternContext> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(0,0,0), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(0,0,0));
    }
    
    protected static final long REACT_PATTERN_TERM = 40;
    protected static final double REACT_PATTERN_SCORE_ESTIMATE_LIMIT = 200;
    protected static final double REACT_PATTERN_SCORE_ESTIMATE_MIN = 40;

    protected Map<String,TreeMap<Long,Pair<Score,TimedPoint>>> reactPatternScoreMap = new HashMap<>();
    @Override
    protected List<MoveType> initialAimTypeList() {
        List<MoveType> moveTypeList = new ArrayList<>();
//        List<MoveType> moveTypeList = super.initialAimTypeList();
        MoveType moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }

    protected Point calcDiffHeadingVecter(RobotPoint enemy){
        double enemyHeadingRadians = (enemy.velocity>0)?enemy.headingRadians:(enemy.headingRadians-Math.PI);
        double enemyDiffHeadingRadians = Util.calcTurnRadians(ctx.my.calcRadians(enemy),enemyHeadingRadians);
        return Util.calcPoint(enemyDiffHeadingRadians,enemy.velocity);
    }
    @Override
    protected void doFire(double power, double distance, String targetName) {
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
        super.doFire(power, distance, targetName);
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
logger.log("(%d/%d) %s: %2.2f => %d",robot.time,robot.timeStamp,s.name,s.score,robot.time-Integer.valueOf(s.name));


        for ( long l = 1; l <= term; l++) {
            long absLogTime = l+Integer.valueOf(s.name);
            RobotPoint logEnemy = logEnemy(robot.name,absLogTime);
            if ( logEnemy != null && logEnemy.delta != null) {
getGraphics().setColor(Color.BLACK);
drawRound(getGraphics(),logEnemy.x,logEnemy.y,10);
                robot.setDelta(logEnemy.delta);
                robot.prospectNext(1);
getGraphics().setColor(Color.WHITE);
drawRound(getGraphics(),robot.x,robot.y,10);
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
    protected void evalSimplePattern(Enemy prevEnemy,Enemy constEnemy){
        super.evalSimplePattern(prevEnemy, constEnemy);
        
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

                    //double pastR = new Point(0,0).calcDegree(p.getValue().second);
                    //double vecR = new Point(0,0).calcDegree(vecter);
                    // logger.log("REACT: %2.2f => %2.2f = %d/%d(%d):(%2.2f/%2.2f):%2.2f",d,s.score,shotTime,pastShotTime,fromShotTime,vecR,pastR,vecter.calcDistance(p.getValue().second));
                }


//                logger.prospect4("REACT(%02d):",shotTime,d,s.score);
                //  logger.log("REACT(%02d):%2.2f => %2.2f = %2.2f",i,d,d,s.score);
                
            }
        }
    }
}
