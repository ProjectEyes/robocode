package boss;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Color;
import jp.crumb.CrumbContext;
import jp.crumb.CrumbRobot;
import robocode.Droid;



/**
 *
 * @author crumb
 */
public abstract class Under extends CrumbRobot<CrumbContext> implements Droid{
    @Override
    public void run() {
        super.run();
        setColors(new Color(70,20,20), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(0,255,0));
    }

    @Override
    protected void cbFirst() {
        super.cbFirst();
//        GT_DIM = 0.4;
//        GT_WEIGHT = 400;
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
            firing(3,1);
        }
    }
  
}
