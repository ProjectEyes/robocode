package jp.crumb.sample;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import robocode.Droid;



/**
 *
 * @author crumb
 */
public class JugemDroid2 extends Jugem implements Droid {

//    @Override
//    protected void cbThinking() {
//        this.setGunMode(ctx.MODE_GUN_LOCKON);
//        if ( ctx.lockonTarget == null ) {
//            super.cbThinking();
//        }
//    }

    @Override
    protected void cbFiring() {
        if ( ctx.isFireMode(ctx.MODE_FIRE_AUTO) ) {
            firing(3,1);
        }
    }
}
