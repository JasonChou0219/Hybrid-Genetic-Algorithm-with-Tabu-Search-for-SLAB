package com.zihao.GA_TS_SLAB.GA;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.ScatterChart;
import javafx.stage.Stage;
import java.util.*;

public class Schedule {
    // the sorted idle time interval of machine
    private Map<Integer, TreeSet<int[]>> idleTimePeriods;
    // the earliest start time of operation e_a
    private Map<Integer, Integer> earliestStartTimes;
    // ass_ak judge whether operation is assigned to machine k
    private boolean[][] assignment;
    private Map<Integer, Integer> startTimes;
    // list of operation that assigned to machine k
    private Map<Integer, List<Integer>> machineAssignments;
    // precedence relationship between operation a and b assigned on machine k
    private Map<Integer, Map<Integer, Map<Integer, Integer>>> y_abk; // 操作之间的相对顺序


    public Schedule(Map<Integer, TreeSet<int[]>> idleTimePeriods, Map<Integer, Integer> earliestStartTimes, boolean[][] assignment, Map<Integer, Integer> startTimes, Map<Integer, List<Integer>> machineAssignments, Map<Integer, Map<Integer, Map<Integer, Integer>>> y_abk) {
        this.idleTimePeriods = idleTimePeriods;
        this.earliestStartTimes = earliestStartTimes;
        this.assignment = assignment;
        this.startTimes = startTimes;
        this.machineAssignments = machineAssignments;
        this.y_abk = y_abk;
    }

    // get functions
    public Map<Integer, TreeSet<int[]>> getIdleTimePeriods() {
        return idleTimePeriods;
    }

    public Map<Integer, Integer> getEarliestStartTimes() {
        return earliestStartTimes;
    }

    public boolean[][] getAssignment() {
        return assignment;
    }

    public Map<Integer, Integer> getStartTimes() {
        return startTimes;
    }

    public Map<Integer, List<Integer>> getMachineAssignments() {
        return machineAssignments;
    }

    public Map<Integer, Map<Integer, Map<Integer, Integer>>> getY_abk() {
        return y_abk;
    }

    //
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schedule:\n");

        sb.append("Idle Time Periods:\n");
        for (Map.Entry<Integer, TreeSet<int[]>> entry : idleTimePeriods.entrySet()) {
            sb.append("Machine ").append(entry.getKey()).append(": ");
            for (int[] period : entry.getValue()) {
                sb.append(Arrays.toString(period)).append(" ");
            }
            sb.append("\n");
        }

        sb.append("Earliest Start Times:\n");
        for (Map.Entry<Integer, Integer> entry : earliestStartTimes.entrySet()) {
            sb.append("Operation ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        sb.append("Assignments:\n");
        for (int i = 0; i < assignment.length; i++) {
            for (int j = 0; j < assignment[i].length; j++) {
                sb.append(assignment[i][j] ? "1 " : "0 ");
            }
            sb.append("\n");
        }

        sb.append("Start Times:\n");
        for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
            sb.append("Operation ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        sb.append("Machine Assignments:\n");
        for (Map.Entry<Integer, List<Integer>> entry : machineAssignments.entrySet()) {
            sb.append("Machine ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        sb.append("Relative Order (y_abk):\n");
        for (Map.Entry<Integer, Map<Integer, Map<Integer, Integer>>> kEntry : y_abk.entrySet()) {
            int k = kEntry.getKey();
            for (Map.Entry<Integer, Map<Integer, Integer>> aEntry : kEntry.getValue().entrySet()) {
                int a = aEntry.getKey();
                for (Map.Entry<Integer, Integer> bEntry : aEntry.getValue().entrySet()) {
                    int b = bEntry.getKey();
                    int value = bEntry.getValue();
                    sb.append("Machine ").append(k).append(", Operation ").append(a).append(" -> Operation ").append(b).append(": ").append(value).append("\n");
                }
            }
        }
        return sb.toString();
    }
}

