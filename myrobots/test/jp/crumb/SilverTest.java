/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import jp.crumb.utils.Enemy;
import java.awt.Graphics2D;
import jp.crumb.utils.MovingPoint;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import robocode.CustomEvent;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

/**
 *
 * @author crumb
 */
public class SilverTest {
    
    public SilverTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of log method, of class Silver.
     */
    @Test
    public void testA() {
        Enemy r = new Enemy();
        r.velocity = 8.0;
        r.heading = 90;
        r.headingRadians = Math.toRadians(r.heading);
        r.x = 343.05330679052315;
        r.y = 553.0008357847164;
        r.prospectNext(new MovingPoint());
        System.out.println(r.x +","+r.y);
        r.prospectNext(new MovingPoint());
        System.out.println(r.x +","+r.y);
    }


}
