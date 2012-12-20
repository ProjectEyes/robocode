package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import robocode.Droid;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;



/**
 * Silver - a robot by (your name here)
 */
abstract public class CrumbRobot<T extends CrumbContext> extends BaseRobo<T> {
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
    protected static final long SELECT_POWER_RANGE_TIME = 12;

    // For gun
    protected  static final double DEFAULT_RANGE_RADAR_LOCKON = 1000;
    protected  double RANGE_RADAR_LOCKON = DEFAULT_RANGE_RADAR_LOCKON;

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


    @Override
    protected CrumbContext createContext(CrumbContext in) {
        if ( in == null ) {
            return new CrumbContext();
        }
        return new CrumbContext(in);
    }

    protected MoveType getBestAimType(RobotPoint target) {
        return new MoveType(MoveType.TYPE_ACCELERATION_FIRST);
    }
    
    @Override
    protected Enemy createEnemy(ScannedRobotEvent e) {
        return new Enemy(ctx.my, e);
    }    


    @Override
    protected boolean prospectNextEnemy(Enemy enemy) {
        MoveType aimType = getBestAimType(enemy);
        return prospectNextRobot(enemy,aimType,1);
    }

    // Should be factory...
    protected boolean prospectNextRobotPinPoint(RobotPoint robot,long term,ProspectContext context){
        return true;
    }
    protected boolean prospectNextRobotInertia(RobotPoint robot,long term,ProspectContext context){
        return robot.inertia(term);
    }
    protected boolean prospectNextRobotAcceleration(RobotPoint robot,long term,ProspectContext context){
        return robot.prospectNext(term);
    }
    protected boolean prospectNextRobotSimplePattern(RobotPoint robot,long term,ProspectContext context){
        throw new UnsupportedOperationException("[SimplePattern] Not supported yet");
    }
    protected boolean prospectNextRobotReactPattern(RobotPoint robot,long term,ProspectContext context){
        throw new UnsupportedOperationException("[SimplePattern] Not supported yet");
    }

