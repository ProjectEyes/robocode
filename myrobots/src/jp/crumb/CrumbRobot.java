package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.crumb.base.BaseRobot;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.MoveType;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.RobotPoint;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;
import robocode.Droid;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;



/**
 * Silver - a robot by (your name here)
 */
abstract public class CrumbRobot<T extends CrumbContext> extends BaseRobot<T> {
    @Override
    public void run() {
        super.run();
        setColors(new Color(255, 255, 150), new Color(255, 255, 150), new Color(255, 255, 150)); // body,gun,radar
        this.setBulletColor(new Color(255,255,255));
    }

    protected static final double PERFECT_SCORE = 100; // Distance
    protected static final int MAX_CALC = 5;

    protected static final double PREPARE_LOCK_TIME=2;
    protected static final int DEFAULT_MAX_HIT_TIME = 50;
    protected int MAX_HIT_TIME = DEFAULT_MAX_HIT_TIME;

//    protected static final long SELECT_POWER_RANGE_TIME = 17;
//    protected static final long SELECT_POWER_RANGE_TIME = 12;

    //
    protected static final double ENEMY_PRIORITY_DIM = 2;
    protected static final double ENEMY_PRIORITY_LEADER_BOOST = 300;

    // For gun
    protected  static final double DEFAULT_RANGE_RADAR_LOCKON = 1000;
    protected  double RANGE_RADAR_LOCKON = DEFAULT_RANGE_RADAR_LOCKON;

    protected static final double DEFAULT_WALL_WEIGHT = 500;
    protected static final double DEFAULT_WALL_DIM = 1.8;
    protected static final double DEFAULT_ENEMY_WEIGHT = 400;
    protected static final double DEFAULT_ENEMY_DIM = 1.8;
    protected static final double DEFAULT_MATE_WEIGHT = 600;
    protected static final double DEFAULT_MATE_DIM = 1.6;
    protected static final double DEFAULT_G_WEIGHT = 10;
    protected static final double DEFAULT_G_DIM = 1.1;
    protected static final double DEFAULT_GT_WEIGHT = 100;
    protected static final double DEFAULT_GT_DIM = 2.8;
    protected static final long   DEFAULT_G_EXPIRE = 5;
    protected static final long   DEFAULT_G_DISTANCE_THRESHIOLD = 20;
    protected static final double DEFAULT_LOCKON_APPROACH = 12;
    protected static final long   DEFAULT_BULLET_PROSPECT_TIME = 12;
    protected static final double DEFAULT_BULLET_WEIGHT = 1000;
    protected static final double DEFAULT_BULLET_DIM = 4;

    protected  double WALL_WEIGHT            = DEFAULT_WALL_WEIGHT;
    protected  double WALL_DIM               = DEFAULT_WALL_DIM;
    protected  double ENEMY_WEIGHT           = DEFAULT_ENEMY_WEIGHT;
    protected  double ENEMY_DIM              = DEFAULT_ENEMY_DIM;
    protected  double MATE_WEIGHT            = DEFAULT_MATE_WEIGHT;
    protected  double MATE_DIM               = DEFAULT_MATE_DIM;
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


    @Override
    protected CrumbContext createContext(CrumbContext in) {
        if ( in == null ) {
            return new CrumbContext();
        }
        return new CrumbContext(in);
    }

    protected MoveType getBestAimType(RobotPoint target) {
        return new MoveType(MoveType.TYPE_ACCELERATION);
    }
    
    @Override
    protected Enemy createEnemy(ScannedRobotEvent e) {
        return new Enemy(ctx.my, e);
    }    


    @Override
    protected boolean prospectNextEnemy(Enemy enemy) {
        MoveType aimType = getBestAimType(enemy);
        return prospectNextRobot(enemy,aimType,1).size() > 0;
    }

