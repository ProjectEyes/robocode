/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import java.awt.Color;
import java.util.Map;
import jp.crumb.CrumbContext;
import jp.crumb.CrumbRobot;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import robocode.ScannedRobotEvent;


/**
 *
 * @author crumb
 */
public class FirePinpoint extends CrumbRobot<CrumbContext> {
    @Override
    protected void cbThinking() {
        for ( Map.Entry<String,Enemy> e : ctx.nextEnemyMap.entrySet() ) {
            if ( ! isTeammate(e.getValue().name) ) {
                ctx.setLockonTarget(e.getValue().name);
            }
        }
        setRadarMode(ctx.MODE_RADAR_LOCKON);
        setGunMode(ctx.MODE_GUN_LOCKON);
        setMoveMode(ctx.MODE_MOVE_LOCKON1);
        setFireMode(ctx.MODE_FIRE_AUTO);
    }

    @Override
    protected void scannedRobot(Enemy r) {

        super.scannedRobot(r);
    }

//    @Override
//    protected void cbFiring() {
//        fire(0.5,0,"");
//    }

    @Override
    protected Enemy calcAbsRobot(ScannedRobotEvent e) {
        return new Enemy(ctx.my, e , Enemy.AIM_TYPE_PINPOINT);
    }

}
