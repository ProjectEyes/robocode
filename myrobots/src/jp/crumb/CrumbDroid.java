/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import robocode.Droid;

/**
 *
 * @author crumb
 */
public class CrumbDroid extends CrumbRobot implements Droid {

    @Override
    protected void cbFiring() {
        firing(3,0);
    }

    public CrumbDroid() {
        GT_DIM = 1.5;
    }

    @Override
    protected void cbThinking() {
        this.setMode(MODE_GUN_LOCKON);
        if ( ctx.lockonTarget != null ) {
            lockOn(ctx.lockonTarget);
            radarLockOn(ctx.lockonTarget);
        }
    }
    
    
}
