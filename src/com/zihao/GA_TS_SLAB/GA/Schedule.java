package com.zihao.GA_TS_SLAB.GA;

import java.util.*;

/**
 * Description: concrete schedule solution
 */

/**
 * Schedule类表示一个具体的调度解决方案
 * 核心功能是维护操作的开始时间和机器分配信息
 * 同时提供了计算空闲时间窗口和验证操作移动可行性的方法
 */
public class Schedule {
    // 静态成员，存储所有操作的处理时间（从0开始索引）
    private static int[] processingTime;

    // 核心数据结构
    private Map<Integer, Integer> startTimes;      // 存储每个操作的开始时间，key是操作ID（从1开始）
    private Map<Integer, Integer> assignedMachine; // 存储每个操作分配的机器，key是操作ID（从1开始）

    /**
     * 初始化处理时间数组
     * @param processingTime 处理时间数组，索引从0开始，对应操作ID-1
     */
    public static void initProcessingTime(int[] processingTime) {
        Schedule.processingTime = processingTime;
    }

    /**
     * 构造函数
     * @param startTimes 操作开始时间映射
     * @param assignedMachine 操作机器分配映射
     */
    public Schedule(Map<Integer, Integer> startTimes, Map<Integer, Integer> assignedMachine) {
        this.startTimes = new HashMap<>(startTimes);
        this.assignedMachine = new HashMap<>(assignedMachine);
    }

    /**
     * 深拷贝构造函数
     * @param other 被复制的Schedule对象
     */
    public Schedule(Schedule other) {
        this.startTimes = new HashMap<>(other.startTimes);
        this.assignedMachine = new HashMap<>(other.assignedMachine);
    }

    /**
     * 获取指定机器上的所有操作列表，按开始时间排序
     * @param machineId 机器ID
     * @return 该机器上的操作列表
     */
    public List<Integer> getMachineOperations(int machineId) {
        List<Integer> ops = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
            if (entry.getValue() == machineId) {
                ops.add(entry.getKey());
            }
        }
        ops.sort(Comparator.comparingInt(startTimes::get));
        return ops;
    }

    /**
     * 获取所有机器的操作分配情况
     * @return 机器到操作列表的映射
     */
    public Map<Integer, List<Integer>> getMachineAssignments() {
        Map<Integer, List<Integer>> machineAssignments = new HashMap<>();

        // 构建机器到操作列表的映射
        for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
            int opId = entry.getKey();
            int machineId = entry.getValue();
            machineAssignments.computeIfAbsent(machineId, k -> new ArrayList<>())
                    .add(opId);
        }

        // 对每个机器的操作列表按开始时间排序
        for (List<Integer> ops : machineAssignments.values()) {
            ops.sort(Comparator.comparingInt(startTimes::get));
        }

        return machineAssignments;
    }

    /**
     * 获取指定机器的空闲时间区间
     * @param machineId 机器ID
     * @return 空闲时间区间列表，每个区间是一个int数组[start, end]
     */
    public List<int[]> getIdleTimePeriods(int machineId) {
        List<Integer> machineOps = getMachineOperations(machineId);
        List<int[]> idlePeriods = new ArrayList<>();

        if (machineOps.isEmpty()) {
            return idlePeriods;
        }

        // 检查机器第一个操作之前是否有空闲时间
        int firstOpStart = startTimes.get(machineOps.get(0));
        if (firstOpStart > 0) {
            idlePeriods.add(new int[]{0, firstOpStart});
        }

        // 计算操作之间的空闲区间
        for (int i = 0; i < machineOps.size() - 1; i++) {
            int currentOp = machineOps.get(i);
            int nextOp = machineOps.get(i + 1);

            int currentEnd = startTimes.get(currentOp) + processingTime[currentOp - 1];
            int nextStart = startTimes.get(nextOp);

            if (nextStart > currentEnd) {
                idlePeriods.add(new int[]{currentEnd, nextStart});
            }
        }

        return idlePeriods;
    }

    /**
     * 检查是否可以将操作移动到指定时间和机器
     * @param operationId 操作ID
     * @param newMachineId 目标机器ID
     * @param newStartTime 目标开始时间
     * @return 是否可以移动
     */
    public boolean canMoveOperation(int operationId, int newMachineId, int newStartTime) {
        int opDuration = processingTime[operationId - 1];

        // 检查新机器是否为空
        List<Integer> machineOps = getMachineOperations(newMachineId);
        if (machineOps.isEmpty()) {
            return newStartTime >= 0;
        }

        // 检查是否在空闲区间内
        List<int[]> idlePeriods = getIdleTimePeriods(newMachineId);
        for (int[] period : idlePeriods) {
            if (period[0] <= newStartTime &&
                    period[1] >= newStartTime + opDuration) {
                return true;
            }
        }

        // 检查是否可以放在最后一个操作之后
        int lastOp = machineOps.get(machineOps.size() - 1);
        int lastEndTime = startTimes.get(lastOp) + processingTime[lastOp - 1];

        return newStartTime >= lastEndTime;
    }

    /**
     * 获取操作的结束时间
     * @param operationId 操作ID
     * @return 操作的结束时间
     */
    public int getOperationEndTime(int operationId) {
        return startTimes.get(operationId) + processingTime[operationId - 1];
    }

    /**
     * 获取所有操作的开始时间映射
     */
    public Map<Integer, Integer> getStartTimes() {
        return startTimes;
    }

    /**
     * 获取所有操作的机器分配映射
     */
    public Map<Integer, Integer> getAssignedMachine() {
        return assignedMachine;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schedule:\n");

        // 按机器组织输出
        Map<Integer, List<Integer>> machineOps = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
            machineOps.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }

        for (Map.Entry<Integer, List<Integer>> entry : machineOps.entrySet()) {
            int machineId = entry.getKey();
            sb.append("Machine ").append(machineId).append(":\n");

            // 输出空闲时间区间
            sb.append("  Idle periods: ");
            for (int[] period : getIdleTimePeriods(machineId)) {
                sb.append("[").append(period[0]).append(",")
                        .append(period[1]).append("] ");
            }
            sb.append("\n");

            // 输出操作信息
            List<Integer> ops = entry.getValue();
            ops.sort(Comparator.comparingInt(startTimes::get));
            for (int op : ops) {
                sb.append("  Operation ").append(op)
                        .append(": start=").append(startTimes.get(op))
                        .append(", duration=").append(processingTime[op - 1])
                        .append("\n");
            }
        }

        return sb.toString();
    }
}