    protected boolean prospectNextRobot(RobotPoint robot,MoveType moveType,long term) {
        return prospectNextRobot(robot, moveType, term ,null);
    }
    protected boolean prospectNextRobot(RobotPoint robot,MoveType moveType,long term,ProspectContext context) {
        if ( moveType.isTypePinPoint() || robot.energy == 0.0 ) {
            return prospectNextRobotPinPoint(robot,term,context);
        }else if ( moveType.isTypeInertia() ) {
            return prospectNextRobotInertia(robot,term,context);
        }else if ( moveType.isTypeAcceleration() ) {
            return prospectNextRobotAcceleration(robot,term,context);
        }else if ( moveType.isTypeSimplePattern()) {
            return prospectNextRobotSimplePattern(robot,term,context);
        }else if ( moveType.isTypeReactPattern()) {
            return prospectNextRobotReactPattern(robot,term,context);
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

    protected double powerLimit(double enemyEnergy,MoveType aimType) {
        double limit = ctx.my.energy / 10;
        double need = Util.powerByDamage(enemyEnergy);
        if ( need < limit ) {
            limit = need;
        }
        return limit;
    }

    protected static double POWER_LIMIT_ERROR = 0.5;
    protected static double POWER_LIMIT_ERROR_MIN = 0.3;
    protected Pair<Double,Double> calcFire(Enemy target,MoveType aimType,long deltaThreshold,long recentThreshold){
        if ( target.delta == null || target.delta.time > deltaThreshold || (ctx.my.timeStamp - target.timeStamp) > recentThreshold ) {
            return new Pair<>(0.0,Util.fieldFullDistance);
        }
        double maxPower = 0.0;
        double aimDistance = Util.fieldFullDistance;
        Enemy prospectTarget = new Enemy(target);
        ProspectContext prospectContext = new ProspectContext();
        double limit = powerLimit(target.energy,aimType);
        if ( limit == 0.0 ) {
            if ( ctx.my.calcDistance(target) > 90 ) { // Prevent ram bonus
                return new Pair<>(0.0,Util.fieldFullDistance);
            }
            limit = 3.0;
        }
        if ( isPaint ) {
            getGraphics().setStroke(new BasicStroke(4.0f));
            getGraphics().setColor(new Color(0,0,255));
            drawRound(getGraphics(),prospectTarget.x,prospectTarget.y,10);
        }
        double limitError = limit*POWER_LIMIT_ERROR;
        limitError = (limitError>POWER_LIMIT_ERROR_MIN)?limitError:POWER_LIMIT_ERROR_MIN;
        for ( int i = 1; i <= MAX_HIT_TIME; i++ ) {
            double d = Util.calcPointToLineRange(ctx.my,prospectTarget,ctx.curGunHeadingRadians);
            if ( d < (Util.tankWidth/2) ) { // crossing shot line
                double bultDistance = Util.calcPointToLineDistance(ctx.my,prospectTarget,ctx.curGunHeadingRadians);
                double power = decideBulletPowerFromDistance(bultDistance,i);
                if ( maxPower < power ) { // hit ? : power more than 0.0
                    logger.fire3("POWER(%s): (%2.2f) => (%2.2f)", target.name,maxPower,power);
                    if ( power > limit ) {
                        if ( power - limit > limitError ) {
                            maxPower = 0.0;
                            aimDistance = Util.fieldFullDistance;
                            logger.fire3("exceeded limit error %2.2f(%2.2f) => %2.2f", limit,limitError, power );
                            break;
                        }
                        logger.fire3("limit errors %2.2f(%2.2f) => %2.2f", limit,limitError, power );
                        maxPower = power;
                        aimDistance = bultDistance;
                        break;
                    }
                    maxPower = power;
                    aimDistance = bultDistance;
System.out.println("p:" + maxPower + " a:" + aimDistance + " i:" + i);
                }
            }
            prospectNextRobot(prospectTarget,aimType,1,prospectContext);
            if ( isPaint ) {
                getGraphics().setStroke(new BasicStroke(1.0f));
                getGraphics().setColor(new Color(0,0,255));
                drawRound(getGraphics(),prospectTarget.x,prospectTarget.y,10);
            }
        }
        return new Pair<>(maxPower,aimDistance);
    }
    protected void firing(long deltaThreshold,long recentThreshold){
        if ( ctx.gunHeat > 0.0 ) {
            return;
        }
        double maxPower = 0.0;
        double aimDistance = Util.fieldFullDistance;
        MoveType aimType = null;
        String targetName = "";
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy target = e.getValue();
            if ( ! isStale(target) && ! isTeammate(e.getKey()) ) {
                MoveType aimtype = getBestAimType(target);
                Pair<Double,Double> result = calcFire(target,aimtype,deltaThreshold,recentThreshold);
                if ( aimDistance > result.second ) {
                    maxPower = result.first;
                    aimDistance = result.second;
                    targetName = target.name;
                    aimType = aimtype;
                }
            }
        }

        if ( aimType != null && ! isTeammate(targetName) && maxPower > 0 ) {
            if ( ctx.enemies > 1 ) {
                normalMode();
            }
            aimType.updateAim();
            doFire(maxPower,aimDistance,targetName,aimType.type);
        }
    }
    
    private double selectPowerFromDistance(double targetEnergy,MoveType aimType,double distance) {
        double power = decideBulletPowerFromDistance(distance,SELECT_POWER_RANGE_TIME);
        double limit = powerLimit(targetEnergy,aimType);
        power = (limit<power)?limit:power;
        return (power==0.0)?0.01:power;
    }

    protected Pair<Long,Double> calcShot(MoveType moveType,RobotPoint target,Point src,double bulletVelocity){
        return calcShot(moveType, target, src, bulletVelocity,0);
    }
    protected Pair<Long,Double> calcShot(MoveType moveType,RobotPoint target,Point src,double bulletVelocity,long deltaTime){
        double distance = src.calcDistance(target);
        double retRadians = 0;
        long   retTime = 0;
        if ( moveType.isTypePinPoint() ) {
            retRadians = src.calcRadians(target);
            retTime    = (long)Math.ceil(Math.abs(distance/bulletVelocity));
        }else {
            // ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/velocity)))
            //  - Calc only fixed time ( hitted )
            //  - for prospecting time ( shoting ) =>  with updating distance each ticks.
            RobotPoint cpTarget = new RobotPoint(target);
            ProspectContext prospectContext = new ProspectContext();
            double hitArea;
            if ( moveType.isTypeFirst() ) {
                hitArea = 0;
            }else if ( moveType.isTypeCenter() ) {
                hitArea = (Util.tankWidth/2);                
            }else{
                throw new UnsupportedOperationException("Unknown type : " + moveType.type);
            }
            for ( retTime = 1 ; retTime <= ((deltaTime>0)?deltaTime:(long)Math.ceil(Math.abs(distance/bulletVelocity))) ; retTime++ ) {
                prospectNextRobot(cpTarget,moveType,1,prospectContext);
                distance = src.calcDistance(cpTarget);
                retRadians = src.calcRadians(cpTarget);
                if ( distance - Math.abs(bulletVelocity*retTime) < hitArea ) { // hit ?
                    logger.prospect4("TYPE(x%02x) : %2.2f  (%2.2f - %2.2f = %2.2f)" ,moveType.type,Math.toDegrees(retRadians),distance,Math.abs(bulletVelocity*retTime),distance - Math.abs(bulletVelocity*retTime));
                    break;
                }
            }
        }
        return new Pair<>(retTime,retRadians);
    }    
 
    protected void lockOn(String lockonTarget) {
        Enemy lockOnTarget = getNextEnemy(lockonTarget);
        if ( lockOnTarget == null ) {
            return;
        }
        MoveType aimType = getBestAimType(lockOnTarget);

        double gunTurn = 0;
        long gunTurnTime = 1;

        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            // prospect me while gun turn
            RobotPoint prospectMy  = new RobotPoint(ctx.my);
            prospectNextMy(prospectMy,gunTurnTime);
            // prospect target while gun turn
            Enemy prospectTarget = new Enemy(lockOnTarget);
            prospectNextRobot(prospectTarget, aimType, gunTurnTime);
            // 
            double distance = prospectMy.calcDistance(prospectTarget);
            double power = this.selectPowerFromDistance(lockOnTarget.energy,aimType,distance);
            double bulletVelocity = Util.bultSpeed(power);

            Pair<Long,Double> shot = calcShot(aimType,prospectTarget,prospectMy,bulletVelocity);
            long bulletTime = shot.first;
            double bulletRadians = shot.second;
            gunTurn = ctx.calcAbsGunTurn(Math.toDegrees(bulletRadians));
            long nextGunTurnTime = (long) Math.ceil(gunTurn / Util.gunTurnSpeed());
            if ( gunTurnTime == nextGunTurnTime) {
                ctx.lockOnPoint = Util.calcPoint(bulletRadians,bulletTime*bulletVelocity).add(prospectMy);
                break;
            }
            gunTurnTime = nextGunTurnTime;
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

    @Override
    protected void cbExtMessage(MessageEvent e) {
        Serializable event = e.getMessage();
        if ( event instanceof LockonEvent ) {
            LockonEvent ev = (LockonEvent)event;
            if ( ! ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
                this.setMoveMode(ctx.MODE_MOVE_LOCKON1);
            }
            ctx.setLockonTarget(ev.lockonTarget);
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
        doTurnRight(10);
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

        List<String> rmList = new ArrayList<>(20);
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( r.timeStamp != 0 && ctx.my.time-r.timeStamp > STALE_AS_DEAD) {
                rmList.add(e.getKey());
            }
        }
        for ( String name : rmList ) {
            ctx.nextEnemyMap.remove(name);
        }
    }
    protected static final long STALE_AS_DEAD = 30;
    @Override
    protected void cbProspectNextTurn(){
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy enemy = e.getValue();
            MoveType aimType = getBestAimType(enemy);
            prospectNextRobot(enemy, aimType, 1);
        }

        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            info.src.inertia(1);
        }

    }    
    
