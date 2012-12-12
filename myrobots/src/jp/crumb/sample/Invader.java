/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.sample;

import java.awt.Color;
import java.io.Serializable;
import jp.crumb.CrumbContext;
import jp.crumb.CrumbRobot;
import jp.crumb.LockonEvent;
import jp.crumb.utils.Point;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;
import robocode.MessageEvent;

/**
 *
 * @author crumb
 */
abstract public class Invader extends CrumbRobot<CrumbContext> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(1.0f,0,0,0.1f),new Color(1.0f,0.5f,0),new Color(1.0f,0,0.5f));
        this.setBulletColor(new Color(255,100,100));
    }

    static final int MODE_CUSTOM_GO        = 0;
    static final int MODE_CUSTOM_NOT_READY = 1;
    static final int MODE_CUSTOM_READY     = 2;
    TimedPoint firstPoint;
    @Override
    protected void cbFirst() {
        super.cbFirst();
        G_DIM = 0;
        G_WEIGHT = -10;
        RANGE_RADAR_LOCKON = Util.fieldFullDistance;
        MAX_HIT_TIME = (int)(Util.fieldFullDistance/Util.bultSpeed(0.1));
        setFireMode(ctx.MODE_FIRE_MANUAL);
        setCustomMode(MODE_CUSTOM_NOT_READY);
        firstPoint = new TimedPoint(new Point(Util.battleFieldWidth/2 ,Util.runnableMinY+Util.tankWidth+1),ctx.my.time);
        if ( isLeader ) {
            int i = 2;
            for(String mate: teammate ) {
                if ( mate != name ) {
                    if ( i > 0 ) {
                        sendMessage(mate,new InvaderEvent(1,new Point(Util.runnableMinX + Util.tankWidth*i ,Util.runnableMinY+Util.tankWidth+1)) );
                        i *= -1;
                    }else {
                        sendMessage(mate,new InvaderEvent(1,new Point(Util.runnableMaxX + Util.tankWidth*i,Util.runnableMinY+Util.tankWidth+1)) );
                        i *= -2;
                    }
                }
            }
        }
    }

    int readyCount = 0;
    @Override
    protected void cbExtMessage(MessageEvent e) {
        Serializable event = e.getMessage();
        if ( event instanceof InvaderEvent ) {
            InvaderEvent ev = (InvaderEvent)event;
            if ( ev.kind == 1 ) {
                firstPoint = new TimedPoint(ev.first,ctx.my.time);
            }else if ( ev.kind == 2) {
                readyCount++;
            }else if ( ev.kind == 3) {
                openFire();
            }
        }
        super.cbExtMessage(e);
    }
    @Override
    protected void cbMoving() {
        if ( ctx.isCustomMode(MODE_CUSTOM_NOT_READY) || ctx.isCustomMode(MODE_CUSTOM_READY) ) {
            if ( ctx.my.calcDistance(firstPoint) < 60 ) {
                setDestination(firstPoint);
                return;
            }
            ctx.G = firstPoint; // Move to point.
            Point dst = super.movingBase();
            setDestination(dst);
            return;
        }

        super.cbMoving();
        if ( ctx.destination != null ) {
            ctx.destination.y = firstPoint.y;
        }
    }

    @Override
    protected void cbThinking() {
        if ( readyCount == teammate.size()+1 || ctx.my.timeStamp == 100 ) {
            openFire();
            broadcastMessage(new InvaderEvent(3,null));
        }
        if ( ctx.isCustomMode(MODE_CUSTOM_NOT_READY) && ctx.my.calcDistance(firstPoint) < 1 ) {
            setCustomMode(MODE_CUSTOM_READY);
            G_WEIGHT = DEFAULT_G_WEIGHT;
            G_DIM    = DEFAULT_G_DIM;
            readyCount++;
        }
        if ( ctx.isCustomMode(MODE_CUSTOM_READY) || ctx.isCustomMode(MODE_CUSTOM_NOT_READY) ) {
            return;
        }
        super.cbThinking();
    }    

    
    private void openFire () {
        setFireMode(ctx.MODE_FIRE_AUTO);
        setCustomMode(MODE_CUSTOM_GO);
    }
}
