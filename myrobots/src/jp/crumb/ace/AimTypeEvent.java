/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.ace;

import java.io.Serializable;
import java.util.List;
import jp.crumb.utils.MoveType;

/**
 *
 * @author crumb
 */
public class AimTypeEvent implements Serializable{
    public String name;
    public List<MoveType> aimTypeList;
    public AimTypeEvent(String name,List<MoveType> aimTypeList) {
        this.name = name;
        this.aimTypeList = aimTypeList;
    }
    
}
