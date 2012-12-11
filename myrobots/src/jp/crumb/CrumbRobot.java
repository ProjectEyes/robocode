package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jp.crumb.base.BaseRobo;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;
import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.Droid;
import robocode.HitByBulletEvent;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;



/**
 * Silver - a robot by (your name here)
 */
abstract public class CrumbRobot<T extends CrumbContext> extends BaseRobo<T> {
    protected static final double PREPARE_LOCK_TIME=2;
    protected static final int DEFAULT_MAX_HIT_TIME = 40;
    protected int MAX_HIT_TIME = DEFAULT_MAX_HIT_TIME;
    protected static final double ENEMY_BULLET_DIFF_THRESHOLD = Math.PI/4; // more than 45 degrees
    protected static final double BULLET_DIFF_THRESHOLD = Math.PI/4; // more than 45 degrees

//    protected static final int MODE_NORMAL = MODE_RADAR_SEARCH;
//    protected static final int MODE_LOCKON = MODE_RADAR_LOCKON | MODE_GUN_LOCKON;
    
    protected static final double AIM_ENAGY_THRESHOLD = 10.0;
    protected static final int AIM_TIMES_THRESHOLD = 3;

    protected static final long SELECT_POWER_RANGE = 17;
    private Map<String,BulletInfo> enemyBulletList = new HashMap<>();

    @Override
    protected CrumbContext createContext(CrumbContext in) {
        if ( in == null ) {
            return new CrumbContext();
        }
        return new CrumbContext(in);
    }

    protected static final double DEFAULT_WALL_WEIGHT = 500;
    protected static final double DEFAULT_WALL_DIM = 1.8;
    protected static final double DEFAULT_ENEMY_WEIGHT = 400;
    protected static final double DEFAULT_ENEMY_DIM = 1.8;
    protected static final double DEFAULT_G_WEIGHT = 50;
    protected static final double DEFAULT_G_DIM = 1.1;
    protected static final double DEFAULT_GT_WEIGHT = 400;
    protected static final double DEFAULT_GT_DIM = 2.8;
    protected static final long   DEFAULT_G_EXPIRE = 5;
    protected static final long   DEFAULT_G_DISTANCE_THRESHIOLD = 80;
    protected static final double DEFAULT_LOCKON_APPROACH = 12;
    protected static final long   DEFAULT_BULLET_PROSPECT_TIME = 12;
    protected static final double DEFAULT_BULLET_WEIGHT = 1000;
    protected static final double DEFAULT_BULLET_DIM = 4;
    protected static final long   DEFAULT_ENEMY_BULLET_PROSPECT_TIME = 20;
    protected static final double DEFAULT_ENEMY_BULLET_WEIGHT = 1000;
    protected static final double DEFAULT_ENEMY_BULLET_DIM = 4;
    protected static final double DEFAULT_ENEMY_BULLET_DISTANCE = 400;

    
    // For move
    protected  double WALL_WEIGHT            = DEFAULT_WALL_WEIGHT;
    protected  double WALL_DIM               = DEFAULT_WALL_DIM;
    protected  double ENEMY_WEIGHT           = DEFAULT_ENEMY_WEIGHT;
    protected  double ENEMY_DIM              = DEFAULT_ENEMY_DIM;
    protected  double G_WEIGHT               = DEFAULT_G_WEIGHT;
    protected  double G_DIM                  = DEFAULT_G_DIM;
    protected  double GT_WEIGHT              = DEFAULT_GT_WEIGHT;
    protected  double GT_DIM                 = DEFAULT_GT_DIM;
    protected  long   G_EXPIRE               = DEFAULT_G_EXPIRE;
    protected  long   G_DISTANCE_THRESHIOLD  = DEFAULT_G_DISTANCE_THRESHIOLD;
    protected  double LOCKON_APPROACH        = DEFAULT_LOCKON_APPROACH;
    protected  long   BULLET_PROSPECT_TIME   = DEFAULT_BULLET_PROSPECT_TIME;
    protected  double BULLET_WEIGHT          = DEFAULT_BULLET_WEIGHT;
    protected  double BULLET_DIM             = DEFAULT_BULLET_DIM;
    protected  long   ENEMY_BULLET_PROSPECT_TIME   = DEFAULT_ENEMY_BULLET_PROSPECT_TIME;
    protected  double ENEMY_BULLET_WEIGHT          = DEFAULT_ENEMY_BULLET_WEIGHT;
    protected  double ENEMY_BULLET_DIM             = DEFAULT_ENEMY_BULLET_DIM;
    protected  double ENEMY_BULLET_DISTANCE        = DEFAULT_ENEMY_BULLET_DISTANCE;
    

    // For gun
    protected  static final double DEFAULT_RANGE_RADAR_LOCKON = 1000;
    protected  double RANGE_RADAR_LOCKON = DEFAULT_RANGE_RADAR_LOCKON;

    protected static Map<String,List<MoveType>> shotTypeMap = new HashMap<>();
    protected static Map<String,List<MoveType>> aimTypeMap = new HashMap();

    protected Map<Long,RobotPoint> myLog = new HashMap<>();
    protected static Map<String, Map<Long,Enemy> > enemyLog = new HashMap<>();

    @Override
    protected void updateCurrent() {
        super.updateCurrent();
        myLog.put(ctx.my.time,ctx.my);
    }
    // TODO: to abs time
//    protected RobotPoint prevMy(long prev) {
//        long size = myLog.size();
//        if ( size > prev )  {
//            return myLog.get((int)size-1-(int)prev);
//        }
//        return null;
//    }
    protected RobotPoint logMy(long absTime) {
        return myLog.get(absTime);
    }
    protected RobotPoint logEnemy(String name,long absTime) {
        Map<Long,Enemy> log = enemyLog.get(name);
        if ( log != null ) {
            return log.get(absTime);
        }
        return null;
    }
    
    
    // TODO: change aimtype 
    @Override
    protected Enemy createEnemy(ScannedRobotEvent e) {
        return new Enemy(ctx.my, e);
    }    
    @Deprecated
    @Override
    protected boolean prospectNextEnemy(Enemy enemy) {
        return enemy.prospectNext();
    }
    @Deprecated
    protected void prospectNextEnemy(Enemy enemy,long interval) {
        for (int i = 1; i <= interval; i++) {
            prospectNextEnemy(enemy);
        }
    }

    protected void prospectNextRobot(RobotPoint robot,MoveType moveType,long term) {
        if ( moveType.isTypePinPoint() ) {
        }else if ( moveType.isTypeInertia() ) {
            robot.inertia(term);
        }else if ( moveType.isTypeAccurate() ) {
            for ( int i = 0 ; i < term ; i++ ) {
                robot.prospectNext();
            }
        }else if ( moveType.isTypeUnknown() ) {
            // TODO: Unknown How do you do ?
            throw new UnsupportedOperationException("Unknown type : " + moveType.type);
        }
    }
    
