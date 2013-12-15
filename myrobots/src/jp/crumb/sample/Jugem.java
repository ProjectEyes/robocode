package jp.crumb.sample;

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
public abstract class Jugem extends AdvCrumbRobot<AdbCrumbContext> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(20,20,20), new Color(50, 50,20), new Color(255, 155, 255)); // body,gun,radar
        this.setBulletColor(new Color(255,180,180));
    }

    protected List<MoveType> initialAimTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>(10);
        MoveType moveType = null;
        moveType = new MoveType(MoveType.TYPE_SIMPLE_PATTERN);
        moveTypeList.add(moveType);
        moveType.score = 100.0; // Initial type (will be overrided by first hit!!)
        moveType = new MoveType(MoveType.TYPE_REACT_PATTERN);
        moveTypeList.add(moveType);
        return moveTypeList;
    }

}
