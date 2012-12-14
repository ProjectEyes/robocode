/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author crumb
 */
public class Score implements Serializable{
    public String name="";
    public int scoreCount;
    public double score;
    public Score(String name){
        this.name = name;
        scoreCount = 0;
        score = 0;
    }
    public Score(Score in){
        this.name   = in.name;        
        this.scoreCount   = in.scoreCount;
        this.score   = in.score;
    }
    public void updateScore(double s,double limit,double min){
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
        updateScore(s,Double.POSITIVE_INFINITY,0);
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