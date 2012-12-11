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
    public void updateScore(double s,double limit){
        double count = scoreCount;
        if ( scoreCount > limit ) {
            count = limit;
        }
        double sumScore = count * score + s;
        scoreCount++;
        count++;
        score = sumScore/(double)count;
    }
    public void updateScore(double s){
        updateScore(s,Double.POSITIVE_INFINITY);
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
    
//    static public final int TYPE_UNKNOWN   = 0xF000;
    static public final int TYPE_PINPOINT  = 0x0010;
    static private final int TYPE_INERTIA   = 0x0020;
    static private final int TYPE_ACCURATE  = 0x0040;
    static private final int TYPE_FIRST     = 0x0001;
    static private final int TYPE_CENTER    = 0x0002;
    static public final int TYPE_INERTIA_FIRST  = TYPE_INERTIA | TYPE_FIRST;
    static public final int TYPE_INERTIA_CENTER = TYPE_INERTIA | TYPE_CENTER;
    static public final int TYPE_ACCURATE_FIRST = TYPE_ACCURATE | TYPE_FIRST;
    static public final int TYPE_ACCURATE_CENTER = TYPE_ACCURATE | TYPE_CENTER;

//    public boolean isTypeUnknown(){
//        return (type & TYPE_UNKNOWN) != 0;
//    }
    public boolean isTypePinPoint(){
        return (type == TYPE_PINPOINT);
    }
    public boolean isTypeInertia(){
        return (type & TYPE_INERTIA) != 0;
    }
    public boolean isTypeAccurate(){
        return (type & TYPE_ACCURATE) != 0;
    }
    public boolean isTypeFirst(){
        return (type & TYPE_FIRST) != 0;
    }
    public boolean isTypeCenter(){
        return (type & TYPE_CENTER) != 0;
    }
    
}