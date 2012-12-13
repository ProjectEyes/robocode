/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import robocode.Robot;

/**
 *
 * @author crumb
 */
public class Swing extends Robot{

    @Override
    public void run() {
        while(true) {
            ahead(150);
            back(150);
        }
    }

}
