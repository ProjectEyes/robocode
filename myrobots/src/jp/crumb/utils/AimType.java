/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import java.io.Serializable;

/**
 *
 * @author crumb
 */
public class AimType implements Comparable<AimType> , Serializable{
    
    public int aim;
    public int hit;
    public double hitrange;
    public double aimrange;
    public AimType(){
    }
    public AimType(AimType in){
        this.aim  = in.aim;
        this.hit  = in.hit;
        this.hitrange = in.hitrange;
        this.aimrange = in.aimrange;
    }
    public double getHitRate(){
        if ( aim == 0 ) {
            return 0;
        }
        return (double)hit/(double)aim;        
    }
    public double getHitRange(){
        return aimrange - hitrange;
    }
    public void updateHitRange(double range){
        double sumRange = hit * hitrange + range;
        hit++;
        hitrange = sumRange/hit;
    }
    public void updateAimRange(double range){
        double sumAim = aim * aimrange + range;
        aim++;
        aimrange = sumAim/(double)aim;
    }
    public void revartAimRange(double range){
        double sumAim = aim * aimrange - range;
        aim--;
        if ( aim == 0 ) {
            aimrange = 0;
        }else{
            aimrange = sumAim /(double)aim;                
        }
    }

    @Override
    public int compareTo(AimType o) {
        if ( aim == 0 ) {
            return -1;
        }
        if ( o.aim == 0 ) {
            return 1;
        }
        return (getHitRate()<o.getHitRate())?-1:1;
    }


}