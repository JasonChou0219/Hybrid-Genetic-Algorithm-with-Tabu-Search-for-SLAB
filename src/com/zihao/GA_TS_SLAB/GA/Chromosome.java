package com.zihao.GA_TS_SLAB.GA;

/**
 * Description: the Chromosome class for Population,
 * it contains OS string and MS string
 *
 */

// import job class
import  java.util.*;
// import File 相关的包
import java.io.*;

import  com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Schedule;
import  com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import  com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
import com.zihao.GA_TS_SLAB.GA.Parameters;

public class Chromosome implements Comparable<Chromosome> {
    private Parameters parameters = new Parameters();
    private static int[][] orderMatrix;
    private static final ProblemSetting problemSetting = ProblemSetting.getInstance();
    private static final int length = ProblemSetting.getInstance().getJobNum();

    private ArrayList<Integer> OS;
    private ArrayList<Integer> MS;
    public Random r;
    double fitness;

    // Randomly generate OS and perform topological sort and
    // generate according compatible MS
    public Chromosome(Random r) {
        OS = new ArrayList<>();
        MS = new ArrayList<>();
        int totalOpNum = problemSetting.getTotalOpNum();

        // Initialize orderMatrix if it hasn't been initialized yet
        if (orderMatrix == null) {
            orderMatrix = new int[totalOpNum + 1][totalOpNum + 1];
            buildOrderMatrix(problemSetting.getDag(), totalOpNum);
            computeTransitiveClosure(totalOpNum);
//            printOrderMatrix(totalOpNum);
        }

        // Generate a random permutation of operation IDs
        List<Integer> operations = new ArrayList<>();
        for (int i = 1; i <= totalOpNum; i++) {
            operations.add(i);
        }
        Collections.shuffle(operations, r);

        // Perform topological sort on the random permutation using the orderMatrix
        List<Integer> topoSortedOperations = topologicalSort(operations);

        // Generate MS based on the topologically sorted OS
        for (int op : topoSortedOperations) {
            OS.add(op);
            List<Integer> compatibleMachines = problemSetting.getOpToCompatibleList().get(op);
            int randomMachine = compatibleMachines.get(r.nextInt(compatibleMachines.size()));
            MS.add(randomMachine);
        }
    }

    // Constructor by OS and MS, used to test GanttGraphPlot
    public Chromosome(List<Integer> OS, List<Integer> MS) {
        this.OS = new ArrayList<>(OS);
        this.MS = new ArrayList<>(MS);
        int totalOpNum = problemSetting.getTotalOpNum();

        if (orderMatrix == null) {
            orderMatrix = new int[totalOpNum + 1][totalOpNum + 1];
            buildOrderMatrix(problemSetting.getDag(), totalOpNum);
            computeTransitiveClosure(totalOpNum);
        }

    }

    private void buildOrderMatrix(DirectedAcyclicGraph dag, int totalOpNum) {
        for (int i = 1; i <= totalOpNum; i++) {
            for (int neighbor : dag.getNeighbors(i)) {
                orderMatrix[i][neighbor] = 1;
            }
        }
    }

    // using the transitive relationship
    private void computeTransitiveClosure(int totalOpNum) {
        for (int k = 1; k <= totalOpNum; k++) {
            for (int i = 1; i <= totalOpNum; i++) {
                for (int j = 1; j <= totalOpNum; j++) {
                    if (orderMatrix[i][k] == 1 && orderMatrix[k][j] == 1) {
                        orderMatrix[i][j] = 1;
                    }
                }
            }
        }
    }

    private List<Integer> topologicalSort(List<Integer> operations) {
        int n = operations.size();
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

    private void printOrderMatrix(int totalOpNum) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order Matrix:\n");
        for (int i = 1; i <= totalOpNum; i++) {
            for (int j = 1; j <= totalOpNum; j++) {
                sb.append(orderMatrix[i][j]).append(" ");
            }
            sb.append(System.lineSeparator());
        }
        System.out.println(sb);
    }

    public List<Integer> getOS() {
        return OS;
    }

    public List<Integer> getMS() {
        return MS;
    }


    // Check if OS meets the precedence constraints
    public boolean checkPrecedenceConstraints() {
        Set<Integer> seen = new HashSet<>();
        for (int op : OS) {
            for (int i = 1; i < orderMatrix.length; i++) {
                if (orderMatrix[i][op] == 1 && !seen.contains(i)) {
                    System.out.println("The Chromosome has violate the precedence constraints of operation " + op + " before operation " + i);
                    return false;
                }
            }
            seen.add(op);
        }
        return true;
    }

    // Check if MS meets the compatibility constraints of OS
    public boolean checkCompatibleMachines() {
        for (int i = 0; i < OS.size(); i++) {
            int op = OS.get(i);
            int machine = MS.get(i);
            List<Integer> compatibleMachines = problemSetting.getOpToCompatibleList().get(op);
            if (!compatibleMachines.contains(machine)) {
                System.out.println("The Chromosome has violate the compatibility constraints of operation " + op + " on machine " + machine);
                return false;
            }
        }
        return true;
    }

