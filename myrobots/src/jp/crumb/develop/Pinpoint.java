/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.crumb.CrumbContext;
import jp.crumb.CrumbRobot;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.RobotPoint;
import robocode.ScannedRobotEvent;


/**
 *
 * @author crumb
 */
public class Pinpoint extends CrumbRobot<CrumbContext> {
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
    protected MoveType getBestAimType(RobotPoint name) {
        return new MoveType(MoveType.TYPE_PINPOINT);
    }

}
