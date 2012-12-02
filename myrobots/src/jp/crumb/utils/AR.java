/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.utils;

import java.util.List;

/**
 *
 * @author crumb
 */
public class AR extends Gradient {
    int N;
    public AR(int N) {
        this.N = N;
    }

    Double getX(int n,List<Double> listX) {
        Double x = 0.0;
        if ( listX.size() > n && n >= 0) {
            x = listX.get(n);
        }
        return x;
    }
    public Double calcXt(List<Double> listX, int head) {
        Double ret = params.get(0);
        for (int i = 1; i < params.size(); i++) {
            Double x = getX(head - i + 1, listX);
            Double a = params.get(i);
            Double r = x*a;
            ret += r;
            Logger.debug4("Xi(%d): , x(%d):%f , a(%d):%f = %f", (head + 1), head - i + 1, x, i, a, ret);
        }
        return ret;
    }

    public double formula() {
        double diffSum = 0;
        for (int i = 1; i < params.size(); i++) {
            Double xans = getX(i, listX);
            Double ans = calcXt(listX, i-1);
            Logger.debug4("Xi(%d): %f, calc(%d):%f", i, xans,i-1,ans);
            diffSum += Math.abs(xans - ans);
        }
        return diffSum;
    }
    List<Double> listX;

    public Double ar(List<Double> listX) {
        this.listX = listX;
        int m = N;
        if (listX.size() < N) {
            m = listX.size()-1;
        }
        System.out.println(listX.size() + " / " + m);
        adjustN(m);
        convergence();
        dumpList("AR", params);
        return calcXt(listX, listX.size() - 1);
    }
    static public void dumpList(String h,List<Double> list) {
        System.out.print(h+":");
        for(Double d : list) {
            System.out.print(d + " ");
        }
        System.out.println("");
    }
}
