package com.zihao.GA_TS_SLAB.Data;

import java.util.*;


import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;


/**
 * Description: the data of the problemsetting needed to tackle the SLAB problem
 */
public class ProblemSetting {
    //machine
    private int machineNum = 0;
    // a map from a machine type to its machine id list
    private Map<Integer, List<Integer>> machineTypeToList = new HashMap<>();
    private String[] machineNames;

    //job
    private int jobNum = 0;
    private int[] opNumEachJob;

    //operation
    private int totalOpNum = 0;
    // a map that map from opID to its compatible machine list
    private int[] processingTime;
    private Map<Integer, List<Integer>> opToCompatibleList = new HashMap<>();
    private Map<Integer, Integer> opToJob = new HashMap<>();

    // TCMB
    private List<TCMB> TCMBList;

    //DAG
    private DirectedAcyclicGraph dag;

    //Reverse DAG
    private DirectedAcyclicGraph reverseDag;

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
        this.machineTypeToList = new HashMap<>();
        this.TCMBList = new ArrayList<>();
        this.dag = new DirectedAcyclicGraph();
        this.reverseDag = new DirectedAcyclicGraph();
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
}
