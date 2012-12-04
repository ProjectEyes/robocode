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
        return new Point(Util.runnableMaxX,Util.runnableMaxY);
    }

    @Override
    protected void cbThinking() {
        this.setTurnGunRight(this.calcAbsGunTurn(270));
        this.setTurnRadarRight(this.calcAbsRadarTurn(270));
    }
    
}