    // Should be factory...
    protected List<Point> prospectNextRobotPinPoint(RobotPoint robot,long term){
        List<Point> ret = new ArrayList<>((int)term);
        for (int i=0;i<term;i++) {
            ret.add(new Point(robot));
        }
        return ret;
    }
    protected List<Point> prospectNextRobotInertia(RobotPoint robot,long term){
        List<Point> ret = new ArrayList<>((int)term);
        for (int i=0;i<term;i++) {
            robot.inertia(1);
            ret.add(new Point(robot));
        }
        return ret;
    }
    protected List<Point> prospectNextRobotAcceleration(RobotPoint robot,long term){
        List<Point> ret = new ArrayList<>((int)term);
        for (int i=0;i<term;i++) {
            robot.prospectNext();
            ret.add(new Point(robot));
        }
        return ret;
    }
    protected List<Point> prospectNextRobotSimplePattern(RobotPoint robot,long term){
        throw new UnsupportedOperationException("[SimplePattern] Not supported yet");
    }
    protected List<Point> prospectNextRobotReactPattern(RobotPoint robot,long term){
        throw new UnsupportedOperationException("[ReactPattern] Not supported yet");
    }

    protected List<Point> prospectNextRobot(RobotPoint robot,MoveType moveType,long term) {
        if ( moveType.isTypePinPoint() || robot.energy == 0.0 ) {
            return prospectNextRobotPinPoint(robot,term);
        }else if ( moveType.isTypeInertia() ) {
            return prospectNextRobotInertia(robot,term);
        }else if ( moveType.isTypeAcceleration() ) {
            return prospectNextRobotAcceleration(robot,term);
        }else if ( moveType.isTypeSimplePattern()) {
            return prospectNextRobotSimplePattern(robot,term);
        }else if ( moveType.isTypeReactPattern()) {
            return prospectNextRobotReactPattern(robot,term);
        }else {
            throw new UnsupportedOperationException("Unknown MoveType : " + moveType.type);
        }
    }
    
