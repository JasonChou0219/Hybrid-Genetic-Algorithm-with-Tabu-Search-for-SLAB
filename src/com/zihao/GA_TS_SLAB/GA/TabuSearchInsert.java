package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


public class TabuSearchInsert {
    private int maxIterations;
    private int tabuSize;
    private ProblemSetting problemSetting;


    public TabuSearchInsert(int maxIterations, int tabuSize) {
        this.maxIterations = maxIterations;
        this.tabuSize = tabuSize;
        this.problemSetting = ProblemSetting.getInstance();
    }

    public List<Chromosome> generateNeighborhood(Chromosome o){
        int[] processingTime = problemSetting.getProcessingTime();
        Map<Integer, Integer> delay = o.getDelay();
        Map<Integer, List<Integer>> jobSuc = problemSetting.getDag().getAdjacencyList();
        Map<Integer, List<Integer>> jobPrev = problemSetting.getReverseDag().getAdjacencyList();
        Map<Integer, Integer> machineSuc = getMachinePrecedence(o.getOS(), o.getMS());
        Map<Integer, Integer> machinePrev = getReverseMachinePrecedence(machineSuc);
        Map<Integer, Integer> L0ToV = getL0ToV(o.getOS(), jobPrev,machinePrev, processingTime, delay);
        Map<Integer, Integer> LUToN = getLUToN(o.getOS(), jobSuc, machineSuc, processingTime, delay);
        List<Integer> OS = o.getOS();
        List<Integer> MS = o.getMS();

        List<Chromosome> neighborhood = new ArrayList<>();
        for (int u : getCriticalPath(L0ToV, LUToN, processingTime)) {
            for (int i = 0; i < OS.size(); i++) {
                if (i != OS.indexOf(u)){
                    int v = OS.get(i);
                    int w = machineSuc.getOrDefault(v, -1);
                    // u is compatible to the machine v is assigned to
                    int vMachine = MS.get(i);
                    if (problemSetting.getOpToCompatibleList().get(u).contains(vMachine) &&
                            isFeasibleInsertion(u, v, w, L0ToV, LUToN, processingTime, jobSuc, jobPrev)) {
                        List<Integer> newOS = new ArrayList<>(OS);
                        int index = newOS.indexOf((Integer) u);
                        newOS.remove(index);
                        newOS.add(i, u);
                        List<Integer> newMS = new ArrayList<>(MS);
                        newMS.remove(index);
                        newMS.add(i, vMachine);
                        Chromosome newChromosome = new Chromosome(newOS, newMS);
                        neighborhood.add(newChromosome);
                    }
                }
            }
        }
        return neighborhood;
    }


