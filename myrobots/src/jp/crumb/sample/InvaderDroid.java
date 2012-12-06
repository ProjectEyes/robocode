/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.sample;

import robocode.Droid;

/**
 *
 * @author crumb
 */
abstract public class InvaderDroid extends Invader implements Droid{
    @Override
    protected void cbFiring() {
        firing(3,0);
    }

    public InvaderDroid() {
        GT_DIM = 1.5;
    }

    @Override
    protected void cbThinking() {
        if ( isMode(MODE_NOT_READY) && ctx.my.calcDistance(firstPoint) < 1 ) {
            setMode(MODE_CLOSE_FIRE | MODE_READY);
            G_WEIGHT = DEFAULT_G_WEIGHT;
            G_DIM    = DEFAULT_G_DIM;
            LOCKON_APPROACH = 6;
            sendMessage(leader,new InvaderEvent(2,null));
        }
        if ( isMode(MODE_READY) || isMode(MODE_NOT_READY) ) {
            return;
        }
        this.setMode(MODE_GUN_LOCKON);
        if ( ctx.lockonTarget != null ) {
            lockOn(ctx.lockonTarget);
            radarLockOn(ctx.lockonTarget);
        }
    }    
}
