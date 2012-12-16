package boss;

import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */





/**
 *
 * @author crumb
 */
public class カンダタ extends Boss {
    @Override
    protected void cbFirst() {
        super.cbFirst();
        setFireMode(ctx.MODE_FIRE_MANUAL);
    }
    String currentMate = null;
    @Override
    protected void cbThinking() {
        super.cbThinking();
        int round = getRoundNum();
        if ( teammate.size() == 0 || round%3 == 2 ) {
            setFireMode(ctx.MODE_FIRE_AUTO);
            return;
        }
        if ( ctx.my.time > 100 ) {
            if ( round%3 == 1 ) {
                broadcastMessage(new BossEvent());
            }
            for( String mname : teammate ) {
                if ( mname.equals(name) ) {
                    continue;
                }
                Enemy mate = ctx.nextEnemyMap.get(mname);
                if ( mate == null ) {
                    continue;
                }
                if ( mate.name.equals(currentMate ) ) {
                    return;
                }
                sendMessage(mate.name,new BossEvent());
                return;
            }
            setFireMode(ctx.MODE_FIRE_AUTO);
        }
    }
}
