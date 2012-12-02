package jp.crumb;
//import java.util.Vector;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;
import jp.crumb.utils.AimType;
import jp.crumb.utils.DeltaMovingPoint;
import jp.crumb.utils.Enemy;
import jp.crumb.utils.Logger;
import jp.crumb.utils.Point;
import jp.crumb.utils.TimedPoint;
import jp.crumb.utils.Util;



/**
 * Silver - a robot by (your name here)
 */
public class SimpleFilre extends CrumbRobo {
    static final double PREPARE_LOCK_TIME=2;
    static final int MAX_HIT_TIME = 20; 

    int mode = MODE_NORMAL;

    // For move
    double WALL_WEIGHT = 160;
    double WALL_DIM = 1.8;
    double ENEMY_WEIGHT = 200;
    double ENEMY_DIM = 1.8;
    double G_WEIGHT = 25;
    double G_DIM = 1;
    double GT_WEIGHT = 25;
    double GT_DIM = 1;
    long   G_EXPIRE = 5;
    long   G_DISTANCE_THRESHIOLD = 80;
    double LOCKON_APPROACH = 4;
    
    TimedPoint G;
    TimedPoint GT;

    // For gun
    double RANGE_SHOT = 70;
    double RANGE_LOCKON = 150;
    double RANGE_CHASE = 250;
    double RANGE_RADAR_LOCKON = 400;

    Point lockOnPoint; // for view
    Enemy lockOnTarget;
    
    @Override
    protected void cbMoving(){
        if ( my.time < 10 ) {
            return;
        }
        if ( lockOnTarget != null ) {
            lockOnTarget = nextEnemyMap.get(lockOnTarget.name);
        }
        // Wall
        Point dst = new Point(my);   
        dst.diff(Util.getGrabity(my, new Point(Util.battleFieldWidth,my.y), WALL_WEIGHT,WALL_DIM));
        dst.diff(Util.getGrabity(my, new Point(0,my.y), WALL_WEIGHT,WALL_DIM));
        dst.diff(Util.getGrabity(my, new Point(my.x,Util.battleFieldHeight), WALL_WEIGHT,WALL_DIM));
        dst.diff(Util.getGrabity(my, new Point(my.x,0), WALL_WEIGHT,WALL_DIM));
        // Enemy
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            dst.diff(Util.getGrabity(my,e.getValue(), ENEMY_WEIGHT,ENEMY_DIM));
        }
        
        if ( G == null ) {
            G = new TimedPoint(Util.getRandomPoint(my,5),my.time);
        }
        
        double gDistance     = my.calcDistance(G);
        double nextGDistance = nextMy.calcDistance(G);
//        if ( distance < (Util.getBrakingDistance(my.velocity)+my.velocity) ) {
        if ( destination != null && (G_DISTANCE_THRESHIOLD > 80 || my.time - G.time > G_EXPIRE )) { 
//            if ( mode == MODE_NORMAL ) {
                double gr = my.calcRadians(destination);
                Point g = Util.calcPoint(gr,50).prod(-1).add(my);
                g = Util.getRandomPoint(g,55);
                G = new TimedPoint(g,my.time);
//            }else if ( mode == MODE_RADAR_LOCKON || lockOnTarget != null ){
            if ( mode == MODE_RADAR_LOCKON && lockOnTarget != null ){
                double tr = my.calcRadians(lockOnTarget);
                double td = my.calcDistance(lockOnTarget);
                double fullDistance = Util.calcPoint(Util.battleFieldWidth,Util.battleFieldHeight).calcDistance(new Point());
                Point random = Util.calcPoint(tr,(td-fullDistance)/LOCKON_APPROACH).add(my);
//                random = Util.getRandomPoint(G.add(random).quot(2),50);
                GT = new TimedPoint(random,my.time);
            }else{
                GT = null;
            }
        }

        if ( G != null ) {
            dst.diff(Util.getGrabity(my, G, G_WEIGHT,G_DIM));
        }
        if ( GT != null ) {
            dst.diff(Util.getGrabity(my, GT, GT_WEIGHT,GT_DIM));
        }
        
