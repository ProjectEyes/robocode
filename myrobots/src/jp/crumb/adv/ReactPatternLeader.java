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
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.Score;
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
        this.setBulletColor(new Color(200,255,100));
    }
    
    protected static final long REACT_PATTERN_TERM = 70;
    protected static final double REACT_PATTERN_SCORE_ESTIMATE_LIMIT = 10;

    protected Map<String,TreeMap<Long,Score>> reactPatternScoreMap = new HashMap<>();
    @Override
    protected List<MoveType> initialAimTypeList() {
        List<MoveType> moveTypeList = new ArrayList<>();
//        List<MoveType> moveTypeList = super.initialAimTypeList();
        MoveType moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }

    @Override
    protected void doFire(double power, double distance, String targetName) {
        if ( ! reactPatternScoreMap.containsKey(targetName)) {
            TreeMap<Long,Score> scores = new TreeMap<>();
            reactPatternScoreMap.put(targetName,scores);
        }
        Map<Long,Score> scores = reactPatternScoreMap.get(targetName);
        scores.put(ctx.my.time,new Score(String.valueOf(ctx.my.time)));
        
        super.doFire(power, distance, targetName);
    }
    
      @Override
    protected boolean prospectNextRobotReactPattern(RobotPoint robot,long term){
        Map<Long,Score> scores = reactPatternScoreMap.get(robot.name);
        if ( scores == null || scores.size() <= term) {
            return robot.prospectNext(term);
        }
        Score s = Score.getByScore(scores.values());
        for ( long l = 1; l <= term; l++) {
            long absLogTime = l+robot.time-Integer.valueOf(s.name);
            RobotPoint logEnemy = logEnemy(robot.name,absLogTime);
            if ( logEnemy != null && logEnemy.delta != null) {
//                logger.prospect4("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
//                logger.log("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
                robot.setDelta(logEnemy.delta);
                robot.add(logEnemy.delta);
            }else{
                robot.prospectNext(1);
            }
        }
        return true;
    }
  
    
    
    @Override
    protected void evalSimplePattern(Enemy prevEnemy,Enemy constEnemy){
        super.evalSimplePattern(prevEnemy, constEnemy);
        
        TreeMap<Long,Score> scores = reactPatternScoreMap.get(constEnemy.name);
        if ( scores == null ) {
            return;
        }
        long deltaTime = constEnemy.time - prevEnemy.time;
        for ( Map.Entry<Long,Score> e : scores.entrySet()) {
            long shotTime = e.getKey();
            Score s = e.getValue();
            for ( Map.Entry<Long,Score> t : scores.tailMap(shotTime,false).entrySet() ) {
                long tTime = t.getKey();
                if ( (constEnemy.time - tTime) > REACT_PATTERN_TERM ) {
                    continue;
                }
                long absTime = shotTime+constEnemy.time - tTime+1;
                RobotPoint logEnemy = logEnemy(constEnemy.name, absTime);
                if ( logEnemy == null ) {
                    continue;
                }
                RobotPoint prospectEnemy = new RobotPoint(prevEnemy);
                prospectEnemy.setDelta(logEnemy.delta);
                prospectEnemy.prospectNext(deltaTime);
                double d = prospectEnemy.calcDistance(constEnemy);
                s.updateScore(Util.fieldFullDistance-d,REACT_PATTERN_SCORE_ESTIMATE_LIMIT);
                logger.prospect4("REACT(%02d):%2.2f => %2.2f",shotTime,d,s.score);
                //  logger.log("REACT(%02d):%2.2f => %2.2f = %2.2f",i,d,d,s.score);
                
//                logger.log("E: %d(%d) :%d(%d)",shotTime,absTime,t.getKey(),constEnemy.time-t.getKey());
            }
        }
    }
}
