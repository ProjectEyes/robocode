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
    protected BaseContext createContext(BaseContext in) {
        return defalutCreateContext(in);
    }

    @Override
    protected Point cbMoving() {
        setTurnGunRight(this.calcAbsGunTurn(270));
        setTurnRadarRight(this.calcAbsRadarTurn(270));
        return new Point(Util.runnableMaxX-2,Util.runnableMaxY-2);
    }
    boolean isMove = true;
    @Override
    protected void cbMoveComplete() {
        setTurnRight(this.calcAbsTurn(270));
    }
    boolean isFire = false;
    @Override
    protected void cbTurnComplete() {
        if (isMove ) {
            isFire = true;
        }
    }

    @Override
    protected void cbThinking() {
    }

    @Override
    protected void cbFiring() {
        if ( isFire) {
            fire(3.0,Util.fieldFullDistance,"UNKNOWN");
        }
    }
    
}
