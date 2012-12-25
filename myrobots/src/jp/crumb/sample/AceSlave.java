package jp.crumb.sample;

import robocode.Droid;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



/**
 *
 * @author crumb
 */
public class AceSlave extends Ace implements Droid{

    @Override
    protected void cbFiring() {
        if ( ctx.isFireMode(ctx.MODE_FIRE_AUTO) ) {
            firing(3,1);
        }
    }
  
}