    protected Enemy getNextEnemy(String name) {
        Enemy ret = ctx.nextEnemyMap.get(name);
        if ( isStale(ret) ) {
            return null;
        }
        return ret;
    }
    
    
    protected Point movingBase(){
        // Wall
        Point dst = new Point(ctx.my);
        dst.diff(Util.getGrabity(ctx.my, new Point(Util.battleFieldWidth,ctx.my.y), WALL_WEIGHT,WALL_DIM,1));
        dst.diff(Util.getGrabity(ctx.my, new Point(0,ctx.my.y), WALL_WEIGHT,WALL_DIM,1));
        dst.diff(Util.getGrabity(ctx.my, new Point(ctx.my.x,Util.battleFieldHeight), WALL_WEIGHT,WALL_DIM,1));
        dst.diff(Util.getGrabity(ctx.my, new Point(ctx.my.x,0), WALL_WEIGHT,WALL_DIM,1));
        // Mate
        for (Map.Entry<String, Enemy> e : ctx.nextMateMap.entrySet()) {
            dst.diff(Util.getGrabity(ctx.my,e.getValue(), MATE_WEIGHT,MATE_DIM,1));
        }
        // Enemy
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            if ( ! isStale(e.getValue() ) ) {
                dst.diff(Util.getGrabity(ctx.my,e.getValue(), ENEMY_WEIGHT,ENEMY_DIM,5));
            }
        }
        // Bullet
        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            if ( ! info.owner.equals(name) ){
                MovingPoint bullet = new MovingPoint(info.src);
                for ( int i = 0 ; i < BULLET_PROSPECT_TIME;i++) {
                    dst.diff(Util.getGrabity(ctx.my,bullet, BULLET_WEIGHT*info.threat,BULLET_DIM));
                    bullet.inertia(1);
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
    
    protected double powerLimit(RobotPoint enemy, MoveType aimType) {
        // TODO : limit
        double limit = 6;
        double need = Util.powerByDamage(enemy.energy);
        if ( need < limit ) {
            limit = need;
        }
        if (ctx.enemies == 1) {
            if ( ctx.my.energy < 10  && enemy.energy > ctx.my.energy * 2 ) {
                return 0.0;
            }
            if ( ctx.my.energy < 30 && enemy.energy < ctx.my.energy && enemy.energy > (ctx.my.energy - limit) ) {
                return 0.0;
            }
            if ( ctx.my.energy < 20 && enemy.energy * 2 < ctx.my.energy && enemy.energy * 2 > (ctx.my.energy - limit)) {
                return 0.1;
            }
            if ( ctx.my.energy < 20 && enemy.energy     > ctx.my.energy * 2 ) {
                return 0.1;
            }
            if ( (ctx.my.energy - limit) < 0.2 && enemy.energy >= 0.1 ) { // last 1 shot limitter
                return 0.0;
            }
        }
        return limit;
    }

    public Pair<Double,Double> calcCollisionDistance(MovingPoint p1 , MovingPoint p2) {
        double s1 =Math.sin(p1.headingRadians);
        double c1 =Math.cos(p1.headingRadians);
        double s2 =Math.sin(p2.headingRadians);
        double c2 =Math.cos(p2.headingRadians);
        double d2 = (p2.y * s1 - p1.y * s1 + p1.x * c1 - p2.x * c1) / (s2 * c1 - c2 * s1) ;
        if ( d2 < 0 ) {
            return new Pair<>(Double.POSITIVE_INFINITY,0.0);
        }
        double t2 = d2 / p2.velocity + p2.time;
        Point xPoint = Util.calcPoint(p2.headingRadians, d2).add(p2);
        double distance1 = xPoint.calcDistance(new MovingPoint(p1).inertia(t2 - p1.time));
        double d1 = p1.calcDistance(xPoint);
        if ( d1 < 0 ) {
            return new Pair<>(Double.POSITIVE_INFINITY,0.0);
        }
        double t1 = d1 / p1.velocity + p1.time;
        double distance2 = xPoint.calcDistance(new MovingPoint(p2).inertia(t1 - p2.time));
        if ( distance2 < distance1) {
            return new Pair<>(distance2,t1);
        }
        return new Pair<>(distance1,t2);
    }

    protected double calcPower(Enemy target,MoveType aimType, double distance){
        double powerFromScore = 3.0*aimType.score/PERFECT_SCORE;
        double powerFromDistance = 0.0;
        if ( distance < 80 ) {
            powerFromDistance = 3.0;
//@@@
        }else if ( distance < 200 ) {
            powerFromDistance = 3.0 - (distance - 100) / 100.0;
        }else if ( distance < 400 ) {
            powerFromDistance = 2.0 - (distance - 200) / 400.0;
        }else if ( distance < 700 ) {
            powerFromDistance = 1.5 - (distance - 400) / 200.0;
//        }else if ( distance < 180 ) {
//            powerFromDistance = 3.0 - (distance - 80) / 200.0;
//        }else if ( distance < 280 ) {
//            powerFromDistance = 2.5 - (distance - 180) / 400.0;
//        }else if ( distance < 380 ) {
//            powerFromDistance = 2.25 - (distance - 280) / 400.0;
//        }else if ( distance < 480 ) {
//            powerFromDistance = 2.00 - (distance - 380) / 400.0;
//        }else if ( distance < 580 ) {
//            powerFromDistance = 1.75 - (distance - 480) / 400.0;
//        }else if ( distance < 680 ) {
//            powerFromDistance = 1.50 - (distance - 580) / 400.0;
//        }else if ( distance < 780 ) {
//            powerFromDistance = 1.25 - (distance - 680) / 200.0;
//        }else if ( distance < 880 ) {
//            powerFromDistance = 0.75 - (distance - 780) / 150.0;
        }else{
            powerFromDistance = 0.1;
        }
        double power = (powerFromScore > powerFromDistance)? powerFromScore : powerFromDistance;

        double limit = powerLimit(target,aimType);
        if ( limit == 0.0 ) {
            if ( ctx.my.calcDistance(target) < 90 ) { // Prevent ram bonus
                limit = 3.0;
            }
        }
        return (limit < power) ? limit : power;
    }

    protected Pair<Double,Double> calcFire(Enemy target,MoveType aimType,long deltaThreshold,long recentThreshold){
        if ( target.delta == null || target.delta.time > deltaThreshold || (ctx.my.timeStamp - target.timeStamp) > recentThreshold ) {
            return new Pair<>(0.0,Util.fieldFullDistance);
        }
        double limit = calcPower(target, aimType,ctx.my.calcDistance(target));
        if ( limit == 0.0 ) {
            return new Pair<>(0.0,Util.fieldFullDistance);
        }
        long term = (long)Math.ceil(ctx.my.calcDistance(target)/Util.bultSpeed(limit)) + 10;
        // TODO: k-nearlest
        for ( int i = 0 ; i < MAX_CALC ; i++ ) {
            Enemy prospectTarget = new Enemy(target);
            double maxPower = 0.0;
            double aimDistance = Util.fieldFullDistance;
            long t = 0;
            for ( Point targetPoint : prospectNextRobot(prospectTarget,aimType,term) ){
                t++;
                if ( isPaint ) {
                    getGraphics().setStroke(new BasicStroke(2.0f));
                    getGraphics().setColor(new Color(0,0,255));
                    drawRound(getGraphics(),targetPoint.x,targetPoint.y,1);
                }
                double d = Util.calcPointToLineRange(ctx.my,targetPoint,ctx.curGunHeadingRadians);
                if ( d < (Util.tankWidth/2) ) { // crossing shot line
                    double bultDistance = Util.calcPointToLineDistance(ctx.my, targetPoint, ctx.curGunHeadingRadians);
                    double power = Util.bultPower( bultDistance/t );
                    if (power > limit) {
                        d = Util.calcPoint(ctx.curGunHeadingRadians,Util.bultSpeed(limit)*t).add(ctx.my).calcDistance(targetPoint);
                        if ( d >= (Util.tankWidth/2) ) { // crossing shot line
                            break;
                        }
                        power = limit;
                    }

                    if (maxPower < power) { // hit ? : power more than 0.0
                        logger.fire3("POWER(%s): (%2.2f) => (%2.2f)", target.name, maxPower, power);
                        // Check collision
                        MovingPoint bullet = new MovingPoint(
                                ctx.my.x, ctx.my.y,
                                ctx.my.time,
                                ctx.curGunHeadingRadians,
                                Util.bultSpeed(power));
                        boolean collision = false;
                        for (Map.Entry<String, BulletInfo> be : ctx.nextBulletList.entrySet()) {
//                        double distance = Util.calcCollisionDistance(bullet,be.getValue().src);
                            Pair<Double, Double> pair = calcCollisionDistance(bullet, be.getValue().src);
                            double distance = pair.first;
                            double xtime = pair.second;
                            MovingPoint xpoint = bullet.inertia(xtime - ctx.my.time);
                            if (distance < 25 && !xpoint.isLimit()) {
                                logger.fire3("COLLISION: [%2.2f] %2.2f (%2.2f) OWN:%s X:%s", power, pair.first, pair.second, be.getKey(), xpoint);
                                collision = true;
                                break;
                            }
                        }
                        if (collision) {
                            continue;
                        }
                        maxPower = power;
                        aimDistance = bultDistance;
                    }
                }
            }
           
            if ( maxPower == 0.0 ) {
                term += 10;
            }else {
                long nextTerm = (long)Math.ceil(aimDistance/Util.bultSpeed(maxPower));
                if ( Math.abs(nextTerm - term) < 10 ) {
                    return new Pair<>(maxPower,aimDistance);
                }
                term = nextTerm;
            }
            
        }
        return new Pair<>(0.0,Util.fieldFullDistance);
    }
    protected void firing(long deltaThreshold,long recentThreshold){
        if ( ctx.gunHeat > 0.0 ) {
            return;
        }
        double maxPower = 0.0;
        double aimDistance = Util.fieldFullDistance;
        MoveType aimType = null;
        Enemy target = null;
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            target = e.getValue();
            if ( ! isStale(target) ) {
                MoveType aimtype = getBestAimType(target);
                Pair<Double,Double> result = calcFire(target,aimtype,deltaThreshold,recentThreshold);
                if ( aimDistance > result.second ) {
                    maxPower = result.first;
                    aimDistance = result.second;
                    aimType = aimtype;
                }
            }
        }

        if ( aimType != null && maxPower > 0 ) {
            if ( ctx.enemies > 1 ) {
                normalMode();
            }
            aimType.updateAim();
            doFire(maxPower,aimDistance,target,aimType.type);
        }
    }
    
    protected Pair<Long,Double> calcShot(MoveType moveType,RobotPoint target,Point src,double bulletVelocity,long deltaTime){
        double distance = src.calcDistance(target);
        double retRadians = 0;
        long   retTime = 0;
        if ( moveType.isTypePinPoint() ) {
            retRadians = src.calcRadians(target);
            retTime    = (long)Math.ceil(distance/bulletVelocity);
        }else if ( moveType.isTypeUnknown() ) {
            retRadians = src.calcRadians(target) + Math.PI/6*(Math.random()-0.5);
            retTime    = (long)Math.ceil(distance/bulletVelocity);
        }else {
            // ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity)))
            //  - Calc only fixed time ( hitted )
            //  - for prospecting time ( shoting ) =>  with updating distance each ticks.
            double hitArea = (Util.tankWidth/2); 
            RobotPoint cpTarget = new RobotPoint(target);
            double closest = Util.fieldFullDistance;
            double radians = 0.0;
            long time = 0;
            for ( Point targetPoint : prospectNextRobot(cpTarget,moveType,deltaTime) ) {
                time++;
                distance = src.calcDistance(targetPoint);
                radians  = src.calcRadians(targetPoint);
                double diff = Math.abs(distance - bulletVelocity*time);
                if ( diff < hitArea && closest > diff ) { // hit ?
                    closest = diff;
                    retTime = time;
                    retRadians = radians;
                    logger.prospect4("TYPE(x%02x) : %2.2f  (%2.2f - %2.2f = %2.2f)" ,moveType.type,Math.toDegrees(retRadians),distance,Math.abs(bulletVelocity*retTime),diff);
//                    break;
                }
            }
//            if ( deltaTime == 0 ) {
//                RobotPoint cpTarget = new RobotPoint(target);
//                // TODO: prospect term for k-nearlest...
//                for ( retTime = 1 ; retTime <= Math.abs(distance/bulletVelocity) ; retTime++ ) {
//                    prospectNextRobot(cpTarget,moveType,1);
//                    distance = src.calcDistance(cpTarget);
//                    retRadians = src.calcRadians(cpTarget);
//                    if ( distance - Math.abs(bulletVelocity*retTime) < hitArea ) { // hit ?
//                        logger.prospect4("TYPE(x%02x) : %2.2f  (%2.2f - %2.2f = %2.2f)" ,moveType.type,Math.toDegrees(retRadians),distance,Math.abs(bulletVelocity*retTime),distance - Math.abs(bulletVelocity*retTime));
//                        break;
//                    }
//                }
//            }else {
//                RobotPoint cpTarget = new RobotPoint(target);
//                prospectNextRobot(cpTarget,moveType,deltaTime);
//                distance = src.calcDistance(cpTarget);
//                retRadians = src.calcRadians(cpTarget);
//            }
        }
        return new Pair<>(retTime,retRadians);
    }    
 
    protected void lockOn(String lockonTarget) {

        Enemy lockOnTarget = getNextEnemy(lockonTarget);
        if ( lockOnTarget == null ) {
            return;
        }
        MoveType aimType = getBestAimType(lockOnTarget);

        double gunTurnRadians = 0;
        long gunTurnTime = 1;

        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            // prospect me while gun turn
            RobotPoint prospectMy  = new RobotPoint(ctx.my);
            prospectNextMy(prospectMy,gunTurnTime);
            // prospect target while gun turn
            
            
//            Enemy prospectTarget = new Enemy(lockOnTarget);
//            prospectNextRobot(prospectTarget, aimType, gunTurnTime);
            // 
            double distance = prospectMy.calcDistance(lockOnTarget);
            double power = this.calcPower(lockOnTarget,aimType,distance);
            double bulletVelocity = Util.bultSpeed(power);
            long term = (long)Math.ceil(prospectMy.calcDistance(lockOnTarget)/bulletVelocity) + gunTurnTime + 10;
            Pair<Long,Double> shot = calcShot(aimType,lockOnTarget,prospectMy,bulletVelocity,term);
            if ( shot.first == term ) {
                term += 10;
                continue;
            }else {
                long nextTerm = shot.first + 10;
                long bulletTime = shot.first-gunTurnTime;
                double bulletRadians = shot.second;
                gunTurnRadians = ctx.calcAbsGunTurnRadians(bulletRadians);
                long nextGunTurnTime = (long) Math.ceil(Math.abs(gunTurnRadians) / Util.gunTurnSpeedRadians());
                if ( Math.abs(nextTerm - term) < 10 ) {
                    if ( gunTurnTime == nextGunTurnTime) {
                        ctx.lockOnPoint = Util.calcPoint(bulletRadians,bulletTime*bulletVelocity).add(prospectMy);
                        break;
                    }
                }else{
                    term = nextTerm;
                }
                gunTurnTime = nextGunTurnTime;
            }
        }
        doTurnGunRightRadians(gunTurnRadians);
    }
    protected void radarLockOn(String lockonTarget) {
        Enemy lockOnTarget = getNextEnemy(lockonTarget);
        if (lockOnTarget == null ) {
            normalMode();
            return;
        }
        double targetBearingRadians = ctx.my.calcRadians(lockOnTarget);
        double radarTurnRadians = ctx.calcAbsRadarTurnRadians(targetBearingRadians);
        if ( Math.abs(radarTurnRadians) < Util.radarTurnSpeedRadians() ) {
            double diffRadians = Util.calcTurnRadians(ctx.curRadarHeadingRadians,targetBearingRadians);
            double swingBearingRadians = targetBearingRadians + (Util.radarTurnSpeedRadians()/2);
            if ( diffRadians != 0.0 ) {
                swingBearingRadians = targetBearingRadians + (Util.radarTurnSpeedRadians()/2)*(Math.abs(diffRadians)/diffRadians);
            }
            radarTurnRadians = ctx.calcAbsRadarTurnRadians(swingBearingRadians);
        }
        this.doTurnRadarRightRadians(radarTurnRadians);
    }

