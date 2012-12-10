/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.base;

import java.io.Serializable;
import jp.crumb.utils.DeltaMovingPoint;
import jp.crumb.utils.MovingPoint;

/**
 *
 * @author crumb
 */
public class BulletInfo implements Serializable{
    public String bulletName;
    public String owner;
    public DeltaMovingPoint target;
    public double distance;
    public MovingPoint src;
    public static String getKey(String name, long time){
        return name + time;
    }
    public BulletInfo(String owner,DeltaMovingPoint target,double distance,MovingPoint src) {
        this.bulletName = getKey(owner,src.time);
        this.owner = owner;
        this.target = target;
        this.distance = distance;
        this.src = src;
    }

    public BulletInfo(BulletInfo in ) {
        this.bulletName = in.bulletName;
        this.owner = in.owner;
        if ( target != null ) {
            this.target = new DeltaMovingPoint(in.target);
        }
        this.distance = in.distance;
        this.src = new MovingPoint(in.src);
    }
    
}
