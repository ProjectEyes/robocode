/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import java.awt.Color;
import java.util.Map;
import jp.crumb.CrumbContext;
import jp.crumb.CrumbRobot;
import jp.crumb.adv.AdbCrumbContext;
import jp.crumb.adv.AdvCrumbRobot;
import jp.crumb.utils.Enemy;


/**
 *
 * @author crumb
 */
public class RadarLockOn extends CrumbRobot<CrumbContext> {
    @Override
    protected void cbThinking() {
        setFireMode(ctx.MODE_FIRE_MANUAL);
        for ( Map.Entry<String,Enemy> e : ctx.nextEnemyMap.entrySet() ) {
            if ( ! isTeammate(e.getValue().name) ) {
                ctx.setLockonTarget(e.getValue().name);
            }
        }
        setRadarMode(ctx.MODE_RADAR_LOCKON);
        setMoveMode(ctx.MODE_MOVE_LOCKON1);
    }



}
