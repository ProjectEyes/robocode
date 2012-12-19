/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import jp.crumb.base.BaseContext;
import jp.crumb.base.BaseRobo;
import robocode.AdvancedRobot;
import robocode.Robot;

/**
 *
 * @author crumb
 */
public class Radar1 extends AdvancedRobot {


    @Override
    public void run() {
        setAdjustRadarForGunTurn(false);
        setAdjustGunForRobotTurn(false);
        setAdjustRadarForRobotTurn(false);
//        turnRadarRight(-getRadarHeading());
        double centerX = getBattleFieldWidth()/2;
        double centerY = getBattleFieldHeight()/2;
        double x = getX();
        double y = getY();
        double radians = Math.atan( (centerX - x) / (centerY - y ));
        turnRight(Math.toDegrees(radians) - getHeading() - 180);
        ahead(Math.sqrt(Math.pow(centerX-x,2) + Math.pow(centerY-y,2) ));
        turnRight(-getHeading());
        setTurnGunRight(60);
        turnRadarRight(135);
        setTurnGunRight(-120);
        turnRadarRight(-270);
        setTurnGunRight(60);
        turnRadarRight(135);


    }



}
