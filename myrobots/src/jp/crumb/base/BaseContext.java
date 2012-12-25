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
    
    public double curTurnRemainingRadians;       
    public double curGunTurnRemainingRadians;            
    public double curRadarTurnRemainingRadians;
    
    public double curGunHeadingRadians;
    public double curRadarHeadingRadians;
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
        this.curTurnRemainingRadians = in.curTurnRemainingRadians;
        this.curGunTurnRemainingRadians = in.curGunTurnRemainingRadians;
        this.curRadarTurnRemainingRadians = in.curRadarTurnRemainingRadians;
        this.curGunHeadingRadians = in.curGunHeadingRadians;
        this.curRadarHeadingRadians = in.curRadarHeadingRadians;
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
   
    public double calcAbsGunTurnDiffRadians(double diffRadians){
        long time = 0;
        double turnRemainingRadians = curTurnRemainingRadians;
        double sumGunTurnRadians = 0.0;
        
        for (int i = 0 ;i < 100;i++){
            double nextTurnRadians     = 0;
            double nextGunTurnRadians  = 0;
            time++;
            if ( turnRemainingRadians != 0.0 ) {
                nextTurnRadians = Util.turnSpeedRadians(my.velocity)*(Math.abs(turnRemainingRadians)/turnRemainingRadians);
                if ( Math.abs(nextTurnRadians) > Math.abs(turnRemainingRadians) ) {
                    nextTurnRadians = turnRemainingRadians;
                }
                turnRemainingRadians -= nextTurnRadians;
            }
//System.out.println(diffDegree + " t:" + nextTurn + " g:" + nextGunTurn);            
            diffRadians -= nextTurnRadians;
            if ( diffRadians != 0.0 ) {
                nextGunTurnRadians = Util.gunTurnSpeedRadians()*(Math.abs(diffRadians)/diffRadians);
                if ( Math.abs(nextGunTurnRadians) >  Math.abs(diffRadians) ) {
                    nextGunTurnRadians = diffRadians;
                }
                diffRadians -= nextGunTurnRadians;
                sumGunTurnRadians += nextGunTurnRadians;
            }
            if ( diffRadians == 0.0 ) {
                break;
            }
        }
        return sumGunTurnRadians;
    }
    public double calcAbsRadarTurnDiffRadians(double diffRadians){
        long time = 0;
        double turnRemainingRadians = curTurnRemainingRadians;
        double gunTurnRemainingRadians = curGunTurnRemainingRadians;

        double sumRadarTurnRadians = 0.0;
        
        for (int i = 0 ;i < 100;i++){
            double nextTurnRadians     = 0;
            double nextGunTurnRadians  = 0;
            double nextRadarTurnRadians= 0;
            time++;
            if ( turnRemainingRadians != 0.0 ) {
                nextTurnRadians = Util.turnSpeedRadians(my.velocity)*(Math.abs(turnRemainingRadians)/turnRemainingRadians);
                if ( Math.abs(nextTurnRadians) > Math.abs(turnRemainingRadians) ) {
                    nextTurnRadians = turnRemainingRadians;
                }
                turnRemainingRadians -= nextTurnRadians;
            }

            if ( gunTurnRemainingRadians != 0.0 ) {
                nextGunTurnRadians = Util.gunTurnSpeedRadians()*(Math.abs(gunTurnRemainingRadians)/gunTurnRemainingRadians);
                if ( Math.abs(nextGunTurnRadians) >  Math.abs(gunTurnRemainingRadians) ) {
                    nextGunTurnRadians = gunTurnRemainingRadians;
                }
                gunTurnRemainingRadians -= nextGunTurnRadians;
                nextGunTurnRadians += nextTurnRadians;
            }
//System.out.println(diffDegree + " t:" + nextTurn + " g:" + nextGunTurn);            
            diffRadians -= nextGunTurnRadians;
            if ( diffRadians != 0.0 ) {
                nextRadarTurnRadians = Util.radarTurnSpeedRadians()*(Math.abs(diffRadians)/diffRadians);
                if ( Math.abs(nextRadarTurnRadians) >  Math.abs(diffRadians) ) {
                    nextRadarTurnRadians = diffRadians;
                }
                diffRadians -= nextRadarTurnRadians;
                sumRadarTurnRadians += nextRadarTurnRadians;
            }
            if ( diffRadians == 0.0 ) {
                break;
            }
        }
        return sumRadarTurnRadians;
    }

    public Pair<Double,Integer> calcAbsTurnRadians(double dstRadians) {
        double aheadTurnRadians = Util.calcTurnRadians(my.headingRadians,dstRadians);
        double backTurnRadians  = Util.calcTurnRadians(my.headingRadians,dstRadians-Math.PI);
        if ( Math.abs(aheadTurnRadians) < Math.abs(backTurnRadians)) { // ahead
            return new Pair<>(aheadTurnRadians,1);
        }else { // back
            return new Pair<>(backTurnRadians,-1);
        }
    }
    
    public double calcAbsGunTurnRadians(double absRadians) {
        double diffRadians1 = (absRadians - curGunHeadingRadians) % (Math.PI*2);
        if (diffRadians1 > Math.PI) {
            diffRadians1 = diffRadians1 - (Math.PI*2);
        } else if (diffRadians1 < -Math.PI) {
            diffRadians1 = diffRadians1 + (Math.PI*2);
        }
        double diffRadians2;
        if ( diffRadians1 < 0 ) {
            diffRadians2 = diffRadians1 + (Math.PI*2);
        }else{
            diffRadians2 = diffRadians1 - (Math.PI*2);
        }
        double realRadians1 = calcAbsGunTurnDiffRadians(diffRadians1);
        double realRadians2 = calcAbsGunTurnDiffRadians(diffRadians2);
        // logger.gun4("CALC: %2.2f => %2.2f : 1 = %2.2f(%2.2f) : 2 = %2.2f(%2.2f)",ctx.curGunHeading,absDegree,diffDegree1,realDegree1,diffDegree2,realDegree2);
        if ( Math.abs(realRadians2) < Math.abs(realRadians1) ) {
            return realRadians2;
        }
        return realRadians1;
    }
    
    public double calcAbsRadarTurnRadians(double absDegree) {
        double diffRadians1 = (absDegree - curRadarHeadingRadians) % (Math.PI*2);
        if (diffRadians1 > Math.PI) {
            diffRadians1 = diffRadians1 - (Math.PI*2);
        } else if (diffRadians1 < -Math.PI) {
            diffRadians1 = diffRadians1 + (Math.PI*2);
        }
        double diffRadians2;
        if ( diffRadians1 < 0 ) {
            diffRadians2 = diffRadians1+(Math.PI*2);
        }else{
            diffRadians2 = diffRadians1-(Math.PI*2);
        }
        double realRadians1 = calcAbsRadarTurnDiffRadians(diffRadians1);
        double realRadians2 = calcAbsRadarTurnDiffRadians(diffRadians2);
        // logger.radar4("CALC: %2.2f => %2.2f : 1 = %2.2f(%2.2f) : 2 = %2.2f(%2.2f)",ctx.curRadarHeading,absDegree,diffDegree1,realDegree1,diffDegree2,realDegree2);
        if ( Math.abs(realRadians2) < Math.abs(realRadians1) ) {
            return realRadians2;
        }
        return realRadians1;
    }    
    

    
}
