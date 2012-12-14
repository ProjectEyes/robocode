/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.sample;

import jp.crumb.CrumbContext;
import jp.crumb.CrumbRobot;
import robocode.Droid;

/**
 *
 * @author crumb
 */
public class BasicDroid extends CrumbRobot<CrumbContext> implements Droid {


    public BasicDroid() {
    }

    @Override
    protected void cbFirst() {
        super.cbFirst();
        GT_DIM = 2.2;
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
