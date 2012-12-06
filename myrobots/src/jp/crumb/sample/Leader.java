/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.sample;

import java.awt.Color;
import jp.crumb.CrumbRobot;


/**
 *
 * @author crumb
 */
public class Leader extends CrumbRobot {

    @Override
    public void run() {
        setColors(new Color(255, 255, 150), new Color(255, 255, 150), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(200,255,100));

        super.run();
    }

}
