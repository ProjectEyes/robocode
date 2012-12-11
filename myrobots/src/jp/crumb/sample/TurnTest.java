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

    int t = 1;
    int i = 5;
    @Override
    protected void cbMoving() {
        doAhead(-100);
        doTurnRight(100);
        if( i <= 1 || i >= 8 ) {
            t*=-1;
        }
        if ( t > 0 ) {
            i++;
        }else {
            i--;
        }
        setMaxVelocity(i);
        setDestination(null);
    }

    @Override
    protected void cbFiring() {

    }
    
}
