package com.zihao.GA_TS_SLAB.GA;

import java.util.*;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

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
    private static ProblemSetting pb = ProblemSetting.getInstance();

    // 核心数据结构
    private Map<Integer, Integer> startTimes;      // 存储每个操作的开始时间，key是操作ID（从1开始）
    private Map<Integer, Integer> assignedMachine; // 存储每个操作分配的机器，key是操作ID（从1开始）


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

            int currentEnd = startTimes.get(currentOp) +
                    pb.getProcessingTime()[currentOp - 1];
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
        int opDuration = pb.getProcessingTime()[operationId - 1];

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
        int lastEndTime = startTimes.get(lastOp) + pb.getProcessingTime()[lastOp - 1];

        return newStartTime >= lastEndTime;
    }

    /**
     * 获取操作的结束时间
     * @param operationId 操作ID
     * @return 操作的结束时间
     */
    public int getOperationEndTime(int operationId) {
        return startTimes.get(operationId) + pb.getProcessingTime()[operationId - 1];
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
                        .append(", duration=").append(pb.getProcessingTime()[op - 1])
                        .append("\n");
            }
        }

        return sb.toString();
    }
    /**
     * 检查调度方案是否满足DAG中的前驱约束
     * @return 如果满足所有前驱约束则返回true，否则返回false
     */
    public boolean checkPrecedenceConstraints() {
        int[][] distanceMatrix = pb.getDistanceMatrix();
        Set<Integer> completedOps = new HashSet<>();

        // 遍历所有操作及其开始时间
        List<Map.Entry<Integer, Integer>> sortedOps = new ArrayList<>(startTimes.entrySet());
        sortedOps.sort(Map.Entry.comparingByValue()); // 按开始时间排序

        for (Map.Entry<Integer, Integer> entry : sortedOps) {
            int op = entry.getKey();

            // 检查op的所有前驱操作是否已完成
            for (int i = 1; i < distanceMatrix.length; i++) {
                // 如果i是op的前驱操作（距离大于0）且未在已完成集合中
                if (distanceMatrix[i][op] > 0 && !completedOps.contains(i)) {
                    System.out.println("操作 " + op + " 的前驱操作 " + i + " 尚未完成，违反了前驱约束");
                    return false;
                }
            }

            // 将当前操作添加到已完成集合
            completedOps.add(op);
        }

        return true;
    }

    /**
     * 检查调度方案中的操作是否被分配给了兼容的机器
     * @return 如果所有操作都分配给了兼容机器则返回true，否则返回false
     */
    public boolean checkCompatibleMachines() {
        for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
            int op = entry.getKey();
            int machine = entry.getValue();

            // 获取操作可兼容的机器列表
            List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);

            // 检查分配的机器是否在兼容列表中
            if (!compatibleMachines.contains(machine)) {
                System.out.println("操作 " + op + " 被分配到了不兼容的机器 " + machine);
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否存在机器占用冲突（同一机器上的操作时间重叠）
     * @return 如果没有机器占用冲突则返回true，否则返回false
     */
    public boolean checkMachineOccupation() {
        // 获取每台机器上的操作分配
        Map<Integer, List<Integer>> machineAssignments = getMachineAssignments();

        // 检查每台机器上的操作
        for (Map.Entry<Integer, List<Integer>> entry : machineAssignments.entrySet()) {
            int machineId = entry.getKey();
            List<Integer> operations = entry.getValue();

            // 对操作按开始时间排序
            operations.sort(Comparator.comparingInt(startTimes::get));

            // 检查相邻操作是否有时间重叠
            for (int i = 0; i < operations.size() - 1; i++) {
                int currentOp = operations.get(i);
                int nextOp = operations.get(i + 1);

                int currentEndTime = startTimes.get(currentOp) + pb.getProcessingTime()[currentOp - 1];
                int nextStartTime = startTimes.get(nextOp);

                if (currentEndTime > nextStartTime) {
                    System.out.println("机器 " + machineId + " 上的操作 " + currentOp +
                            " (结束时间: " + currentEndTime + ") 与操作 " +
                            nextOp + " (开始时间: " + nextStartTime + ") 存在时间重叠");
                    return false;
                }
            }
        }

        return true;
    }
    /**
     * 按照机器顺序输出每台机器上的操作调度情况
     * 输出格式为：Machine x: op[start,end] op[start,end] ...
     * @return 格式化的调度输出字符串
     */
    public String printMachineSchedule() {
        int totalMachines = pb.getMachineNum();
        int[] processingTimes = pb.getProcessingTime();
        StringBuilder result = new StringBuilder();

        // 为每台机器创建一个列表，存储分配给该机器的操作
        Map<Integer, List<OpSchedule>> machineSchedules = new HashMap<>();
        for (int m = 1; m <= totalMachines; m++) {
            machineSchedules.put(m, new ArrayList<>());
        }

        // 收集每个操作的调度信息
        for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
            int opId = entry.getKey();
            int machineId = entry.getValue();
            int startTime = startTimes.get(opId);
            int procTime = processingTimes[opId - 1]; // 处理时间数组是从0开始索引的
            int endTime = startTime + procTime;

            // 创建操作调度对象并添加到对应机器的列表中
            machineSchedules.get(machineId).add(new OpSchedule(opId, startTime, endTime));
        }

        // 对每台机器上的操作按开始时间排序
        for (int m = 1; m <= totalMachines; m++) {
            List<OpSchedule> schedules = machineSchedules.get(m);
            Collections.sort(schedules);

            // 格式化输出该机器的调度
            result.append("Machine ").append(m).append(": ");
            for (OpSchedule op : schedules) {
                result.append("op").append(op.opId)
                        .append("[").append(op.startTime).append(",").append(op.endTime).append("] ");
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * 内部辅助类，表示一个操作的调度信息
     */
    private static class OpSchedule implements Comparable<OpSchedule> {
        int opId;
        int startTime;
        int endTime;

        public OpSchedule(int opId, int startTime, int endTime) {
            this.opId = opId;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public int compareTo(OpSchedule other) {
            // 按开始时间排序
            return Integer.compare(this.startTime, other.startTime);
        }
    }
}