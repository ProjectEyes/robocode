/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.adv;

import java.io.Serializable;

/**
 *
 * @author crumb
 */
public class CancelEnemyBalletEvent implements Serializable{
    public String key;

    public CancelEnemyBalletEvent(String key) {
        this.key = key;
    }
}
