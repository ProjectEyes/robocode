/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

/**
 *
 * @author crumb
 */
public class Point {

    public double x;
    public double y;

    public Point() {
    }

    public Point(Point in) {
        this.x = in.x;
        this.y = in.y;
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point add(Point p) {
        this.x += p.x;
        this.y += p.y;
        return this;
    }

    public Point limit(double limitX, double limitY) {
        if (x < 0) {
            x = 0;
        } else if (x > limitX) {
            x = limitX;
        }
        if (y < 0) {
            y = 0;
        } else if (y > limitY) {
            y = limitY;
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format("(%2.2f,%2.2f)", x, y);
    }
    
    public double calcDistance(Point dst) {
        return Math.sqrt((this.x - dst.x) * (this.x - dst.x) + (this.y - dst.y) * (this.y - dst.y));
    }

    public double calcRadians(Point dst) {
        if (dst.y == this.y) {
            return (dst.x > this.x) ? Math.PI / 2 : Math.PI / -2;
        } else if (dst.y > this.y) {
            return Math.atan((this.x - dst.x) / (this.y - dst.y));
        } else {
            return Math.atan((this.x - dst.x) / (this.y - dst.y)) - Math.PI;
        }
    }

    public double calcDegree(Point dst) {
        if (this.y == dst.y) {
            return (dst.x > this.x) ? 90 : -90;
        } else if (dst.y > this.y) {
            return Math.toDegrees(Math.atan((this.x-dst.x)/(this.y-dst.y)));
        } else {
            return Math.toDegrees(Math.atan((this.x-dst.x)/(this.y-dst.y))) - 180;
        }
    }    
}
