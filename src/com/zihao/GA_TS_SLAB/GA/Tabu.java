package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.util.Random;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

public class Tabu {

    private int maxIterations;
    private int tabuListSize;
    private ProblemSetting problemSetting;
    private Random random;

    public Tabu(int maxIterations, int tabuListSize) {
        this.maxIterations = maxIterations;
        this.tabuListSize = tabuListSize;
        this.problemSetting = ProblemSetting.getInstance();
        this.random = new Random();
    }
}
