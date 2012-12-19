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
public class MoveType extends Score implements Comparable<MoveType> , Serializable{
    
    public int type;
    public int aimCount;
    public int hitCount;
    public MoveType(int type){
        super(type);
        this.type = type;
    }
    public MoveType(MoveType in){
        super(in);
        this.type = in.type;
        this.aimCount  = in.aimCount;
        this.hitCount  = in.hitCount;
    }
    public double getHitRate(){
        if ( aimCount == 0 ) {
            return 0;
        }
        return (double)hitCount/(double)aimCount;
    }
    public void updateAim(){
        this.aimCount+=1;
    }
    public void updateHit(){
        this.hitCount+=1;
    }
    public void revartAim(){
        this.aimCount-=1;
    }
    public double avrage(){
        return (double)hitCount/(double)aimCount;
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
    
//    static public final int TYPE_UNKNOWN   = 0xF000;
    static public final int TYPE_PINPOINT          = 0x0010;
    static private final int TYPE_INERTIA          = 0x0020;
    static private final int TYPE_ACCELERATION         = 0x0040;
    static private final int TYPE_SIMPLE_PATTERN   = 0x0100;
    static private final int TYPE_REACT_PATTERN    = 0x0200;
    static private final int TYPE_FIRST            = 0x0001;
    static private final int TYPE_CENTER           = 0x0002;
    static public final int TYPE_INERTIA_FIRST  = TYPE_INERTIA | TYPE_FIRST;
    static public final int TYPE_INERTIA_CENTER = TYPE_INERTIA | TYPE_CENTER;
    static public final int TYPE_ACCELERATION_FIRST = TYPE_ACCELERATION | TYPE_FIRST;
    static public final int TYPE_ACCELERATION_CENTER = TYPE_ACCELERATION | TYPE_CENTER;
    static public final int TYPE_SIMPLE_PATTERN_FIRST = TYPE_SIMPLE_PATTERN | TYPE_FIRST;
    static public final int TYPE_SIMPLE_PATTERN_CENTER = TYPE_SIMPLE_PATTERN | TYPE_CENTER;
    static public final int TYPE_REACT_PATTERN_FIRST = TYPE_REACT_PATTERN | TYPE_FIRST;
    static public final int TYPE_REACT_PATTERN_CENTER = TYPE_REACT_PATTERN | TYPE_CENTER;

//    public boolean isTypeUnknown(){
//        return (type & TYPE_UNKNOWN) != 0;
//    }
    public boolean isTypePinPoint(){
        return (type == TYPE_PINPOINT);
    }
    public boolean isTypeInertia(){
        return (type & TYPE_INERTIA) != 0;
    }
    public boolean isTypeAcceleration(){
        return (type & TYPE_ACCELERATION) != 0;
    }
    public boolean isTypeSimplePattern(){
        return (type & TYPE_SIMPLE_PATTERN) != 0;
    }
    public boolean isTypeReactPattern(){
        return (type & TYPE_REACT_PATTERN) != 0;
    }
    public boolean isTypeFirst(){
        return (type & TYPE_FIRST) != 0;
    }
    public boolean isTypeCenter(){
        return (type & TYPE_CENTER) != 0;
    }
    
}