    @Override
    protected void cbExtMessage(MessageEvent e) {
        Serializable event = e.getMessage();
        if ( event instanceof LockonEvent ) {
            LockonEvent ev = (LockonEvent)event;
            if ( ! ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
                this.setMoveMode(ctx.MODE_MOVE_LOCKON1);
            }
            ctx.setLeaderTarget(ev.lockonTarget);
            if ( ev.lockonTarget == null) {
                ctx.setLockonTarget(null);
            }else{
                Enemy lockOnTarget = calcLockOnTarget();
                ctx.setLockonTarget(lockOnTarget.name);
            }
        }
        
    }

    @Override
    protected void cbFirst() {
        setMoveMode(ctx.MODE_MOVE_AUTO);
        setGunMode(ctx.MODE_GUN_AUTO);
        setRadarMode(ctx.MODE_RADAR_SEARCH);
        setFireMode(ctx.MODE_FIRE_AUTO);
        // Todo: initial move (towards radar scan)
        doAhead(50);
        doTurnRightRadians(Util.turnSpeedRadians(0));
        super.cbFirst();
    }
    
    @Override
    protected void cbUnprospectiveNextTurn(){
        List<String> rmBullet = new ArrayList<>(50);
        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            if ( e.getValue().src.isOutOfField() ) {
                rmBullet.add(e.getKey());
            }
        }
        for( String n : rmBullet ) {
            removeBulletInfo(n);
        }

