package com.zihao.GA_TS_SLAB.GA;

import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Schedule;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
import java.util.*;


public class TcmbConstraintSolver {

    // 初始化 OR-Tools 环境
    static {
        try {
            System.load("/opt/homebrew/opt/or-tools/lib/libjniortools.dylib");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();  // 打印详细的异常栈信息
            System.err.println("Failed to load libjniortools.dylib. Please check the path and permissions.");
        }
    }

    public static Schedule solve(Chromosome chromosome) {
        // 初始化 CP 模型
        CpModel model = new CpModel();

        ProblemSetting problemSetting = ProblemSetting.getInstance();
        List<TCMB> tcmbList = problemSetting.getTCMBList();
        int[] processingTime = problemSetting.getProcessingTime();

        Schedule schedule = chromosome.getSchedule();
        Map<Integer, Integer> initialStartTimes = schedule.getStartTimes();
        Map<Integer, List<Integer>> compatibleMachines = problemSetting.getOpToCompatibleList();

        int totalOps = problemSetting.getTotalOpNum();

        // 定义变量
        IntVar[] startTimes = new IntVar[totalOps];
        IntVar[] durations = new IntVar[totalOps];
        IntervalVar[] intervals = new IntervalVar[totalOps];
        IntVar[] machineVars = new IntVar[totalOps]; // 新增：操作的机器分配变量

        for (int i = 0; i < totalOps; i++) {
            if (initialStartTimes.get(i + 1) == null) {
                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
            }

            startTimes[i] = model.newIntVar(initialStartTimes.get(i + 1), 10000, "start_" + (i + 1));
            durations[i] = model.newConstant(processingTime[i]);





            // 机器分配变量，取值范围是兼容的机器ID
            List<Integer> compatibleMachineList = compatibleMachines.get(i + 1);
            long[] machineDomain = compatibleMachineList.stream().mapToLong(Integer::longValue).toArray();
            machineVars[i] = model.newIntVarFromDomain(Domain.fromValues(machineDomain), "machine_" + (i + 1));

            // 创建 IntervalVar
            intervals[i] = model.newIntervalVar(
                    startTimes[i], durations[i],
                    model.newIntVar(0, 10000, "end_" + (i + 1)),
                    "interval_" + (i + 1)
            );
        }

        // DAG 约束：操作的执行顺序
        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
                model.addLessOrEqual(
                        LinearExpr.sum(new IntVar[]{startTimes[opA - 1], durations[opA - 1]}),
                        startTimes[opB - 1]
                );
            }
        }


        int totalMachines = problemSetting.getMachineNum(); // 获取机器数量

        // 确保同一台机器上的操作不会重叠
        for (int machine = 1; machine <= totalMachines; machine++) {
            List<IntervalVar> machineIntervals = new ArrayList<>();
            for (int op = 0; op < totalOps; op++) {
                BoolVar isOnMachine = model.newBoolVar("is_op_" + (op + 1) + "_on_machine_" + machine);
                model.addEquality(machineVars[op], machine).onlyEnforceIf(isOnMachine);
                model.addDifferent(machineVars[op], machine).onlyEnforceIf(isOnMachine.not());

                // 只在该操作属于该机器时，加入 NoOverlap 约束
                machineIntervals.add(
                        model.newOptionalIntervalVar(startTimes[op], durations[op],
                                model.newIntVar(0, 10000, "end_" + (op + 1)), isOnMachine, "interval_" + (op + 1))
                );
            }

            // 机器上所有的操作必须 NoOverlap
            model.addNoOverlap(machineIntervals.toArray(new IntervalVar[0]));
        }

        // 机器分配约束：每个操作必须分配到一个兼容的机器
        for (int op = 0; op < totalOps; op++) {
            List<Integer> compatibleMachineList = compatibleMachines.get(op + 1);
            BoolVar[] machineAssigned = new BoolVar[compatibleMachineList.size()];

            for (int j = 0; j < compatibleMachineList.size(); j++) {
                int machineId = compatibleMachineList.get(j);
                machineAssigned[j] = model.newBoolVar("machine_assigned_" + (op + 1) + "_" + machineId);
            }

            // 确保每个操作被分配到 **且仅分配到** 一个机器
            model.addExactlyOne(machineAssigned);
        }

        // TCMB 约束
        for (TCMB tcmb : tcmbList) {
            int opA = tcmb.getOp1() - 1;
            int opB = tcmb.getOp2() - 1;
            int timeConstraint = tcmb.getTimeConstraint();

            model.addLessOrEqual(
                    LinearExpr.sum(new IntVar[]{startTimes[opB], model.newConstant(-processingTime[opA] - timeConstraint)}),
                    startTimes[opA]
            );
        }

        // 目标函数：最小化 makespan
        IntVar makespan = model.newIntVar(0, 10000, "makespan");

        // 定义一个包含所有结束时间的数组
        IntVar[] endTimes = new IntVar[totalOps];

        for (int i = 0; i < totalOps; i++) {
            endTimes[i] = model.newIntVar(0, 10000, "end_" + (i + 1));

            // 结束时间 = 开始时间 + 处理时间
            model.addEquality(endTimes[i], LinearExpr.sum(new IntVar[]{startTimes[i], durations[i]}));
        }

        // 让 makespan 等于所有 endTimes 的最大值
        model.addMaxEquality(makespan, endTimes);

        // 目标函数：最小化 makespan
        model.minimize(makespan);


        // 解决冲突：定义冲突惩罚
        IntVar conflictPenalty = model.newIntVar(0, 10000, "conflictPenalty");
        model.minimize(LinearExpr.sum(new IntVar[]{makespan, conflictPenalty}));

        // 求解
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

