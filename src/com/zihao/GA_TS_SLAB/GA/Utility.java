package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;


/**
 * Description: calculate fitness of a schedule solution
 */
public class Utility {
    private static ProblemSetting problemSetting = ProblemSetting.getInstance();
    private static Parameters parameters =  new Parameters();;


    public static double calculateFitness(Schedule schedule) {
        int makespan = calculateMakespan(schedule);
        List<TCMB> listTCMB = problemSetting.getTCMBList();

        double sumViolation = 0;
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();

//        System.out.println("=================================================================");
        for (TCMB tcmb : listTCMB) {
            int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
            int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
            int pi_a = processingTimes[tcmb.getOp1() - 1];
            int pi_b = processingTimes[tcmb.getOp2() - 1];

            int timeLag = s_b - (s_a + pi_a);
            if (timeLag > tcmb.getTimeConstraint()) {
//                sumViolation += timeLag - tcmb.getTimeConstraint();
                int diff = timeLag - tcmb.getTimeConstraint();
                sumViolation += Parameters.PENALTY_WEIGHT_2 * diff * diff + Parameters.PENALTY_WEIGHT_1 * diff;

//                System.out.println("Operation " + tcmb.getOp1() + " starts at " + s_a + ", ends at " + (s_a + pi_a) +
//                        ", Operation " + tcmb.getOp2() + " starts at " + s_b + ", ends at " + (s_b + pi_b) +
//                        ", the time lag is " + timeLag + " which violates the constraint " + tcmb.getTimeConstraint());
            }
        }
//        System.out.println("=================================================================");
//        return makespan + Parameters.PENALTY_WEIGHT * sumViolation;
        return makespan + sumViolation;
    }


    private static int calculateMakespan(Schedule schedule) {
        int makespan = 0;
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();

        for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
            int op = entry.getKey();
            int startTime = entry.getValue();
            int endTime = startTime + processingTimes[op - 1];
            makespan = Math.max(makespan, endTime);
        }
//        System.out.println("The makespan is " + makespan);
        return makespan;
    }


    public static List<Integer> topologicalSort(List<Integer> operations) {
        int n = operations.size();
        int[][] distanceMatrix = problemSetting.getDistanceMatrix();

        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                int dis = distanceMatrix[operations.get(j)][operations.get(minIndex)];
                // 检查 operations.get(j) 是否是 operations.get(minIndex) 的前置节点
                if (dis > 0) {
                    minIndex = j;
                }
            }
            // 交换 operations 中 i 和 minIndex 位置的元素
            int temp = operations.get(minIndex);
            operations.set(minIndex, operations.get(i));
            operations.set(i, temp);
        }

        return operations;
    }


//    public static List<Integer> kahnTopologicalSort(List<Integer> operations) {
//        int [][] orderMatrix = problemSetting.getOrderMatrix();
//        int n = operations.size();
//        int[] inDegree = new int[n + 1];
//        for (int i = 1; i <= n; i++) {
//            for (int j = 1; j <= n; j++) {
//                if (orderMatrix[i][j] == 1) {
//                    inDegree[j]++;
//                }
//            }
//        }
//
//        Queue<Integer> queue = new LinkedList<>();
//        for (int i : operations) {
//            if (inDegree[i] == 0) {
//                queue.add(i);
//            }
//        }
//
//        List<Integer> sortedOperations = new ArrayList<>();
//        while (!queue.isEmpty()) {
//            int op = queue.poll();
//            sortedOperations.add(op);
//
//            for (int j = 1; j <= n; j++) {
//                if (orderMatrix[op][j] == 1) {
//                    if (--inDegree[j] == 0) {
//                        queue.add(j);
//                    }
//                }
//            }
//        }
//
//        return sortedOperations;
//    }


    public static List<Integer> compatibleAdjust(List<Integer> MS, List<Integer> OS){
        Random r = new Random();
        List<Integer> compatibleMS = new ArrayList<>();
        for (int i = 0; i < OS.size(); i++) {
            int op = OS.get(i);
            int machine = MS.get(i);
            List<Integer> compatibleMachines = problemSetting.getOpToCompatibleList().get(op);
            if (!compatibleMachines.contains(machine)) {
                machine = compatibleMachines.get(r.nextInt(compatibleMachines.size()));
            }
            compatibleMS.add(machine);
        }
        return compatibleMS;
    }

    public static void printViolation(Schedule schedule){
        List<TCMB> listTCMB = problemSetting.getTCMBList();
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();

        System.out.println("========================================================================================");
        for (TCMB tcmb : listTCMB) {
            int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
            int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
            int pi_a = processingTimes[tcmb.getOp1() - 1];
            int pi_b = processingTimes[tcmb.getOp2() - 1];

            int timeLag = s_b - (s_a + pi_a);
            if (timeLag > tcmb.getTimeConstraint()) {

                System.out.println("Operation " + tcmb.getOp1() + " starts at " + s_a + ", ends at " + (s_a + pi_a) +
                        ", Operation " + tcmb.getOp2() + " starts at " + s_b + ", ends at " + (s_b + pi_b) +
                        ", the time lag is " + timeLag + " which violates the constraint " + tcmb.getTimeConstraint());
            }
        }
        System.out.println("========================================================================================");
    }

    public static boolean checkViolation(Chromosome chromosome) {
        List<TCMB> tcmbList = problemSetting.getTCMBList();
        Schedule schedule = chromosome.getSchedule();
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();

        for (TCMB tcmb : tcmbList) {
            int opA = tcmb.getOp1();
            int opB = tcmb.getOp2();
            int endA = startTimes.get(opA) + processingTimes[opA - 1];
            int startB = startTimes.get(opB);

            int timeLag = startB - endA;
            if (timeLag > tcmb.getTimeConstraint()) {
                return true;
            }
        }
        return false;
    }

}
