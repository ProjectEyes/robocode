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

    public Invader() {
        G_DIM = 0;
        G_WEIGHT = -10;
    }
    static final int MODE_NOT_READY = 0x10000000;
    static final int MODE_READY     = 0x20000000;
    TimedPoint firstPoint;
    @Override
    protected void cbFirst() {
        RANGE_RADAR_LOCKON = Util.fieldFullDistance;
        MAX_HIT_TIME = (int)(Util.fieldFullDistance/Util.bultSpeed(0.1));
//        ctx.mode = MODE_CLOSE_FIRE | MODE_NOT_READY | MODE_RADAR_SEARCH;
        ctx.mode = MODE_NORMAL;
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
        super.cbFirst();
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
                ctx.mode = MODE_NORMAL;
            }
        }
        super.cbExtMessage(e);
    }
    
    @Override
    protected Point cbMoving() {
        if ( isMode(MODE_NOT_READY) || isMode(MODE_READY) ) {
            if ( ctx.my.calcDistance(firstPoint) < 40 ) {
                return firstPoint;
            }
            ctx.G = firstPoint;
            return super.movingBase();
        }

        Point dst = super.cbMoving();
        if ( dst != null ) {
            dst.y = firstPoint.y;
        }
        return dst;
    }

    @Override
    protected void cbThinking() {
        if ( readyCount == teammate.size()+1 || ctx.my.time == 100 ) {
            ctx.mode = MODE_NORMAL;
            broadcastMessage(new InvaderEvent(3,null));
        }
        if ( isMode(MODE_NOT_READY) && ctx.my.calcDistance(firstPoint) < 1 ) {
            setMode(MODE_CLOSE_FIRE | MODE_READY | MODE_RADAR_SEARCH);
            G_WEIGHT = DEFAULT_G_WEIGHT;
            G_DIM    = DEFAULT_G_DIM;
            LOCKON_APPROACH = 6;
            readyCount++;
        }
        if ( isMode(MODE_READY) || isMode(MODE_NOT_READY) ) {
            return;
        }
        super.cbThinking();
    }    

    @Override
    public void run() {
        setColors(new Color(1.0f,0,0,0.1f),new Color(1.0f,0.5f,0),new Color(1.0f,0,0.5f));
        this.setBulletColor(new Color(255,100,100));
        super.run();
    }
    
}
