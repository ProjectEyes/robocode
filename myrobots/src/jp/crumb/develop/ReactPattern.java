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
public class ReactPattern extends AdvCrumbRobot<AdbCrumbContext> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(0,0,0), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(0,0,0));
    }
    

    @Override
    protected List<MoveType> initialAimTypeList() {
        List<MoveType> moveTypeList = new ArrayList<>(2);
//        MoveType moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_FIRST);
//        moveTypeList.add(moveType);
        MoveType moveType = new MoveType(MoveType.TYPE_REACT_PATTERN_CENTER);
        moveTypeList.add(moveType);
        return moveTypeList;
    }

}
