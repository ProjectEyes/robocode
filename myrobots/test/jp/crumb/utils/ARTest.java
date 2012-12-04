/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import jp.crumb.utils.ar.AR;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author crumb
 */
public class ARTest {
    
    public ARTest() {
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
    @Test
    public void testAr() throws Exception {
      Double[] data = { 10.0 , 11.0 , 52.0 ,13.0, 14.0 , 205.0, 16.0 , 30.0 , 31.0 , 102.0 , 33.0 , 34.0 , 305.0 , 36.0 , 20.0 , 21.0 , 82.0 , 23.0 , 24.0 , 255.0, 26.0 , 40.0 , 41.0 , 152.0, 43.0,44.0, 405.0,45.0};
//      Double[] data = { 1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0,11.0,12.0,13.0,14.0 };
      List<Double> list = Arrays.asList(data);
        AR ar = new AR(27);
        for( int i = 2; i <list.size();i++) {
            Double ret = ar.ar(list.subList(0, i));
            AR.dumpList("X:" + ret , list.subList(0, i+1));
        }
    }
}
