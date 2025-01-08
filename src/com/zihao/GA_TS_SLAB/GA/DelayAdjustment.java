package com.zihao.GA_TS_SLAB.GA;

import java.util.*;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;


public class DelayAdjustment {
    public Chromosome chromosome;
    public ProblemSetting problemSetting;

    public DelayAdjustment(Chromosome chromosome) {
        this.chromosome = new Chromosome(chromosome);
        this.problemSetting = ProblemSetting.getInstance();

    }

    public Map<Integer, Integer> getMachinePrecedence() {
        Map<Integer, Integer> machinePrecedenceMap = new HashMap<>();
        Map<Integer, Integer> lastOperationOnMachine = new HashMap<>();

        List<Integer> OS = chromosome.getOS();
        List<Integer> MS = chromosome.getMS();
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

    public Map<Integer, Integer> getReverseMachinePrecedence(Map<Integer, Integer> machineSuc) {
        Map<Integer, Integer> machinePrev = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : machineSuc.entrySet()) {
            machinePrev.put(entry.getValue(), entry.getKey());
        }
        return machinePrev;
    }


    public Map<Integer, Integer> getL0ToV(Map<Integer, List<Integer>> jobPrev,
                                          Map<Integer, Integer> machinePrev, int[] processingTime,
                                          Map<Integer, Integer> delay, Map<Integer, Integer> predecessors) {
        Map<Integer, Integer> L0ToV = new HashMap<>();

        List<Integer> OS = chromosome.getOS();
        for (int op : OS) {
            int distance = 0;
            int predecessor = -1; // 前驱节点初始化为 -1

            // 计算机器前置操作的距离
            if (machinePrev.containsKey(op)) {
                int prevOp = machinePrev.get(op);
                int tempDistance = L0ToV.getOrDefault(prevOp, 0) + processingTime[prevOp - 1];
                if (tempDistance > distance) {
                    distance = tempDistance;
                    predecessor = prevOp; // 记录前驱节点
                }
            }

            // 计算工作前置操作的距离
            if (jobPrev.containsKey(op)) {
                for (int prevJobOp : jobPrev.get(op)) {
                    int tempDistance = L0ToV.getOrDefault(prevJobOp, 0) + processingTime[prevJobOp - 1];
                    if (tempDistance > distance) {
                        distance = tempDistance;
                        predecessor = prevJobOp; // 更新前驱节点
                    }
                }
            }

            // 加上延迟
            distance += delay.getOrDefault(op, 0);

            // 存储计算出的距离和前驱节点
            L0ToV.put(op, distance);
            predecessors.put(op, predecessor); // 记录前驱节点
        }

        return L0ToV;
    }

    void adjustByDelay(){
        Map<Integer, Integer> machineSuc = new HashMap<>();
        Map<Integer, Integer> machinePrev = new HashMap<>();
        Map<Integer, List<Integer>> jobPrev = new HashMap<>();

        machineSuc = getMachinePrecedence();
        machinePrev = getReverseMachinePrecedence(machineSuc);
        jobPrev = problemSetting.getReverseDag().getAdjacencyList();
        Map<Integer, Integer> delay = chromosome.getDelay();
        Map<Integer, Integer> predecessors = new HashMap<>();
        Map<Integer, Integer> L0ToV = getL0ToV(jobPrev, machinePrev, problemSetting.getProcessingTime(), delay, predecessors);

    }
}
