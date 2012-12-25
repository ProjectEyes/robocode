/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import jp.crumb.base.BaseContext;
import jp.crumb.base.BaseRobot;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;


/**
 *
 * @author crumb
 */
public class Move extends BaseRobot<BaseContext> {

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
        setDestination(new Point(100,100));
    }

    @Override
    protected void cbFiring() {

    }
    
}
