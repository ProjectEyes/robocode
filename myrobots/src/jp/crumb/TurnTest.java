/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;
import jp.crumb.utils.DeltaMovingPoint;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;

/**
 *
 * @author crumb
 */
public class TurnTest extends CrumbRobo {

    @Override
    public void run() {
        super.run();
    }

    @Override
    protected void cbMoving() {
        if ( lockOnTarget != null ) {
            lockOnTarget = nextEnemyMap.get(lockOnTarget.name);
        }
        setTurnRight(10000);
        setTurnGunRight(10000);
    }
    
    Point lockOnPoint;
    Enemy lockOnTarget;
    
  
    static {
        Logger.LOGLV = Logger.LOGLV_GUN;
    }
    
    void radarLockOn(Enemy lockOnTarget) {
        if (lockOnTarget == null) {
            this.setTurnRadarRight(10000);
            return;
        }
        double radarTurn = calcAbsRadarTurn(lockOnTarget.bearing);
        this.setTurnRadarRight(radarTurn);
    }
    void lockOn(Enemy lockOnTarget) {
        if (lockOnTarget == null) {
            return;
        }
        double gunTurn = calcAbsGunTurn(lockOnTarget.bearing);
        this.setTurnGunRight(gunTurn);
    }
    
    
    @Override
    protected void cbThinking() {
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( lockOnTarget == null ){
                lockOnTarget = r;
                continue;
            }
            if ( r.energy< 10.0 ) {
                lockOnTarget = r;
                break;
            }
            if ( lockOnTarget.distance > r.distance && (my.time - r.time) < SCAN_STALE ) {
                lockOnTarget = r;
            }
        }
        lockOn(lockOnTarget);
        radarLockOn(lockOnTarget);
    }    

    @Override
    protected void paint(Graphics2D g) {
        super.paint(g);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0, 0.7f, 0, PAINT_OPACITY));
        Color stringColor = new Color(0,1.0f,0,PAINT_OPACITY);
        if ( Math.abs(my.velocity) < 7.5 ) {
            stringColor = new Color(
                    (float)stringColor.getRed()/255.0f,
                    (float)stringColor.getGreen()/255.0f,
                    0.7f,
                    PAINT_OPACITY);
        } 
        if ( gunHeat == 0.0 ) {
            stringColor = new Color(
                    0.7f,
                    (float)stringColor.getGreen()/255.0f,
                    (float)stringColor.getBlue()/255.0f,
                    PAINT_OPACITY);
        } 
        g.setColor(stringColor);
        if ( lockOnTarget != null ) {
            g.drawString(String.format("targ: %s", lockOnTarget.name), (int) my.x - 20, (int) my.y- 45);
        }
        
        
        g.setStroke(new BasicStroke(4.0f));
        if ( lockOnPoint != null ) {
            g.setColor(new Color(1.0f, 1.0f, 0,PAINT_OPACITY));
            drawRound(g, lockOnPoint.x, lockOnPoint.y, 5);
        }
    }
}
