/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

/**
 *
 * @author crumb
 */
public class MyPoint extends DeltaMovingPoint {
    public MyPoint() {
    }

    public MyPoint(MyPoint in) {
        this.set(in);
    }
    public void set(MyPoint in) {
        super.set(in);
    }
}