        setDestination(dst);

//        }
    }
    
    @Override
    protected void cbFiring(){
        if ( mode == MODE_RADAR_LOCKON && gunHeat == 0.0 ) {
            if ( curGunTurnRemaining == 0.0 ) {
                setMode(MODE_NORMAL);
                fire(3.0,0,"");
            }
        }
    }
    
    double selectPowerFromDistance(double distance) {
        if (distance < RANGE_SHOT) {
            return 3;
        } else if (distance < RANGE_LOCKON) {
            return 2;
        } else if (distance < RANGE_CHASE) {
            return 1;
        } else if (distance < RANGE_RADAR_LOCKON) {
            return 0.3;
        } else {
            return 0.0;
        }
    }
    
    void lockOn(Enemy lockOnTarget) {
        if (lockOnTarget == null) {
            return;
        }
        
        double dist = lockOnTarget.distance;
        double power = selectPowerFromDistance(dist);
        double gunTurn = 0;
        double allTime = dist / Util.bultSpeed(power)+1; // 1 = gunturn
        
        
        DeltaMovingPoint prospectMy  = new DeltaMovingPoint(my);
        prospectMy.prospectNext();
        
        for (int i = 0 ; i < MAX_CALC ; i++ ) {
            Enemy prospectTarget = new Enemy(lockOnTarget);
            prospectNext(prospectTarget,(int)Math.ceil(allTime));
            dist = nextMy.calcDistance(prospectTarget);
            power = this.selectPowerFromDistance(dist);
            double bultTime = dist / Util.bultSpeed(power);
            gunTurn = calcAbsGunTurn(nextMy.calcDegree(prospectTarget));
            double gunTurnTime = (long) Math.ceil(gunTurn / Util.gunTurnSpeed());
            // Todo:
            if (Math.abs(allTime - (bultTime + gunTurnTime)) < 1) { 
                lockOnPoint = prospectTarget;
                break;
            }
            allTime = bultTime + gunTurnTime;
        }
        setTurnGunRight(gunTurn);
    }
    void radarLockOn(Enemy lockOnTarget) {
        if (lockOnTarget == null || (my.time - lockOnTarget.time) > SCAN_STALE ) {
            setMode(MODE_NORMAL);
            return;
        }
        

        
        double radarTurn = calcAbsRadarTurn(lockOnTarget.bearing);
        if ( Math.abs(radarTurn) < Util.radarTurnSpeed() ) {
            double diffDegree = Util.calcTurn(curRadarHeading,lockOnTarget.bearing);
            double swingBearing = lockOnTarget.bearing + (Util.radarTurnSpeed()/2);
            if ( diffDegree != 0.0 ) {
                swingBearing = lockOnTarget.bearing + (Util.radarTurnSpeed()/2)*(Math.abs(diffDegree)/diffDegree);
            }
            radarTurn = calcAbsRadarTurn(swingBearing);
        }
        this.setTurnRadarRight(radarTurn);
    }

    private double limitRange(Enemy e ) {
        return e.distance;
    }
    static final double AIM_ENAGY_THRESHOLD = 10.0;
    static final int AIM_TIMES_THRESHOLD = 3;
    private double calcPriorityDistance (Enemy e) { 
        if ( e.energy == 0.0 ) {
            return 0; // High priority
        }
        if ( e.energy< AIM_ENAGY_THRESHOLD ) {
            return 10; // High priority
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
    protected void cbThinking() {
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            Enemy r = e.getValue();
            if ( lockOnTarget == null ){
                lockOnTarget = r;
                continue;
            }
            if ( (my.time - r.time) < SCAN_STALE ) {
                if ( calcPriorityDistance(lockOnTarget) > calcPriorityDistance(r) ){
                    lockOnTarget = r;
                }
            }
        }
        if ( lockOnTarget != null &&
                ( lockOnTarget.distance < RANGE_RADAR_LOCKON && 
                gunHeat/Util.gunCoolingRate < PREPARE_LOCK_TIME ||
                others == 1 || lockOnTarget.energy == 0) ) {
            this.setMode(MODE_RADAR_LOCKON);
            
            
        }else{
            this.setMode(MODE_NORMAL);
        }
        if ( mode == MODE_RADAR_LOCKON ) {
            lockOn(lockOnTarget);
            radarLockOn(lockOnTarget);
        }
    }
    
    int TOGGLE_RADAR_TOWARDS = 1;
    void setMode( int mode ){
        if ( this.mode == mode ) {
            return;
        }
        Logger.ctrl1("CHANGE MODE: %d => %d", this.mode , mode);
        this.mode = mode;
        if ( mode == MODE_NORMAL ) {
            setTurnRadarRight(3*Util.radarTurnSpeed()*TOGGLE_RADAR_TOWARDS);
            setTurnGunRight(3*Util.gunTurnSpeed()*TOGGLE_RADAR_TOWARDS); 
        }
    }

    @Override
    protected void cbRadarTurnComplete() {
        Logger.radar4("myRadarTurnComplete");
        if ( mode == MODE_NORMAL ) {
            TOGGLE_RADAR_TOWARDS *= -1;
            setTurnRadarRight(6*Util.radarTurnSpeed()*TOGGLE_RADAR_TOWARDS);
            setTurnGunRight(6*Util.gunTurnSpeed()*TOGGLE_RADAR_TOWARDS); 
        }
    }

    @Override
    public void run() {
        setColors(new Color(0, 0, 0), new Color(0, 0, 0), new Color(0, 0, 0)); // body,gun,radar
        this.setBulletColor(new Color(200,255,100));
        super.run();
    }

    @Override
    protected void paint(Graphics2D g) {
        super.paint(g);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(0.7f, 0.7f, 0, PAINT_OPACITY));
        drawRound(g, my.x, my.y, RANGE_RADAR_LOCKON * 2);

        Color stringColor = new Color(0,1.0f,0,PAINT_OPACITY);
        if (mode == MODE_RADAR_LOCKON) {
            stringColor = new Color(1.0f, 0.7f, 0,PAINT_OPACITY);
        } 
        if ( Math.abs(my.velocity) < 7.5 ) {
            stringColor = new Color(
                    (float)stringColor.getRed()/255.0f,
                    (float)stringColor.getGreen()/255.0f,
                    0.7f,
                    PAINT_OPACITY);
        } 
        if ( gunHeat == 0.0 ) {
            stringColor = new Color(
                    0.7f,
                    (float)stringColor.getGreen()/255.0f,
                    (float)stringColor.getBlue()/255.0f,
                    PAINT_OPACITY);
        } 
        g.setColor(stringColor);
        if ( lockOnTarget != null ) {
            g.drawString(String.format("targ: %s", lockOnTarget.name), (int) my.x - 20, (int) my.y- 40);
        }
        for (Map.Entry<String, Enemy> e : nextEnemyMap.entrySet()) {
            Enemy enemy = e.getValue();
            Point priority = Util.calcPoint(my.calcRadians(enemy),calcPriorityDistance(enemy)).add(my);
            g.setColor(new Color(1.0f, 0.7f, 0,PAINT_OPACITY));
            g.drawLine((int)enemy.x,(int)enemy.y, (int)priority.x,(int)priority.y);
            g.drawString(String.format("%2.2f", calcPriorityDistance(enemy)), (int) enemy.x - 20, (int) enemy.y- 90);
            
        }
                
        
        
        g.setStroke(new BasicStroke(4.0f));
        if ( lockOnPoint != null ) {
            g.setColor(new Color(1.0f, 1.0f, 0,PAINT_OPACITY));
            drawRound(g, lockOnPoint.x, lockOnPoint.y, 5);
        }
        if ( G != null ) {
            g.setColor(new Color(0, 0, 0,PAINT_OPACITY));
            drawRound(g,G.x,G.y,10);
        }
        if ( GT != null ) {
            g.setColor(new Color(1.0f, 1.0f, 1.0f,PAINT_OPACITY));
            drawRound(g,GT.x,GT.y,10);
        }
    }

    
}
