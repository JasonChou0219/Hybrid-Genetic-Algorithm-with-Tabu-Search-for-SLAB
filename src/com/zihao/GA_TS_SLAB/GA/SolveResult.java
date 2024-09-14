package com.zihao.GA_TS_SLAB.GA;

public class SolveResult {
    private Schedule bestSchedule;
    private double diversity;
    private double noiseRatio;

    public SolveResult(Schedule bestSchedule, double diversity, double noiseRatio) {
        this.bestSchedule = bestSchedule;
        this.diversity = diversity;
        this.noiseRatio = noiseRatio;
    }

    public Schedule getBestSchedule() {
        return bestSchedule;
    }

    public double getDiversity() {
        return diversity;
    }

    public double getNoiseRatio() {
        return noiseRatio;
    }
}