// 处理求解结果
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            // 创建符合新 Schedule 类要求的数据结构
            Map<Integer, Integer> resultStartTimes = new HashMap<>();     // 存储开始时间
            Map<Integer, Integer> resultAssignedMachine = new HashMap<>(); // 存储机器分配

            // 遍历所有操作，获取求解结果
            for (int i = 0; i < startTimes.length; i++) {
                int operationId = i + 1;  // 操作ID从1开始
                int machine = (int) solver.value(machineVars[i]);    // 获取分配的机器
                int startTime = (int) solver.value(startTimes[i]);   // 获取开始时间

                // 将结果存入对应的Map中
                resultStartTimes.put(operationId, startTime);
                resultAssignedMachine.put(operationId, machine);
            }

            System.out.println("Makespan = " + solver.value(makespan));

            // 使用新的构造函数创建 Schedule 对象
            return new Schedule(resultStartTimes, resultAssignedMachine);
        } else {
            System.out.println("No solution found.");
            return null;
        }
    }


//    public static Schedule solve(Chromosome chromosome) {
//        // 初始化模型
//        CpModel model = new CpModel();
//
//        ProblemSetting problemSetting = ProblemSetting.getInstance();  // 从实例中获取问题配置
//        List<TCMB> tcmbList = problemSetting.getTCMBList();           // 获取 TCMB 约束
//        int[] processingTime = problemSetting.getProcessingTime();     // 获取每个操作的处理时间
//
//        Schedule schedule = chromosome.getSchedule();
//        Map<Integer, Integer> initialStartTimes = schedule.getStartTimes();
//        Map<Integer, List<Integer>> compatibleMachines = problemSetting.getOpToCompatibleList();
//        int totalOps = problemSetting.getTotalOpNum();
//        // 定义变量：每个操作的开始时间
//        IntVar[] startTimes = new IntVar[problemSetting.getTotalOpNum()];
//        IntVar[] durations = new IntVar[problemSetting.getTotalOpNum()];
//        IntervalVar[] intervals = new IntervalVar[problemSetting.getTotalOpNum()];  // 定义 IntervalVars
//        IntVar[] machineVars = new IntVar[totalOps]; // 新增：操作的机器分配变量
//
//        for (int i = 0; i < problemSetting.getTotalOpNum(); i++) {
//            // 定义开始时间、持续时间和结束时间
//            if (initialStartTimes.get(i + 1) == null) {  // 修正索引为从 1 开始
//                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
//            }
//            startTimes[i] = model.newIntVar(initialStartTimes.get(i + 1), 10000, "start_" + (i + 1));
//
//            durations[i] = model.newConstant(processingTime[i]);  // 持续时间是一个常数
//
//            // 创建间隔变量
//            intervals[i] = model.newIntervalVar(startTimes[i], durations[i],
//                    model.newIntVar(0, 10000, "end_" + (i + 1)), "interval_" + (i + 1));
//        }
//
//        // 添加操作顺序约束（例如 DAG 约束）
//        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
//            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
//                // 确保 opA 在 opB 之前完成
//                model.addLessOrEqual(LinearExpr.sum(new IntVar[]{startTimes[opA - 1], durations[opA - 1]}), startTimes[opB - 1]);
//            }
//        }
//
//        // 添加机器使用冲突约束（每台机器一次只能运行一个操作）
//        Map<Integer, List<Integer>> machineAssignments = schedule.getMachineAssignments();
//        for (int machine : machineAssignments.keySet()) {
//            List<Integer> machineOps = machineAssignments.get(machine);
//            List<IntervalVar> machineIntervals = new ArrayList<>();
//
//            // 收集每台机器上的所有间隔变量
//            for (int op : machineOps) {
//                machineIntervals.add(intervals[op - 1]);  // 修正索引为从 1 开始
//            }
//
//            // 使用 NoOverlap 来确保同一台机器上的操作不重叠
//            model.addNoOverlap(machineIntervals.toArray(new IntervalVar[0]));
//        }
//
//        // 添加兼容性约束
//        for (int op = 0; op < problemSetting.getTotalOpNum(); op++) {
//            // 约束每个操作只能被分配到它兼容的机器
//            List<Integer> compatibleMachineList = compatibleMachines.get(op + 1);  // 操作编号是从1开始的
//            BoolVar[] machineAssigned = new BoolVar[compatibleMachineList.size()];
//
//            for (int j = 0; j < compatibleMachineList.size(); j++) {
//                int machineId = compatibleMachineList.get(j);
//                // 为每个兼容的机器分配一个布尔变量
//                machineAssigned[j] = model.newBoolVar("machine_assigned_" + (op + 1) + "_" + machineId);
//            }
//
//            // 至少有一个兼容的机器被分配
//            model.addAtMostOne(machineAssigned);
//        }
//
//        // 添加 TCMB 约束
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1() - 1;  // 操作编号减一
//            int opB = tcmb.getOp2() - 1;
//            int timeConstraint = tcmb.getTimeConstraint();
//
//            // 确保 (s_B - (s_A + p_A)) <= timeConstraint
//            model.addLessOrEqual(LinearExpr.sum(new IntVar[]{startTimes[opB], model.newConstant(-processingTime[opA] - timeConstraint)}), startTimes[opA]);
//        }
//
//        // 定义目标函数（最小化总完成时间）
//        IntVar makespan = model.newIntVar(0, 10000, "makespan");
//        model.addMaxEquality(makespan, startTimes);  // makespan = max{startTime + processingTime}
//        model.minimize(makespan);
//
//        // 创建求解器并求解
//        CpSolver solver = new CpSolver();
//        CpSolverStatus status = solver.solve(model);
//
//        // 输出结果并返回 Schedule 对象
//        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
//            Map<Integer, Integer> resultStartTimes = new HashMap<>();
//            Map<Integer, List<Integer>> resultMachineAssignments = new HashMap<>();
//            for (int i = 0; i < startTimes.length; i++) {
//                int machine = chromosome.getMS().get(i);  // 获取初始解中的机器分配
//                int startTime = (int) solver.value(startTimes[i]);
//                resultStartTimes.put(i + 1, startTime);  // 操作编号从1开始
//
//                // 更新机器分配
//                resultMachineAssignments.putIfAbsent(machine, new ArrayList<>());
//                resultMachineAssignments.get(machine).add(i + 1);
//            }
//            System.out.println("Makespan = " + solver.value(makespan));
//
//            // 构建并返回 Schedule 对象
//            return new Schedule(null, null, null, resultStartTimes, resultMachineAssignments, null, null);
//        } else {
//            System.out.println("No solution found.");
//            return null;
//        }
//    }

