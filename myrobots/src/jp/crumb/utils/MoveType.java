/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author crumb
 */
public class MoveType implements Comparable<MoveType> , Serializable{
    static public final int TYPE_UNKNOWN  = 0;
    static public final int TYPE_PINPOINT = 1;
    static public final int TYPE_INERTIA1  = 2;
    static public final int TYPE_INERTIA2  = 3;
    static public final int TYPE_ACCURATE1 = 4;
    static public final int TYPE_ACCURATE2 = 5;

    public int type;
    public int aimCount;
    public int hitCount;
    public int scoreCount;
    public double aimTime;
    public double hitTime;
    public double score;
//    public double hitrange;
//    public double aimrange;
    public MoveType(int type){
        this.type = type;
    }
    public MoveType(MoveType in){
        this.type = in.type;
        this.aimCount  = in.aimCount;
        this.hitCount  = in.hitCount;
        this.aimTime = in.aimTime;
        this.hitTime = in.hitTime;
        this.scoreCount   = in.scoreCount;
        this.score   = in.score;
    }
    public double getHitRate(){
        if ( aimCount == 0 ) {
            return 0;
        }
        return (double)hitCount/(double)aimCount;
    }
    public void updateAim(double time){
        double sumAim = aimCount * aimTime + time;
        aimCount++;
        aimTime = sumAim/(double)aimCount;
    }
    public void updateHit(double time){
        double sumHit = hitCount * hitTime + time;
        hitCount++;
        hitTime = sumHit/(double)hitCount;
    }
    public void updateScore(double s){
        double sumScore = scoreCount * score + s;
        scoreCount++;
        score = sumScore/(double)scoreCount;
    }
    public void revartAim(double time){
        double sumAim = aimCount * aimTime - time;
        aimCount--;
        if ( aimCount == 0 ) {
            aimTime = 0;
        }else{
            aimTime = sumAim /(double)aimCount;
        }
    }

    @Override
    public int compareTo(MoveType o) {
        if ( aimCount == 0 ) {
            return -1;
        }
        if ( o.aimCount == 0 ) {
            return 1;
        }
        return (getHitRate()<o.getHitRate())?-1:1;
    }
    public static MoveType getMoveTypeByScore(List<MoveType> list) {
        MoveType ret = null;
        double score = Double.NEGATIVE_INFINITY;
        for( MoveType  moveType : list ) {
            if ( moveType.score > score ) {
                score =moveType.score;
                ret = moveType;
            }
        }
        return ret;
    }
}