package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;


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

        for (TCMB tcmb : listTCMB) {
            int s_a = startTimes.getOrDefault(tcmb.getOp1(), 0);
            int s_b = startTimes.getOrDefault(tcmb.getOp2(), 0);
            int pi_a = processingTimes[tcmb.getOp1() - 1];
            int pi_b = processingTimes[tcmb.getOp2() - 1];

            int timeLag = s_b - (s_a + pi_a);
            if (timeLag > tcmb.getTimeConstraint()) {
                sumViolation += timeLag - tcmb.getTimeConstraint();
//                System.out.println("Operation " + tcmb.getOp1() + " starts at " + s_a + ", ends at " + (s_a + pi_a) +
//                        ", Operation " + tcmb.getOp2() + " starts at " + s_b + ", ends at " + (s_b + pi_b) +
//                        ", the time lag is " + timeLag + " which violates the constraint " + tcmb.getTimeConstraint());
            }
        }
        return makespan + Parameters.PENALTY_WEIGHT * sumViolation;
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
        int [][] orderMatrix = problemSetting.getOrderMatrix();
        for (int i = 0; i < n - 1; i++){
            int minIndex = i;
            for (int j = i + 1; j < n; j++){
                if (orderMatrix[operations.get(j)][operations.get(minIndex)] == 1) {
                    minIndex = j;
                }
            }
            int temp = operations.get(minIndex);
            operations.set(minIndex, operations.get(i));
            operations.set(i, temp);
        }

        return operations;
    }

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

}