//    public static Schedule solveWithTcmbAdjustments(Chromosome chromosome) {
//        Schedule schedule = chromosome.getSchedule();
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//
//        // 获取初步的操作起始时间和机器分配
//        Map<Integer, Integer> startTimes = new HashMap<>(schedule.getStartTimes());
//        Map<Integer, List<Integer>> machineAssignments = schedule.getMachineAssignments();
//
//        // 获取TCMB约束列表
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//        int[] processingTimes = problemSetting.getProcessingTime();
//
//        // 迭代调整违反的TCMB对
//        boolean adjustmentsMade = true;
//        while (adjustmentsMade) {
//            adjustmentsMade = false;
//
//            for (TCMB tcmb : tcmbList) {
//                int opA = tcmb.getOp1();
//                int opB = tcmb.getOp2();
//                int timeConstraint = tcmb.getTimeConstraint();
//
//                int opAEndTime = startTimes.get(opA) + processingTimes[opA - 1];
//                int opBStartTime = startTimes.get(opB);
//
//                // 检查TCMB是否违反
//                if ((opBStartTime - opAEndTime) > timeConstraint) {
//                    System.out.println("Violation detected between Operation " + opA + " and Operation " + opB);
//                    // 调整策略
//                    boolean success = tryAdjustOperations(opA, opB, timeConstraint, startTimes, processingTimes, problemSetting);
//                    if (success) {
//                        adjustmentsMade = true;
//                    } else {
//                        System.out.println("Unable to resolve TCMB constraint between " + opA + " and " + opB);
//                    }
//                }
//            }
//        }
//
//        // 返回调整后的 Schedule
//        return new Schedule(null, null, null, startTimes, machineAssignments, null, null);
//    }

