/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * @author crumb
 */
public class Score implements Serializable{
    public long time=0;
    public int scoreCount;
    public double score;
    public int round;
    public Score(long time,int round){
        this(time);
        this.round = round;
    }
    public Score(long time){
        this.time = time;
        scoreCount = 0;
        score = 0;
        round = 0;
    }
    public Score(Score in){
        this.time   = in.time;
        this.scoreCount   = in.scoreCount;
        this.score   = in.score;
        this.round   = in.round;
    }
    public void reset (){
        scoreCount = 0;
        score = 0;
    }
    public void updateScore(double s,double limit,double min){
        s = (s<0)?0:s;
        double count = scoreCount;
        if ( scoreCount > limit ) {
            count = limit;
        }
        if ( count < min ) {
            count = min;
        }
        double sumScore = count * score + s;
        scoreCount++;
        count++;
        score = sumScore/(double)count;
    }
    public void updateScore(double s){
        updateScore((s<0)?0:s,Double.POSITIVE_INFINITY,0);
    }
    public static <T extends Score> T getByScore(Collection<T> list) {
        T ret = null;
        double score = Double.NEGATIVE_INFINITY;
        for( T  elem : list ) {
            if ( elem.score > score ) {
                score =elem.score;
                ret = elem;
            }
        }
        return ret;
    }
 
}