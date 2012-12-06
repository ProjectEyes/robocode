/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.sample;

import jp.crumb.base.BaseContext;
import jp.crumb.base.BaseRobo;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;


/**
 *
 * @author crumb
 */
public class TurnTest extends BaseRobo<BaseContext> {

    @Override
    public void run() {
        super.run();
    }

    @Override
    protected void cbFirst() {
        super.cbFirst();
    }
    
    @Override
    protected BaseContext createContext(BaseContext in) {
        return defalutCreateContext(in);
    }

    @Override
    protected void cbMoving() {
        if ( ctx.my.calcDistance(new Point(Util.runnableMaxX-1,Util.runnableMaxY-1)) < 1.0 ) {
            setTurnGunRight(ctx.calcAbsGunTurn(270));
            setTurnRadarRight(ctx.calcAbsRadarTurn(270));
        }
        setDestination(new Point(Util.runnableMaxX-1,Util.runnableMaxY-1));
    }

    @Override
    protected void cbFiring() {
        fire(3.0,Util.fieldFullDistance,"UNKNOWN");
    }
    
}
