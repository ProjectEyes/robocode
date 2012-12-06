/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.sample;
import java.io.Serializable;
import jp.crumb.utils.Point;

/**
 *
 * @author crumb
 */
public class InvaderEvent implements Serializable {
    public int kind;
    public Point first;

    public InvaderEvent(int kind, Point first) {
        this.kind = kind;
        this.first = first;
    }
}