    private double calcPriority (Enemy e) {
        return ctx.my.calcDistance(e)*e.energy/221;
    }

    @Override
    protected void cbThinking() {
//        Enemy lockOnTarget = getNextEnemy(ctx.lockonTarget);
        Enemy lockOnTarget = null;
        if (isLeader || teammate.isEmpty() || ctx.nextEnemyMap.get(leader) == null ) {
            for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
                Enemy r = e.getValue();
                if (teammate.contains(r.name)) {
                    continue;
                }
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
            String lockonTarget = null;
            if (lockOnTarget != null) {
                if ( ! ctx.isMoveMode(ctx.MODE_MOVE_MANUAL )) {
                    this.setMoveMode(ctx.MODE_MOVE_LOCKON1);
                }
                lockonTarget = lockOnTarget.name;
            }
            ctx.setLockonTarget(lockonTarget);
            if (isLeader) {
                broadcastMessage(new LockonEvent(lockonTarget));
            }
        }else{
            lockOnTarget = ctx.nextEnemyMap.get(ctx.lockonTarget);
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
            if ( enemyMap.entrySet().size() >= ctx.others ) {
                isAllScan = true;
                for ( Map.Entry<String,Enemy> e : enemyMap.entrySet() ) {
                    if ( ! e.getValue().scanned && e.getValue().timeStamp != 0 ) { // not scanned &&not dead
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
                Enemy base = enemyMap.get(e.getKey());
                Enemy enemy = e.getValue();
                g.setColor(new Color(1.0f, 0.7f, 0, PAINT_OPACITY));
                if (isStale(enemy)) {
                    g.setColor(new Color(1.0f, 0.0f, 0, PAINT_OPACITY));
                }
                Point priority = Util.calcPoint(ctx.my.calcRadians(enemy), calcPriority(enemy)).add(ctx.my);
                g.drawLine((int) enemy.x, (int) enemy.y, (int) priority.x, (int) priority.y);
                double enemyDistance = ctx.my.calcDistance(enemy);
                double enemyBearing  = ctx.my.calcDegree(enemy);
                g.drawString(String.format("%s : %s", enemy.name, enemy), (int) base.x - 20, (int) base.y - 40);
                g.drawString(String.format("dist(degr): %2.2f(%2.2f)", enemyDistance,enemyBearing), (int) base.x - 20, (int) base.y - 50);
                g.drawString(String.format("head(velo): %2.2f(%2.2f)", enemy.heading, enemy.velocity), (int) base.x - 20, (int) base.y - 60);
                g.drawString(String.format("%2.2f", calcPriority(enemy)), (int) base.x - 20, (int) base.y - 70);
                g.setColor(new Color(0.2f, 1.0f, 0.7f, PAINT_OPACITY));
                drawRound(g, enemy.x, enemy.y, 35);

                g.setColor(new Color(0.3f, 0.5f, 1.0f,PAINT_OPACITY));
                Enemy next = new Enemy(enemy);
                MoveType aimType = getBestAimType(next);
                for (int i = 1; i < 20; i++) {
                    prospectNextRobot(next, aimType,1);
                    drawRound(g, next.x, next.y, 2);
                }
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