//    private static boolean tryAdjustOperations(int opA, int opB, int timeConstraint,
//                                               Map<Integer, Integer> startTimes, int[] processingTimes,
//                                               ProblemSetting problemSetting) {
//        int opAEndTime = startTimes.get(opA) + processingTimes[opA - 1];
//        int opBStartTime = startTimes.get(opB);
//        int timeLag = opBStartTime - opAEndTime;
//
//        // 先尝试延迟 opB 的开始时间，确保不违反 precedence 约束
//        int newOpBStartTime = opAEndTime + timeConstraint;
//        if (canAdjustOperationStartTime(opB, newOpBStartTime, startTimes, problemSetting)) {
//            startTimes.put(opB, newOpBStartTime);
//            return true;
//        }
//
//        // 如果无法延迟 opB，尝试提前 opA 的结束时间
//        int newOpAEndTime = opBStartTime - timeConstraint;
//        int newOpAStartTime = newOpAEndTime - processingTimes[opA - 1];
//        if (canAdjustOperationStartTime(opA, newOpAStartTime, startTimes, problemSetting)) {
//            startTimes.put(opA, newOpAStartTime);
//            return true;
//        }
//
//        // 如果两者都无法调整，返回 false
//        return false;
//    }
//
//    private static boolean canAdjustOperationStartTime(int op, int newStartTime,
//                                                       Map<Integer, Integer> startTimes,
//                                                       ProblemSetting problemSetting) {
//        // 检查 job precedence 约束
//        for (int precedingOp : problemSetting.getDag().getAdjacencyList().getOrDefault(op, new ArrayList<>())) {
//            int precedingEndTime = startTimes.get(precedingOp) + problemSetting.getProcessingTime()[precedingOp - 1];
//            if (newStartTime < precedingEndTime) {
//                return false;
//            }
//        }
//
//        // 检查 machine compatibility 约束
//        int machineId = problemSetting.getMachineAssignment(op);
//        for (int otherOp : problemSetting.getMachineToOperations().get(machineId)) {
//            if (op != otherOp && overlapWithOtherOperations(op, newStartTime, otherOp, startTimes, problemSetting)) {
//                return false;
//            }
//        }
//
//        return true;
//    }
//
//    private static boolean overlapWithOtherOperations(int op, int newStartTime, int otherOp,
//                                                      Map<Integer, Integer> startTimes,
//                                                      ProblemSetting problemSetting) {
//        int opEndTime = newStartTime + problemSetting.getProcessingTime()[op - 1];
//        int otherOpStartTime = startTimes.get(otherOp);
//        int otherOpEndTime = otherOpStartTime + problemSetting.getProcessingTime()[otherOp - 1];
//
//        return newStartTime < otherOpEndTime && opEndTime > otherOpStartTime;
//    }
//public static Schedule solve(Chromosome chromosome) {
//    // 获取问题配置
//    ProblemSetting problemSetting = ProblemSetting.getInstance();
//    int[] processingTimes = problemSetting.getProcessingTime();
//    DirectedAcyclicGraph dag = problemSetting.getDag();
//    DirectedAcyclicGraph reverseDag = problemSetting.getReverseDag();
//    Map<Integer, Integer> startTimes = chromosome.getSchedule().getStartTimes();
//    Map<Integer, List<Integer>> machineAssignments = chromosome.getSchedule().getMachineAssignments();
//    List<TCMB> tcmbList = problemSetting.getTCMBList();
//    Map<Integer, List<Integer>> opToCompatibleList = problemSetting.getOpToCompatibleList();
//
//    boolean feasible = true;
//
//    // 初始化冲突队列
//    Queue<TCMB> conflictQueue = new LinkedList<>(tcmbList);
//
//    // 逐一解决冲突
//    while (!conflictQueue.isEmpty()) {
//        TCMB tcmb = conflictQueue.poll();
//        int opA = tcmb.getOp1();
//        int opB = tcmb.getOp2();
//        int timeConstraint = tcmb.getTimeConstraint();
//
//        int startA = startTimes.get(opA);
//        int endA = startA + processingTimes[opA - 1];
//        int startB = startTimes.get(opB);
//        int timeLag = startB - endA;
//
//        // 如果违反 TCMB 约束
//        if (timeLag > timeConstraint) {
//            System.out.println("TCMB Constraint violated between Operation " + opA + " and Operation " + opB);
//            int newStartB = endA + timeConstraint;
//
//            // 检查 job precedence 约束
//            if (!checkPrecedence(opB, newStartB, dag, processingTimes, startTimes)) {
//                System.out.println("Precedence constraint violated for operation " + opB);
//                feasible = false;
//                continue;
//            }
//
//            // 检查 machine capacity 约束
//            if (!checkMachineCapacity(opB, newStartB, machineAssignments, startTimes, processingTimes)) {
//                System.out.println("Machine capacity constraint violated for operation " + opB);
//                feasible = false;
//                continue;
//            }
//
//            // 更新 startTimes，并检查其他可能引入的冲突
//            startTimes.put(opB, newStartB);
//            updateConflicts(tcmbList, conflictQueue, startTimes, processingTimes);
//        }
//    }
//
//    if (feasible) {
//        System.out.println("Solution found with adjusted TCMB, precedence, and machine capacity constraints.");
//        return new Schedule(null, null, null, startTimes, machineAssignments, null, null);
//    } else {
//        System.out.println("Unable to find a feasible solution.");
//        return new Schedule(null, null, null, startTimes, machineAssignments, null, null);  // 返回部分调整的调度
//    }
//}
//
//    // 检查前置操作约束
//    private static boolean checkPrecedence(int op, int newStart, DirectedAcyclicGraph dag, int[] processingTimes, Map<Integer, Integer> startTimes) {
//        List<Integer> parents = dag.getParents(op);
//        for (int parent : parents) {
//            int parentEnd = startTimes.get(parent) + processingTimes[parent - 1];
//            if (newStart < parentEnd) {
//                return false;  // 前置操作尚未完成，违反了约束
//            }
//        }
//        return true;
//    }
//
//    // 检查机器容量约束
//    private static boolean checkMachineCapacity(int op, int newStart, Map<Integer, List<Integer>> machineAssignments, Map<Integer, Integer> startTimes, int[] processingTimes) {
//        int machine = getMachineForOperation(op, machineAssignments);
//        int newEnd = newStart + processingTimes[op - 1];
//
//        for (int otherOp : machineAssignments.get(machine)) {
//            if (otherOp != op) {
//                int startOther = startTimes.get(otherOp);
//                int endOther = startOther + processingTimes[otherOp - 1];
//                if ((newStart >= startOther && newStart < endOther) || (newEnd > startOther && newEnd <= endOther)) {
//                    return false;  // 存在冲突
//                }
//            }
//        }
//        return true;
//    }
//
//    // 获取某个操作分配的机器
//    private static int getMachineForOperation(int op, Map<Integer, List<Integer>> machineAssignments) {
//        for (Map.Entry<Integer, List<Integer>> entry : machineAssignments.entrySet()) {
//            if (entry.getValue().contains(op)) {
//                return entry.getKey();
//            }
//        }
//        throw new IllegalStateException("Operation " + op + " not assigned to any machine.");
//    }
//
//    // 更新冲突队列
//    private static void updateConflicts(List<TCMB> tcmbList, Queue<TCMB> conflictQueue, Map<Integer, Integer> startTimes, int[] processingTimes) {
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1();
//            int opB = tcmb.getOp2();
//            int timeConstraint = tcmb.getTimeConstraint();
//
//            int startA = startTimes.get(opA);
//            int endA = startA + processingTimes[opA - 1];
//            int startB = startTimes.get(opB);
//            int timeLag = startB - endA;
//
//            if (timeLag > timeConstraint) {
//                // 如果有新的 TCMB 约束冲突，重新加入冲突队列
//                if (!conflictQueue.contains(tcmb)) {
//                    conflictQueue.offer(tcmb);
//                }
//            }
//        }
//    }


}


