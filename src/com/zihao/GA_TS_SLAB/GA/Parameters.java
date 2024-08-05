package com.zihao.GA_TS_SLAB.GA;


/**
 * Description: Parameters needed for employ GA
 */
public final class Parameters {
    // Population size
    public static final int POP_NUM = 400;
//    public static final int POP_NUM = 2;
    // Weight of penalty on accumulative time constraint violation
    public static final double PENALTY_WEIGHT = 10.0;
    // Ratio of elist selection
    public static final double ELIT_RATIO = 0.1;
    // Probability  to trigger crossover
    public static final double CROSSOVER_RATE = 0.8;
    // Ratio of choosing POX of JBX
    public static final double OS_CROSSOVER_RATIO = 0.5;
    // Probability to trigger OS mutation of each position
    public static final double OS_MUTATION_RATE = 0.1;
    // Probability to trigger MS reassignment for half position
    public static final double MS_MUTATION_RATE = 0.4;

    // The ratio of reassignment of machine for MS
    public static final double MACHINE_MUTATION_RATIO = 0.5;
    // The max number of revolution times of the population
    public static final int MAX_GENERATION = 200;
//    public static final int MAX_GENERATION = 10;
    // The time limit of the running the algorithm
//    public static final int timeLimit = -1;


//    private final int maxT = 9;// tabu list length
//    private final int maxTabuLimit = 100;// maxTSIterSize = maxTabuLimit * (Gen / maxGen)
//    private final double pt = 0.05;// tabu probability
//    private final double pp = 0.30;// perturbation probability

//    private final int maxStagnantStep = 30;// max iterator no improve
//    private final int timeLimit = -1;// no time limit
}
