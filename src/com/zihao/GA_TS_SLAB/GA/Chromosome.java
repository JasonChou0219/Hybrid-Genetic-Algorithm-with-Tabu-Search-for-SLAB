package com.zihao.GA_TS_SLAB.GA;

/**
 * Description: the Chromosome class for Population,
 * it contains OS string and MS string
 *
 */

// import job class
import  java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.zihao.GA_TS_SLAB.Data.TCMB;
import  com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import  com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;


/**
 * Description: Chromosome of an individual
 */
public class Chromosome implements Comparable<Chromosome> {
    private static final ProblemSetting problemSetting = ProblemSetting.getInstance();

//    private static final int totalOpNum = problemSetting.getTotalOpNum();
//    private static final int totalMachines = problemSetting.getMachineNum();
//    private static final int[] processingTimes = problemSetting.getProcessingTime();
//    private static final DirectedAcyclicGraph dag = problemSetting.getDag();


    // 使用静态缓存存储已解码的结果
//    private static final Map<String, Schedule> scheduleCache = new ConcurrentHashMap<>(1000);
//    private static final Map<String, Double> fitnessCache = new ConcurrentHashMap<>(1000);

    private List<Integer> OS;
    private List<Integer> MS;
    private Map<Integer, Integer> delay;
    // need to make sure that every change to teh OS or MS also update schedule and fitness
    private Schedule schedule;
    private Random r;
    double fitness;

    public Chromosome(Chromosome other) {
        this.OS = new ArrayList<>(other.getOS());
        this.MS = Utility.compatibleAdjust(other.getMS(),other.getOS());
        this.delay = new HashMap<>(other.getDelay());
        this.r = new Random();
        updateScheduleAndFitness();
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

        // Generate MS based on the topologically sorted OS
        for (int op : topoSortedOperations) {
            OS.add(op);
            List<Integer> compatibleMachines = problemSetting.getOpToCompatibleList().get(op);
            int randomMachine = compatibleMachines.get(r.nextInt(compatibleMachines.size()));
            MS.add(randomMachine);
        }

//        //random delay
//        Map<Integer, Integer> maxDelay = new HashMap<>();
//        double lambda = 0.1;
//        for (TCMB tcmb : problemSetting.getTCMBList()) {
//            int curDelay = maxDelay.getOrDefault(tcmb.getOp1(), 0);
//            if (curDelay == 0) {
//                // 存疑
//                int randomDelay = (int)Math.min(-Math.log(1 - r.nextDouble()) / lambda, 20);
//
////                int randomDelay = (int)(-Math.log(1 - r.nextDouble()) / lambda);
//                maxDelay.put(tcmb.getOp1(), randomDelay);
//            }
//        }
//
//        this.delay = maxDelay;
        assignRandomDelays(r);
        // 计算调度和适应度
        updateScheduleAndFitness();
    }

    //暂时用不到
    /**
     * 创建一个新的染色体，确保更新schedule和fitness
     */
    public Chromosome(List<Integer> OS, List<Integer> MS, Map<Integer, Integer> delay) {
        this.OS = new ArrayList<>(OS);
        this.OS = Utility.compatibleAdjust(MS, OS); // 确保机器兼容性
        this.delay = new HashMap<>(delay);
        this.r = new Random();
        updateScheduleAndFitness();
        validateConstraints();
    }

    /**
     * 生成随机延迟
     */
    private void assignRandomDelays(Random r) {
        Map<Integer, Integer> maxDelay = new HashMap<>();
        double lambda = 0.1;

        for (TCMB tcmb : problemSetting.getTCMBList()) {
            int op1 = tcmb.getOp1();
            if (!maxDelay.containsKey(op1)) {
                int randomDelay = (int) Math.min(-Math.log(1 - r.nextDouble()) / lambda, 20);
                maxDelay.put(op1, randomDelay);
            }
        }

        this.delay = maxDelay;
    }




    // Remember to update schedule and fitness after changing OS and MS
    public void setOS(List<Integer>OS) {
        this.OS = OS;
//        updateScheduleAndFitness();
    }

    public void setMS(List<Integer>MS){
        this.MS = MS;
//        updateScheduleAndFitness();
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
//        updateScheduleAndFitness();
    }

    public void setFitness(double fitness){
        this.fitness = fitness;
//        updateScheduleAndFitness();
    }

    /**
     * 验证染色体的约束条件
     */
    private void validateConstraints() {
        if (!checkPrecedenceConstraints()) {
            throw new IllegalStateException("前序约束被违反");
        }
        if (!checkCompatibleMachines()) {
            throw new IllegalStateException("机器兼容性约束被违反");
        }
    }



    public boolean checkPrecedenceConstraints() {
        Set<Integer> seen = new HashSet<>();
        int[][] distanceMatrix = problemSetting.getDistanceMatrix();

        for (int op : OS) {
            for (int i = 1; i < distanceMatrix.length; i++) {
                // 检查 i 是否是 op 的前置节点
                if (distanceMatrix[i][op] > 0 &&  !seen.contains(i)) {
                    System.out.println("The Chromosome has violated the precedence constraints of operation " + op + " before operation " + i);
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

        Map<Integer, Integer> assignedMachine = new HashMap<>();

//        Map<Integer, Integer> machineLastEndTime = new HashMap<>();
//        for (int k = 1; k <= totalMachines; k++) {
//            machineLastEndTime.put(k, 0);
//        }

        for (int t = 0; t < totalOpNum; t++) {
            int a = OS.get(t);
            int k = MS.get(t);
            assignedMachine.put(a, k);
            TreeSet<int[]> idlePeriods = idleTimePeriods.get(k);
//            int es_a = Math.max(earliestStartTimes.get(a), machineLastEndTime.get(k));
            int es_a = earliestStartTimes.get(a);
            int opDelay = delay.getOrDefault(a, 0);
            es_a += opDelay;


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

                    int endTime = s_a + pi_a;
//                    machineLastEndTime.put(k, endTime);

                    if (es_a <= s_idle) {
                        idlePeriods.add(new int[]{s_idle + pi_a, e_idle});
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
//            // update earliest start time along machine
//            for (int i = t + 1; i < totalOpNum; i++) {
//                int nextOp = OS.get(i);
//                if (MS.get(i) == k) {
//                    earliestStartTimes.put(nextOp, Math.max(earliestStartTimes.get(nextOp), startTimes.get(a) + pi_a));
//                }
//            }

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

        // Return the schedule solution
        return new Schedule(startTimes, assignedMachine);
    }



    @Override
    public String toString() {
        return String.format("Chromosome{fitness=%.2f, os=%s, ms=%s}",
                fitness, OS, MS);
    }


    // lead to reverse descending sort by fitness when using Collection.sort
    @Override
    public int compareTo(Chromosome other) {
        return Double.compare(this.fitness, other.fitness);
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







