package com.zihao.GA_TS_SLAB.Data;

import java.util.*;


import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;


/**
 * Description: the data of the problemsetting needed to tackle the SLAB problem
 */
public class ProblemSetting {
    //machine
    private int machineNum;
    // a map from a machine type to its machine id list
    private Map<Integer, List<Integer>> machineTypeToList;
    private String[] machineNames;

    //job
    private int jobNum;
    private int[] opNumEachJob;

    //operation
    private int totalOpNum;
    // a map that map from opID to its compatible machine list
    private int[] processingTime;
    private Map<Integer, List<Integer>> opToCompatibleList;
    private Map<Integer, Integer> opToJob;

    // TCMB
    private List<TCMB> TCMBList;

    // Delay list
//    private List<Integer> delayList;

    private Set<Integer> tcmbOps;

    //DAG
    private DirectedAcyclicGraph dag;

    //Reverse DAG
    private DirectedAcyclicGraph reverseDag;

    private int[][] distanceMatrix;

    private Set<Integer> nonTcmbOps;


    // only a single instance globally
    private static ProblemSetting instance;


    // Private constructor to prevent instantiation
    private ProblemSetting() {
        this.totalOpNum = 0;
        this.jobNum = 0;
        this.machineNum = 0;
        this.opToCompatibleList = new HashMap<>();
        this.processingTime = new int[0];
        this.opNumEachJob = new int[0];
        this.machineNames = new String[0];
        this.machineTypeToList = new HashMap<>();
        this.opToJob = new HashMap<>();
        this.TCMBList = new ArrayList<>();
//        this.delayList = new ArrayList<>();
        this.tcmbOps = new HashSet<>();
        this.dag = new DirectedAcyclicGraph();
        this.reverseDag = new DirectedAcyclicGraph();
        this.distanceMatrix = new int[0][0];
        this.nonTcmbOps = new HashSet<>();
    }

    // Method to get the single instance of the class
    public static synchronized ProblemSetting getInstance() {
        if (instance == null) {
            instance = new ProblemSetting();
        }
        return instance;
    }

    // machine
    public int getMachineNum() {
        return machineNum;
    }

    public void setMachineNum(int machineNum) {
        this.machineNum = machineNum;
    }

    public Map<Integer, List<Integer>> getMachineTypeToList() {
        return machineTypeToList;
    }

    public void setMachineTypeToList(Map<Integer, List<Integer>> machineTypeToList) {
        this.machineTypeToList = machineTypeToList;
    }

    public String[] getMachineNames() {
        return machineNames;
    }

    public void setMachineNames(String[] machineNames) {
        this.machineNames = machineNames;
    }

    //job
    public int getJobNum() {
        return jobNum;
    }

    public void setJobNum(int jobNum) {
        this.jobNum = jobNum;
    }

    public int[] getOpNumEachJob() {
        return opNumEachJob;
    }

    public void setOpNumEachJob(int[] opNumEachJob) {
        this.opNumEachJob = opNumEachJob;
    }

    // operation
    public int getTotalOpNum() {
        return totalOpNum;
    }

    public void setTotalOpNum(int totalOpNum) {
        this.totalOpNum = totalOpNum;
    }


    public int[] getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(int[] processingTime) {
        this.processingTime = processingTime;
    }


    // return the compatible machine list of operation id
    public Map<Integer, List<Integer>> getOpToCompatibleList() {
        return opToCompatibleList;
    }

    public void setOpToCompatibleList(Map<Integer, List<Integer>> opToCompatibleList) {
        this.opToCompatibleList = opToCompatibleList;
    }

    public void setOpToJob(Map<Integer, Integer> opToJob) {
        this.opToJob = opToJob;
    }

    public Map<Integer, Integer> getOpToJob() {
        return opToJob;
    }

    //TCMB
    public List<TCMB> getTCMBList() {
        return TCMBList;
    }

    public void setTCMBList(List<TCMB> TCMBList) {
        this.TCMBList = TCMBList;
    }

    public Set<Integer> getTcmbOps(){
        return tcmbOps;
    }