        List<String> rmMateList = new ArrayList<>(20);
        for (Map.Entry<String, Enemy> e : ctx.nextMateMap.entrySet()) {
            Enemy r = e.getValue();
//            if ( r.timeStamp != 0 && ctx.my.time-r.timeStamp > STALE_AS_DEAD) {
            if ( ctx.my.time-r.timeStamp > STALE_AS_DEAD) {
                rmMateList.add(e.getKey());
            }
        }
        for ( String name : rmMateList ) {
            ctx.nextMateMap.remove(name);
        }

        List<String> rmEnemyList = new ArrayList<>(20);
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
//            if ( r.timeStamp != 0 && ctx.my.time-r.timeStamp > STALE_AS_DEAD) {
            if ( ctx.my.time-r.timeStamp > STALE_AS_DEAD) {
                rmEnemyList.add(e.getKey());
            }
        }
        for ( String name : rmEnemyList ) {
            ctx.nextEnemyMap.remove(name);
        }
    }
    protected static final long STALE_AS_DEAD = 30;
    @Override
    protected void cbProspectNextTurn(){
        for (Map.Entry<String, Enemy> e : ctx.nextMateMap.entrySet()) {
            Enemy enemy = e.getValue();
            if ( enemy.time - enemy.timeStamp > 2 ) { // Receive data was ( Timestamp + 2 )
                enemy.prospectNext();
            }
        }
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy enemy = e.getValue();
            MoveType aimType = getBestAimType(enemy);
            prospectNextRobot(enemy, aimType, 1);
            enemy.heat -= Util.gunCoolingRate;
        }

        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            info.src.inertia(1);
        }

    }    

    private double calcPriority (Enemy e) {
        double distance = ctx.my.calcDistance(e);
        if ( ctx.isLeaderTarget(e.name) ) {
            distance = distance - ENEMY_PRIORITY_LEADER_BOOST;
        }
        if ( distance <= 0.0 ) {
            return 0;
        }
        return Math.pow(distance,ENEMY_PRIORITY_DIM)*e.energy/221;
    }

    protected Enemy calcLockOnTarget() {
        Enemy lockOnTarget = null;
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( isStale(r) ) {
                continue;
            }
            if (lockOnTarget == null) {
                lockOnTarget = r;
                continue;
            }
            if (calcPriority(lockOnTarget) > calcPriority(r)) {
                lockOnTarget = r;
            }
        }
        return lockOnTarget;
    }

    @Override
    protected void cbThinking() {
//        Enemy lockOnTarget = getNextEnemy(ctx.lockonTarget);
        Enemy lockOnTarget = null;
        if (isLeader || teammate.isEmpty() || ctx.nextMateMap.get(leader) == null ) {
            lockOnTarget = calcLockOnTarget();

            String lockOnTargetName = null;
            if (lockOnTarget != null) {
                if ( ! ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
                    this.setMoveMode(ctx.MODE_MOVE_LOCKON1);
                }
                lockOnTargetName = lockOnTarget.name;
            }
            ctx.setLockonTarget(lockOnTargetName);
            if (isLeader) {
                broadcastMessage(new LockonEvent(lockOnTargetName));
            }
        }else{
            lockOnTarget = getNextEnemy(ctx.lockonTarget);
        }
        if ( lockOnTarget != null ) {
            double distance = ctx.my.calcDistance(lockOnTarget);
            if (    ctx.enemies == 1 ||
                    (ctx.gunHeat/Util.gunCoolingRate) < PREPARE_LOCK_TIME ){
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
        
        if ( ctx.G == null ) {
            ctx.G = new TimedPoint(Util.getRandomPoint(ctx.my,5),ctx.my.time);
        }
        
        if ( ctx.destination != null && ( ctx.G.calcDistance(ctx.my) > G_DISTANCE_THRESHIOLD || ctx.my.time - ctx.G.time > G_EXPIRE) ) {
            double gr = ctx.my.calcRadians(ctx.destination);
            //Point g = Util.calcPoint(gr,50).prod(-1).add(ctx.my);
            Point g = Util.calcPoint(gr,10).prod(-1).add(ctx.my);
            g = Util.getRandomPoint(g,11);
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
            if ( ctx.lockonTarget != null ) {
                lockOn(ctx.lockonTarget);
            }
        }
    }
    @Override
    protected void cbRadar() {
        if ( ctx.isRadarMode(ctx.MODE_RADAR_LOCKON)) {
            radarLockOn(ctx.lockonTarget);
        }
        if ( ctx.isRadarMode(ctx.MODE_RADAR_SEARCH) && ! (this instanceof Droid) ) {
            boolean isAllScan = false;
            if ( enemyMap.entrySet().size() >= ctx.enemies ) {
                isAllScan = true;
                for ( Map.Entry<String,Enemy> e : enemyMap.entrySet() ) {
                    if ( ! e.getValue().scanned && e.getValue().timeStamp != 0 ) { // not scanned &&not dead
                        isAllScan = false;
                        break;
                    }
                }
            }
            if ( ctx.curRadarTurnRemainingRadians == 0.0 || isAllScan ) {
                ctx.toggleRadarTowards();
                doTurnRadarRightRadians(6*Util.radarTurnSpeedRadians()*ctx.radarTowards);
                doTurnGunRightRadians(6*Util.gunTurnSpeedRadians()*ctx.radarTowards);
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

    @Override
    protected Enemy cbScannedRobot(Enemy enemy) {
        enemy.scanned = true;
        Enemy constEnemy = super.cbScannedRobot(enemy);
        if ( constEnemy == null ) {
            return null;
        }
        
        ctx.nextEnemyMap.put(constEnemy.name, new Enemy(constEnemy));
        return constEnemy;
    }
    
    
    @Override
    protected void cbRobotDeath(RobotDeathEvent e) {
        super.cbRobotDeath(e);
        ctx.nextEnemyMap.remove(e.getName());
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
            doTurnRadarRightRadians(3*Util.radarTurnSpeedRadians()*ctx.radarTowards);
            doTurnGunRightRadians(3*Util.gunTurnSpeedRadians()*ctx.radarTowards);
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
    protected void paint(Graphics2D g) {
        if ( isPaint) {
            super.paint(g);
            g.setStroke(new BasicStroke(1.0f));
//            g.setColor(new Color(0.7f, 0.7f, 0, PAINT_OPACITY));
//            drawRound(g, ctx.my.x, ctx.my.y, RANGE_RADAR_LOCKON * 2);

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
//                g.drawString(String.format("targ: %s", ctx.lockonTarget), (int) ctx.my.x - 20, (int) ctx.my.y - 40);
            }
            for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
                Enemy base = enemyMap.get(e.getKey());
                Enemy enemy = e.getValue();
                g.setColor(new Color(1.0f, 0.7f, 0, PAINT_OPACITY));
                if (isStale(enemy)) {
                    g.setColor(new Color(1.0f, 0.0f, 0, PAINT_OPACITY));
                }
                Point priority = Util.calcPoint(ctx.my.calcRadians(enemy), calcPriority(enemy)).add(ctx.my);
                g.drawLine((int) enemy.x, (int) enemy.y, (int) priority.x, (int) priority.y);
//                double enemyDistance = ctx.my.calcDistance(enemy);
//                double enemyBearingRadians  = ctx.my.calcRadians(enemy);
//                g.drawString(String.format("%s : %s", enemy.name, enemy), (int) base.x - 20, (int) base.y - 40);
//                g.drawString(String.format("dist(degr): %2.2f(%2.2f)", enemyDistance,Math.toDegrees(enemyBearingRadians)), (int) base.x - 20, (int) base.y - 50);
//                g.drawString(String.format("head(velo): %2.2f(%2.2f)", Math.toDegrees(enemy.headingRadians), enemy.velocity), (int) base.x - 20, (int) base.y - 60);
//                g.drawString(String.format("%2.2f", calcPriority(enemy)), (int) base.x - 20, (int) base.y - 70);
                g.setColor(new Color(0.2f, 1.0f, 0.7f, PAINT_OPACITY));
                drawRound(g, enemy.x, enemy.y, 35);

                g.setColor(new Color(0.3f, 0.5f, 1.0f,PAINT_OPACITY));
                Enemy next = new Enemy(enemy);
                MoveType aimType = getBestAimType(next);
//                for (Point p : prospectNextRobot(next, aimType,20) ) {
//                    drawRound(g, p.x, p.y, 2);
//                }
            }
            // Bullet
            g.setColor(new Color(0, 0, 0, PAINT_OPACITY));
            g.setStroke(new BasicStroke(1.0f));
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

