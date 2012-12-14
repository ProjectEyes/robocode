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
        if ( ctx.isFireMode(ctx.MODE_FIRE_AUTO) ) {
            firing(3,1);
        }
    }

    public InvaderDroid() {
        GT_DIM = 1.5;
    }

    @Override
    protected void cbThinking() {
        if ( ctx.isCustomMode(MODE_CUSTOM_NOT_READY) && ctx.my.calcDistance(firstPoint) < 1 ) {
            setCustomMode(MODE_CUSTOM_READY);
            G_WEIGHT = DEFAULT_G_WEIGHT;
            G_DIM    = DEFAULT_G_DIM;
            sendMessage(leader,new InvaderEvent(2,null));
        }
        if ( ctx.isCustomMode(MODE_CUSTOM_READY) || ctx.isCustomMode(MODE_CUSTOM_NOT_READY) ) {
            return;
        }
        this.setGunMode(ctx.MODE_GUN_LOCKON);
    }    
}
