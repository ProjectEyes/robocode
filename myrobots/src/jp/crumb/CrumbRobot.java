package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.Map;
import jp.crumb.base.BaseRobo;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.AimType;
import jp.crumb.utils.DeltaMovingPoint;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.MovingPoint;
import jp.crumb.utils.Pair;
import jp.crumb.utils.Point;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;
import robocode.Droid;
import robocode.MessageEvent;
import robocode.ScannedRobotEvent;



/**
 * Silver - a robot by (your name here)
 */
abstract public class CrumbRobot<T extends CrumbContext> extends BaseRobo<T> {
    protected static final double PREPARE_LOCK_TIME=2;
    protected static final int DEFAULT_MAX_HIT_TIME = 15; 
    protected int MAX_HIT_TIME = DEFAULT_MAX_HIT_TIME;

    protected static final int MODE_RADAR_SEARCH = 0x002;
    protected static final int MODE_RADAR_LOCKON = 0x001;
    protected static final int MODE_GUN_LOCKON   = 0x010;
    protected static final int MODE_CLOSE_FIRE   = 0x100;
    protected static final int MODE_NORMAL = MODE_RADAR_SEARCH;
    protected static final int MODE_LOCKON = MODE_RADAR_LOCKON | MODE_GUN_LOCKON;
    
    protected static final double AIM_ENAGY_THRESHOLD = 10.0;
    protected static final int AIM_TIMES_THRESHOLD = 3;

    protected static final long SELECT_POWER_RANGE = 17;

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
    protected static final double DEFAULT_GT_DIM = 2.5;
    protected static final long   DEFAULT_G_EXPIRE = 5;
    protected static final long   DEFAULT_G_DISTANCE_THRESHIOLD = 80;
    protected static final double DEFAULT_LOCKON_APPROACH = 12;
    protected static final long   DEFAULT_BULLET_PROSPECT_TIME = 12;
    protected static final double DEFAULT_BULLET_WEIGHT = 1000;
    protected static final double DEFAULT_BULLET_DIM = 4;

    
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
    

    // For gun
    protected  static final double DEFAULT_RANGE_RADAR_LOCKON = 1000;
    protected  double RANGE_RADAR_LOCKON = DEFAULT_RANGE_RADAR_LOCKON;

    
    
    @Override
    protected Enemy calcAbsRobot(ScannedRobotEvent e) {
        return new Enemy(ctx.my, e , Enemy.AIM_TYPE_ACCERARATE);
    }    
    
    @Override
    protected void prospectNextEnemy(Enemy enemy) {
        if ( enemy.aimType == Enemy.AIM_TYPE_ACCERARATE ) {
            enemy.prospectNext(ctx.my);
        }else if ( enemy.aimType == Enemy.AIM_TYPE_INERTIA ) {
            enemy.inertia(1);
            enemy.calcPosition(ctx.my);
        }else if ( enemy.aimType == Enemy.AIM_TYPE_DIFF_ERROR ) {
            
        }
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
        if ( ctx.G != null ) {
            dst.diff(Util.getGrabity(ctx.my, ctx.G, G_WEIGHT,G_DIM));
        }
        if ( ctx.GT != null ) {
            dst.diff(Util.getGrabity(ctx.my, ctx.GT, GT_WEIGHT,GT_DIM));
        }
        return dst;
    }
    
    @Override
    protected Point cbMoving(){
        if ( ctx.my.time < 10 ) {
            return null;
        }
        
        if ( ctx.G == null ) {
            ctx.G = new TimedPoint(Util.getRandomPoint(ctx.my,5),ctx.my.time);
        }
        
        double gDistance     = ctx.my.calcDistance(ctx.G);
        double nextGDistance = ctx.nextMy.calcDistance(ctx.G);
        if ( ctx.destination != null && (G_DISTANCE_THRESHIOLD > 80 || ctx.my.time - ctx.G.time > G_EXPIRE) ) { 
                double gr = ctx.my.calcRadians(ctx.destination);
                Point g = Util.calcPoint(gr,50).prod(-1).add(ctx.my);
                g = Util.getRandomPoint(g,55);
                ctx.G = new TimedPoint(g,ctx.my.time);
                Enemy lockOnTarget = getEnemy(ctx.lockonTarget);
            if ( lockOnTarget != null ){
                double tr = ctx.my.calcRadians(lockOnTarget);
                double td = ctx.my.calcDistance(lockOnTarget);
                double fullDistance = Util.fieldFullDistance;
                Point random = Util.calcPoint(tr,(td-fullDistance)/LOCKON_APPROACH).add(ctx.my);
                ctx.GT = new TimedPoint(random,ctx.my.time);
            }else{
                ctx.GT = null;
            }
        }
        return movingBase();
    }
    
