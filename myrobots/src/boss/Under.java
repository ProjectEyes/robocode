package boss;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Color;
import java.io.Serializable;
import jp.crumb.adv.AdbCrumbContext;
import jp.crumb.adv.AdvCrumbRobot;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;
import robocode.Droid;
import robocode.MessageEvent;



/**
 *
 * @author crumb
 */
public abstract class Under extends AdvCrumbRobot<AdbCrumbContext> implements Droid {
    @Override
    public void run() {
        super.run();
        setColors(new Color(70,20,20), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(0,255,0));
    }

    @Override
    protected void cbFirst() {
        super.cbFirst();
        setFireMode(ctx.MODE_FIRE_MANUAL);
        int round = getRoundNum();
        if ( round%3 == 2 ) {
            setFireMode(ctx.MODE_FIRE_AUTO);
        }
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
    protected Point movingBase() {
        Point dst = super.movingBase();
        if ( ctx.isFireMode(ctx.MODE_FIRE_AUTO) ) {
            return dst;
        }
        Enemy boss = ctx.nextMateMap.get(leader);
        if ( boss != null ) {
            dst.diff(Util.getGrabity(ctx.my,boss, -5,0));
        }
        return dst;
    }

    @Override
    protected void cbExtMessage(MessageEvent e) {
        Serializable event = e.getMessage();
        if ( event instanceof BossEvent ) {
            BossEvent ev = (BossEvent)event;
            setFireMode(ctx.MODE_FIRE_AUTO);
            GT_DIM = 0.4;
            GT_WEIGHT = 400;
        }else{
            super.cbExtMessage(e);
        }
    }

    @Override
    protected void cbFiring() {
        if ( ctx.isFireMode(ctx.MODE_FIRE_AUTO) ) {
            firing(3,1);
        }
    }
}