    // Get processing times corresponding to OS
    public void getProcessingTimes() {
        List<Integer> processingTimes = new ArrayList<>();
        int[] processingTimeArray = problemSetting.getProcessingTime();

        for (int op : OS) {
            processingTimes.add(processingTimeArray[op - 1]);
        }

        System.out.println("OS corresponding processing times: " + processingTimes);
    }

    // Print the adjacencyList of reverse DAg
    public void printReverseDag() {
        DirectedAcyclicGraph reverseDag = problemSetting.getReverseDag();
        StringBuilder sb = new StringBuilder();
        for (int node : reverseDag.getAdjacencyList().keySet()) {
            sb.append(node).append(" : ").append(reverseDag.getNeighbors(node)).append("\n");
        }
        System.out.println(sb.toString());
    }

    public Schedule decode() {
        int totalOpNum = problemSetting.getTotalOpNum();
        int totalMachines = problemSetting.getMachineNum();

        // Initialize idle time periods for all machines
        Map<Integer, TreeSet<int[]>> idleTimePeriods = new HashMap<>();
        for (int k = 1; k <= totalMachines; k++) {
            TreeSet<int[]> treeSet = new TreeSet<>(Comparator.comparingInt(a -> a[0]));
            treeSet.add(new int[]{0, Integer.MAX_VALUE});
            idleTimePeriods.put(k, treeSet);
        }

        // Initialize earliest start times for all operations
        Map<Integer, Integer> earliestStartTimes = new HashMap<>();
        for (int a = 1; a <= totalOpNum; a++) {
            earliestStartTimes.put(a, 0);
        }

        // Initialize assignment matrix (false default)
        boolean[][] assignment = new boolean[totalOpNum + 1][totalMachines + 1];

        // Initialize start times
        Map<Integer, Integer> startTimes = new HashMap<>();

        // Initialize operation processing times
        int[] processingTimes = problemSetting.getProcessingTime();

        for (int t = 0; t < totalOpNum; t++) {
            int a = OS.get(t);
            int k = MS.get(t);
            TreeSet<int[]> idlePeriods = idleTimePeriods.get(k);
            int es_a = earliestStartTimes.get(a);
            int pi_a = processingTimes[a - 1];

            for (int[] idlePeriod : idlePeriods) {
                int s_idle = idlePeriod[0];
                int e_idle = idlePeriod[1];

                if (Math.max(s_idle, es_a) + pi_a <= e_idle) {
                    idlePeriods.remove(idlePeriod);
                    assignment[a][k] = true;

                    int s_a = Math.max(s_idle, es_a);
                    startTimes.put(a, s_a);

                    if (es_a <= s_idle) {
                        idlePeriods.add(new int[]{s_a + pi_a, e_idle});
                    } else {
                        idlePeriods.add(new int[]{s_idle, es_a});
                        idlePeriods.add(new int[]{es_a + pi_a, e_idle});
                    }
                    break;
                }
            }

            // Update earliest start times for adjacent operations
            for (int b : problemSetting.getDag().getNeighbors(a)) {
                earliestStartTimes.put(b, Math.max(earliestStartTimes.get(b), startTimes.get(a) + pi_a));
            }
        }

        // Generate List_ass for machines
        Map<Integer, List<Integer>> machineAssignments = new HashMap<>();
        for (int k = 1; k <= totalMachines; k++) {
            machineAssignments.put(k, new ArrayList<>());
        }

        // Ensure machineAssignments are ordered according to OS and MS
        for (int t = 0; t < totalOpNum; t++) {
            int a = OS.get(t);
            int k = MS.get(t);
            machineAssignments.get(k).add(a);
        }

        // Initialize y_abk based on List_ass
        Map<Integer, Map<Integer, Map<Integer, Integer>>> y_abk = new HashMap<>();
        for (int k = 1; k <= totalMachines; k++) {
            List<Integer> assignments = machineAssignments.get(k);
            y_abk.put(k, new HashMap<>());
            for (int i = 0; i < assignments.size() - 1; i++) {
                for (int j = i + 1; j < assignments.size(); j++) {
                    int a = assignments.get(i);
                    int b = assignments.get(j);
                    y_abk.get(k).putIfAbsent(a, new HashMap<>());
                    y_abk.get(k).putIfAbsent(b, new HashMap<>());
                    y_abk.get(k).get(a).put(b, 1);
                    y_abk.get(k).get(b).put(a, -1);
                }
            }
        }

        // Return the schedule solution
        return new Schedule(idleTimePeriods, earliestStartTimes, assignment, startTimes, machineAssignments, y_abk);
    }


    @Override
    public String toString() {
        return "Chromosome{" +
                "OS=" + OS +
                ", MS=" + MS +
                '}';
    }

    @Override
    public int compareTo(Chromosome o) {
        if (o.fitness > this.fitness) {
            return 1;
        } else if (this.fitness == o.fitness){
            return 0;
        } else{
            return -1;
        }
    }
//    public static void main(String[] args) {
//        Random random = new Random();
//        File parentDir = new File("src/Dataset/Gu2016/N1");
//        Input input = new Input(parentDir);
//        input.getProblemDesFromFile();
//        Chromosome chromosome = new Chromosome(random);
//        chromosome.checkPrecedenceConstraints();
//        chromosome.checkCompatibleMachines();
//        chromosome.getProcessingTimes();
//        chromosome.printReverseDag();
//
//        System.out.println(chromosome);
//    }
}







