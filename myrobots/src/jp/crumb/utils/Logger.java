/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import jp.crumb.utils.Util;
import robocode.Robot;

/**
 *
 * @author crumb
 */
public class Logger {
    public static final int LOGLV_ALL   = 0xFF;
    public static final int LOGLV_GUN  = 0x01;
    public static final int LOGLV_CTRL  = 0x02;
    public static final int LOGLV_SCAN  = 0x04;
    public static final int LOGLV_MOVE  = 0x08;
    public static final int LOGLV_TRACE = 0x80;
//    public static int LOGLV = LOGLV_ALL;
    public static int LOGLV = LOGLV_MOVE;

    public static void log(String format, Object... args){
        System.out.println(String.format("%3d : ",Util.NOW) + String.format(format, args) );
    }
    public static void gun_log(String format, Object... args){
        if ( (LOGLV&LOGLV_GUN) != 0 ) {
            log(format,args);
        }
    }
    public static void ctrl_log(String format, Object... args){
        if ( (LOGLV&LOGLV_CTRL) != 0 ) {
            log(format,args);
        }
    }
    public static void scan_log(String format, Object... args){
        if ( (LOGLV&LOGLV_SCAN) != 0 ) {
            log(format,args);
        }
    }
    public static void move_log(String format, Object... args){
        if ( (LOGLV&LOGLV_MOVE) != 0 ) {
            log(format,args);
        }
    }
    public static void trace(String format, Object... args){
        if ( (LOGLV&LOGLV_TRACE) != 0 ) {
            log(format,args);
        }
    }
    
}
