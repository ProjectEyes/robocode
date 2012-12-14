/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;


/**
 *
 * @author crumb
 */
public class Logger {
    public static final int LOGLV_ALL      = 0xFFFFFFFF;
    public static final int LOGLV_CTRL     = 0x0000000F;
    public static final int LOGLV_CTRL1    = 0x00000001;
    public static final int LOGLV_CTRL2    = 0x00000002;
    public static final int LOGLV_CTRL3    = 0x00000004;
    public static final int LOGLV_CTRL4    = 0x00000008;
    public static final int LOGLV_RADAR    = 0x000000F0;
    public static final int LOGLV_RADAR1   = 0x00000010;
    public static final int LOGLV_RADAR2   = 0x00000020;
    public static final int LOGLV_RADAR3   = 0x00000040;
    public static final int LOGLV_RADAR4   = 0x00000080;
    public static final int LOGLV_GUN      = 0x00000F00;
    public static final int LOGLV_GUN1     = 0x00000100;
    public static final int LOGLV_GUN2     = 0x00000200;
    public static final int LOGLV_GUN3     = 0x00000400;
    public static final int LOGLV_GUN4     = 0x00000800;
    public static final int LOGLV_FIRE     = 0x0000F000;
    public static final int LOGLV_FIRE1    = 0x00001000;
    public static final int LOGLV_FIRE2    = 0x00002000;
    public static final int LOGLV_FIRE3    = 0x00004000;
    public static final int LOGLV_FIRE4    = 0x00008000;
    public static final int LOGLV_PROSPECT = 0x000F0000;
    public static final int LOGLV_PROSPECT1= 0x00010000;
    public static final int LOGLV_PROSPECT2= 0x00020000;
    public static final int LOGLV_PROSPECT3= 0x00040000;
    public static final int LOGLV_PROSPECT4= 0x00080000;
    public static final int LOGLV_CRASH    = 0x00100000;
    public static final int LOGLV_SCAN     = 0x00200000;
    public static final int LOGLV_MOVE     = 0x00400000;
    public static final int LOGLV_TRACE    = 0x0F000000;
    public static final int LOGLV_DEBUG1   = 0x10000000;
    public static final int LOGLV_DEBUG2   = 0x20000000;
    public static final int LOGLV_DEBUG3   = 0x40000000;
    public static final int LOGLV_DEBUG4   = 0x80000000;
    public static final int LOGLV_DEBUG    = 0xF0000000;
//    public static int LOGLV = LOGLV_ALL;
//    public int LOGLV = LOGLV_CRASH |  LOGLV_GUN1 | LOGLV_GUN2;
    public int LOGLV = LOGLV_CRASH |  LOGLV_GUN1 | LOGLV_GUN2;
//    public static int LOGLV = LOGLV_DEBUG1 | LOGLV_DEBUG2;

    public Logger(int loglv) {
        LOGLV = loglv;
    }

    public Logger() {
        this(LOGLV_CRASH | LOGLV_GUN1);
    }

