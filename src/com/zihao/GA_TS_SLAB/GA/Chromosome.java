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
        int[][] distanceMatrix = problemSetting.getDistanceMatrix();
        Set<Integer>nonTcmbOps = problemSetting.getNonTcmbOps();
        Set<Integer>tcmbOps = problemSetting.getTcmbOps();

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

//        for (TCMB tcmb : problemSetting.getTCMBList()) {
//            int curDelay = maxDelay.getOrDefault(tcmb.getOp1(), 0);
//            if (curDelay == 0) {
//                // 使用均匀分布在 0 到 10 之间生成随机延迟
//                int randomDelay = r.nextInt(11); // 生成一个 0 到 10 的随机整数
//                maxDelay.put(tcmb.getOp1(), randomDelay);
//            }
//        }

//        for (int op : nonTcmbOps) {
//            // 找到到任意 TCMB 操作的最小层级距离
//            int minDistance = Integer.MAX_VALUE;
//            for (int tcmbOp : tcmbOps) {
//                if (distanceMatrix[op][tcmbOp] > 0 && distanceMatrix[op][tcmbOp] < minDistance) {
//                    minDistance = distanceMatrix[op][tcmbOp];
//                }
//            }
//
//            // 根据最小距离计算概率（距离越小，概率越大）
//            double probability = minDistance == Integer.MAX_VALUE? 0 : 1.0 / (minDistance + 1); // 加 1 避免除以零
//
//            // 使用该概率决定是否为当前非 TCMB 操作初始化延迟
//            if (r.nextDouble() < probability) {
//                // 初始化延迟，使用反比分布（距离越大，延迟越小）
////                int maxDelayValue = Math.max(1, 10 - minDistance); // 避免延迟为零
//                int randomDelay = r.nextInt(1); // 生成 0 到 maxDelayValue 的随机整数
//                maxDelay.put(op, randomDelay);
//            }
//        }
        // 这里进行了修改,需要注意!!
        // 只进行maxDelay的计算,没有进行赋值
        this.delay = maxDelay;
        this.schedule = this.decode();
        this.fitness = Utility.calculateFitness(schedule);
        checkPrecedenceConstraints();
        checkCompatibleMachines();
    }

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



    // Check if OS meets the precedence constraints
//    public boolean checkPrecedenceConstraints() {
//        Set<Integer> seen = new HashSet<>();
//        int[][] orderMatrix = problemSetting.getOrderMatrix();
//        for (int op : OS) {
//            for (int i = 1; i < orderMatrix.length; i++) {
//                if (orderMatrix[i][op] == 1 && !seen.contains(i)) {
//                    System.out.println("The Chromosome has violate the precedence constraints of operation " + op + " before operation " + i);
//                    return false;
//                }
//            }
//            seen.add(op);
//        }
//        return true;
//    }
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

        // unsed
//        List<Integer> delayList = problemSetting.getDelayList();

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

        // Return the schedule solution
        return new Schedule(startTimes, assignedMachine);
    }


    public double[] getFeatureVector() {
        // 将OS和MS序列转换为一个特征向量
        // 这里只是一个示例，你需要根据你的具体需求返回特征向量
        double[] features = new double[OS.size() + MS.size()];
        for (int i = 0; i < OS.size(); i++) {
            features[i] = OS.get(i);
        }
        for (int i = 0; i < MS.size(); i++) {
            features[OS.size() + i] = MS.get(i);
        }
        return features;
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







