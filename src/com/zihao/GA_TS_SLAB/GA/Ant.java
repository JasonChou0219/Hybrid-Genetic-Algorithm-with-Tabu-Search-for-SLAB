package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;

import java.util.*;


public class Ant {
//    private Map<Integer, Integer> startTimes;
//    private Map<Integer, List<Integer>> machineAssignments;
//    private int[] processingTimes;
//    private List<TCMB> tcmbList;
//    private double fitness;
//    private List<int[]> operationPairs;
//
//    public Ant(Map<Integer, Integer> startTimes, Map<Integer, List<Integer>> machineAssignments, int[] processingTimes, List<TCMB> tcmbList) {
//        this.startTimes = new HashMap<>(startTimes);
//        this.machineAssignments = machineAssignments;
//        this.processingTimes = processingTimes;
//        this.tcmbList = tcmbList;
//        this.operationPairs = new ArrayList<>();
//    }
//
//    public void constructSolution() {
//        for (int i = 0; i < numOperations - 1; i++) {
//            // 选择下一步操作
//            int from = currentOperation;
//            int to = selectNextOperation();
//
//            if (from >= 0 && from < numOperations && to >= 0 && to < numOperations) {
//                operationPairs.add(new int[]{from, to});
//                currentOperation = to;
//            } else {
//                System.err.println("Error: Operation index out of bounds during solution construction: " + from + ", " + to);
//            }
//        }
//    }
//
//
//    private double getTransitionProbability(double[][] pheromones, int opA, int opB) {
//        // 检查索引是否在有效范围内
//        if (opA >= pheromones.length || opB >= pheromones[opA].length) {
//            System.err.println("Error: Operation index out of bounds during pheromone update: " + opA + ", " + opB);
//            return 0.0;  // 返回默认的低概率
//        }
//
//        double pheromone = pheromones[opA][opB];
//        double heuristic = 1.0 / (Math.abs(startTimes.get(opA) - startTimes.get(opB)) + 1.0);
//        return Math.pow(pheromone, ACO_TcmbSolver.PHEROMONE_IMPORTANCE) * Math.pow(heuristic, ACO_TcmbSolver.HEURISTIC_IMPORTANCE);
//    }
//
//    private void adjustOperationStartTimes(int opA, int opB) {
//        int endA = startTimes.get(opA) + processingTimes[opA - 1];
//        int newStartB = endA + 10;  // 加入约束调整逻辑
//        startTimes.put(opB, newStartB);
//    }
//
//    public void calculateFitness(List<TCMB> tcmbList, int[] processingTimes) {
//        fitness = 0.0;
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1();
//            int opB = tcmb.getOp2();
//            int timeLag = startTimes.get(opB) - (startTimes.get(opA) + processingTimes[opA - 1]);
//            if (timeLag > tcmb.getTimeConstraint()) {
//                fitness += (timeLag - tcmb.getTimeConstraint());  // 罚值
//            }
//        }
//    }
//
//    public double getFitness() {
//        return fitness;
//    }
//
//    public List<int[]> getOperationPairs() {
//        return operationPairs;
//    }
//
//    public Schedule toSchedule() {
//        // 将蚂蚁的解转换为调度对象
//        return new Schedule(null, null, null, startTimes, machineAssignments, null, null);
//    }
}

