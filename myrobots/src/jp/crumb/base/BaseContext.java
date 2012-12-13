/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.base;

import java.io.Serializable;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.Util;

/**
 *
 * @author crumb
 */
public class BaseContext implements Serializable{
    public RobotPoint my = new RobotPoint();
    public RobotPoint nextMy = new RobotPoint();
    
    public double curTurnRemaining;
    public double curTurnRemainingRadians;       
    public double curGunTurnRemaining;        
    public double curGunTurnRemainingRadians;            
    public double curRadarTurnRemaining;            
    public double curRadarTurnRemainingRadians;
    
    public double curGunHeadingRadians;
    public double curGunHeading;
    public double curRadarHeadingRadians;
    public double curRadarHeading;
    public double curDistanceRemaining;
    public double gunHeat;
    public int others;
    public int enemies;
    
    
    public double prevRadarHeadingRadians;
    // For auto move
    public Point destination;


    public BaseContext() {
    }

    public BaseContext(BaseContext in){
        this.curTurnRemaining = in.curTurnRemaining;
        this.curTurnRemainingRadians = in.curTurnRemainingRadians;
        this.curGunTurnRemaining = in.curGunTurnRemaining;
        this.curGunTurnRemainingRadians = in.curGunTurnRemainingRadians;
        this.curRadarTurnRemaining = in.curRadarTurnRemaining;
        this.curRadarTurnRemainingRadians = in.curRadarTurnRemainingRadians;
        this.curGunHeadingRadians = in.curGunHeadingRadians;
        this.curGunHeading = in.curGunHeading;
        this.curRadarHeadingRadians = in.curRadarHeadingRadians;
        this.curRadarHeading = in.curRadarHeading;
        this.curDistanceRemaining = in.curDistanceRemaining;
        this.gunHeat = in.gunHeat;
        this.others = in.others;
        this.enemies = in.enemies;
        this.prevRadarHeadingRadians = in.prevRadarHeadingRadians;
        if ( in.destination != null ) {
            this.destination = new Point(in.destination);
        }
        this.my = new RobotPoint(in.my);
        this.nextMy = new RobotPoint(in.nextMy);
        

    }    
   
    public double calcAbsGunTurnDiff(double diffDegree){
        long time = 0;
        double turnRemaining = curTurnRemaining;
        double sumGunTurn = 0.0;
        
        for (int i = 0 ;i < 100;i++){
            double nextTurn     = 0;
            double nextGunTurn  = 0;
            time++;
            if ( turnRemaining != 0.0 ) {
                nextTurn = Util.turnSpeed(my.velocity)*(Math.abs(turnRemaining)/turnRemaining);
                if ( Math.abs(nextTurn) > Math.abs(turnRemaining) ) {
                    nextTurn = turnRemaining;
                }
                turnRemaining -= nextTurn;
            }
//System.out.println(diffDegree + " t:" + nextTurn + " g:" + nextGunTurn);            
            diffDegree -= nextTurn;
            if ( diffDegree != 0.0 ) {
                nextGunTurn = Util.gunTurnSpeed()*(Math.abs(diffDegree)/diffDegree);
                if ( Math.abs(nextGunTurn) >  Math.abs(diffDegree) ) {
                    nextGunTurn = diffDegree;
                }
                diffDegree -= nextGunTurn;
                sumGunTurn += nextGunTurn;
            }
            if ( diffDegree == 0.0 ) {
                break;
            }
        }
        return sumGunTurn;
    }
    public double calcAbsRadarTurnDiff(double diffDegree){
        long time = 0;
        double turnRemaining = curTurnRemaining;
        double gunTurnRemaining = curGunTurnRemaining;

        double sumRadarTurn = 0.0;
        
        for (int i = 0 ;i < 100;i++){
            double nextTurn     = 0;
            double nextGunTurn  = 0;
            double nextRadarTurn= 0;
            time++;
            if ( turnRemaining != 0.0 ) {
                nextTurn = Util.turnSpeed(my.velocity)*(Math.abs(turnRemaining)/turnRemaining);
                if ( Math.abs(nextTurn) > Math.abs(turnRemaining) ) {
                    nextTurn = turnRemaining;
                }
                turnRemaining -= nextTurn;
            }

            if ( gunTurnRemaining != 0.0 ) {
                nextGunTurn = Util.gunTurnSpeed()*(Math.abs(gunTurnRemaining)/gunTurnRemaining);
                if ( Math.abs(nextGunTurn) >  Math.abs(gunTurnRemaining) ) {
                    nextGunTurn = gunTurnRemaining;
                }
                gunTurnRemaining -= nextGunTurn;
                nextGunTurn += nextTurn;
            }
//System.out.println(diffDegree + " t:" + nextTurn + " g:" + nextGunTurn);            
            diffDegree -= nextGunTurn;
            if ( diffDegree != 0.0 ) {
                nextRadarTurn = Util.radarTurnSpeed()*(Math.abs(diffDegree)/diffDegree);
                if ( Math.abs(nextRadarTurn) >  Math.abs(diffDegree) ) {
                    nextRadarTurn = diffDegree;
                }
                diffDegree -= nextRadarTurn;
                sumRadarTurn += nextRadarTurn;
            }
            if ( diffDegree == 0.0 ) {
                break;
            }
        }
        return sumRadarTurn;
    }

