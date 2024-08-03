package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Parameters;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.GA.Schedule;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Description: calculate fitness of a schedule solution
 */
public class Calculation {
    private ProblemSetting problemSetting;
    private Parameters parameters;

    private static Calculation instance;

    private Calculation(){
        problemSetting = ProblemSetting.getInstance();
        parameters = new Parameters();
    }


    public static synchronized Calculation getInstance() {
        if (instance == null) {
            instance = new Calculation();
        }
        return instance;
    }


    public double calculateFitness(Schedule schedule) {
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
                System.out.println("Operation " + tcmb.getOp1() + " starts at " + s_a + ", ends at " + (s_a + pi_a) +
                        ", Operation " + tcmb.getOp2() + " starts at " + s_b + ", ends at " + (s_b + pi_b) +
                        ", the time lag is " + timeLag + " which violates the constraint " + tcmb.getTimeConstraint());
            }
        }
        return makespan + parameters.penaltyWeight * sumViolation;
    }

    private int calculateMakespan(Schedule schedule) {
        int makespan = 0;
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        int[] processingTimes = problemSetting.getProcessingTime();

        for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
            int op = entry.getKey();
            int startTime = entry.getValue();
            int endTime = startTime + processingTimes[op - 1];
            makespan = Math.max(makespan, endTime);
        }
        System.out.println("The makespan is " + makespan);
        return makespan;
    }

}