    public Map<Integer, Integer> getMachinePrecedence(List<Integer> OS, List<Integer> MS) {
        Map<Integer, Integer> machinePrecedenceMap = new HashMap<>();
        Map<Integer, Integer> lastOperationOnMachine = new HashMap<>();

        for (int i = 0; i < OS.size(); i++) {
            int operation = OS.get(i);
            int machine = MS.get(i);

            if (lastOperationOnMachine.containsKey(machine)) {
                int lastOperation = lastOperationOnMachine.get(machine);
                machinePrecedenceMap.put(lastOperation, operation);
            }
            lastOperationOnMachine.put(machine, operation);
        }
        return machinePrecedenceMap;
    }
    public Map<Integer, Integer> getReverseMachinePrecedence(Map<Integer, Integer> machineSuc){
        Map<Integer, Integer> machinePrev = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : machineSuc.entrySet()) {
            int op1 = entry.getKey();
            int op2 = entry.getValue();
            machinePrev.put(op2, op1);
        }
        return machinePrev;
    }

    public Map<Integer, Integer> getL0ToV(List<Integer> OS, Map<Integer, List<Integer>> jobPrev,
                                          Map<Integer, Integer> machinePrev, int[] processingTime,
                                          Map<Integer, Integer> delay) {
        Map<Integer, Integer> L0ToV = new HashMap<>();
        for (int i = 0; i < OS.size(); i++) {
            int op = OS.get(i);
            int distance = 0;

            // Calculate machine precedence distance
            if (machinePrev.containsKey(op)) {
                int prevOp = machinePrev.get(op);
                distance = L0ToV.getOrDefault(prevOp, 0) + processingTime[prevOp - 1];
            }

            // Calculate job precedence distance
            if (jobPrev.containsKey(op)) {
                for (int prevJobOp : jobPrev.get(op)) {
                    int tempDis = L0ToV.getOrDefault(prevJobOp, 0) + processingTime[prevJobOp - 1];
                    distance = Math.max(distance, tempDis);
                }
            }

            // Add delay if any
            distance += delay.getOrDefault(op, 0);

            // Store the calculated distance in L0ToV
            L0ToV.put(op, distance);
        }
        return L0ToV;
    }

    public Map<Integer, Integer> getLUToN(List<Integer> OS, Map<Integer, List<Integer>> jobSuc,
                                          Map<Integer, Integer> machineSuc, int[] processingTime,
                                          Map<Integer, Integer> delay) {
        Map<Integer, Integer> LUToN = new HashMap<>();
        int n = OS.size();

        for (int i = n - 1; i >= 0; i--) {
            int op = OS.get(i);
            int distance = 0;

            // Calculate machine precedence distance
            if (machineSuc.containsKey(op)) {
                int nextOp = machineSuc.get(op);
                distance = LUToN.getOrDefault(nextOp, 0) +
                        processingTime[op - 1] + delay.getOrDefault(nextOp, 0);
            }

            // Calculate job precedence distance
            if (jobSuc.containsKey(op)) {
                for (int nextJobOp : jobSuc.get(op)) {
                    int tempDis = LUToN.getOrDefault(nextJobOp, 0)
                            + processingTime[op - 1] + delay.getOrDefault(nextJobOp, 0);
                    distance = Math.max(distance, tempDis);
                }
            }

            // Add delay if any

            // Store the calculated distance in LUToN
            LUToN.put(op, distance);
        }

        return LUToN;
    }

    public boolean isFeasibleInsertion(int u, int v, int w, Map<Integer, Integer> L0ToV, Map<Integer, Integer> LUToN,
                                       int[] processingTime, Map<Integer, List<Integer>> jobSuc,
                                       Map<Integer, List<Integer> >jobPre) {
//        int ru = L0ToV.get(u);
        int rv = L0ToV.getOrDefault(v, -1);
        int rw = L0ToV.getOrDefault(w, -1);
//        int piu = processingTime[u - 1];
        int piv = 0;
        int piw = 0;
        if (v != -1) {
            piv = processingTime[v - 1];
        }
        if (w != -1){
            piw = processingTime[w - 1];
        }

        if (jobSuc.containsKey(u)) {
            for (int suc : jobSuc.get(u)) {
                if (rv != -1) {
                    if (L0ToV.get(suc) + processingTime[suc - 1] < rv) {
                        return false;
                    }
                }
            }
        }

        if (jobPre.containsKey(u)){
            for (int prev : jobPre.get(u)){
                if (rw != -1){
                    if (L0ToV.get(prev) > rw + piw){
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public List<Integer> getCriticalPath(Map<Integer, Integer> L0ToV, Map<Integer, Integer> LUToN, int[] processingTime) {
        List<Integer> criticalPath = new ArrayList<>();

        int Cmax = Integer.MIN_VALUE;
        for (int op : L0ToV.keySet()) {
            int finishTime = L0ToV.get(op) + processingTime[op - 1];
            if (finishTime > Cmax) {
                Cmax = finishTime;
            }
        }

        for (int op : L0ToV.keySet()) {
            int startTime = L0ToV.get(op);
            int finishTime = startTime + processingTime[op - 1];

            // Check if this operation is on the critical path
            if (startTime + LUToN.get(op) == Cmax) {
                criticalPath.add(op);
            }
        }

        return criticalPath;
    }

}
