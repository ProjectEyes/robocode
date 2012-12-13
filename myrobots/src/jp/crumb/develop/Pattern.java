/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import java.util.Map;
import jp.crumb.ace.AceContext;
import jp.crumb.ace.AceRobot;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;


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
    protected MoveType getAimType(String name) {
        return new MoveType(MoveType.TYPE_SIMPLE_PATTERN_CENTER);
    }


}
