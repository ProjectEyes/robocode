/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import java.util.HashMap;
import java.util.Map;
import jp.crumb.ace.AceContext;
import jp.crumb.ace.AceRobot;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.Score;
import jp.crumb.utils.Util;


/**
 *
 * @author crumb
 */
public class Pattern extends AceRobot<AceContext> {
    @Override
    protected void cbThinking() {
        for ( Map.Entry<String,Enemy> e : ctx.nextEnemyMap.entrySet() ) {
            if ( ! isTeammate(e.getValue().name) && e.getValue().timeStamp != 0 ) {
                ctx.setLockonTarget(e.getValue().name);
            }
        }
        setRadarMode(ctx.MODE_RADAR_LOCKON);
        setGunMode(ctx.MODE_GUN_LOCKON);
        setMoveMode(ctx.MODE_MOVE_LOCKON1);
        setFireMode(ctx.MODE_FIRE_AUTO);
    }

    @Override
    protected boolean prospectNextRobot(RobotPoint robot, MoveType moveType, long term) {
        if ( moveType.isTypePinPoint() ) {
            return true;
        }else if ( moveType.isTypeInertia() ) {
            return robot.inertia(term);
        }else if ( moveType.isTypeAccurate() ) {
            return robot.prospectNext(term);
        }else if ( moveType.isTypeSimplePattern()) {
            Map<Long,Score> scores = simplePatternScoreMap.get(robot.name);
            if ( scores == null ) {
                return robot.prospectNext(term);
            }
            Score s = Score.getByScore(scores.values());
            for ( long l = 0; l < term; l++) {
                long absLogTime = l+robot.time-Integer.valueOf(s.name);
                RobotPoint logEnemy = logEnemy(robot.name,absLogTime);
                if ( logEnemy != null ) {
                    logger.log("%d: %s(%d) : %s",l,s.name,absLogTime,logEnemy.delta);
                    robot.setDelta(logEnemy.delta); 
                }
                robot.prospectNext(1);
            }
            return true;
        }else {
            throw new UnsupportedOperationException("Unknown MoveType : " + moveType.type);
        }
    }
    static final long SIMPLE_PATTERN_TERM=100;
    Map<String,Map<Long,Score>> simplePatternScoreMap = new HashMap<>();
    void evalSimplePattern(Enemy prevEnemy,Enemy constEnemy){
        Map<Long,Score> scores = simplePatternScoreMap.get(constEnemy.name);
        long deltaTime = constEnemy.time - prevEnemy.time;
        for ( long i = 1 ; i <= SIMPLE_PATTERN_TERM; i++) {
            long absTime = constEnemy.time - i;
            RobotPoint logEnemy = logEnemy(constEnemy.name, absTime);
            if ( logEnemy == null ) {
                continue;
            }
            RobotPoint prospectEnemy = new RobotPoint(prevEnemy);
            prospectEnemy.setDelta(logEnemy.delta);
            prospectEnemy.prospectNext(deltaTime);
            double d = prospectEnemy.calcDistance(constEnemy);
            Score s = scores.get(i);
            s.updateScore(Util.fieldFullDistance-d,10);
//            logger.log("** SIMPLE(%02d):%2.2f => %2.2f = %2.2f",i,d,d,s.score);
        }
    }
    
    @Override
    protected Enemy cbScannedRobot(Enemy enemy) {
        Enemy prevEnemy = enemyMap.get(enemy.name);
        Enemy constEnemy = super.cbScannedRobot(enemy);
        if ( constEnemy == null ) {
            return null;
        }
        if ( ! simplePatternScoreMap.containsKey(enemy.name)) {
            Map<Long,Score> scores = new HashMap<>();
            for ( long i = 1 ; i <= SIMPLE_PATTERN_TERM; i++) {
                scores.put(i,new Score(String.valueOf(i)));
            }
            simplePatternScoreMap.put(enemy.name,scores);
        }
        if ( prevEnemy != null ) {        
            evalSimplePattern(prevEnemy, constEnemy);
        }
        
        
        return constEnemy;
    }

    @Override
    protected MoveType getAimType(String name) {
        return new MoveType(MoveType.TYPE_SIMPLE_PATTERN_CENTER);
    }


}
