/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.base;

import java.io.Serializable;
import jp.crumb.utils.Enemy;

/**
 *
 * @author crumb
 */
public class TeammateInfoEvent implements Serializable{
    public Enemy e;
    public boolean isLeader;
    public TeammateInfoEvent(Enemy e,boolean isLeader) {
        this.e = e;
        this.isLeader = isLeader;
    }
}
