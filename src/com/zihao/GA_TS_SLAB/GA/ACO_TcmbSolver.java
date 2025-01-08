package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.*;

public class ACO_TcmbSolver {
//    // 参数
//    public static final int MAX_ITERATIONS = 100;
//    public static final double PHEROMONE_INITIAL = 1.0;
//    public static final double EVAPORATION_RATE = 0.5;
//    public static final double PHEROMONE_IMPORTANCE = 1.0;
//    public static final double HEURISTIC_IMPORTANCE = 2.0;
//    public static final double PHEROMONE_INCREASE = 100.0;
//
//    // 蚁群算法的数据结构
//    public double[][] pheromones;
//    public ProblemSetting problemSetting = ProblemSetting.getInstance();
//    public int numOperations = problemSetting.getTotalOpNum();
//    public Random random = new Random();
//
//    public ACO_TcmbSolver() {
//        // 初始化信息素矩阵
//        pheromones = new double[numOperations][numOperations];
//        for (int i = 0; i < numOperations; i++) {
//            Arrays.fill(pheromones[i], PHEROMONE_INITIAL);
//        }
//    }
//
//    public Schedule solve(Chromosome chromosome) {
//        Schedule initialSchedule = chromosome.getSchedule();
//        Map<Integer, Integer> startTimes = initialSchedule.getStartTimes();
//        Map<Integer, List<Integer>> machineAssignments = initialSchedule.getMachineAssignments();
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//        int[] processingTimes = problemSetting.getProcessingTime();
//
//        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
//            List<Ant> ants = createAnts(startTimes, machineAssignments, processingTimes, tcmbList);
//
//            // 每只蚂蚁构建解
//            for (Ant ant : ants) {
//                ant.constructSolution(pheromones);
//                ant.calculateFitness(tcmbList, processingTimes);
//            }
//
//            // 选择最佳解
//            Ant bestAnt = getBestAnt(ants);
//
//            // 更新信息素
//            updatePheromones(bestAnt);
//
//            // 判断是否收敛
//            if (checkConvergence(ants)) {
//                System.out.println("Solution converged after " + iteration + " iterations.");
//                return bestAnt.toSchedule();
//            }
//        }
//
//        System.out.println("Reached max iterations without convergence.");
//        return null;
//    }
//
//    private List<Ant> createAnts(Map<Integer, Integer> startTimes, Map<Integer, List<Integer>> machineAssignments, int[] processingTimes, List<TCMB> tcmbList) {
//        List<Ant> ants = new ArrayList<>();
//        for (int i = 0; i < numOperations; i++) {
//            ants.add(new Ant(startTimes, machineAssignments, processingTimes, tcmbList));
//        }
//        return ants;
//    }
//
//    private void updatePheromones(Ant bestAnt) {
//        // 信息素挥发
//        for (int i = 0; i < numOperations; i++) {
//            for (int j = 0; j < numOperations; j++) {
//                pheromones[i][j] *= (1 - EVAPORATION_RATE);
//            }
//        }
//
//        // 增加最佳解的信息素
//        for (int[] operationPair : bestAnt.getOperationPairs()) {
//            int from = operationPair[0];
//            int to = operationPair[1];
//
//            // 边界检查，确保索引在范围内
//            if (from >= 0 && from < numOperations && to >= 0 && to < numOperations) {
//                pheromones[from][to] += PHEROMONE_INCREASE;
//            } else {
//                System.err.println("Error: Operation index out of bounds during pheromone update: " + from + ", " + to);
//                System.err.println("Operation pair: " + Arrays.toString(operationPair));
//            }
//        }
//    }
//
//
//
//
//
//    private boolean checkConvergence(List<Ant> ants) {
//        // 可以根据目标值的波动或蚂蚁解的相似性实现判断逻辑
//        // 这里我们以蚂蚁找到的解的相似性作为简单的收敛标准
//        Set<Integer> distinctSolutions = new HashSet<>();
//        for (Ant ant : ants) {
//            distinctSolutions.add(ant.hashCode());
//        }
//        return distinctSolutions.size() == 1;  // 如果所有解都相同则认为收敛
//    }
//
//    private Ant getBestAnt(List<Ant> ants) {
//        return ants.stream().min(Comparator.comparingDouble(Ant::getFitness)).orElse(null);
//    }
}




