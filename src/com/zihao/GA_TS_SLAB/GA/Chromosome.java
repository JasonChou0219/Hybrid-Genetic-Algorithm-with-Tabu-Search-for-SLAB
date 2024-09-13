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
import  com.zihao.GA_TS_SLAB.GA.Utility;


/**
 * Description: Chromosome of an individual
 */
public class Chromosome implements Comparable<Chromosome> {
    private static final ProblemSetting problemSetting = ProblemSetting.getInstance();

    private List<Integer> OS;
    private List<Integer> MS;
    private Map<Integer, Integer> delay;
    // need to make sure that every change to teh OS or MS also update schedule and fitness
    // use Chromosome(OS,MS) to ensure this (especially for mutation)
    private Schedule schedule;
    private Random r;
    double fitness;



    public Chromosome(Chromosome other) {
        this.OS = new ArrayList<>(other.getOS());
        this.MS = Utility.compatibleAdjust(other.getMS(),other.getOS());
        this.delay = new HashMap<>(other.getDelay());
        this.r = new Random();
        this.schedule = this.decode();
        this.fitness = Utility.calculateFitness(schedule);
        checkPrecedenceConstraints();
        checkCompatibleMachines();
    }
    // Randomly generate OS and perform topological sort and
    // generate according compatible MS
    public Chromosome(Random r) {
        OS = new ArrayList<>();
        MS = new ArrayList<>();
        delay = new HashMap<Integer, Integer>();
        this.r = r;
        int totalOpNum = problemSetting.getTotalOpNum();

        // Generate a random permutation of operation IDs
        List<Integer> operations = new ArrayList<>();
        for (int i = 1; i <= totalOpNum; i++) {
            operations.add(i);
        }
        Collections.shuffle(operations, r);

        // Perform topological sort on the random permutation using the orderMatrix
        List<Integer> topoSortedOperations = Utility.topologicalSort(operations);

//        List<Integer> topoSortedOperations = Utility.kahnTopologicalSort(operations);
        // Generate MS based on the topologically sorted OS
        for (int op : topoSortedOperations) {
            OS.add(op);
            List<Integer> compatibleMachines = problemSetting.getOpToCompatibleList().get(op);
            int randomMachine = compatibleMachines.get(r.nextInt(compatibleMachines.size()));
            MS.add(randomMachine);
        }

        //random delay
        Map<Integer, Integer> maxDelay = new HashMap<>();
        double lambda = 0.1;
        for (TCMB tcmb : problemSetting.getTCMBList()) {
            int curDelay = maxDelay.getOrDefault(tcmb.getOp1(), 0);
            if (curDelay == 0) {
                // 存疑
                int randomDelay = (int)Math.min(-Math.log(1 - r.nextDouble()) / lambda, 20);

//                int randomDelay = (int)(-Math.log(1 - r.nextDouble()) / lambda);
                maxDelay.put(tcmb.getOp1(), randomDelay);
            }
        }
        // 这里进行了修改,需要注意!!
        // 只进行maxDelay的计算,没有进行赋值
        this.delay = maxDelay;
        this.schedule = this.decode();
        this.fitness = Utility.calculateFitness(schedule);
        checkPrecedenceConstraints();
        checkCompatibleMachines();
    }

    // Constructor by OS and MS, used to test GanttGraphPlot

    public Chromosome(List<Integer> OS, List<Integer> MS, Map<Integer, Integer> delay) {
        this.OS = new ArrayList<>(OS);
        this.MS = new ArrayList<>();
        int totalOpNum = problemSetting.getTotalOpNum();

        // Check compatibility and replace incompatible machines
        r = new Random();
        this.MS = Utility.compatibleAdjust(MS,OS);
        this.delay = delay;
        this.schedule = decode();
        this.fitness = Utility.calculateFitness(schedule);
        checkCompatibleMachines();
        checkPrecedenceConstraints();
    }

    // Remember to update schedule and fitness after changing OS and MS
    public void setOS(List<Integer>OS) {
        this.OS = OS;
    }

    public void setMS(List<Integer>MS){
        this.MS = MS;
    }
    public List<Integer> getOS() {
        return OS;
    }

    public List<Integer> getMS() {
        return MS;
    }

    public Map<Integer, Integer> getDelay(){
        return delay;
    }

    public void setDelay(Map<Integer, Integer> delay) {
        this.delay = delay;
    }

    public void setFitness(double fitness){
        this.fitness = fitness;
    }


    // Check if OS meets the precedence constraints
    public boolean checkPrecedenceConstraints() {
        Set<Integer> seen = new HashSet<>();
        int[][] orderMatrix = problemSetting.getOrderMatrix();
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

    public void updateScheduleAndFitness() {
        schedule = decode();
        fitness = Utility.calculateFitness(schedule);
    }

    public  Schedule getSchedule(){
        return schedule;
    }

    public double getFitness(){
        return fitness;
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

        List<Integer> delayList = problemSetting.getDelayList();

        Map<Integer, Integer> assignedMachine = new HashMap<>();

        for (int t = 0; t < totalOpNum; t++) {
            int a = OS.get(t);
            int k = MS.get(t);
            assignedMachine.put(a, k);
            TreeSet<int[]> idlePeriods = idleTimePeriods.get(k);
            int es_a = earliestStartTimes.get(a);

            int opDelay = delay.getOrDefault(a, 0);
            es_a += opDelay;

//            if (a == 4) es_a += 7;
//            if (delayList.contains(t) && t != totalOpNum - 1){
//                int maxDelay = 10;
//                double lambda = 0.5;
//                int randomDelay = (int)(-Math.log(1 - r.nextDouble()) / lambda);
//                randomDelay = Math.min(randomDelay, maxDelay);
//
////                int next = OS.get(t + 1);
//                // remained to be done, might cause problem if they are the same
////                int maxDelay = earliestStartTimes.get(next) - es_a;
////                System.out.println("Earliest next is " + earliestStartTimes.get(next));
////                System.out.println("earliest current is " + es_a);
////                int randomDelay = r.nextInt(10);
//                delay.put(a, randomDelay);
//                System.out.println("Operation " + a + " has been delayed for " + randomDelay + " minutes");
//                es_a += randomDelay;
//            }

//            System.out.println("Operation: " + a + ", Earliest Start Time: " + es_a);
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
        return new Schedule(idleTimePeriods, earliestStartTimes, assignment, startTimes, machineAssignments, y_abk, assignedMachine);
    }


    @Override
    public String toString() {
        return "Chromosome{" +
                "OS=" + OS +
                ", MS=" + MS +
                '}';
    }

    // remained to be done
    // lead to reverse descending sort by fitness when using Collection.sort
    @Override
    public int compareTo(Chromosome o) {
        if (this.fitness < o.fitness) {
            return -1;
        } else if (this.fitness == o.fitness){
            return 0;
        } else{
            return 1;
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







