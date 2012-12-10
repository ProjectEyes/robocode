/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.develop;

import java.awt.Color;
import jp.crumb.CrumbContext;
import jp.crumb.CrumbRobot;


/**
 *
 * @author crumb
 */
public class RadarScan extends CrumbRobot<CrumbContext> {

    @Override
    protected void cbThinking() {
        setFireMode(ctx.MODE_FIRE_MANUAL);
        setRadarMode(ctx.MODE_RADAR_SEARCH);
    }

}
