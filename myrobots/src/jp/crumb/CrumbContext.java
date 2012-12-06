/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import jp.crumb.base.BaseContext;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Point;
import jp.crumb.utils.TimedPoint;

/**
 *
 * @author crumb
 */
public class CrumbContext extends BaseContext {
    public int mode = CrumbRobot.MODE_NORMAL;
    public TimedPoint G;
    public TimedPoint GT;
    public Point lockOnPoint; // for view
    public String lockonTarget;
//  public Enemy lockOnTarget;
    public CrumbContext() {
    }

    public CrumbContext(CrumbContext in){
        super(in);
        if ( in.G != null ) {
            this.G = new TimedPoint(in.G);
        }
        if ( in.GT != null ) {
            this.GT = new TimedPoint(in.GT);
        }
        if ( in.lockOnPoint != null ) {
            this.lockOnPoint = new Point(in.lockOnPoint);
        }
        this.lockonTarget = in.lockonTarget;
//        if ( in.lockOnTarget != null ) {
//            this.lockOnTarget = new Enemy(in.lockOnTarget);
//        }
    }
    public void setLockonTarget(String lockonTarget) {
        this.lockonTarget = lockonTarget;
    }
    
}