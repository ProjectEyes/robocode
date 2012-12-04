/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import java.io.Serializable;

/**
 *
 * @author crumb
 */
public class LockonEvent implements Serializable{
    public String lockonTarget;

    public LockonEvent(String lockonTarget) {
        this.lockonTarget = lockonTarget;
    }
    
}