    protected Enemy getNextEnemy(String name) {
        Enemy ret = ctx.nextEnemyMap.get(name);
        if ( isStale(ret) ) {
            return null;
        }
        return ret;
    }
    
    
    protected final Point movingBase(){
        // Wall
        Point dst = new Point(ctx.my);   
        dst.diff(Util.getGrabity(ctx.my, new Point(Util.battleFieldWidth,ctx.my.y), WALL_WEIGHT,WALL_DIM));
        dst.diff(Util.getGrabity(ctx.my, new Point(0,ctx.my.y), WALL_WEIGHT,WALL_DIM));
        dst.diff(Util.getGrabity(ctx.my, new Point(ctx.my.x,Util.battleFieldHeight), WALL_WEIGHT,WALL_DIM));
        dst.diff(Util.getGrabity(ctx.my, new Point(ctx.my.x,0), WALL_WEIGHT,WALL_DIM));
        // Enemy
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            if ( ! isStale(e.getValue() ) ) {
                dst.diff(Util.getGrabity(ctx.my,e.getValue(), ENEMY_WEIGHT,ENEMY_DIM));
            }
        }
        // Bullet
        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            if ( ! info.owner.equals(name) ){
                MovingPoint bullet = new MovingPoint(info.src);
                for ( int i = 0 ; i < BULLET_PROSPECT_TIME;i++) {
                    dst.diff(Util.getGrabity(ctx.my,bullet, BULLET_WEIGHT,BULLET_DIM));
                    bullet.inertia(1);
                }
            }
        }
        // Enemy Bullet
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            if ( info.distance < ENEMY_BULLET_DISTANCE || ctx.enemies == 1 )  {
                if ( ! info.owner.equals(name) ){
                    MovingPoint bullet = new MovingPoint(info.src);
                    for ( int i = 0 ; i < ENEMY_BULLET_PROSPECT_TIME;i++) {
                        dst.diff(Util.getGrabity(ctx.my,bullet, ENEMY_BULLET_WEIGHT,ENEMY_BULLET_DIM));
                        bullet.inertia(1);
                    }
                }
            }
        }
        if ( ctx.G != null ) {
            dst.diff(Util.getGrabity(ctx.my, ctx.G, G_WEIGHT,G_DIM));
        }
        if ( ctx.GT != null ) {
            dst.diff(Util.getGrabity(ctx.my, ctx.GT, GT_WEIGHT,GT_DIM));
        }
        return dst;
    }
    
    protected final double decideBulletPowerFromDistance(double distance,long time){
        return Util.bultPower( distance/time );
    }
    
    
    private Pair<Double,Double> calcFire(Enemy target,long deltaThreshold,long recentThreshold){
        if ( target.delta == null || target.delta.time > deltaThreshold || (ctx.my.time - target.time) > recentThreshold ) {
            return new Pair<>(0.0,Util.fieldFullDistance);
        }
        double maxPower = 0.0;
        double aimDistance = Util.fieldFullDistance;
        MoveType aimType = MoveType.getMoveTypeByScore(aimTypeMap.get(target.name));
        Enemy prospectTarget = new Enemy(target);
        for ( int i = 1; i <= MAX_HIT_TIME; i++ ) {
            double d = Util.calcPointToLineRange(ctx.my,prospectTarget,ctx.curGunHeadingRadians);
            if ( d < (Util.tankWidth/2) ) { // crossing shot line
                double bultDistance = Util.calcPointToLineDistance(ctx.my,prospectTarget,ctx.curGunHeadingRadians);
                double power = decideBulletPowerFromDistance(bultDistance,i);
                if ( maxPower < power ) { // hit ? : power more than 0.0
                    logger.fire3("POWER(%s): (%2.2f) => (%2.2f)", target.name,maxPower,power);
                    maxPower = power;
                    aimDistance = bultDistance;
                }
            }
            // TODO: calcFire() CHECK : using aimType
            prospectNextRobot(prospectTarget,aimType,1);
        }
        return new Pair<>(maxPower,aimDistance);
    }
    protected void firing(long deltaThreshold,long recentThreshold){
        if ( ctx.gunHeat > 0.0 ) {
            return;
        }
        double maxPower = 0.0;
        double aimDistance = Util.fieldFullDistance;
        String targetName = "";
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy target = e.getValue();
            if ( ! isStale(target) ) {
                Pair<Double,Double> result = calcFire(target,1,0);
                if ( aimDistance > result.second ) {
                    maxPower = result.first;
                    aimDistance = result.second;
                    targetName = target.name;
                    if ( target.energy == 0 ) {
                        maxPower = 0.00001;
                    }                    
                }
            }
        }
        
        if ( ! isTeammate(targetName) && maxPower > 0 ) {
            if ( ctx.enemies > 1 ) {
                normalMode();
            }
            doFire(maxPower,aimDistance,targetName);
        }
    }
    
    private double selectPowerFromDistance(double distance) {
        double power = decideBulletPowerFromDistance(distance,SELECT_POWER_RANGE);
        return (power==0.0)?0.01:power;
    }

    protected Pair<Long,Double> calcShot(MoveType moveType,RobotPoint target,Point src,double bulletVelocity){
        return calcShot(moveType, target, src, bulletVelocity,0);
    }
    protected Pair<Long,Double> calcShot(MoveType moveType,RobotPoint target,Point src,double bulletVelocity,long deltaTime){
        double distance = src.calcDistance(target);
        double retRadians = 0;
        long   retTime = 0;
        if ( moveType.isTypeUnknown() ) {
            //TODO: Unknown How do you do ? 
            throw new UnsupportedOperationException("Unknown type : " + moveType.type);
        }else if ( moveType.isTypeUnknown() ) {
            retRadians = src.calcRadians(target);
            retTime    = (long)Math.ceil(src.calcDistance(target));
        }else if ( moveType.isTypeInertia() || moveType.isTypeAccurate()  ) {
            // ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity)))
            //  - Calc only fixed time ( hitted )
            //  - for prospecting time ( shoting ) =>  with updating distance each ticks.
            RobotPoint cpMyAsEnemy = new RobotPoint(target);
            double hitArea;
            if ( moveType.isTypeFirst() ) {
                hitArea = 0;
            }else if ( moveType.isTypeCenter() ) {
                hitArea = (Util.tankWidth/2);                
            }else if ( moveType.isTypeLast() ) {
                hitArea = -(Util.tankWidth/2);                
            }else{
                throw new UnsupportedOperationException("Unknown type : " + moveType.type);
            }
            // TODO : TEST !!
            for ( retTime = 1 ; retTime <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/bulletVelocity))) ; retTime++ ) {
                prospectNextRobot(cpMyAsEnemy,moveType,1);
                distance = src.calcDistance(cpMyAsEnemy);
                retRadians = src.calcRadians(cpMyAsEnemy);
                if ( distance - Math.abs(bulletVelocity*retTime) < hitArea ) { // hit ?
                    logger.prospect4("INERTIA1 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(retRadians),distance,Math.abs(bulletVelocity*retTime),distance - Math.abs(bulletVelocity*retTime));
                    break;
                }
            }
        }
        return new Pair<>(retTime,retRadians);
    }    
