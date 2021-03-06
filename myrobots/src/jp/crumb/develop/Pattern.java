package jp.crumb.develop;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import jp.crumb.adv.AdbCrumbContext;
import jp.crumb.adv.AdvCrumbRobot;
import jp.crumb.utils.MoveType;



/**
 *
 * @author crumb
 */
public class Pattern extends AdvCrumbRobot<AdbCrumbContext> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(0,0,0), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(0,255,0));
    }
    

    @Override
    protected List<MoveType> initialAimTypeList() {
        List<MoveType> moveTypeList = new ArrayList<>(5);
        MoveType moveType = new MoveType(MoveType.TYPE_SIMPLE_PATTERN);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_REACT_PATTERN);
        moveTypeList.add(moveType);
        return moveTypeList;
    }

    

  
}
