/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.crumb.utils.Copy;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.MyPoint;
import jp.crumb.utils.Point;
import jp.crumb.utils.Util;

/**
 *
 * @author crumb
 */
public class BaseContext implements Serializable{
    public MyPoint my = new MyPoint();
    public MyPoint nextMy = new MyPoint();
    
    public double curTurnRemaining;
    public double curTurnRemainingRadians;       
    public double curGunTurnRemaining;        
    public double curGunTurnRemainingRadians;            
    public double curRadarTurnRemaining;            
    public double curRadarTurnRemainingRadians;
    
    public double energy;
    public double curGunHeadingRadians;
    public double curGunHeading;
    public double curRadarHeadingRadians;
    public double curRadarHeading;
    public double curDistanceRemaining;
    public double gunHeat;
    public int others;
    public int enemies;
    
    
    public double prevRadarHeadingRadians;
    // For auto move
    public Point destination;

    public Map<String, Enemy> nextEnemyMap = new HashMap<>();
    public Map<String, List<MovingPoint> > enemyPatternMap = new HashMap<>();
    public Map<String,BulletInfo> nextBulletList = new HashMap<>();

    public BaseContext() {
        destination = new Point(my);
    }

    public BaseContext(BaseContext in){
        this.curTurnRemaining = in.curTurnRemaining;
        this.curTurnRemainingRadians = in.curTurnRemainingRadians;
        this.curGunTurnRemaining = in.curGunTurnRemaining;
        this.curGunTurnRemainingRadians = in.curGunTurnRemainingRadians;
        this.curRadarTurnRemaining = in.curRadarTurnRemaining;
        this.curRadarTurnRemainingRadians = in.curRadarTurnRemainingRadians;
        this.energy = in.energy;
        this.curGunHeadingRadians = in.curGunHeadingRadians;
        this.curGunHeading = in.curGunHeading;
        this.curRadarHeadingRadians = in.curRadarHeadingRadians;
        this.curRadarHeading = in.curRadarHeading;
        this.curDistanceRemaining = in.curDistanceRemaining;
        this.gunHeat = in.gunHeat;
        this.others = in.others;
        this.enemies = in.enemies;
        this.prevRadarHeadingRadians = in.prevRadarHeadingRadians;
        this.destination = new Point(in.destination);
        this.my = new MyPoint(in.my);
        this.nextMy = new MyPoint(in.nextMy);
        
        this.nextEnemyMap = Util.deepCopyHashMap(in.nextEnemyMap , new Copy<Enemy>(){
            @Override
            public Enemy copy(Enemy e) {
                return new Enemy(e);
            }
        });
        this.nextBulletList = Util.deepCopyHashMap(in.nextBulletList,new Copy<BulletInfo>(){
            @Override
            public BulletInfo copy(BulletInfo e) {
                return new BulletInfo(e);
            }
        });
        
        // this.enemyPatternMap = new HashMap<>(in.enemyPatternMap);
    }    
}
