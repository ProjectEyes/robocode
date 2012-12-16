package boss;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Color;
import jp.crumb.adv.AdbCrumbContext;
import jp.crumb.adv.AdvCrumbRobot;



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
  
}
