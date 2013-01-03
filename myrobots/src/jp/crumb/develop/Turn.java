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
public class Turn extends BaseRobot<BaseContext> {

    @Override
    public void run() {
        super.run();
    }

    @Override
    protected void cbFirst() {
        super.cbFirst();
        range = 250 + Math.random()*150;
    }
    
    @Override
    protected BaseContext createContext(BaseContext in) {
        return defalutCreateContext(in);
    }
    boolean start = true;
    double range = 200;
    @Override
    protected void cbMoving() {
        if ( start ) {
            setDestination(new Point(200,200));
            if ( ctx.my.calcDistance(new Point(200,200)) < 1 ) {
                start = false;
            }
        }
        if ( ctx.my.calcDistance(new Point(200,200)) < 1 ) {
            setDestination(new Point(200,range));
        }else if ( ctx.my.calcDistance(new Point(200,range)) < 1 ) {
            setDestination(new Point(range,range));
        }else if ( ctx.my.calcDistance(new Point(range,range)) < 1 ) {
            setDestination(new Point(range,200));
        }else if ( ctx.my.calcDistance(new Point(range,200)) < 1 ) {
            setDestination(new Point(200,200));
        }
    }

    @Override
    protected void cbFiring() {
        doFire(0.001,0,null,0);
    }
    
}
