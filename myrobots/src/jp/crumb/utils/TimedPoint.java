/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

/**
 *
 * @author crumb
 */
public class TimedPoint extends Point{

    public long time;

    public TimedPoint() {
    }

    public TimedPoint(TimedPoint in) {
        this.set(in);
    }
    public TimedPoint(Point in,long time) {
        this.set(in);
        this.time = time;
    }

    public void set(TimedPoint in) {
        super.set(in);
        this.time = in.time;
    }

}