//    protected Pair<Long,Double> calcShotRadians(RobotPoint target,double velocity,Point src,int shotType,long deltaTime){
//        double distance = src.calcDistance(target);
//        double retRadians = 0;
//        long   retTime = 0;
//        if ( shotType == MoveType.TYPE_UNKNOWN ) {
//        }else if ( shotType == MoveType.TYPE_PINPOINT ) {
//            retRadians = src.calcRadians(target);
//            retTime    = (long)Math.ceil(src.calcDistance(target));
//        }else if ( shotType == MoveType.TYPE_INERTIA1 ) {
//            // ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity)))
//            //  - Calc only fixed time ( hitted )
//            //  - for prospecting time ( shoting ) =>  with updating distance each ticks.
//            RobotPoint cpMyAsEnemy = new RobotPoint(target);
//            for ( retTime = 1 ; retTime <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity))) ; retTime++ ) {
//                cpMyAsEnemy.inertia(1);
//                distance = src.calcDistance(cpMyAsEnemy);
//                retRadians = src.calcRadians(cpMyAsEnemy);
//                if ( distance - Math.abs(velocity*retTime) < (Util.tankWidth/2) ) { // hit ?
//                    logger.prospect4("INERTIA1 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(retRadians),distance,Math.abs(velocity*retTime),distance - Math.abs(velocity*retTime));
//                    break;
//                }
//            }
//        }else if ( shotType == MoveType.TYPE_INERTIA2 ) {
//            RobotPoint cpMyAsEnemy = new RobotPoint(target);
//            for ( retTime = 1 ; retTime <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity))) ; retTime++ ) {
//                cpMyAsEnemy.inertia(1);
//                distance = src.calcDistance(cpMyAsEnemy);
//                retRadians = src.calcRadians(cpMyAsEnemy);
//                if ( distance - Math.abs(velocity*retTime) < 0 ) { // hit ?
//                    logger.prospect4("INERTIA2 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(retRadians),distance,Math.abs(velocity*retTime),distance - Math.abs(velocity*retTime));
//                    break;
//                }
//            }
//        }else if ( shotType == MoveType.TYPE_ACCURATE1 ) {
//            RobotPoint cpMyAsEnemy = new RobotPoint(target);
//            for ( retTime = 1 ; retTime <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity))) ; retTime++ ) {
//                cpMyAsEnemy.prospectNext();
//                distance = src.calcDistance(cpMyAsEnemy);
//                retRadians = src.calcRadians(cpMyAsEnemy);
//                if ( distance - Math.abs(velocity*retTime) < (Util.tankWidth/2) ) { // hit ?
//                    logger.prospect4("ACCURATE1 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(retRadians),distance,Math.abs(velocity*retTime),distance - Math.abs(velocity*retTime));
//                    break;
//                }
//            }
//        }else if ( shotType == MoveType.TYPE_ACCURATE2 ) {
//            RobotPoint cpMyAsEnemy = new RobotPoint(target);
//            for ( retTime = 1 ; retTime <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity))) ; retTime++ ) {
//                cpMyAsEnemy.prospectNext();
//                distance = src.calcDistance(cpMyAsEnemy);
//                retRadians = src.calcRadians(cpMyAsEnemy);
//                if ( distance - Math.abs(velocity*retTime) < 0 ) { // hit ?
//                    logger.prospect4("ACCURATE2 : %2.2f  (%2.2f - %2.2f = %2.2f)" ,Math.toDegrees(retRadians),distance,Math.abs(velocity*retTime),distance - Math.abs(velocity*retTime));
//                    break;
//                }
//            }
//        }
//        return new Pair<>(retTime,retRadians);
//    }    
    protected static final int MAX_CALC = 5;
    protected void lockOn(String lockonTarget) {
        Enemy lockOnTarget = getNextEnemy(lockonTarget);
        if ( lockOnTarget == null ) {
            return;
        }

        double distance = ctx.my.calcDistance(lockOnTarget);
        double power = selectPowerFromDistance(distance);
        double gunTurn = 0;
        double allTime = distance / Util.bultSpeed(power)+1; // 1 = gunturn



        {
        MoveType aimType = MoveType.getMoveTypeByScore(aimTypeMap.get(lockonTarget));
        long gunTurnTime = 1;
        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            // prospect me while gun turn
            RobotPoint prospectMy  = new RobotPoint(ctx.my);
            prospectNextMy(prospectMy,gunTurnTime);
            // prospect target while gun turn
            Enemy prospectTarget = new Enemy(lockOnTarget);
            prospectNextRobot(prospectTarget, aimType, gunTurnTime);
            // 
            distance = prospectMy.calcDistance(prospectTarget);
            power = this.selectPowerFromDistance(distance);
            double bulletVelocity = Util.bultSpeed(power);
            Pair<Long,Double> shot = calcShot(aimType,prospectTarget,prospectMy,bulletVelocity);
            long bulletTime = shot.first;
            double bulletRadians = shot.second;
            gunTurn = ctx.calcAbsGunTurn(bulletRadians);
            long nextGunTurnTime = (long) Math.ceil(gunTurn / Util.gunTurnSpeed());
            if ( gunTurnTime == nextGunTurnTime) {
                ctx.lockOnPoint = Util.calcPoint(bulletRadians,bulletTime*bulletVelocity);
                logger.log("GUNTURN1 : " + gunTurn);
                break;
            }
        }
        }
        // TODO: lockOn() Use calcShotRadians => should remove this loop
        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            Enemy prospectTarget = new Enemy(lockOnTarget);
            prospectNextEnemy(prospectTarget,(int)Math.ceil(allTime));
            distance = ctx.nextMy.calcDistance(prospectTarget);
            power = this.selectPowerFromDistance(distance);
            double bultTime = distance / Util.bultSpeed(power);
            gunTurn = ctx.calcAbsGunTurn(ctx.nextMy.calcDegree(prospectTarget));
            double gunTurnTime = (long) Math.ceil(gunTurn / Util.gunTurnSpeed());
            // Todo:
            if (Math.abs(allTime - (bultTime + gunTurnTime)) < 1) {
                ctx.lockOnPoint = prospectTarget;
                logger.log("GUNTURN2 : " + gunTurn);
                break;
            }
            allTime = bultTime + gunTurnTime;
        }
        doTurnGunRight(gunTurn);
    }
    protected void radarLockOn(String lockonTarget) {
        Enemy lockOnTarget = getNextEnemy(lockonTarget);
        if (lockOnTarget == null ) {
            normalMode();
            return;
        }
        double targetBearing = ctx.my.calcDegree(lockOnTarget);
        double radarTurn = ctx.calcAbsRadarTurn(targetBearing);
        if ( Math.abs(radarTurn) < Util.radarTurnSpeed() ) {
            double diffDegree = Util.calcTurn(ctx.curRadarHeading,targetBearing);
            double swingBearing = targetBearing + (Util.radarTurnSpeed()/2);
            if ( diffDegree != 0.0 ) {
                swingBearing = targetBearing + (Util.radarTurnSpeed()/2)*(Math.abs(diffDegree)/diffDegree);
            }
            radarTurn = ctx.calcAbsRadarTurn(swingBearing);
        }
        this.doTurnRadarRight(radarTurn);
    }
    // TODO: calcPriorityDistance
    private double calcPriorityDistance (Enemy e) {
        return ctx.my.calcDistance(e);

//        if ( e.energy == 0.0 ) {
//            return 0; // High priority
//        }
//        if ( e.energy< AIM_ENAGY_THRESHOLD ) {
//            return e.distance/10; // High priority
//        }
//        MoveType aimType = e.getAimType();
//        if ( aimType.aim < AIM_TIMES_THRESHOLD || e.distance < aimType.hitrange ) {
//            return e.distance; // In range
//        }
//        double hitrate = aimType.getHitRate();
//        double diffrange = aimType.getHitRange();
//        return e.distance + diffrange*(1-hitrate);
    }

    @Override
    protected void cbExtMessage(MessageEvent e) {
        Serializable event = e.getMessage();
        if ( event instanceof LockonEvent ) {
            LockonEvent ev = (LockonEvent)event;
            if ( ! ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
                this.setMoveMode(ctx.MODE_MOVE_LOCKON1);
            }
            ctx.setLockonTarget(ev.lockonTarget);
        }else if (event instanceof CancelEnemyBalletEvent ) {
            CancelEnemyBalletEvent ev = (CancelEnemyBalletEvent)event;
            removeEnemyBulletInfo(ev.key);
        }else if (event instanceof ShotTypeEvent ) {
            ShotTypeEvent ev = (ShotTypeEvent)event;
            shotTypeMap.put(ev.name,ev.shotTypeList);
        }else if (event instanceof AimTypeEvent ) {
            AimTypeEvent ev = (AimTypeEvent)event;
            aimTypeMap.put(ev.name,ev.aimTypeList);
        }
        
    }

    @Override
    protected void cbFirst() {
        setMoveMode(ctx.MODE_MOVE_AUTO);
        setGunMode(ctx.MODE_GUN_AUTO);
        setRadarMode(ctx.MODE_RADAR_SEARCH);
        setFireMode(ctx.MODE_FIRE_AUTO);
        super.cbFirst();
    }
    
    
    @Override
    protected void cbProspectNextTurn(){
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            prospectNextEnemy(r);
        }

        List<String> rmBullet = new ArrayList<>();
        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            info.src.inertia(1);
            if ( info.src.isLimit() ) {
                rmBullet.add(e.getKey());
            }
        }
        for( String n : rmBullet ) {
            removeBulletInfo(n);
        }

        List<String> rmEnemyBullet = new ArrayList<>();
        for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            info.src.inertia(1);
            if ( info.src.isLimit() ) {
                rmEnemyBullet.add(e.getKey());
            }
        }
        for( String n : rmEnemyBullet ) {
            removeEnemyBulletInfo(n);
        }
    }    
    

    @Override
    protected void cbThinking() {
        Enemy lockOnTarget = getNextEnemy(ctx.lockonTarget);
        if (isLeader || teammate.isEmpty() ) {
            for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
                Enemy r = e.getValue();
                if (teammate.contains(r.name)) {
                    continue;
                }
                if ( isStale(e.getValue() ) ) {
                    continue;
                }
                if (lockOnTarget == null) {
                    lockOnTarget = r;
                    continue;
                }
                if (calcPriorityDistance(lockOnTarget) > calcPriorityDistance(r)) {
                    lockOnTarget = r;
                }
            }
            if (lockOnTarget != null) {
                if ( ! ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
                    this.setMoveMode(ctx.MODE_MOVE_LOCKON1);
                }
                ctx.setLockonTarget(lockOnTarget.name);
                if (isLeader) {
                    broadcastMessage(new LockonEvent(ctx.lockonTarget));
                }
            }
        }
        // TODO : TEST !!
        if ( lockOnTarget != null ) {
            double distance = ctx.my.calcDistance(lockOnTarget);
            if (    ctx.enemies == 1 || 
                    lockOnTarget.energy == 0 ||
                    distance < RANGE_RADAR_LOCKON && (ctx.gunHeat/Util.gunCoolingRate) < PREPARE_LOCK_TIME){
                if ( ! ctx.isGunMode(ctx.MODE_GUN_MANUAL )) {
                    setGunMode(ctx.MODE_GUN_LOCKON);
                }
                if ( ! ctx.isRadarMode(ctx.MODE_RADAR_MANUAL )) {
                    setRadarMode(ctx.MODE_RADAR_LOCKON);
                }
            }else{
                normalMode();
            }
        }else{
            normalMode();
        }
    }
    
    
   @Override
    protected void cbMoving(){
        if ( ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
            return;
        }
        if ( ctx.my.time < 10 ) {
            return;
        }
        
        if ( ctx.G == null ) {
            ctx.G = new TimedPoint(Util.getRandomPoint(ctx.my,5),ctx.my.time);
        }
        
        if ( ctx.destination != null && (G_DISTANCE_THRESHIOLD > 80 || ctx.my.time - ctx.G.time > G_EXPIRE) ) { 
            double gr = ctx.my.calcRadians(ctx.destination);
            Point g = Util.calcPoint(gr,50).prod(-1).add(ctx.my);
            g = Util.getRandomPoint(g,55);
            ctx.G = new TimedPoint(g,ctx.my.time);
        }
            Enemy lockOnTarget = getNextEnemy(ctx.lockonTarget);
            if ( lockOnTarget != null && ctx.isMoveMode(ctx.MODE_MOVE_LOCKON1 )) {
                double fullDistance = Util.fieldFullDistance;
                double tr = ctx.my.calcRadians(lockOnTarget);
                double td = ctx.my.calcDistance(lockOnTarget);
                double approaching = (td-fullDistance)/LOCKON_APPROACH;
                double turn1 = Util.calcTurnRadians(ctx.my.headingRadians,tr +Math.PI/2);
                double turn2= Util.calcTurnRadians(ctx.my.headingRadians,tr -Math.PI/2);
                double turn = tr +Math.PI/2;
                if ( Math.abs(turn1) > Math.abs(turn2) ) {
                    turn = tr - Math.PI/2;
                }
//                Point horming = new Point(Util.calcPoint(turn,50).add(ctx.my));

//                Point horming = Util.calcPoint(tr+turn,approaching).add(ctx.my);
                Point horming = Util.calcPoint(tr,approaching).add(Util.calcPoint(turn,50)).add(ctx.my);

//logger.log("%2.2f , %2.2f", Math.toDegrees(turn1),Math.toDegrees(turn2));
//                if ( ctx.destination != null ) {
//                    double trd = ctx.destination.calcRadians(ctx.my);
//                    double tdd = ctx.destination.calcDistance(ctx.my);
//                    horming.add(Util.calcPoint(trd,0-approaching));
//                }
                ctx.GT = new TimedPoint(horming,ctx.my.time);
            }else if ( lockOnTarget != null && ctx.isMoveMode(ctx.MODE_MOVE_LOCKON2 )) {
                double fullDistance = Util.fieldFullDistance;
                double tr = ctx.my.calcRadians(lockOnTarget);
                double td = ctx.my.calcDistance(lockOnTarget);
                Point horming = Util.calcPoint(tr,(td-fullDistance)/LOCKON_APPROACH).add(ctx.my);
                ctx.GT = new TimedPoint(horming,ctx.my.time);
            }else{
                ctx.GT = null;
            }
        Point dst = movingBase();
        setDestination(dst);
    }    


    @Override
    protected void cbGun() {
        if ( ctx.isGunMode(ctx.MODE_GUN_LOCKON) ) {
            lockOn(ctx.lockonTarget);
        }
    }
    @Override
    protected void cbRadar() {
        if ( ctx.isRadarMode(ctx.MODE_RADAR_LOCKON)) {
            radarLockOn(ctx.lockonTarget);
        }
        if ( ctx.isRadarMode(ctx.MODE_RADAR_SEARCH) && ! (this instanceof Droid) ) {
            boolean isAllScan = false;
            if ( enemyMap.entrySet().size() >= ctx.others ) {
                isAllScan = true;
                for ( Map.Entry<String,Enemy> e : enemyMap.entrySet() ) {
                    if ( ! e.getValue().scanned && e.getValue().time != 0 ) { // not scanned &&not dead
                        isAllScan = false;
                        break;
                    }
                }
            }
            if ( ctx.curRadarTurnRemaining == 0.0 || isAllScan ) {
                ctx.toggleRadarTowards();
                doTurnRadarRight(6*Util.radarTurnSpeed()*ctx.radarTowards);
                doTurnGunRight(6*Util.gunTurnSpeed()*ctx.radarTowards);
                for ( Map.Entry<String,Enemy> e : enemyMap.entrySet() ) {
                    e.getValue().scanned = false;
                }
            }
        }
    }

    @Override
    protected void cbFiring(){
        if ( ctx.isFireMode(ctx.MODE_FIRE_AUTO) ) {
            firing(1,0);
        }
    }
    
    protected List<MoveType> initalMoveTypeList(){
        List<MoveType> moveTypeList = new ArrayList<>();
        MoveType moveType = new MoveType(MoveType.TYPE_UNKNOWN);
        moveType.score = -1; // 
        moveTypeList.add(new MoveType(moveType));
        moveType = new MoveType(MoveType.TYPE_PINPOINT);
        moveType.score = 0.001; // Initial type (will be overrided by first hit!!)
        moveTypeList.add(new MoveType(moveType));
        moveType = new MoveType(MoveType.TYPE_INERTIA_FIRST);
        moveTypeList.add(new MoveType(moveType));
        moveType = new MoveType(MoveType.TYPE_INERTIA_CENTER);
        moveTypeList.add(new MoveType(moveType));
        moveType = new MoveType(MoveType.TYPE_INERTIA_LAST);
        moveTypeList.add(new MoveType(moveType));
        moveType = new MoveType(MoveType.TYPE_ACCURATE_FIRST);
        moveTypeList.add(new MoveType(moveType));
        moveType = new MoveType(MoveType.TYPE_ACCURATE_CENTER);
        moveTypeList.add(new MoveType(moveType));
        moveType = new MoveType(MoveType.TYPE_ACCURATE_LAST);
        moveTypeList.add(new MoveType(moveType));
        return moveTypeList;
    }

    @Override
    protected Enemy cbScannedRobot(Enemy enemy) {
        enemy.scanned = true;
        Enemy constEnemy = super.cbScannedRobot(enemy);
        if ( constEnemy == null ) {
            return null;
        }
        ctx.nextEnemyMap.put(constEnemy.name, new Enemy(constEnemy));
        if ( ! isTeammate(enemy.name)) {
            if ( ! enemyLog.containsKey(enemy.name)) {
                enemyLog.put(enemy.name,new HashMap<Long,Enemy>());
            }
            enemyLog.get(enemy.name).put(enemy.time,new Enemy(constEnemy));
        }
        
        if ( ! shotTypeMap.containsKey(enemy.name) ) {
            shotTypeMap.put(enemy.name,initalMoveTypeList());
        }
        if ( ! aimTypeMap.containsKey(enemy.name) ) {
            aimTypeMap.put(enemy.name,initalMoveTypeList());
        }
        Enemy prevR = enemyMap.get(enemy.name);
        if ( prevR != null ) {
            // Aimed
            if ( (prevR.energy - enemy.energy) >= 0.1 && (prevR.energy - enemy.energy) <= 3 ) {
                enemyBullet(prevR,enemy);
            }
        }
        
        return constEnemy;
    }
    
    
    @Override
    protected void cbRobotDeath(RobotDeathEvent e) {
        Enemy nextEnemy = getNextEnemy(e.getName());
        if ( nextEnemy != null ) {
            nextEnemy.time = 0;
        }
    }
    /*
     * Bullets 
     */
    @Override
    protected void addBulletInfo(BulletInfo bulletInfo) {
        super.addBulletInfo(bulletInfo);
        ctx.nextBulletList.put(bulletInfo.bulletName,new BulletInfo(bulletInfo));
    }
    @Override
    protected void removeBulletInfo(String key){
        super.removeBulletInfo(key);
        ctx.nextBulletList.remove(key);
    }

    private void impactEnemyBulletInfo(String key){
        broadcastMessage(new CancelEnemyBalletEvent(key));
        removeEnemyBulletInfo(key);
    }
    protected void addEnemyBulletInfo(BulletInfo bulletInfo) {
        logger.fire2("SHOT(enemy): %s",bulletInfo.bulletName);
        enemyBulletList.put(bulletInfo.bulletName,bulletInfo);
        ctx.nextEnemyBulletList.put(bulletInfo.bulletName,new BulletInfo(bulletInfo));
    }
    protected void removeEnemyBulletInfo(String key){
        logger.fire2("IMPACT(enemy): %s",key);
        enemyBulletList.remove(key);
        ctx.nextEnemyBulletList.remove(key);
    }

    @Override
    protected Map.Entry<String, BulletInfo> cbBulletMissed(BulletMissedEvent e) {
        Map.Entry<String,BulletInfo> entry = super.cbBulletMissed(e);
        if ( entry != null ) {
            reEvalShot(entry.getValue());
        }
        return entry;
    }

    protected void reEvalShot(BulletInfo info) {
        // TODO: AIMING estimate
        long deltaTime = ctx.my.time - info.src.time;
        // TODO: prevMy
        RobotPoint prevMy = logMy(info.src.time);
        // Map<Long,Enemy> histEnemies = enemyLog.get(info.targetName);
        List<MoveType> aimTypeList = aimTypeMap.get(info.targetName);
        for (MoveType moveType : aimTypeList) {
            // double tankWidthRadians = Math.asin((Util.tankWidth/2)/distance);

            if (moveType.isTypeUnknown()) {
            } else {
                Pair<Long, Double> shot = calcShot(moveType, prevMy, info.src, info.src.velocity);
                MovingPoint bulletPoint = new MovingPoint(info.src);
                bulletPoint.headingRadians = shot.second;
                bulletPoint.heading = Math.toDegrees(shot.second);
                // Validate on history
                RobotPoint prevTarget = null;
                double closest = Util.fieldFullDistance;
                double closestDistance = Util.fieldFullDistance;
                double bulletDistance = 0;
                for (long i = info.src.time; i <= ctx.my.time; i++) {
                    RobotPoint target = logEnemy(info.targetName, i);
                    if (target == null) {
                        // No data 
                        prospectNextRobot(prevTarget, moveType, 1);
                        target = prevTarget;
                    }
                    prevTarget = target;
                    if (prevTarget == null) {
                        continue;
                    }
                    bulletPoint.inertia(1);

                    double d = bulletPoint.calcDistance(target);
                    if (d < (Util.tankWidth / 2)) { // hit 
                        closest = 0.0;
                        closestDistance = bulletDistance;
                        break;
                    }
                    if (d < closest) {
                        closest = d;
                        closestDistance = bulletDistance;
                    }
                    bulletDistance += info.src.velocity;
                }
                // Closest will nearly equals right angle with the bullet line. 
                double diffRadians = closest / closestDistance; // So use sin
                diffRadians = (Math.abs(diffRadians) < ENEMY_BULLET_DIFF_THRESHOLD) ? diffRadians : BULLET_DIFF_THRESHOLD;
                moveType.updateScore(Math.PI / 2 - diffRadians);
                logger.prospect4("AIMTYPE : %d  degree: %2.2f => %2.2f = %2.2f", moveType.type, closest, Math.toDegrees(diffRadians), Math.toDegrees(moveType.score));
            }
        }
        broadcastMessage(new AimTypeEvent(info.targetName, aimTypeList));
    }
    
    @Override
    protected Map.Entry<String, BulletInfo> cbBulletHit(BulletHitEvent e) {
        Map.Entry<String,BulletInfo> entry = super.cbBulletHit(e);
        Bullet bullet = e.getBullet();
        Point dst = new Point(bullet.getX(),bullet.getY());
        for ( Map.Entry<String,BulletInfo> ebi : enemyBulletList.entrySet() ) {
            BulletInfo bulletInfo = ebi.getValue();
            if ( e.getTime() == bulletInfo.src.time && bulletInfo.src.calcDegree(dst) < Util.tankSize ) {
                logger.fire2("CANCEL BULLET() %s : %s ", dst,bulletInfo.src);
                impactEnemyBulletInfo(bulletInfo.bulletName);
                break;
            }
        }
        if ( entry != null ) {
            reEvalShot(entry.getValue());
        }
        return entry;
    }

    @Override
    protected void cbHitByBullet(HitByBulletEvent e) {
        long time = e.getTime();
        Bullet bullet = e.getBullet();
        double bulletVelocity = e.getVelocity();
        double bulletRadians = e.getHeadingRadians();
        String enemyName = e.getName();
        Map.Entry<String,BulletInfo> entry = Util.calcBulletSrc(ctx.my.time,bullet,enemyBulletList);
        if ( entry != null ) {
            BulletInfo info = entry.getValue();
            impactEnemyBulletInfo(entry.getKey());
        // TODO: hit by bullet
            long deltaTime = ctx.my.time-info.src.time;
            // TODO: prevMy
            // RobotPoint prevMy = prevMy(deltaTime);
            RobotPoint prevMy = logMy(info.src.time);
            double distance = info.src.calcDistance(prevMy);
            
            List<MoveType> shotTypeList = shotTypeMap.get(enemyName);
            for ( MoveType moveType : shotTypeList ) {
                double tankWidthRadians = Math.asin((Util.tankWidth/2)/distance);
                
                if ( moveType.isTypeUnknown() ) {
                }else{
                    double shotRadians = calcShot(moveType,prevMy,info.src,bulletVelocity,deltaTime).second;
                    double diffRadians = Util.calcTurnRadians(bulletRadians,shotRadians);
                    diffRadians = (Math.abs(diffRadians)<ENEMY_BULLET_DIFF_THRESHOLD)?diffRadians:ENEMY_BULLET_DIFF_THRESHOLD;
                    double correctedRadians = Math.abs(diffRadians) - Math.abs(tankWidthRadians);
                    correctedRadians = (correctedRadians<0)?0:correctedRadians;
                    moveType.updateScore(Math.PI/2-correctedRadians);
                    logger.prospect4("TYPE : %d  degree: %2.2f => %2.2f = %2.2f",moveType.type,Math.toDegrees(diffRadians), Math.toDegrees(correctedRadians),Math.toDegrees(moveType.score));
                }
            }
            broadcastMessage(new ShotTypeEvent(enemyName, shotTypeList));

        }
    }
    
    protected void enemyBullet(Enemy prev,Enemy enemy){

        Enemy cpPrev = new Enemy(prev);
        prospectNextEnemy(cpPrev);
        cpPrev.time ++;
        // Detect collsion
        for ( Map.Entry<String,BulletInfo> entry : enemyBulletList.entrySet() ) {
            BulletInfo info =  entry.getValue();
            if ( info.src.time == cpPrev.time && info.src.calcDistance(cpPrev) <= Util.tankSize * 1.5 ) {
                logger.fire4("ENEMY(collision): %s = %s dist(%2.2f)", info.src,cpPrev , info.src.calcDistance(cpPrev));
                removeEnemyBulletInfo(entry.getKey());
                return;// Maybe collision
                // TODO: Cannot judge collision with teammate & myself.Maybe should treat only myself and receive it from teammate
                // 
            }
        }
        // Detect wall
        for ( long i = prev.time; i < enemy.time; i++  ) {
            boolean isMove = prospectNextEnemy(cpPrev);
            if ( ! isMove ) {
                logger.fire4("ENEMY(wall): %s ", cpPrev);
                return; // Maybe hit wall
            }
        }
        // Bullet src
        cpPrev = new Enemy(prev);
        prospectNextEnemy(cpPrev);
        cpPrev.time++;
        MovingPoint src = cpPrev;

        if ( ! isTeammate(enemy.name) ) {
            // TODO: prevMy
            // RobotPoint prevMy = prevMy(ctx.my.time-src.time+1);
            RobotPoint prevMy = logMy(src.time-1); // Detect enemy-firing after one turn from actual.
            double distance = src.calcDistance(prevMy);
            MoveType shotType =MoveType.getMoveTypeByScore(shotTypeMap.get(enemy.name));
            double bulletVelocity = Util.bultSpeed(prev.energy-enemy.energy);
            logger.prospect2("ENEMY(fire): %d : %s(%2.2f)", shotType.type,Util.bultPower(bulletVelocity), Util.bultSpeed(prev.energy-enemy.energy));
            double radians = calcShot(shotType,prevMy,src,bulletVelocity).second; //

            src.headingRadians = radians;
            src.heading  = Math.toDegrees(radians);
            src.velocity = bulletVelocity;
            BulletInfo bulletInfo = new BulletInfo(enemy.name,name,distance,src);
            addEnemyBulletInfo(bulletInfo);
        }
    }    
    
    /*
     * MODE control
     */
    private void normalMode(){
        if ( ! ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
            this.setMoveMode(ctx.MODE_MOVE_AUTO);
        }
        if ( ! ctx.isGunMode(ctx.MODE_GUN_MANUAL )) {
            this.setGunMode(ctx.MODE_GUN_AUTO);
        }
        if ( ! ctx.isRadarMode(ctx.MODE_RADAR_MANUAL )) {
            this.setRadarMode(ctx.MODE_RADAR_SEARCH);
        }
    }

    protected final void setMoveMode( int m ){
        if ( ctx.modeMove == m ) {
            return;
        }
        ctx.modeMove = m;
        logger.ctrl1("CHANGE MOVE MODE: %d => %d", ctx.modeMove , m);
        changeMoveMode();
    }
    protected void changeMoveMode(){
    }
    protected final void setGunMode( int m ){
        if ( ctx.modeGun == m ) {
            return;
        }
        ctx.modeGun = m;
        logger.ctrl1("CHANGE GUN  MODE: %d => %d", ctx.modeGun , m);
        changeGunMode();
    }
    protected void changeGunMode(){
    }
    protected final void setRadarMode( int m ){
        if ( ctx.modeRadar == m ) {
            return;
        }
        ctx.modeRadar = m;
        logger.ctrl1("CHANGE RADAR MODE: %d => %d", ctx.modeRadar , m);
        changeRadarMode();
    }
    protected void changeRadarMode(){
        if ( ctx.isRadarMode(ctx.MODE_RADAR_SEARCH) && ! (this instanceof Droid) ) {
            doTurnRadarRight(3*Util.radarTurnSpeed()*ctx.radarTowards);
            doTurnGunRight(3*Util.gunTurnSpeed()*ctx.radarTowards);
        }
    }

    protected final void setFireMode( int m ){
        if ( ctx.modeFire == m ) {
            return;
        }
        ctx.modeFire = m;
        logger.ctrl1("CHANGE FIRE MODE: %d => %d", ctx.modeFire , m);
        changeFireMode();
    }
    protected void changeFireMode(){
    }
    protected final void setCustomMode( int m ){
        if ( ctx.modeCustom == m ) {
            return;
        }
        ctx.modeCustom = m;
        logger.ctrl1("CHANGE FIRE MODE: %d => %d", ctx.modeCustom , m);
        changeCustomMode();
    }
    protected void changeCustomMode(){
    }
    
    
    @Override
    public void run() {
//        setColors(new Color(255, 255, 150), new Color(255, 255, 150), new Color(255, 255, 150)); // body,gun,radar
//        this.setBulletColor(new Color(200,255,100));
        super.run();
    }

    @Override
    protected void paint(Graphics2D g) {
        if ( isPaint) {
            super.paint(g);
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(0.7f, 0.7f, 0, PAINT_OPACITY));
            drawRound(g, ctx.my.x, ctx.my.y, RANGE_RADAR_LOCKON * 2);

            Color stringColor = new Color(0, 1.0f, 0, PAINT_OPACITY);
            if (ctx.isRadarMode(ctx.MODE_RADAR_LOCKON)) {
                stringColor = new Color(1.0f, 0.7f, 0, PAINT_OPACITY);
            }
            if (Math.abs(ctx.my.velocity) < 7.5) {
                stringColor = new Color(
                        (float) stringColor.getRed() / 255.0f,
                        (float) stringColor.getGreen() / 255.0f,
                        0.7f,
                        PAINT_OPACITY);
            }
            if (ctx.gunHeat == 0.0) {
                stringColor = new Color(
                        0.7f,
                        (float) stringColor.getGreen() / 255.0f,
                        (float) stringColor.getBlue() / 255.0f,
                        PAINT_OPACITY);
            }
            g.setColor(stringColor);
            if (ctx.lockonTarget != null) {
                g.drawString(String.format("targ: %s", ctx.lockonTarget), (int) ctx.my.x - 20, (int) ctx.my.y - 40);
            }
            for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
                Enemy enemy = e.getValue();
                g.setColor(new Color(1.0f, 0.7f, 0, PAINT_OPACITY));
                if (isStale(enemy)) {
                    g.setColor(new Color(1.0f, 0.0f, 0, PAINT_OPACITY));
                }
                Point priority = Util.calcPoint(ctx.my.calcRadians(enemy), calcPriorityDistance(enemy)).add(ctx.my);
                g.drawLine((int) enemy.x, (int) enemy.y, (int) priority.x, (int) priority.y);
                g.drawString(String.format("%2.2f", calcPriorityDistance(enemy)), (int) enemy.x - 20, (int) enemy.y - 80);
//              g.drawString(String.format("( %2.2f , %2.2f )", enemy.x , enemy.y), (int) r.x - 20, (int) r.y- 45);
                double enemyDistance = ctx.my.calcDistance(enemy);
                double enemyBearing  = ctx.my.calcDegree(enemy);
                g.drawString(String.format("dist(degr): %2.2f(%2.2f)", enemyDistance,enemyBearing), (int) enemy.x - 20, (int) enemy.y - 50);
                g.drawString(String.format("head(velo): %2.2f(%2.2f)", enemy.heading, enemy.velocity), (int) enemy.x - 20, (int) enemy.y - 60);
                g.setColor(new Color(0.2f, 1.0f, 0.7f, PAINT_OPACITY));

                drawRound(g, enemy.x, enemy.y, 35);
                g.setColor(new Color(0.3f, 0.5f, 1.0f,PAINT_OPACITY));
                Enemy next = new Enemy(enemy);
                for (int i = 1; i < 20; i++) {
                    prospectNextEnemy(next);
                    drawRound(g, next.x, next.y, 2);
                }
            }
            // Bullet
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(1.0f, 0.5f, 0.5f, PAINT_OPACITY));
            for (Map.Entry<String, BulletInfo> e : enemyBulletList.entrySet()) {
                BulletInfo info = e.getValue();
                g.drawLine((int) info.src.x, (int) info.src.y, (int) (Math.sin(info.src.headingRadians) * Util.fieldFullDistance + info.src.x), (int) (Math.cos(info.src.headingRadians) * Util.fieldFullDistance + info.src.y));
                g.setStroke(new BasicStroke(4.0f));
                Point dst = Util.calcPoint(info.src.headingRadians, info.distance).add(info.src);
                drawRound(g, dst.x, dst.y, 5);
            }            
            g.setColor(new Color(0, 0, 0, PAINT_OPACITY));
            for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
                BulletInfo info = e.getValue();
                if (!info.owner.equals(name)) {
                    MovingPoint bullet = new MovingPoint(info.src);
                    for (int i = 0; i < BULLET_PROSPECT_TIME; i++) {
                        drawRound(g, bullet.x, bullet.y, 5);
                        bullet.inertia(1);
                    }
                }
            }
            g.setColor(new Color(0, 0, 0, PAINT_OPACITY));
            for (Map.Entry<String, BulletInfo> e : ctx.nextEnemyBulletList.entrySet()) {
                BulletInfo info = e.getValue();
                if (info.distance < ENEMY_BULLET_DISTANCE || ctx.enemies == 1) {
                    if (!info.owner.equals(name)) {
                        MovingPoint bullet = new MovingPoint(info.src);
                        for (int i = 0; i < ENEMY_BULLET_PROSPECT_TIME; i++) {
                            drawRound(g, bullet.x, bullet.y, 5);
                            bullet.inertia(1);
                        }
                    }
                }
            }


            g.setStroke(new BasicStroke(4.0f));
            if (ctx.lockOnPoint != null) {
                g.setColor(new Color(1.0f, 1.0f, 0, PAINT_OPACITY));
                drawRound(g, ctx.lockOnPoint.x, ctx.lockOnPoint.y, 5);
            }
            if (ctx.G != null) {
                g.setColor(new Color(0, 0, 0, PAINT_OPACITY));
                drawRound(g, ctx.G.x, ctx.G.y, 10);
            }
            if (ctx.GT != null) {
                g.setColor(new Color(1.0f, 1.0f, 1.0f, PAINT_OPACITY));
                drawRound(g, ctx.GT.x, ctx.GT.y, 10);
            }
        }
    }
}


