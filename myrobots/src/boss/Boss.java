package boss;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.crumb.adv.AdbCrumbContext;
import jp.crumb.adv.AdvCrumbRobot;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;

/**
 *
 * @author crumb
 */
public abstract class Boss extends AdvCrumbRobot<AdbCrumbContext> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(70,20,20), new Color(0, 0,0), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(0,255,0));
    }
    @Override
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

    @Override
    protected Enemy calcLockOnTarget() {
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( r.role == Enemy.ROLE_LEADER ) {
                return r;
            }
        }
        return super.calcLockOnTarget();
    }
}
