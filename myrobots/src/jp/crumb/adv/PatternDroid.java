/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.ace;

import robocode.Droid;

/**
 *
 * @author crumb
 */
abstract public class AceDroid extends AceRobot<AceContext> implements Droid {


    public AceDroid() {
    }

    @Override
    protected void cbFirst() {
        super.cbFirst();
        GT_DIM = 2.0;
    }

    @Override
    protected void cbThinking() {
        if ( ! ctx.isGunMode(ctx.MODE_GUN_MANUAL )) {
            this.setGunMode(ctx.MODE_GUN_LOCKON);
        }
        if ( ctx.lockonTarget == null ) {
            super.cbThinking();
        }
    }
    @Override
    protected void cbFiring() {
        if ( ctx.isFireMode(ctx.MODE_FIRE_AUTO) ) {
            firing(3,0);
        }
    }
    
    
}
