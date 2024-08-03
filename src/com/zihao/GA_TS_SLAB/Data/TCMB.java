package com.zihao.GA_TS_SLAB.Data;

/**
 * Description: data structure of TCMB item
 */

public class TCMB {
    private final int op1;
    private final int op2;
    private final int timeConstraint;

    public TCMB(int op1, int op2, int timeConstraint) {
        this.op1 = op1;
        this.op2 = op2;
        this.timeConstraint = timeConstraint;
    }
    public int getOp1() {
        return op1;
    }
    public int getOp2(){
        return op2;
    }
    public double getTimeConstraint(){
        return timeConstraint;
    }
    @Override
    public String toString() {
        return "TCMB{" +
                "op1=" + op1 +
                ", op2=" + op2 +
                ", timeConstraint=" + timeConstraint +
                '}';
    }
}
