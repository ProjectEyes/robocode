package jp.crumb.sample;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Color;
import jp.crumb.adv.AdbCrumbContext;
import jp.crumb.adv.AdvCrumbRobot;



/**
 *
 * @author crumb
 */
public class AceSlave extends Ace {

    @Override
    protected void cbThinking() {
        this.setGunMode(ctx.MODE_GUN_LOCKON);
        if ( ctx.lockonTarget == null ) {
            super.cbThinking();
        }
    }
  
}
