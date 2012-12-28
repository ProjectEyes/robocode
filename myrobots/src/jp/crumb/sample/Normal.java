package jp.crumb.sample;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import jp.crumb.develop.*;
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
public class Normal extends AdvCrumbRobot<AdbCrumbContext> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(100,100,100), new Color(100,100,100), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(255,255,200));
    }
    

    @Override
    protected List<MoveType> initialAimTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>(5);
        MoveType moveType = new MoveType(MoveType.TYPE_PINPOINT);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_INERTIA);
        moveTypeList.add(moveType);
        moveType = new MoveType(MoveType.TYPE_ACCELERATION);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(moveType);
        return moveTypeList;
    }

    

  
}
