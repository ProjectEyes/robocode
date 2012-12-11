/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import java.io.Serializable;
import java.util.List;
import jp.crumb.utils.MoveType;

/**
 *
 * @author crumb
 */
public class ShotTypeEvent implements Serializable{
    public String name;
    public List<MoveType> shotTypeList;
    public ShotTypeEvent(String name,List<MoveType> shotTypeList) {
        this.name = name;
        this.shotTypeList = shotTypeList;
    }
    
}
