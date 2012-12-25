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
public class Point implements Serializable{

    public double x;
    public double y;

    public Point() {
    }

    public Point(Point in) {
        this.set(in);
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public void set(Point in) {
        this.x = in.x;
        this.y = in.y;
    }

    public Point add(Point p) {
        this.x += p.x;
        this.y += p.y;
        return this;
    }
    public Point diff(Point p) {
        this.x -= p.x;
        this.y -= p.y;
        return this;
    }
    public Point prod(double q) {
        this.x *= q;
        this.y *= q;
        return this;
    }
    public Point quot(double q) {
        this.x /= q;
        this.y /= q;
        return this;
    }


    public double calcDistance(Point dst) {
        return Util.calcDistance((this.x - dst.x),(this.y - dst.y));
    }
    public double calcRadians(Point dst) {
        return Util.calcRadians((dst.x-this.x),(dst.y-this.y));
    }

    @Override
    public String toString() {
        return String.format("(%2.2f,%2.2f)", x, y);
    }
}
