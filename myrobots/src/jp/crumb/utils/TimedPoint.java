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

    public long timeStamp;
    public long time;

    public TimedPoint() {
    }

    public TimedPoint(TimedPoint in) {
        this.set(in);
    }
    public TimedPoint(Point in,long time) {
        this.set(in);
        this.timeStamp = time;
        this.time = time;
    }

    public void set(TimedPoint in) {
        super.set(in);
        this.time = in.time;
        this.timeStamp = in.time;
    }

    public TimedPoint add(TimedPoint p) {
        super.add(p);
        this.time += p.time;
        return this;
    }
    public TimedPoint diff(TimedPoint p) {
        super.diff(p);
        this.time -= p.time;
        return this;
    }
    @Override
    public TimedPoint prod(double q) {
        super.prod(q);
        this.time *= q;
        return this;
    }
    @Override
    public TimedPoint quot(double q) {
        super.quot(q);
        this.time /= q;
        return this;
    }
    
}
    
    