    public Pair<Double,Integer> calcAbsTurn(double dstDegree) {
        double aheadTurnDegree = Util.calcTurn(my.heading,dstDegree);
        double backTurnDegree  = Util.calcTurn(my.heading,dstDegree-180);
        if ( Math.abs(aheadTurnDegree) < Math.abs(backTurnDegree)) { // ahead
            return new Pair<>(aheadTurnDegree,1);
        }else { // back
            return new Pair<>(backTurnDegree,-1);
        }
    }
    
    public double calcAbsGunTurn(double absDegree) {
        double diffDegree1 = (absDegree - curGunHeading) % 360;
        if (diffDegree1 > 180) {
            diffDegree1 = diffDegree1 - 360;
        } else if (diffDegree1 < -180) {
            diffDegree1 = diffDegree1 + 360;
        }
        double diffDegree2;
        if ( diffDegree1 < 0 ) {
            diffDegree2 = 360+diffDegree1;
        }else{
            diffDegree2 = diffDegree1-360;
        }
        double realDegree1 = calcAbsGunTurnDiff(diffDegree1);
        double realDegree2 = calcAbsGunTurnDiff(diffDegree2);
        // logger.gun4("CALC: %2.2f => %2.2f : 1 = %2.2f(%2.2f) : 2 = %2.2f(%2.2f)",ctx.curGunHeading,absDegree,diffDegree1,realDegree1,diffDegree2,realDegree2);
        if ( Math.abs(realDegree2) < Math.abs(realDegree1) ) {
            return realDegree2;
        }
        return realDegree1;
    }
    
    public double calcAbsRadarTurn(double absDegree) {
        double diffDegree1 = (absDegree - curRadarHeading) % 360;
        if (diffDegree1 > 180) {
            diffDegree1 = diffDegree1 - 360;
        } else if (diffDegree1 < -180) {
            diffDegree1 = diffDegree1 + 360;
        }
        double diffDegree2;
        if ( diffDegree1 < 0 ) {
            diffDegree2 = 360+diffDegree1;
        }else{
            diffDegree2 = diffDegree1-360;
        }
        double realDegree1 = calcAbsRadarTurnDiff(diffDegree1);
        double realDegree2 = calcAbsRadarTurnDiff(diffDegree2);
        // logger.radar4("CALC: %2.2f => %2.2f : 1 = %2.2f(%2.2f) : 2 = %2.2f(%2.2f)",ctx.curRadarHeading,absDegree,diffDegree1,realDegree1,diffDegree2,realDegree2);
        if ( Math.abs(realDegree2) < Math.abs(realDegree1) ) {
            return realDegree2;
        }
        return realDegree1;
    }    
    

    
}