    public void setTcmbOps(Set<Integer> tcmbOps){
        this.tcmbOps = tcmbOps;
    }
//    //delay list
//    public List<Integer> getDelayList(){
//        return delayList;
//    }

//    public void setDelayList(List<Integer> delayList) {
//        this.delayList = delayList;
//    }

    //DAG
    public DirectedAcyclicGraph getDag() {
        return dag;
    }

    public void setDag(DirectedAcyclicGraph dag) {
        this.dag = dag;
    }

    //Reverse DAG
    public DirectedAcyclicGraph getReverseDag() {
        return reverseDag;
    }

    public void setReverseDag(DirectedAcyclicGraph reverseDag){
        this.reverseDag =reverseDag;
    }


    public int[][] getDistanceMatrix() {
        return distanceMatrix;
    }

    public Set<Integer> getNonTcmbOps() {
        return nonTcmbOps;
    }

    public void setNonTcmbOps(Set<Integer>nonTcmbOps){
        this.nonTcmbOps = nonTcmbOps;
    }

//    public void buildOrderMatrix(DirectedAcyclicGraph dag, int totalOpNum) {
//        orderMatrix = new int[totalOpNum + 1][totalOpNum + 1];
//        for (int i = 1; i <= totalOpNum; i++) {
//            for (int neighbor : dag.getNeighbors(i)) {
////                System.out.println(i + " " + neighbor);
//                orderMatrix[i][neighbor] = 1;
//            }
//        }
//
//        // using the transitive relationship
//        for (int k = 1; k <= totalOpNum; k++) {
//            for (int i = 1; i <= totalOpNum; i++) {
//                for (int j = 1; j <= totalOpNum; j++) {
//                    if (orderMatrix[i][k] == 1 && orderMatrix[k][j] == 1) {
//                        orderMatrix[i][j] = 1;
//                    }
//                }
//            }
//        }
////        printOrderMatrix(totalOpNum);
//    }


    public void buildDistanceMatrix(DirectedAcyclicGraph dag, int totalOpNum) {
        distanceMatrix = new int[totalOpNum + 1][totalOpNum + 1];
        final int INF = Integer.MAX_VALUE / 2;  // Prevent overflow during addition
        for (int i = 1; i <= totalOpNum; i++) {
            for (int j = 1; j <= totalOpNum; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0; // Distance to self is 0
                } else {
                    distanceMatrix[i][j] = INF; // Initialize with a large value
                }
            }
        }

        // Populate the distanceMatrix with direct edges from the DAG
        for (int i = 1; i <= totalOpNum; i++) {
            for (int neighbor : dag.getNeighbors(i)) {
                distanceMatrix[i][neighbor] = 1; // Direct dependency distance is 1
            }
        }

        // Use Floyd-Warshall algorithm to compute the shortest paths
        for (int k = 1; k <= totalOpNum; k++) {
            for (int i = 1; i <= totalOpNum; i++) {
                for (int j = 1; j <= totalOpNum; j++) {
                    if (distanceMatrix[i][j] > distanceMatrix[i][k] + distanceMatrix[k][j]) {
                        distanceMatrix[i][j] = distanceMatrix[i][k] + distanceMatrix[k][j];
                    }
                }
            }
        }

        for (int i = 1; i <= totalOpNum; i++) {
            for (int j = 1; j <= totalOpNum; j++) {
                if (distanceMatrix[i][j] == INF) {
                    distanceMatrix[i][j] = -1;
                }
            }
        }
//        printDistanceMatrix(totalOpNum);
    }


    public void printDistanceMatrix(int totalOpNum) {
        StringBuilder sb = new StringBuilder();
        sb.append("Distance Matrix:\n");
        for (int i = 1; i <= totalOpNum; i++) {
            for (int j = 1; j <= totalOpNum; j++) {
                int dis = distanceMatrix[i][j];
                sb.append(dis).append(" ");
            }
            sb.append(System.lineSeparator());
        }
        System.out.println(sb);
    }

    public void printDag() {
        System.out.println("DAG content:");
        for (Map.Entry<Integer, List<Integer>> entry : dag.getAdjacencyList().entrySet()) {
            int from = entry.getKey();
            for (int to : entry.getValue()) {
                System.out.println("Edge from " + from + " to " + to);
            }
        }
    }


}