    protected final double calcBulletPowerFromDistance(double distance,long time){
        return Util.bultPower( distance/time );
    }
    private Pair<Double,Double> calcFire(Enemy target,long deltaThreshold,long recentThreshold){
        if ( target.delta == null || target.delta.time > deltaThreshold || (ctx.my.time - target.time) > recentThreshold ) {
            return new Pair<>(0.0,Util.fieldFullDistance);
        }
        double maxPower = 0.0;
        double aimDistance = Util.fieldFullDistance;
        Enemy prospectTarget = new Enemy(target);
        for ( int i = 1; i <= MAX_HIT_TIME; i++ ) {
            double d = Util.calcPointToLineRange(ctx.my,prospectTarget,ctx.curGunHeadingRadians);
            if ( d < (Util.tankWidth/2) ) { // hit ?
                double bultDistance = Util.calcPointToLineDistance(ctx.my,prospectTarget,ctx.curGunHeadingRadians);
                double power = calcBulletPowerFromDistance(bultDistance,i);
                if ( maxPower < power ) {
                    logger.gun3("POWER(%s): (%2.2f) => (%2.2f)", target.name,maxPower,power);
                    maxPower = power;
                    aimDistance = bultDistance;
                }
            }
            prospectNextEnemy(prospectTarget);
        }
        return new Pair<>(maxPower,aimDistance);
    }
    protected void firing(long deltaThreshold,long recentThreshold){
        if ( ctx.gunHeat > 0.0 || isMode(MODE_CLOSE_FIRE)) {
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
                setMode(MODE_NORMAL);
            }
            fire(maxPower,aimDistance,targetName);
        }
    }
    
    @Override
    protected void cbFiring(){
        firing(1,0);
    }
    private double selectPowerFromDistance(double distance) {
        double power = calcBulletPowerFromDistance(distance,SELECT_POWER_RANGE);
        return (power==0.0)?0.01:power;
//        if (distance < RANGE_SHOT) {
//            return 3;
//        } else if (distance < RANGE_LOCKON) {
//            return 2;
//        } else if (distance < RANGE_CHASE) {
//            return 1;
//        } else if (distance < RANGE_RADAR_LOCKON) {
//            return 0.3;
//        } else {
//            return 0.0;
//        }
    }
    
    protected void lockOn(String lockonTarget) {
        Enemy lockOnTarget = getEnemy(lockonTarget);
        if ( lockOnTarget == null ) {
            return;
        }

        double distance = lockOnTarget.distance;
        double power = selectPowerFromDistance(distance);
        double gunTurn = 0;
        double allTime = distance / Util.bultSpeed(power)+1; // 1 = gunturn
        
        
        DeltaMovingPoint prospectMy  = new DeltaMovingPoint(ctx.my);
        prospectMy.prospectNext();
        
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
                break;
            }
            allTime = bultTime + gunTurnTime;
        }
        setTurnGunRight(gunTurn);
    }
    protected void radarLockOn(String lockonTarget) {
        Enemy lockOnTarget = getEnemy(lockonTarget);
        if (lockOnTarget == null ) {
            setMode(MODE_NORMAL);
            return;
        }
        double radarTurn = ctx.calcAbsRadarTurn(lockOnTarget.bearing);
        if ( Math.abs(radarTurn) < Util.radarTurnSpeed() ) {
            double diffDegree = Util.calcTurn(ctx.curRadarHeading,lockOnTarget.bearing);
            double swingBearing = lockOnTarget.bearing + (Util.radarTurnSpeed()/2);
            if ( diffDegree != 0.0 ) {
                swingBearing = lockOnTarget.bearing + (Util.radarTurnSpeed()/2)*(Math.abs(diffDegree)/diffDegree);
            }
            radarTurn = ctx.calcAbsRadarTurn(swingBearing);
        }
        this.setTurnRadarRight(radarTurn);
    }