//        int PAST = 40;
//            pattarnAvgMap.put("I", new HashMap<String,Pair<Long,Double>>());
//            pattarnAvgMap.put("A", new HashMap<String,Pair<Long,Double>>());
//            for ( int i = 1;i<=PAST;i++) {
//                pattarnAvgMap.put(String.valueOf(i), new HashMap<String,Pair<Long,Double>>());
//                pattarnPastMap.put(String.valueOf(i), new HashMap<String,Pair<Long,Double>>());
//            }
//        }
//        if ( ! pattarnAvgMap.get("I").containsKey(r.name) )  {
//            pattarnAvgMap.get("I").put(r.name, new Pair<>(0L,0.0));
//        }
//        if ( ! pattarnAvgMap.get("A").containsKey(r.name) )  {
//            pattarnAvgMap.get("A").put(r.name, new Pair<>(0L,0.0));
//        }
//
//        for ( int i = 1;i<=PAST;i++) {
//            if ( ! pattarnAvgMap.get(String.valueOf(i)).containsKey(r.name)) {
//                pattarnAvgMap.get(String.valueOf(i)).put(r.name, new Pair<>(0L,0.0));
//            }
//            if ( ! pattarnPastMap.get(String.valueOf(i)).containsKey(r.name)) {
//                pattarnPastMap.get(String.valueOf(i)).put(r.name, new Pair<>(0L,0.0));
//            }
//        }
//
//
//        if ( prevR != null ) {
//            List<MovingPoint> patternList = enemyPatternMap.get(r.name);
//            MovingPoint pt = new MovingPoint(r).diff(prevR);
//            patternList.add(0,pt);
//            // @@@
//            logger.LOGLV = Logger.LOGLV_SCAN;
//            {
//                Enemy rr = new Enemy(r);
//                Enemy p = new Enemy(prevR);
//                p.inertia(1);
//                rr.diff(p);
//                Pair<Long,Double> pp = pattarnAvgMap.get("I").get(r.name);
//                double distance = rr.calcDistance(new Point());
//                pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                logger.log("I : %2.2f / %d = %2.2f (%2.2f)",pp.second,pp.first,pp.second/pp.first,distance);
//            }
//            {
//                Enemy rr = new Enemy(r);
//                Enemy p = new Enemy(prevR);
//                p.prospectNext();
//                rr.diff(p);
//                Pair<Long,Double> pp = pattarnAvgMap.get("A").get(r.name);
//                double distance = rr.calcDistance(new Point());
//                pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                logger.log("A : %2.2f / %d = %2.2f (%2.2f)",pp.second,pp.first,pp.second/pp.first,distance);
//            }
//            {
//                for ( int N = 2; N <=PAST;N++) {
//                    if ( N < patternList.size() ) {
//                        MovingPoint diff = new MovingPoint();
//                        for ( int i = 2; i <=N; i++ ) {
//                            diff.add(patternList.get(i));
//                            diff.time = patternList.get(i).time;
//                        }
//                        diff.quot(N-1);
//
//                        Enemy rr = new Enemy(r);
//                        Enemy p = new Enemy(prevR);
//                        long diffTime = rr.time-diff.time;
//                        if ( diffTime > PAST) {
//                            break;
//                        }
//                        diff.time = 1;
//                        // logger.log("Diff(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f (%d)",N,diff.x,diff.y,diff.calcDistance(new Point()),diff.heading,diff.time - past.time);
//                        p.setDelta(diff);
//                        p.prospectNext();
//                        rr.diff(p);
//                        Pair<Long,Double> pp = pattarnAvgMap.get(String.valueOf(diffTime)).get(r.name);
//                        double distance = rr.calcDistance(new Point());
//                        pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                        logger.log("A%2d : %2.2f / %d = %2.2f (%2.2f)",diffTime,pp.second,pp.first,pp.second/pp.first,distance);
//                        // logger.log("Avrage(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f",N,rr.x,rr.y,rr.calcDistance(new Point()),rr.heading);
//
//                    }
//                }
//            }
//            {
//                for ( int N = 2; N <=PAST;N++) {
//                    if ( N < patternList.size() ) {
//
//                        MovingPoint diff = new MovingPoint(patternList.get(N));
//                        Enemy rr = new Enemy(r);
//                        Enemy p = new Enemy(prevR);
//                        long diffTime = rr.time-diff.time;
//                        if ( diffTime > PAST) {
//                            break;
//                        }
//                        diff.time = 1;
//                        // diff.diff(past).quot(diffTime);
//                        // logger.log("Diff(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f (%d)",N,diff.x,diff.y,diff.calcDistance(new Point()),diff.heading,diff.time - past.time);
//                        p.setDelta(diff);
//                        p.prospectNext();
//                        rr.diff(p);
//                        Pair<Long,Double> pp = pattarnPastMap.get(String.valueOf(diffTime)).get(r.name);
//                        double distance = rr.calcDistance(new Point());
//                        pp.second = (pp.first * pp.second+distance) / ++pp.first;
//                        logger.log("P%2d : %2.2f / %d = %2.2f (%2.2f)",diffTime,pp.second,pp.first,pp.second/pp.first,distance);
//                        // logger.log("Avrage(%d): (%2.2f,%2.2f) d: %2.2f h: %2.2f",N,rr.x,rr.y,rr.calcDistance(new Point()),rr.heading);
//
//                    }
//                }
//            }
//        }