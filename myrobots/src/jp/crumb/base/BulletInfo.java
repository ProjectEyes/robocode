/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.base;

import java.io.Serializable;
import jp.crumb.utils.MovingPoint;

/**
 *
 * @author crumb
 */
public class BulletInfo implements Serializable{
    public String bulletName;
    public String owner;
    public String targetName;
    public double distance;
    public MovingPoint src;
    public int type;
    public static String getKey(String name, long time){
        return name + time;
    }
    public BulletInfo(String owner,String targetName,double distance,MovingPoint src,int type) {
        this.bulletName = getKey(owner,src.timeStamp);
        this.owner = owner;
        this.targetName = targetName;
        this.distance = distance;
        this.src = src;
        this.type = type;
    }

    public BulletInfo(BulletInfo in ) {
        this.bulletName = in.bulletName;
        this.owner = in.owner;
        this.targetName = in.targetName;
        this.distance = in.distance;
        this.src = new MovingPoint(in.src);
        this.type = in.type;
    }
    
}