    public static void log(String format, Object... args){
        System.out.println(String.format("%3d : ",Util.NOW) + String.format(format, args) );
    }
    public void scan(String format, Object... args){
        if ( (LOGLV&LOGLV_SCAN) != 0 ) {
            log(format,args);
        }
    }
    public void move_log(String format, Object... args){
        if ( (LOGLV&LOGLV_MOVE) != 0 ) {
            log(format,args);
        }
    }
    public void crash(String format, Object... args){
        if ( (LOGLV&LOGLV_CRASH) != 0 ) {
            log(format,args);
        }
    }
    public  void gun1(String format, Object... args){
        if ( (LOGLV&LOGLV_GUN1) != 0 ) {
            log("GUN::"+format,args);
        }
    }
    public  void gun2(String format, Object... args){
        if ( (LOGLV&LOGLV_GUN2) != 0 ) {
            log("GUN::"+format,args);
        }
    }
    public  void gun3(String format, Object... args){
        if ( (LOGLV&LOGLV_GUN3) != 0 ) {
            log("GUN::"+format,args);
        }
    }
    public  void gun4(String format, Object... args){
        if ( (LOGLV&LOGLV_GUN4) != 0 ) {
            log("GUN::"+format,args);
        }
    }
    public  void fire1(String format, Object... args){
        if ( (LOGLV&LOGLV_FIRE1) != 0 ) {
            log("FIRE::"+format,args);
        }
    }
    public  void fire2(String format, Object... args){
        if ( (LOGLV&LOGLV_FIRE2) != 0 ) {
            log("FIRE::"+format,args);
        }
    }
    public  void fire3(String format, Object... args){
        if ( (LOGLV&LOGLV_FIRE3) != 0 ) {
            log("FIRE::"+format,args);
        }
    }
    public  void fire4(String format, Object... args){
        if ( (LOGLV&LOGLV_FIRE4) != 0 ) {
            log("FIRE::"+format,args);
        }
    }
    public  void prospect1(String format, Object... args){
        if ( (LOGLV&LOGLV_PROSPECT1) != 0 ) {
            log("PROSPECT::"+format,args);
        }
    }
    public  void prospect2(String format, Object... args){
        if ( (LOGLV&LOGLV_PROSPECT2) != 0 ) {
            log("PROSPECT::"+format,args);
        }
    }
    public  void prospect3(String format, Object... args){
        if ( (LOGLV&LOGLV_PROSPECT3) != 0 ) {
            log("PROSPECT::"+format,args);
        }
    }
    public  void prospect4(String format, Object... args){
        if ( (LOGLV&LOGLV_PROSPECT4) != 0 ) {
            log("PROSPECT::"+format,args);
        }
    }
    public  void radar1(String format, Object... args){
        if ( (LOGLV&LOGLV_RADAR1) != 0 ) {
            log("RADAR::"+format,args);
        }
    }
    public  void radar2(String format, Object... args){
        if ( (LOGLV&LOGLV_RADAR2) != 0 ) {
            log("RADAR::"+format,args);
        }
    }
    public  void radar3(String format, Object... args){
        if ( (LOGLV&LOGLV_RADAR3) != 0 ) {
            log("RADAR::"+format,args);
        }
    }
    public  void radar4(String format, Object... args){
        if ( (LOGLV&LOGLV_RADAR4) != 0 ) {
            log("RADAR::"+format,args);
        }
    }
    public  void ctrl1(String format, Object... args){
        if ( (LOGLV&LOGLV_CTRL1) != 0 ) {
            log("CTRL::"+format,args);
        }
    }
    public  void ctrl2(String format, Object... args){
        if ( (LOGLV&LOGLV_CTRL2) != 0 ) {
            log("CTRL::"+format,args);
        }
    }
    public  void ctrl3(String format, Object... args){
        if ( (LOGLV&LOGLV_CTRL3) != 0 ) {
            log("CTRL::"+format,args);
        }
    }
    public  void ctrl4(String format, Object... args){
        if ( (LOGLV&LOGLV_CTRL4) != 0 ) {
            log("CTRL::"+format,args);
        }
    }

    public  void trace(String format, Object... args){
        if ( (LOGLV&LOGLV_TRACE) != 0 ) {
            log(format,args);
        }
    }
    public  void debug1(String format, Object... args){
        if ( (LOGLV&LOGLV_DEBUG1) != 0 ) {
            log(format,args);
        }
    }
    public  void debug2(String format, Object... args){
        if ( (LOGLV&LOGLV_DEBUG2) != 0 ) {
            log(format,args);
        }
    }
    public  void debug3(String format, Object... args){
        if ( (LOGLV&LOGLV_DEBUG3) != 0 ) {
            log(format,args);
        }
    }
    public  void debug4(String format, Object... args){
        if ( (LOGLV&LOGLV_DEBUG4) != 0 ) {
            log(format,args);
        }
    }
    
}