//    private double limitRange(Enemy e ) {
//        return e.distance;
//    }
    
    private double calcPriorityDistance (Enemy e) { 
        if ( e.energy == 0.0 ) {
            return 0; // High priority
        }
        if ( e.energy< AIM_ENAGY_THRESHOLD ) {
            return e.distance/10; // High priority
        }
        AimType aimType = e.getAimType();
        if ( aimType.aim < AIM_TIMES_THRESHOLD || e.distance < aimType.hitrange ) {
            return e.distance; // In range
        }
        double hitrate = aimType.getHitRate();
        double diffrange = aimType.getHitRange();
        return e.distance + diffrange*(1-hitrate);
    }

    @Override
    protected void cbExtMessage(MessageEvent e) {
        Serializable event = e.getMessage();
        if ( event instanceof LockonEvent ) {
            LockonEvent ev = (LockonEvent)event;
            ctx.setLockonTarget(ev.lockonTarget);
        }
    }
    

    @Override
    protected void cbThinking() {
        Enemy lockOnTarget = getEnemy(ctx.lockonTarget);
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
                ctx.setLockonTarget(lockOnTarget.name);
                if (isLeader) {
                    broadcastMessage(new LockonEvent(ctx.lockonTarget));
                }
            }
        }
        if (lockOnTarget != null
                && (lockOnTarget.distance < RANGE_RADAR_LOCKON
                && ctx.gunHeat / Util.gunCoolingRate < PREPARE_LOCK_TIME
                || ctx.enemies == 1 || lockOnTarget.energy == 0)) {
            this.setMode(MODE_LOCKON);
        } else {
            this.setMode(MODE_NORMAL);
        }
        if ( isMode(MODE_RADAR_LOCKON)) {
            lockOn(ctx.lockonTarget);
        }
        if ( isMode(MODE_GUN_LOCKON) ) {
            radarLockOn(ctx.lockonTarget);
        }
    }
    
    protected final void addMode( int m ){
        if ( ! isMode(m) ) {
            setMode(m | ctx.mode);
        }
    }
    protected final void setMode( int m ){
        if ( ctx.mode == m ) {
            return;
        }
        ctx.mode = m;
        logger.ctrl1("CHANGE MODE: %d => %d", ctx.mode , m);
        changeMode();
    }

    int TOGGLE_RADAR_TOWARDS = 1;
    protected void changeMode(){
        if ( isMode(MODE_RADAR_SEARCH) && ! (this instanceof Droid) ) {
            setTurnRadarRight(3*Util.radarTurnSpeed()*TOGGLE_RADAR_TOWARDS);
            setTurnGunRight(3*Util.gunTurnSpeed()*TOGGLE_RADAR_TOWARDS); 
        }
    }
    protected final boolean isMode(int m) {
        return ((ctx.mode & m) != 0 );
    }

    
    @Override
    protected void cbRadarTurnComplete() {
        logger.radar4("myRadarTurnComplete");
        if ( isMode(MODE_RADAR_SEARCH) && ! (this instanceof Droid) ) {
            TOGGLE_RADAR_TOWARDS *= -1;
            setTurnRadarRight(6*Util.radarTurnSpeed()*TOGGLE_RADAR_TOWARDS);
            setTurnGunRight(6*Util.gunTurnSpeed()*TOGGLE_RADAR_TOWARDS); 
        }
    }

    @Override
    public void run() {
        logger.LOGLV = 0;
        super.run();
    }

    @Override
    protected void paint(Graphics2D g) {
        super.paint(g);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0.7f, 0.7f, 0, PAINT_OPACITY));
        drawRound(g, ctx.my.x, ctx.my.y, RANGE_RADAR_LOCKON * 2);

        Color stringColor = new Color(0,1.0f,0,PAINT_OPACITY);
        if ( isMode(MODE_RADAR_LOCKON)) {
            stringColor = new Color(1.0f, 0.7f, 0,PAINT_OPACITY);
        } 
        if ( Math.abs(ctx.my.velocity) < 7.5 ) {
            stringColor = new Color(
                    (float)stringColor.getRed()/255.0f,
                    (float)stringColor.getGreen()/255.0f,
                    0.7f,
                    PAINT_OPACITY);
        } 
        if ( ctx.gunHeat == 0.0 ) {
            stringColor = new Color(
                    0.7f,
                    (float)stringColor.getGreen()/255.0f,
                    (float)stringColor.getBlue()/255.0f,
                    PAINT_OPACITY);
        } 
        g.setColor(stringColor);
        if ( ctx.lockonTarget != null ) {
            g.drawString(String.format("targ: %s", ctx.lockonTarget), (int) ctx.my.x - 20, (int) ctx.my.y- 40);
        }
        for (Map.Entry<String, Enemy> e : ctx.nextEnemyMap.entrySet()) {
            Enemy enemy = e.getValue();
            g.setColor(new Color(1.0f, 0.7f, 0,PAINT_OPACITY));
            if ( isStale(enemy)  ) {
                g.setColor(new Color(1.0f, 0.0f, 0,PAINT_OPACITY));
            }
            Point priority = Util.calcPoint(ctx.my.calcRadians(enemy),calcPriorityDistance(enemy)).add(ctx.my);
            g.drawLine((int)enemy.x,(int)enemy.y, (int)priority.x,(int)priority.y);
            g.drawString(String.format("%2.2f", calcPriorityDistance(enemy)), (int) enemy.x - 20, (int) enemy.y- 80);
            
        }
        // Bullet
        g.setColor(new Color(0, 0, 0,PAINT_OPACITY));
        for (Map.Entry<String, BulletInfo> e : ctx.nextBulletList.entrySet()) {
            BulletInfo info = e.getValue();
            if ( ! info.owner.equals(name) ){
                MovingPoint bullet = new MovingPoint(info.src);
                for ( int i = 0 ; i < BULLET_PROSPECT_TIME;i++) {
                    drawRound(g,bullet.x,bullet.y,5);
                    bullet.inertia(1);
                }
            }
        }
        
        
        g.setStroke(new BasicStroke(4.0f));
        if ( ctx.lockOnPoint != null ) {
            g.setColor(new Color(1.0f, 1.0f, 0,PAINT_OPACITY));
            drawRound(g, ctx.lockOnPoint.x, ctx.lockOnPoint.y, 5);
        }
        if ( ctx.G != null ) {
            g.setColor(new Color(0, 0, 0,PAINT_OPACITY));
            drawRound(g,ctx.G.x,ctx.G.y,10);
        }
        if ( ctx.GT != null ) {
            g.setColor(new Color(1.0f, 1.0f, 1.0f,PAINT_OPACITY));
            drawRound(g,ctx.GT.x,ctx.GT.y,10);
        }
    }

    
}
