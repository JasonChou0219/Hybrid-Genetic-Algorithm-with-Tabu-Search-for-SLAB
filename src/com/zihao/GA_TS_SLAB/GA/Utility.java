package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Test.TestConstraint;

import java.util.*;


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

    public static int countViolations(Schedule schedule){
        int count = 0;
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();
        List<TCMB> listTCMB = problemSetting.getTCMBList();
//        System.out.println("=================================================================");
        for (TCMB tcmb : listTCMB) {
            int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
            int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
            int pi_a = processingTimes[tcmb.getOp1() - 1];
            int pi_b = processingTimes[tcmb.getOp2() - 1];

            int timeLag = s_b - (s_a + pi_a);
            if (timeLag > tcmb.getTimeConstraint()) {
                count++;
            }
        }
        return count;
    }

    public static int sumViolations(Schedule schedule){
        int sumViolation = 0;
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();
        List<TCMB> listTCMB = problemSetting.getTCMBList();
        for (TCMB tcmb : listTCMB) {
            int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
            int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
            int pi_a = processingTimes[tcmb.getOp1() - 1];
            int pi_b = processingTimes[tcmb.getOp2() - 1];

            int timeLag = s_b - (s_a + pi_a);
            if (timeLag > tcmb.getTimeConstraint()) {
                sumViolation += timeLag - tcmb.getTimeConstraint();
            }
        }
        return sumViolation;
    }

    public static double calculateEnergy(Schedule schedule, double curTemp, double initTemp){
        int makespan = calculateMakespan(schedule);
        double violation = getViolation(schedule);
        return makespan + lambda(curTemp,initTemp) * violation;
    }

    public static double lambda(double curTemp, double initTemp) {
        // 温度越低，惩罚权重越高
        double base = 1000; // 基础惩罚系数
        return base * (1 + (initTemp - curTemp) / initTemp);
    }

    public static double getViolation(Schedule schedule) {
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();
        List<TCMB> listTCMB = problemSetting.getTCMBList();
        if (hasSevereViolations(schedule)) {
            // 阶段1：平方惩罚严重超限
            double sumSquare = 0;


            for (TCMB tcmb : listTCMB) {
                int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
                int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
                int pi_a = processingTimes[tcmb.getOp1() - 1];
                int pi_b = processingTimes[tcmb.getOp2() - 1];

                int timeLag = s_b - (s_a + pi_a);
                if (timeLag > tcmb.getTimeConstraint()) {
                    sumSquare += Math.pow(timeLag - tcmb.getTimeConstraint(), 2);
                }
            }
            return sumSquare;
        } else {
            // 阶段2：混合惩罚
            int count = 0;
            int sumViolation = 0;
            for (TCMB tcmb : listTCMB) {
                int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
                int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
                int pi_a = processingTimes[tcmb.getOp1() - 1];
                int pi_b = processingTimes[tcmb.getOp2() - 1];

                int timeLag = s_b - (s_a + pi_a);
                if (timeLag > tcmb.getTimeConstraint()) {
                    sumViolation += timeLag - tcmb.getTimeConstraint();
                    count++;
                }
            }
            return 10 * count + sumViolation;
        }
    }

    public static boolean hasSevereViolations(Schedule schedule){
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();
        List<TCMB> listTCMB = problemSetting.getTCMBList();
        for (TCMB tcmb : listTCMB) {
            int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
            int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
            int pi_a = processingTimes[tcmb.getOp1() - 1];
            int pi_b = processingTimes[tcmb.getOp2() - 1];
            int timeLag = s_b - (s_a + pi_a);
            //判定严重violation的阈值
            if (timeLag > 5) {
                return true;
            }
        }
        return false;
    }

    public static int calculateMakespan(Schedule schedule) {
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

    public static double getSAFitness(Schedule schedule){
        int makespan = calculateMakespan(schedule);
        double totalPenalty = 0;

        for (TCMB tcmb : ProblemSetting.getInstance().getTCMBList()) {
            int opA = tcmb.getOp1();
            int opB = tcmb.getOp2();
            int endA = schedule.getStartTimes().get(opA)
                    + ProblemSetting.getInstance().getProcessingTime()[opA - 1];
            int startB = schedule.getStartTimes().get(opB);
            int timeLag = startB - endA;

            if (timeLag > tcmb.getTimeConstraint()) {
                int violation = timeLag - tcmb.getTimeConstraint();
                totalPenalty += Parameters.PENALTY_WEIGHT_1 * violation
                        + Parameters.PENALTY_WEIGHT_2 * Math.pow(violation, 2);
            }
        }

        return makespan + totalPenalty;

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

    public static boolean checkViolation(Schedule schedule){
        List<TCMB> listTCMB = problemSetting.getTCMBList();
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();
        for (TCMB tcmb : listTCMB) {
            int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
            int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
            int pi_a = processingTimes[tcmb.getOp1() - 1];
            int pi_b = processingTimes[tcmb.getOp2() - 1];

            int timeLag = s_b - (s_a + pi_a);
            if (timeLag > tcmb.getTimeConstraint()) {
                return true;
            }
        }
        return false;
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

//    public boolean checkDependencyBySchedule(Schedule schedule){
//        Set<Integer> seen = new HashSet<>();
//        int[][] distanceMatrix = problemSetting.getDistanceMatrix();
//
//    }



}
