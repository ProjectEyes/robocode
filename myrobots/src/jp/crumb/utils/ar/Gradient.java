/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils.ar;

import java.util.ArrayList;
import java.util.List;
import jp.crumb.utils.Logger;

/**
 *
 * @author crumb
 */
abstract public class Gradient {
    static final double DEFAULT_DELTA = 0.0000000001;
    static final double DEFAULT_INIT_ACCERARATOR = 0.1;
    static final double DEFAULT_THRESHOLD = 0.000000001;
    static final int    DEFAULT_LOOP_MAX = 10000;
    static final double DEFAULT_DECLEASE_ACCERARATE_THRESHILD = 1.0;
    static final double DEFAULT_IGNORE_PARAMS_THRESHILD = 1.01;
    static final double DEFAULT_INCREASE_ACCERARATOR = 1.1;
    static final double DEFAULT_DECLEASE_ACCERARATOR = 0.8;
    double DECLEASE_ACCERARATE_THRESHILD;
    double IGNORE_PARAMS_THRESHILD;
    double INCREASE_ACCERARATOR;
    double DECLEASE_ACCERARATOR;
    double DELTA;
    double INIT_ACCERARATE;
    double THRESHOLD;
    int LOOP_MAX;
    public Gradient() {
        DELTA = DEFAULT_DELTA;
        INIT_ACCERARATE= DEFAULT_INIT_ACCERARATOR;
        THRESHOLD = DEFAULT_THRESHOLD;
        LOOP_MAX = DEFAULT_LOOP_MAX;
        DECLEASE_ACCERARATE_THRESHILD = DEFAULT_DECLEASE_ACCERARATE_THRESHILD;
        IGNORE_PARAMS_THRESHILD = DEFAULT_IGNORE_PARAMS_THRESHILD;
        INCREASE_ACCERARATOR = DEFAULT_INCREASE_ACCERARATOR;
        DECLEASE_ACCERARATOR = DEFAULT_DECLEASE_ACCERARATOR;
    }
    List<Double> params; // [0] is the ERROR fixed
    public void resetN(List<Double> initParams ){
        params = initParams;
    }
    public void adjustN(int N){
        if ( params == null ) {
            params = new ArrayList<>();
        }
        for ( ;params.size() <= N; ) {
            params.add(0.0);
        }
        if ( params.size() > N+1 ) {
            params.subList(0, N+1);
        }
    }
    abstract double formula();
    public void convergence() {
        double accerarate = INIT_ACCERARATE;
        double cur = formula();
        double prev = cur;
        int n;
        for (n = 0; n < LOOP_MAX; n++) {
            List<Double> newParams = new ArrayList();
            for (int i = 0; i < params.size(); i++) {
                double d = params.get(i);
                params.set(i, d + DELTA);
                double ret = formula();
                params.set(i, d);
                newParams.add(d - ((ret - prev) / DELTA) * accerarate);
            }
            List<Double> backupPrames = params;
            params = newParams;
            cur = formula();
            Logger.debug3("dyw: (%f / %f , %f) : %s", cur,prev, accerarate,params.toString());
            
            if (Math.abs(cur - prev) < THRESHOLD) {
                break;
            }
            if (Math.abs(cur / prev) > DECLEASE_ACCERARATE_THRESHILD) {
                accerarate *= DECLEASE_ACCERARATOR;
            }
            if (Math.abs(cur / prev) > IGNORE_PARAMS_THRESHILD) { // rollback
                params = backupPrames;
                continue;
            }
            accerarate *= INCREASE_ACCERARATOR;
            prev = cur;
        }
        Logger.debug2("DYW(%d): (%f / %f , %f) : %s", n,cur,prev, accerarate,params.toString());

    }
}

