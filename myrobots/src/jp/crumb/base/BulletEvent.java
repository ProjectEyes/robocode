/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.base;

import java.io.Serializable;

/**
 *
 * @author crumb
 */
public class BulletEvent implements Serializable{
    public BulletInfo bulletInfo;

    public BulletEvent(BulletInfo bulletInfo) {
        this.bulletInfo = bulletInfo;
    }
}
