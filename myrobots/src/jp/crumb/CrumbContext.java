/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb;

import java.util.HashMap;
import java.util.Map;
import jp.crumb.base.BaseContext;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.Copy;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Point;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;

/**
 *
 * @author crumb
 */
public class CrumbContext extends BaseContext {
    public static final int MODE_MOVE_MANUAL  = 0;
    public static final int MODE_MOVE_LOCKON1 = 2;
    public static final int MODE_MOVE_LOCKON2 = 3;
    public static final int MODE_MOVE_AUTO    = 80;
    public static final int MODE_RADAR_MANUAL = 0;
    public static final int MODE_RADAR_SEARCH = 1;
    public static final int MODE_RADAR_LOCKON = 2;
    public static final int MODE_GUN_MANUAL   = 0;
    public static final int MODE_GUN_LOCKON   = 2;
    public static final int MODE_GUN_AUTO     = 80;
    public static final int MODE_FIRE_MANUAL  = 0;
    public static final int MODE_FIRE_CLOSE   = 1;
    public static final int MODE_FIRE_AUTO    = 80;

    public int modeMove  = MODE_MOVE_MANUAL;
    public int modeRadar = MODE_RADAR_MANUAL;
    public int modeGun   = MODE_GUN_MANUAL;
    public int modeFire  = MODE_FIRE_MANUAL;
    public int modeCustom= 0;
    
    public int radarTowards= 1;
    public TimedPoint G;
    public TimedPoint GT;
    public Point lockOnPoint; // for view
    public String lockonTarget;
    public String leaderTarget;
//  public Enemy lockOnTarget;
    
    public Map<String, Enemy> nextEnemyMap = new HashMap<>(15,0.95f);
    public Map<String,BulletInfo> nextBulletList = new HashMap<>(50,0.95f);
    
    
    public CrumbContext() {
    }

    public CrumbContext(CrumbContext in){
        super(in);
        if ( in.G != null ) {
            this.G = new TimedPoint(in.G);
        }
        if ( in.GT != null ) {
            this.GT = new TimedPoint(in.GT);
        }
        if ( in.lockOnPoint != null ) {
            this.lockOnPoint = new Point(in.lockOnPoint);
        }
        this.lockonTarget = in.lockonTarget;
//        if ( in.lockOnTarget != null ) {
//            this.lockOnTarget = new Enemy(in.lockOnTarget);
//        }

        nextEnemyMap = Util.deepCopyHashMap(in.nextEnemyMap , new Copy<Enemy>(){
            @Override
            public Enemy copy(Enemy e) {
                return new Enemy(e);
            }
        });
        nextBulletList = Util.deepCopyHashMap(in.nextBulletList,new Copy<BulletInfo>(){
            @Override
            public BulletInfo copy(BulletInfo e) {
                return new BulletInfo(e);
            }
        });
    
    }
    public boolean isLeaderTarget(String name) {
        return leaderTarget != null && name.compareTo(leaderTarget) == 0;
    }
    public void setLeaderTarget(String leaderTarget) {
        this.leaderTarget = leaderTarget;
    }
    public void setLockonTarget(String lockonTarget) {
        this.lockonTarget = lockonTarget;
    }
    public final boolean isMoveMode(int m) {
        return (modeMove  == m);
    }
    public final boolean isGunMode(int m) {
        return (modeGun  == m);
    }
    public final boolean isRadarMode(int m) {
        return (modeRadar  == m);
    }
    public final boolean isFireMode(int m) {
        return (modeFire  == m);
    }
    public final boolean isCustomMode(int m) {
        return (modeCustom  == m);
    }
    public final void toggleRadarTowards(){
        radarTowards *= -1;
    }
    
    
    
}