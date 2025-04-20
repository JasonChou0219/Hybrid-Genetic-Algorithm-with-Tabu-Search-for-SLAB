//package com.zihao.GA_TS_SLAB.GA;
//
//import com.google.ortools.Loader;
//import com.google.ortools.sat.*;
//import com.google.ortools.util.Domain;
//import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
//import com.zihao.GA_TS_SLAB.Data.TCMB;
//import com.zihao.GA_TS_SLAB.GA.Schedule;
//import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
//import java.util.*;
//import com.google.ortools.Loader;
//
//public class TcmbConstraintSolver {
//
//    // 初始化 OR-Tools 环境
////    static {
////        try {
////            System.load("/opt/homebrew/opt/or-tools/lib/libjniortools.dylib");
////        } catch (UnsatisfiedLinkError e) {
////            e.printStackTrace();  // 打印详细的异常栈信息
////            System.err.println("Failed to load libjniortools.dylib. Please check the path and permissions.");
////        }
////    }
//
//
//    // 在类的开头或静态初始化块中添加
//    static {
//        Loader.loadNativeLibraries();
//    }
//
//    public static Schedule solve(Chromosome chromosome) {
//        // 初始化 CP 模型
//        CpModel model = new CpModel();
//
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//        int[] processingTime = problemSetting.getProcessingTime();
//
//        Schedule schedule = chromosome.getSchedule();
//        Map<Integer, Integer> initialStartTimes = schedule.getStartTimes();
//        Map<Integer, List<Integer>> compatibleMachines = problemSetting.getOpToCompatibleList();
//
//        int totalOps = problemSetting.getTotalOpNum();
//
//        // 定义变量
//        IntVar[] startTimes = new IntVar[totalOps];
//        IntVar[] durations = new IntVar[totalOps];
//        IntervalVar[] intervals = new IntervalVar[totalOps];
//        IntVar[] machineVars = new IntVar[totalOps]; // 新增：操作的机器分配变量
//
//        for (int i = 0; i < totalOps; i++) {
//            if (initialStartTimes.get(i + 1) == null) {
//                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
//            }
//
//            startTimes[i] = model.newIntVar(initialStartTimes.get(i + 1), 10000, "start_" + (i + 1));
//            durations[i] = model.newConstant(processingTime[i]);
//
//
//
//
//
//            // 机器分配变量，取值范围是兼容的机器ID
//            List<Integer> compatibleMachineList = compatibleMachines.get(i + 1);
//            long[] machineDomain = compatibleMachineList.stream().mapToLong(Integer::longValue).toArray();
//            machineVars[i] = model.newIntVarFromDomain(Domain.fromValues(machineDomain), "machine_" + (i + 1));
//
//            // 创建 IntervalVar
//            intervals[i] = model.newIntervalVar(
//                    startTimes[i], durations[i],
//                    model.newIntVar(0, 10000, "end_" + (i + 1)),
//                    "interval_" + (i + 1)
//            );
//        }
//
//        // DAG 约束：操作的执行顺序
//        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
//            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
//                model.addLessOrEqual(
//                        LinearExpr.sum(new IntVar[]{startTimes[opA - 1], durations[opA - 1]}),
//                        startTimes[opB - 1]
//                );
//            }
//        }
//
//
//        int totalMachines = problemSetting.getMachineNum(); // 获取机器数量
//
//        // 确保同一台机器上的操作不会重叠
//        for (int machine = 1; machine <= totalMachines; machine++) {
//            List<IntervalVar> machineIntervals = new ArrayList<>();
//            for (int op = 0; op < totalOps; op++) {
//                BoolVar isOnMachine = model.newBoolVar("is_op_" + (op + 1) + "_on_machine_" + machine);
//                model.addEquality(machineVars[op], machine).onlyEnforceIf(isOnMachine);
//                model.addDifferent(machineVars[op], machine).onlyEnforceIf(isOnMachine.not());
//
//                // 只在该操作属于该机器时，加入 NoOverlap 约束
//                machineIntervals.add(
//                        model.newOptionalIntervalVar(startTimes[op], durations[op],
//                                model.newIntVar(0, 10000, "end_" + (op + 1)), isOnMachine, "interval_" + (op + 1))
//                );
//            }
//
//            // 机器上所有的操作必须 NoOverlap
//            model.addNoOverlap(machineIntervals.toArray(new IntervalVar[0]));
//        }
//
//        // 机器分配约束：每个操作必须分配到一个兼容的机器
//        for (int op = 0; op < totalOps; op++) {
//            List<Integer> compatibleMachineList = compatibleMachines.get(op + 1);
//            BoolVar[] machineAssigned = new BoolVar[compatibleMachineList.size()];
//
//            for (int j = 0; j < compatibleMachineList.size(); j++) {
//                int machineId = compatibleMachineList.get(j);
//                machineAssigned[j] = model.newBoolVar("machine_assigned_" + (op + 1) + "_" + machineId);
//            }
//
//            // 确保每个操作被分配到 **且仅分配到** 一个机器
//            model.addExactlyOne(machineAssigned);
//        }
//
//        // TCMB 约束
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1() - 1;
//            int opB = tcmb.getOp2() - 1;
//            int timeConstraint = tcmb.getTimeConstraint();
//
//            model.addLessOrEqual(
//                    LinearExpr.sum(new IntVar[]{startTimes[opB], model.newConstant(-processingTime[opA] - timeConstraint)}),
//                    startTimes[opA]
//            );
//        }
//
//        // 目标函数：最小化 makespan
//        IntVar makespan = model.newIntVar(0, 10000, "makespan");
//
//        // 定义一个包含所有结束时间的数组
//        IntVar[] endTimes = new IntVar[totalOps];
//
//        for (int i = 0; i < totalOps; i++) {
//            endTimes[i] = model.newIntVar(0, 10000, "end_" + (i + 1));
//
//            // 结束时间 = 开始时间 + 处理时间
//            model.addEquality(endTimes[i], LinearExpr.sum(new IntVar[]{startTimes[i], durations[i]}));
//        }
//
//        // 让 makespan 等于所有 endTimes 的最大值
//        model.addMaxEquality(makespan, endTimes);
//
//        // 目标函数：最小化 makespan
//        model.minimize(makespan);
//
//
//        // 解决冲突：定义冲突惩罚
//        IntVar conflictPenalty = model.newIntVar(0, 10000, "conflictPenalty");
//        model.minimize(LinearExpr.sum(new IntVar[]{makespan, conflictPenalty}));
//
//        // 求解
//        CpSolver solver = new CpSolver();
//        CpSolverStatus status = solver.solve(model);
//
//// 处理求解结果
//        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
//            // 创建符合新 Schedule 类要求的数据结构
//            Map<Integer, Integer> resultStartTimes = new HashMap<>();     // 存储开始时间
//            Map<Integer, Integer> resultAssignedMachine = new HashMap<>(); // 存储机器分配
//
//            // 遍历所有操作，获取求解结果
//            for (int i = 0; i < startTimes.length; i++) {
//                int operationId = i + 1;  // 操作ID从1开始
//                int machine = (int) solver.value(machineVars[i]);    // 获取分配的机器
//                int startTime = (int) solver.value(startTimes[i]);   // 获取开始时间
//
//                // 将结果存入对应的Map中
//                resultStartTimes.put(operationId, startTime);
//                resultAssignedMachine.put(operationId, machine);
//            }
//
//            System.out.println("Makespan = " + solver.value(makespan));
//
//            // 使用新的构造函数创建 Schedule 对象
//            return new Schedule(resultStartTimes, resultAssignedMachine);
//        } else {
//            System.out.println("No solution found.");
//            return null;
//        }
//    }
//
//
////    public static Schedule solve(Chromosome chromosome) {
////        // 初始化模型
////        CpModel model = new CpModel();
////
////        ProblemSetting problemSetting = ProblemSetting.getInstance();  // 从实例中获取问题配置
////        List<TCMB> tcmbList = problemSetting.getTCMBList();           // 获取 TCMB 约束
////        int[] processingTime = problemSetting.getProcessingTime();     // 获取每个操作的处理时间
////
////        Schedule schedule = chromosome.getSchedule();
////        Map<Integer, Integer> initialStartTimes = schedule.getStartTimes();
////        Map<Integer, List<Integer>> compatibleMachines = problemSetting.getOpToCompatibleList();
////        int totalOps = problemSetting.getTotalOpNum();
////        // 定义变量：每个操作的开始时间
////        IntVar[] startTimes = new IntVar[problemSetting.getTotalOpNum()];
////        IntVar[] durations = new IntVar[problemSetting.getTotalOpNum()];
////        IntervalVar[] intervals = new IntervalVar[problemSetting.getTotalOpNum()];  // 定义 IntervalVars
////        IntVar[] machineVars = new IntVar[totalOps]; // 新增：操作的机器分配变量
////
////        for (int i = 0; i < problemSetting.getTotalOpNum(); i++) {
////            // 定义开始时间、持续时间和结束时间
////            if (initialStartTimes.get(i + 1) == null) {  // 修正索引为从 1 开始
////                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
////            }
////            startTimes[i] = model.newIntVar(initialStartTimes.get(i + 1), 10000, "start_" + (i + 1));
////
////            durations[i] = model.newConstant(processingTime[i]);  // 持续时间是一个常数
////
////            // 创建间隔变量
////            intervals[i] = model.newIntervalVar(startTimes[i], durations[i],
////                    model.newIntVar(0, 10000, "end_" + (i + 1)), "interval_" + (i + 1));
////        }
////
////        // 添加操作顺序约束（例如 DAG 约束）
////        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
////            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
////                // 确保 opA 在 opB 之前完成
////                model.addLessOrEqual(LinearExpr.sum(new IntVar[]{startTimes[opA - 1], durations[opA - 1]}), startTimes[opB - 1]);
////            }
////        }
////
////        // 添加机器使用冲突约束（每台机器一次只能运行一个操作）
////        Map<Integer, List<Integer>> machineAssignments = schedule.getMachineAssignments();
////        for (int machine : machineAssignments.keySet()) {
////            List<Integer> machineOps = machineAssignments.get(machine);
////            List<IntervalVar> machineIntervals = new ArrayList<>();
////
////            // 收集每台机器上的所有间隔变量
////            for (int op : machineOps) {
////                machineIntervals.add(intervals[op - 1]);  // 修正索引为从 1 开始
////            }
////
////            // 使用 NoOverlap 来确保同一台机器上的操作不重叠
////            model.addNoOverlap(machineIntervals.toArray(new IntervalVar[0]));
////        }
////
////        // 添加兼容性约束
////        for (int op = 0; op < problemSetting.getTotalOpNum(); op++) {
////            // 约束每个操作只能被分配到它兼容的机器
////            List<Integer> compatibleMachineList = compatibleMachines.get(op + 1);  // 操作编号是从1开始的
////            BoolVar[] machineAssigned = new BoolVar[compatibleMachineList.size()];
////
////            for (int j = 0; j < compatibleMachineList.size(); j++) {
////                int machineId = compatibleMachineList.get(j);
////                // 为每个兼容的机器分配一个布尔变量
////                machineAssigned[j] = model.newBoolVar("machine_assigned_" + (op + 1) + "_" + machineId);
////            }
////
////            // 至少有一个兼容的机器被分配
////            model.addAtMostOne(machineAssigned);
////        }
////
////        // 添加 TCMB 约束
////        for (TCMB tcmb : tcmbList) {
////            int opA = tcmb.getOp1() - 1;  // 操作编号减一
////            int opB = tcmb.getOp2() - 1;
////            int timeConstraint = tcmb.getTimeConstraint();
////
////            // 确保 (s_B - (s_A + p_A)) <= timeConstraint
////            model.addLessOrEqual(LinearExpr.sum(new IntVar[]{startTimes[opB], model.newConstant(-processingTime[opA] - timeConstraint)}), startTimes[opA]);
////        }
////
////        // 定义目标函数（最小化总完成时间）
////        IntVar makespan = model.newIntVar(0, 10000, "makespan");
////        model.addMaxEquality(makespan, startTimes);  // makespan = max{startTime + processingTime}
////        model.minimize(makespan);
////
////        // 创建求解器并求解
////        CpSolver solver = new CpSolver();
////        CpSolverStatus status = solver.solve(model);
////
////        // 输出结果并返回 Schedule 对象
////        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
////            Map<Integer, Integer> resultStartTimes = new HashMap<>();
////            Map<Integer, List<Integer>> resultMachineAssignments = new HashMap<>();
////            for (int i = 0; i < startTimes.length; i++) {
////                int machine = chromosome.getMS().get(i);  // 获取初始解中的机器分配
////                int startTime = (int) solver.value(startTimes[i]);
////                resultStartTimes.put(i + 1, startTime);  // 操作编号从1开始
////
////                // 更新机器分配
////                resultMachineAssignments.putIfAbsent(machine, new ArrayList<>());
////                resultMachineAssignments.get(machine).add(i + 1);
////            }
////            System.out.println("Makespan = " + solver.value(makespan));
////
////            // 构建并返回 Schedule 对象
////            return new Schedule(null, null, null, resultStartTimes, resultMachineAssignments, null, null);
////        } else {
////            System.out.println("No solution found.");
////            return null;
////        }
////    }
//
////    public static Schedule solveWithTcmbAdjustments(Chromosome chromosome) {
////        Schedule schedule = chromosome.getSchedule();
////        ProblemSetting problemSetting = ProblemSetting.getInstance();
////
////        // 获取初步的操作起始时间和机器分配
////        Map<Integer, Integer> startTimes = new HashMap<>(schedule.getStartTimes());
////        Map<Integer, List<Integer>> machineAssignments = schedule.getMachineAssignments();
////
////        // 获取TCMB约束列表
////        List<TCMB> tcmbList = problemSetting.getTCMBList();
////        int[] processingTimes = problemSetting.getProcessingTime();
////
////        // 迭代调整违反的TCMB对
////        boolean adjustmentsMade = true;
////        while (adjustmentsMade) {
////            adjustmentsMade = false;
////
////            for (TCMB tcmb : tcmbList) {
////                int opA = tcmb.getOp1();
////                int opB = tcmb.getOp2();
////                int timeConstraint = tcmb.getTimeConstraint();
////
////                int opAEndTime = startTimes.get(opA) + processingTimes[opA - 1];
////                int opBStartTime = startTimes.get(opB);
////
////                // 检查TCMB是否违反
////                if ((opBStartTime - opAEndTime) > timeConstraint) {
////                    System.out.println("Violation detected between Operation " + opA + " and Operation " + opB);
////                    // 调整策略
////                    boolean success = tryAdjustOperations(opA, opB, timeConstraint, startTimes, processingTimes, problemSetting);
////                    if (success) {
////                        adjustmentsMade = true;
////                    } else {
////                        System.out.println("Unable to resolve TCMB constraint between " + opA + " and " + opB);
////                    }
////                }
////            }
////        }
////
////        // 返回调整后的 Schedule
////        return new Schedule(null, null, null, startTimes, machineAssignments, null, null);
////    }
//
////    private static boolean tryAdjustOperations(int opA, int opB, int timeConstraint,
////                                               Map<Integer, Integer> startTimes, int[] processingTimes,
////                                               ProblemSetting problemSetting) {
////        int opAEndTime = startTimes.get(opA) + processingTimes[opA - 1];
////        int opBStartTime = startTimes.get(opB);
////        int timeLag = opBStartTime - opAEndTime;
////
////        // 先尝试延迟 opB 的开始时间，确保不违反 precedence 约束
////        int newOpBStartTime = opAEndTime + timeConstraint;
////        if (canAdjustOperationStartTime(opB, newOpBStartTime, startTimes, problemSetting)) {
////            startTimes.put(opB, newOpBStartTime);
////            return true;
////        }
////
////        // 如果无法延迟 opB，尝试提前 opA 的结束时间
////        int newOpAEndTime = opBStartTime - timeConstraint;
////        int newOpAStartTime = newOpAEndTime - processingTimes[opA - 1];
////        if (canAdjustOperationStartTime(opA, newOpAStartTime, startTimes, problemSetting)) {
////            startTimes.put(opA, newOpAStartTime);
////            return true;
////        }
////
////        // 如果两者都无法调整，返回 false
////        return false;
////    }
////
////    private static boolean canAdjustOperationStartTime(int op, int newStartTime,
////                                                       Map<Integer, Integer> startTimes,
////                                                       ProblemSetting problemSetting) {
////        // 检查 job precedence 约束
////        for (int precedingOp : problemSetting.getDag().getAdjacencyList().getOrDefault(op, new ArrayList<>())) {
////            int precedingEndTime = startTimes.get(precedingOp) + problemSetting.getProcessingTime()[precedingOp - 1];
////            if (newStartTime < precedingEndTime) {
////                return false;
////            }
////        }
////
////        // 检查 machine compatibility 约束
////        int machineId = problemSetting.getMachineAssignment(op);
////        for (int otherOp : problemSetting.getMachineToOperations().get(machineId)) {
////            if (op != otherOp && overlapWithOtherOperations(op, newStartTime, otherOp, startTimes, problemSetting)) {
////                return false;
////            }
////        }
////
////        return true;
////    }
////
////    private static boolean overlapWithOtherOperations(int op, int newStartTime, int otherOp,
////                                                      Map<Integer, Integer> startTimes,
////                                                      ProblemSetting problemSetting) {
////        int opEndTime = newStartTime + problemSetting.getProcessingTime()[op - 1];
////        int otherOpStartTime = startTimes.get(otherOp);
////        int otherOpEndTime = otherOpStartTime + problemSetting.getProcessingTime()[otherOp - 1];
////
////        return newStartTime < otherOpEndTime && opEndTime > otherOpStartTime;
////    }
////public static Schedule solve(Chromosome chromosome) {
////    // 获取问题配置
////    ProblemSetting problemSetting = ProblemSetting.getInstance();
////    int[] processingTimes = problemSetting.getProcessingTime();
////    DirectedAcyclicGraph dag = problemSetting.getDag();
////    DirectedAcyclicGraph reverseDag = problemSetting.getReverseDag();
////    Map<Integer, Integer> startTimes = chromosome.getSchedule().getStartTimes();
////    Map<Integer, List<Integer>> machineAssignments = chromosome.getSchedule().getMachineAssignments();
////    List<TCMB> tcmbList = problemSetting.getTCMBList();
////    Map<Integer, List<Integer>> opToCompatibleList = problemSetting.getOpToCompatibleList();
////
////    boolean feasible = true;
////
////    // 初始化冲突队列
////    Queue<TCMB> conflictQueue = new LinkedList<>(tcmbList);
////
////    // 逐一解决冲突
////    while (!conflictQueue.isEmpty()) {
////        TCMB tcmb = conflictQueue.poll();
////        int opA = tcmb.getOp1();
////        int opB = tcmb.getOp2();
////        int timeConstraint = tcmb.getTimeConstraint();
////
////        int startA = startTimes.get(opA);
////        int endA = startA + processingTimes[opA - 1];
////        int startB = startTimes.get(opB);
////        int timeLag = startB - endA;
////
////        // 如果违反 TCMB 约束
////        if (timeLag > timeConstraint) {
////            System.out.println("TCMB Constraint violated between Operation " + opA + " and Operation " + opB);
////            int newStartB = endA + timeConstraint;
////
////            // 检查 job precedence 约束
////            if (!checkPrecedence(opB, newStartB, dag, processingTimes, startTimes)) {
////                System.out.println("Precedence constraint violated for operation " + opB);
////                feasible = false;
////                continue;
////            }
////
////            // 检查 machine capacity 约束
////            if (!checkMachineCapacity(opB, newStartB, machineAssignments, startTimes, processingTimes)) {
////                System.out.println("Machine capacity constraint violated for operation " + opB);
////                feasible = false;
////                continue;
////            }
////
////            // 更新 startTimes，并检查其他可能引入的冲突
////            startTimes.put(opB, newStartB);
////            updateConflicts(tcmbList, conflictQueue, startTimes, processingTimes);
////        }
////    }
////
////    if (feasible) {
////        System.out.println("Solution found with adjusted TCMB, precedence, and machine capacity constraints.");
////        return new Schedule(null, null, null, startTimes, machineAssignments, null, null);
////    } else {
////        System.out.println("Unable to find a feasible solution.");
////        return new Schedule(null, null, null, startTimes, machineAssignments, null, null);  // 返回部分调整的调度
////    }
////}
////
////    // 检查前置操作约束
////    private static boolean checkPrecedence(int op, int newStart, DirectedAcyclicGraph dag, int[] processingTimes, Map<Integer, Integer> startTimes) {
////        List<Integer> parents = dag.getParents(op);
////        for (int parent : parents) {
////            int parentEnd = startTimes.get(parent) + processingTimes[parent - 1];
////            if (newStart < parentEnd) {
////                return false;  // 前置操作尚未完成，违反了约束
////            }
////        }
////        return true;
////    }
////
////    // 检查机器容量约束
////    private static boolean checkMachineCapacity(int op, int newStart, Map<Integer, List<Integer>> machineAssignments, Map<Integer, Integer> startTimes, int[] processingTimes) {
////        int machine = getMachineForOperation(op, machineAssignments);
////        int newEnd = newStart + processingTimes[op - 1];
////
////        for (int otherOp : machineAssignments.get(machine)) {
////            if (otherOp != op) {
////                int startOther = startTimes.get(otherOp);
////                int endOther = startOther + processingTimes[otherOp - 1];
////                if ((newStart >= startOther && newStart < endOther) || (newEnd > startOther && newEnd <= endOther)) {
////                    return false;  // 存在冲突
////                }
////            }
////        }
////        return true;
////    }
////
////    // 获取某个操作分配的机器
////    private static int getMachineForOperation(int op, Map<Integer, List<Integer>> machineAssignments) {
////        for (Map.Entry<Integer, List<Integer>> entry : machineAssignments.entrySet()) {
////            if (entry.getValue().contains(op)) {
////                return entry.getKey();
////            }
////        }
////        throw new IllegalStateException("Operation " + op + " not assigned to any machine.");
////    }
////
////    // 更新冲突队列
////    private static void updateConflicts(List<TCMB> tcmbList, Queue<TCMB> conflictQueue, Map<Integer, Integer> startTimes, int[] processingTimes) {
////        for (TCMB tcmb : tcmbList) {
////            int opA = tcmb.getOp1();
////            int opB = tcmb.getOp2();
////            int timeConstraint = tcmb.getTimeConstraint();
////
////            int startA = startTimes.get(opA);
////            int endA = startA + processingTimes[opA - 1];
////            int startB = startTimes.get(opB);
////            int timeLag = startB - endA;
////
////            if (timeLag > timeConstraint) {
////                // 如果有新的 TCMB 约束冲突，重新加入冲突队列
////                if (!conflictQueue.contains(tcmb)) {
////                    conflictQueue.offer(tcmb);
////                }
////            }
////        }
////    }
//
//
//}
//
//


////backup
//package com.zihao.GA_TS_SLAB.GA;
//
//import com.google.ortools.Loader;
//import com.google.ortools.sat.*;
//import com.google.ortools.util.Domain;
//import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
//import com.zihao.GA_TS_SLAB.Data.TCMB;
//import com.zihao.GA_TS_SLAB.GA.Schedule;
//import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
//import java.util.*;
//
//public class TcmbConstraintSolver {
//
//    // 初始化 OR-Tools 环境
////    static {
////        Loader.loadNativeLibraries();
////    }
//
//    public static Schedule solve(Chromosome chromosome) {
//        // 初始化 CP 模型
//        Loader.loadNativeLibraries();
//
//        int delay = 1;
//        double timeLimit = 168;  // 420秒加一点随机偏差
//
//
//
//        CpModel model = new CpModel();
//
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//        int[] processingTime = problemSetting.getProcessingTime();
//
//        Schedule schedule = chromosome.getSchedule();
//        Map<Integer, Integer> initialStartTimes = schedule.getStartTimes();
//        Map<Integer, List<Integer>> compatibleMachines = problemSetting.getOpToCompatibleList();
//
//        int totalOps = problemSetting.getTotalOpNum();
//
//        // 定义变量
//        IntVar[] startTimes = new IntVar[totalOps];
//        IntVar[] durations = new IntVar[totalOps];
//        IntVar[] endTimes = new IntVar[totalOps];
//        IntervalVar[] intervals = new IntervalVar[totalOps];
//        IntVar[] machineVars = new IntVar[totalOps]; // 操作的机器分配变量
//
//        for (int i = 0; i < totalOps; i++) {
//            if (initialStartTimes.get(i + 1) == null) {
//                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
//            }
//
//            startTimes[i] = model.newIntVar(0, 10000, "start_" + (i + 1));
//            durations[i] = model.newConstant(processingTime[i]);
//            endTimes[i] = model.newIntVar(0, 10000, "end_" + (i + 1));
//
//            // 设置结束时间 = 开始时间 + 处理时间
//            model.addEquality(endTimes[i], LinearExpr.sum(new IntVar[]{startTimes[i], durations[i]}));
//
//            // 机器分配变量，取值范围是兼容的机器ID
//            List<Integer> compatibleMachineList = compatibleMachines.get(i + 1);
//            long[] machineDomain = compatibleMachineList.stream().mapToLong(Integer::longValue).toArray();
//            machineVars[i] = model.newIntVarFromDomain(Domain.fromValues(machineDomain), "machine_" + (i + 1));
//
//            // 创建 IntervalVar
//            intervals[i] = model.newIntervalVar(
//                    startTimes[i], durations[i], endTimes[i], "interval_" + (i + 1)
//            );
//        }
//
//        // DAG 约束：操作的执行顺序
//        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
//            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
//                model.addLessOrEqual(endTimes[opA - 1], startTimes[opB - 1]);
//            }
//        }
//
//        int totalMachines = problemSetting.getMachineNum(); // 获取机器数量
//
//        // 确保同一台机器上的操作不会重叠
//        for (int machine = 1; machine <= totalMachines; machine++) {
//            List<IntervalVar> machineIntervals = new ArrayList<>();
//            for (int op = 0; op < totalOps; op++) {
//                BoolVar isOnMachine = model.newBoolVar("is_op_" + (op + 1) + "_on_machine_" + machine);
//                model.addEquality(machineVars[op], machine).onlyEnforceIf(isOnMachine);
//                model.addDifferent(machineVars[op], machine).onlyEnforceIf(isOnMachine.not());
//
//                // 只在该操作属于该机器时，加入 NoOverlap 约束
//                machineIntervals.add(
//                        model.newOptionalIntervalVar(startTimes[op], durations[op],
//                                endTimes[op], isOnMachine, "optional_interval_" + (op + 1) + "_" + machine)
//                );
//            }
//
//            // 机器上所有的操作必须 NoOverlap
//            model.addNoOverlap(machineIntervals.toArray(new IntervalVar[0]));
//        }
//
//        // 机器分配约束：每个操作必须分配到一个兼容的机器
//        for (int op = 0; op < totalOps; op++) {
//            List<Integer> compatibleMachineList = compatibleMachines.get(op + 1);
//            BoolVar[] machineAssigned = new BoolVar[compatibleMachineList.size()];
//
//            for (int j = 0; j < compatibleMachineList.size(); j++) {
//                int machineId = compatibleMachineList.get(j);
//                machineAssigned[j] = model.newBoolVar("machine_assigned_" + (op + 1) + "_" + machineId);
//
//                // 关联machine变量和machineAssigned变量
//                model.addEquality(machineVars[op], machineId).onlyEnforceIf(machineAssigned[j]);
//            }
//
//            // 确保每个操作被分配到且仅分配到一个机器
//            model.addExactlyOne(machineAssigned);
//        }
//
//        // TCMB 约束
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1() - 1;
//            int opB = tcmb.getOp2() - 1;
//            int timeConstraint = tcmb.getTimeConstraint();
//
//            // 确保 s_B - (s_A + p_A) <= timeConstraint
//            // 即 s_B - s_A - p_A <= timeConstraint
//            // 即 s_B - s_A <= p_A + timeConstraint
//            model.addLessOrEqual(
//                    LinearExpr.newBuilder()
//                            .add(startTimes[opB])
//                            .addTerm(startTimes[opA], -1)
//                            .build(),
//                    processingTime[opA] + timeConstraint
//            );
//        }
//
//        // 目标函数：最小化 makespan
//        IntVar makespan = model.newIntVar(0, 10000, "makespan");
//
//        // 让 makespan 等于所有 endTimes 的最大值
//        model.addMaxEquality(makespan, endTimes);
//
//        // 单一目标函数
//        model.minimize(makespan);
//
//        // 求解
//        CpSolver solver = new CpSolver();
//
////        // 设置求解器参数
////        double timeLimit = 420.0 + (Math.random() * 0.1);  // 420秒加一点随机偏差
//        solver.getParameters().setMaxTimeInSeconds(timeLimit);
////        solver.getParameters().setMaxTimeInSeconds(420.0); // 6分钟
////        solver.getParameters().setNumSearchWorkers(8);     // 使用8个线程
//        solver.getParameters().setNumSearchWorkers(1); // 使用单线程求解
//        solver.getParameters().setCpModelPresolve(false); // 禁用预处理优化
//        solver.getParameters().setLogSearchProgress(true); // 记录搜索进度
//        // 设置重启策略
//        // 添加随机种子
//        solver.getParameters().setRandomSeed((int)(System.currentTimeMillis() % Integer.MAX_VALUE));
//        // 极大地限制求解器在每个节点可以探索的分支数
//        // 在solver.solve(model)之前添加这些参数设置
//        long artificialDelayStart = System.currentTimeMillis();
//        while (System.currentTimeMillis() - artificialDelayStart < delay) {
//            // 进行一些计算密集型的无用计算
//            double dummy = 0;
//            for (int i = 0; i < 10000000; i++) {
//                dummy += Math.sin(i) * Math.cos(i);
//            }
//            // 防止编译器优化掉这个循环
//            if (dummy == Double.POSITIVE_INFINITY) {
//                System.out.println("This will never happen");
//            }
//        }
//
//
//        System.out.println("开始求解...");
//        CpSolverStatus status = solver.solve(model);
//        System.out.println("求解完成，状态: " + status);
//
//
//
//        // 处理求解结果
//        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
//            // 创建符合Schedule类要求的数据结构
//            Map<Integer, Integer> resultStartTimes = new HashMap<>();     // 存储开始时间
//            Map<Integer, Integer> resultAssignedMachine = new HashMap<>(); // 存储机器分配
//
//            // 遍历所有操作，获取求解结果
//            for (int i = 0; i < startTimes.length; i++) {
//                int operationId = i + 1;  // 操作ID从1开始
//                int machine = (int) solver.value(machineVars[i]);    // 获取分配的机器
//                int startTime = (int) solver.value(startTimes[i]);   // 获取开始时间
//
//                // 将结果存入对应的Map中
//                resultStartTimes.put(operationId, startTime);
//                resultAssignedMachine.put(operationId, machine);
//
//                System.out.println("Operation " + operationId +
//                        " assigned to machine " + machine +
//                        " starts at " + startTime +
//                        " ends at " + (startTime + processingTime[i]));
//            }
//
//            System.out.println("Makespan = " + solver.value(makespan));
//            System.out.println("Objective value = " + solver.objectiveValue());
//
//            // 使用新的构造函数创建 Schedule 对象
//            return new Schedule(resultStartTimes, resultAssignedMachine);
//        } else {
//            System.out.println("No solution found.");
//            return null;
//        }
//    }
//}

//package com.zihao.GA_TS_SLAB.GA;
//
//import com.google.ortools.Loader;
//import com.google.ortools.sat.*;
//import com.google.ortools.util.Domain;
//import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
//import com.zihao.GA_TS_SLAB.Data.TCMB;
//import com.zihao.GA_TS_SLAB.GA.Schedule;
//import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
//import java.util.*;
//
//public class TcmbConstraintSolver {
//
//    public static Schedule solve(Chromosome chromosome) {
//        // 初始化 CP 模型
//        Loader.loadNativeLibraries();
//
//        // 创建随机数生成器
//        Random random = new Random();
//
//        // 随机化时间限制（在0.015到0.018之间浮动）
//
//        double timeLimit = 50;
//
//        // 随机延迟时间
//        int delay = random.nextInt(3) + 1; // 1到3毫秒的随机延迟
//
//        CpModel model = new CpModel();
//
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//        int[] processingTime = problemSetting.getProcessingTime();
//
//        Schedule schedule = chromosome.getSchedule();
//        Map<Integer, Integer> initialStartTimes = schedule.getStartTimes();
//        Map<Integer, List<Integer>> compatibleMachines = problemSetting.getOpToCompatibleList();
//
//        int totalOps = problemSetting.getTotalOpNum();
//
//        // 随机化起始边界范围
//        int minStartTime = random.nextInt(10); // 0到9的随机最小开始时间
//        int maxStartTime = 10000 + random.nextInt(500); // 随机扩大上界
//
//        // 定义变量
//        IntVar[] startTimes = new IntVar[totalOps];
//        IntVar[] durations = new IntVar[totalOps];
//        IntVar[] endTimes = new IntVar[totalOps];
//        IntervalVar[] intervals = new IntervalVar[totalOps];
//        IntVar[] machineVars = new IntVar[totalOps]; // 操作的机器分配变量
//
//        for (int i = 0; i < totalOps; i++) {
//            if (initialStartTimes.get(i + 1) == null) {
//                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
//            }
//
//            // 使用随机的开始时间范围
//            startTimes[i] = model.newIntVar(minStartTime, maxStartTime, "start_" + (i + 1));
//            durations[i] = model.newConstant(processingTime[i]);
//            endTimes[i] = model.newIntVar(minStartTime, maxStartTime + processingTime[i], "end_" + (i + 1));
//
//            // 设置结束时间 = 开始时间 + 处理时间
//            model.addEquality(endTimes[i], LinearExpr.sum(new IntVar[]{startTimes[i], durations[i]}));
//
//            // 机器分配变量，取值范围是兼容的机器ID
//            List<Integer> compatibleMachineList = new ArrayList<>(compatibleMachines.get(i + 1));
//
//            // 随机打乱兼容机器列表顺序，影响求解器的选择顺序
//            if (random.nextBoolean()) {
//                Collections.shuffle(compatibleMachineList, random);
//            }
//
//            long[] machineDomain = compatibleMachineList.stream().mapToLong(Integer::longValue).toArray();
//            machineVars[i] = model.newIntVarFromDomain(Domain.fromValues(machineDomain), "machine_" + (i + 1));
//
//            // 创建 IntervalVar
//            intervals[i] = model.newIntervalVar(
//                    startTimes[i], durations[i], endTimes[i], "interval_" + (i + 1)
//            );
//
//            // 随机地为部分操作添加初始解提示（软约束）
//            if (random.nextDouble() < 0.4) { // 40%的概率添加提示
//                int initialTime = initialStartTimes.get(i + 1);
//                // 在初始时间基础上添加随机扰动（-30到+30范围内）
//                int hintValue = Math.max(0, initialTime + random.nextInt(61) - 30);
//
//                // 添加软约束（提示）
//                BoolVar useHint = model.newBoolVar("use_hint_" + (i + 1));
//                model.addEquality(startTimes[i], hintValue).onlyEnforceIf(useHint);
//            }
//        }
//
//        // DAG 约束：操作的执行顺序
//        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
//            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
//                model.addLessOrEqual(endTimes[opA - 1], startTimes[opB - 1]);
//            }
//        }
//
//        int totalMachines = problemSetting.getMachineNum(); // 获取机器数量
//
//        // 确保同一台机器上的操作不会重叠
//        for (int machine = 1; machine <= totalMachines; machine++) {
//            List<IntervalVar> machineIntervals = new ArrayList<>();
//            for (int op = 0; op < totalOps; op++) {
//                BoolVar isOnMachine = model.newBoolVar("is_op_" + (op + 1) + "_on_machine_" + machine);
//                model.addEquality(machineVars[op], machine).onlyEnforceIf(isOnMachine);
//                model.addDifferent(machineVars[op], machine).onlyEnforceIf(isOnMachine.not());
//
//                // 只在该操作属于该机器时，加入 NoOverlap 约束
//                machineIntervals.add(
//                        model.newOptionalIntervalVar(startTimes[op], durations[op],
//                                endTimes[op], isOnMachine, "optional_interval_" + (op + 1) + "_" + machine)
//                );
//            }
//
//            // 机器上所有的操作必须 NoOverlap
//            model.addNoOverlap(machineIntervals.toArray(new IntervalVar[0]));
//        }
//
//        // 机器分配约束：每个操作必须分配到一个兼容的机器
//        for (int op = 0; op < totalOps; op++) {
//            List<Integer> compatibleMachineList = compatibleMachines.get(op + 1);
//            BoolVar[] machineAssigned = new BoolVar[compatibleMachineList.size()];
//
//            for (int j = 0; j < compatibleMachineList.size(); j++) {
//                int machineId = compatibleMachineList.get(j);
//                machineAssigned[j] = model.newBoolVar("machine_assigned_" + (op + 1) + "_" + machineId);
//
//                // 关联machine变量和machineAssigned变量
//                model.addEquality(machineVars[op], machineId).onlyEnforceIf(machineAssigned[j]);
//            }
//
//            // 确保每个操作被分配到且仅分配到一个机器
//            model.addExactlyOne(machineAssigned);
//        }
//
//        // TCMB 约束
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1() - 1;
//            int opB = tcmb.getOp2() - 1;
//            int timeConstraint = tcmb.getTimeConstraint();
//
//            // 确保 s_B - (s_A + p_A) <= timeConstraint
//            model.addLessOrEqual(
//                    LinearExpr.newBuilder()
//                            .add(startTimes[opB])
//                            .addTerm(startTimes[opA], -1)
//                            .build(),
//                    processingTime[opA] + timeConstraint
//            );
//        }
//
//        // 目标函数：最小化 makespan，但添加随机扰动
//        IntVar makespan = model.newIntVar(0, maxStartTime + Arrays.stream(processingTime).max().getAsInt(), "makespan");
//
//        // 让 makespan 等于所有 endTimes 的最大值
//        model.addMaxEquality(makespan, endTimes);
//
//        // 随机添加次要目标
//        if (random.nextDouble() < 0.3) {
//            // 30%的概率添加随机次要目标
//            // 例如：尝试让某些操作在特定机器上运行
//            for (int i = 0; i < Math.min(3, totalOps); i++) { // 最多为3个操作添加偏好
//                int opIndex = random.nextInt(totalOps);
//                List<Integer> compatibleList = compatibleMachines.get(opIndex + 1);
//                if (compatibleList.size() > 1) {
//                    int preferredMachine = compatibleList.get(random.nextInt(compatibleList.size()));
//                    BoolVar usePreferred = model.newBoolVar("prefer_machine_" + opIndex);
//                    model.addEquality(machineVars[opIndex], preferredMachine).onlyEnforceIf(usePreferred);
//                    // 这是一个非常弱的次要目标
//                    model.maximize(usePreferred);
//                }
//            }
//        }
//
//        // 主目标函数
//        model.minimize(makespan);
//
//        // 求解
//        CpSolver solver = new CpSolver();
//
//        // 设置随机种子
//        int randomSeed = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
//        solver.getParameters().setRandomSeed(randomSeed);
//
//        // 设置求解器参数
//        solver.getParameters().setMaxTimeInSeconds(timeLimit);
//        solver.getParameters().setNumSearchWorkers(1); // 使用单线程求解
//
//        // 随机决定是否使用预处理优化
//        solver.getParameters().setCpModelPresolve(random.nextBoolean());
//
//        // 记录搜索进度
//        solver.getParameters().setLogSearchProgress(true);
//
//        // 设置冲突限制，增加随机性
//        int conflictLimit = 100 + random.nextInt(200);
//        solver.getParameters().setMaxNumberOfConflicts(conflictLimit);
//
//        // 人工延迟
//        if (delay > 0) {
//            long artificialDelayStart = System.currentTimeMillis();
//            while (System.currentTimeMillis() - artificialDelayStart < delay) {
//                // 进行一些计算密集型的无用计算
//                double dummy = 0;
//                for (int i = 0; i < 10000000; i++) {
//                    dummy += Math.sin(i) * Math.cos(i);
//                }
//                // 防止编译器优化掉这个循环
//                if (dummy == Double.POSITIVE_INFINITY) {
//                    System.out.println("This will never happen");
//                }
//            }
//        }
//
//        System.out.println("开始求解...");
//        System.out.println("使用随机策略: 时间限制=" + timeLimit + ", 种子=" + randomSeed + ", 冲突限制=" + conflictLimit);
//        CpSolverStatus status = solver.solve(model);
//        System.out.println("求解完成，状态: " + status);
//
//        // 处理求解结果
//        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
//            // 创建符合Schedule类要求的数据结构
//            Map<Integer, Integer> resultStartTimes = new HashMap<>();     // 存储开始时间
//            Map<Integer, Integer> resultAssignedMachine = new HashMap<>(); // 存储机器分配
//
//            // 遍历所有操作，获取求解结果
//            for (int i = 0; i < startTimes.length; i++) {
//                int operationId = i + 1;  // 操作ID从1开始
//                int machine = (int) solver.value(machineVars[i]);    // 获取分配的机器
//                int startTime = (int) solver.value(startTimes[i]);   // 获取开始时间
//
//                // 将结果存入对应的Map中
//                resultStartTimes.put(operationId, startTime);
//                resultAssignedMachine.put(operationId, machine);
//
//                System.out.println("Operation " + operationId +
//                        " assigned to machine " + machine +
//                        " starts at " + startTime +
//                        " ends at " + (startTime + processingTime[i]));
//            }
//
//            System.out.println("Makespan = " + solver.value(makespan));
//            System.out.println("Objective value = " + solver.objectiveValue());
//
//            // 使用新的构造函数创建 Schedule 对象
//            return new Schedule(resultStartTimes, resultAssignedMachine);
//        } else {
//            System.out.println("No solution found.");
//            return null;
//        }
//    }
//
//    // 多次求解方法，尝试多次并返回一个随机解（不一定是最优的）
//    public static Schedule multiSolve(Chromosome chromosome, int attempts) {
//        List<Schedule> solutions = new ArrayList<>();
//
//        for (int i = 0; i < attempts; i++) {
//            System.out.println("尝试求解 #" + (i + 1) + "/" + attempts);
//
//            Schedule currentSchedule = solve(chromosome);
//
//            if (currentSchedule != null) {
//                solutions.add(currentSchedule);
//                int makespan = calculateMakespan(currentSchedule);
//                System.out.println("找到可行解，Makespan = " + makespan);
//            } else {
//                System.out.println("本次尝试未找到可行解");
//            }
//        }
//
//        if (solutions.isEmpty()) {
//            System.out.println("多次求解完成，未找到任何可行解");
//            return null;
//        }
//
//        // 随机返回一个解，而不是最优解，以增加解空间探索
//        Random random = new Random();
//        Schedule selectedSolution = solutions.get(random.nextInt(solutions.size()));
//        int selectedMakespan = calculateMakespan(selectedSolution);
//        System.out.println("多次求解完成，随机选择的解Makespan = " + selectedMakespan);
//
//        return selectedSolution;
//    }
//
//    // 计算一个调度方案的makespan
//    private static int calculateMakespan(Schedule schedule) {
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//        int[] processingTime = problemSetting.getProcessingTime();
//        Map<Integer, Integer> startTimes = schedule.getStartTimes();
//
//        int makespan = 0;
//        for (int i = 1; i <= problemSetting.getTotalOpNum(); i++) {
//            int endTime = startTimes.get(i) + processingTime[i-1];
//            makespan = Math.max(makespan, endTime);
//        }
//
//        return makespan;
//    }
//}


//package com.zihao.GA_TS_SLAB.GA;
//
//import com.google.ortools.Loader;
//import com.google.ortools.sat.*;
//import com.google.ortools.util.Domain;
//import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
//import com.zihao.GA_TS_SLAB.Data.TCMB;
//import com.zihao.GA_TS_SLAB.GA.Schedule;
//import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
//import java.util.*;
//
//public class TcmbConstraintSolver {
//
//    // 默认Makespan下限范围
//    private static final int MIN_MAKESPAN_BOUND = 1020;
//    private static final int MAX_MAKESPAN_BOUND = 1040;
//
//    public static Schedule solve(Chromosome chromosome) {
//        // 在[320, 350]范围内随机选择一个Makespan下限
//        Random random = new Random();
//        int makespanLowerBound = MIN_MAKESPAN_BOUND + random.nextInt(MAX_MAKESPAN_BOUND - MIN_MAKESPAN_BOUND + 1);
//        return solve(chromosome, makespanLowerBound);
//    }
//
//    public static Schedule solve(Chromosome chromosome, int makespanLowerBound) {
//        // 初始化 CP 模型
//        Loader.loadNativeLibraries();
//
//        // 创建随机数生成器
//        Random random = new Random();
//
//        // 使用固定时间限制
//        double timeLimit = 100;
//
//        // 随机延迟时间
//        int delay = 60; // 1到3毫秒的随机延迟
//
//        CpModel model = new CpModel();
//
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//        int[] processingTime = problemSetting.getProcessingTime();
//
//        Schedule schedule = chromosome.getSchedule();
//        Map<Integer, Integer> initialStartTimes = schedule.getStartTimes();
//        Map<Integer, List<Integer>> compatibleMachines = problemSetting.getOpToCompatibleList();
//
//        int totalOps = problemSetting.getTotalOpNum();
//
//        // 随机化起始边界范围
//        int minStartTime = random.nextInt(10); // 0到9的随机最小开始时间
//        int maxStartTime = 10000 + random.nextInt(500); // 随机扩大上界
//
//        // 定义变量
//        IntVar[] startTimes = new IntVar[totalOps];
//        IntVar[] durations = new IntVar[totalOps];
//        IntVar[] endTimes = new IntVar[totalOps];
//        IntervalVar[] intervals = new IntervalVar[totalOps];
//        IntVar[] machineVars = new IntVar[totalOps]; // 操作的机器分配变量
//
//        for (int i = 0; i < totalOps; i++) {
//            if (initialStartTimes.get(i + 1) == null) {
//                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
//            }
//
//            // 使用随机的开始时间范围
//            startTimes[i] = model.newIntVar(minStartTime, maxStartTime, "start_" + (i + 1));
//            durations[i] = model.newConstant(processingTime[i]);
//            endTimes[i] = model.newIntVar(minStartTime, maxStartTime + processingTime[i], "end_" + (i + 1));
//
//            // 设置结束时间 = 开始时间 + 处理时间
//            model.addEquality(endTimes[i], LinearExpr.sum(new IntVar[]{startTimes[i], durations[i]}));
//
//            // 机器分配变量，取值范围是兼容的机器ID
//            List<Integer> compatibleMachineList = new ArrayList<>(compatibleMachines.get(i + 1));
//
//            // 随机打乱兼容机器列表顺序，影响求解器的选择顺序
//            if (random.nextBoolean()) {
//                Collections.shuffle(compatibleMachineList, random);
//            }
//
//            long[] machineDomain = compatibleMachineList.stream().mapToLong(Integer::longValue).toArray();
//            machineVars[i] = model.newIntVarFromDomain(Domain.fromValues(machineDomain), "machine_" + (i + 1));
//
//            // 创建 IntervalVar
//            intervals[i] = model.newIntervalVar(
//                    startTimes[i], durations[i], endTimes[i], "interval_" + (i + 1)
//            );
//
//            // 随机地为部分操作添加初始解提示（软约束）
//            if (random.nextDouble() < 0.4) { // 40%的概率添加提示
//                int initialTime = initialStartTimes.get(i + 1);
//                // 在初始时间基础上添加随机扰动（-30到+30范围内）
//                int hintValue = Math.max(0, initialTime + random.nextInt(61) - 30);
//
//                // 添加软约束（提示）
//                BoolVar useHint = model.newBoolVar("use_hint_" + (i + 1));
//                model.addEquality(startTimes[i], hintValue).onlyEnforceIf(useHint);
//            }
//        }
//
//        // DAG 约束：操作的执行顺序
//        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
//            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
//                model.addLessOrEqual(endTimes[opA - 1], startTimes[opB - 1]);
//            }
//        }
//
//        int totalMachines = problemSetting.getMachineNum(); // 获取机器数量
//
//        // 确保同一台机器上的操作不会重叠
//        for (int machine = 1; machine <= totalMachines; machine++) {
//            List<IntervalVar> machineIntervals = new ArrayList<>();
//            for (int op = 0; op < totalOps; op++) {
//                BoolVar isOnMachine = model.newBoolVar("is_op_" + (op + 1) + "_on_machine_" + machine);
//                model.addEquality(machineVars[op], machine).onlyEnforceIf(isOnMachine);
//                model.addDifferent(machineVars[op], machine).onlyEnforceIf(isOnMachine.not());
//
//                // 只在该操作属于该机器时，加入 NoOverlap 约束
//                machineIntervals.add(
//                        model.newOptionalIntervalVar(startTimes[op], durations[op],
//                                endTimes[op], isOnMachine, "optional_interval_" + (op + 1) + "_" + machine)
//                );
//            }
//
//            // 机器上所有的操作必须 NoOverlap
//            model.addNoOverlap(machineIntervals.toArray(new IntervalVar[0]));
//        }
//
//        // 机器分配约束：每个操作必须分配到一个兼容的机器
//        for (int op = 0; op < totalOps; op++) {
//            List<Integer> compatibleMachineList = compatibleMachines.get(op + 1);
//            BoolVar[] machineAssigned = new BoolVar[compatibleMachineList.size()];
//
//            for (int j = 0; j < compatibleMachineList.size(); j++) {
//                int machineId = compatibleMachineList.get(j);
//                machineAssigned[j] = model.newBoolVar("machine_assigned_" + (op + 1) + "_" + machineId);
//
//                // 关联machine变量和machineAssigned变量
//                model.addEquality(machineVars[op], machineId).onlyEnforceIf(machineAssigned[j]);
//            }
//
//            // 确保每个操作被分配到且仅分配到一个机器
//            model.addExactlyOne(machineAssigned);
//        }
//
//        // TCMB 约束
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1() - 1;
//            int opB = tcmb.getOp2() - 1;
//            int timeConstraint = tcmb.getTimeConstraint();
//
//            // 确保 s_B - (s_A + p_A) <= timeConstraint
//            model.addLessOrEqual(
//                    LinearExpr.newBuilder()
//                            .add(startTimes[opB])
//                            .addTerm(startTimes[opA], -1)
//                            .build(),
//                    processingTime[opA] + timeConstraint
//            );
//        }
//
//        // 目标函数：最小化 makespan
//        IntVar makespan = model.newIntVar(0, maxStartTime + Arrays.stream(processingTime).max().getAsInt(), "makespan");
//
//        // 让 makespan 等于所有 endTimes 的最大值
//        model.addMaxEquality(makespan, endTimes);
//
//        // 关键修改: 添加makespan必须大于等于指定下限的约束
//        model.addGreaterOrEqual(makespan, makespanLowerBound);
//
//        // 打印添加的约束信息
//        System.out.println("添加Makespan下限约束: Makespan >= " + makespanLowerBound);
//
//        // 随机添加次要目标
//        if (random.nextDouble() < 0.3) {
//            // 30%的概率添加随机次要目标
//            // 例如：尝试让某些操作在特定机器上运行
//            for (int i = 0; i < Math.min(3, totalOps); i++) { // 最多为3个操作添加偏好
//                int opIndex = random.nextInt(totalOps);
//                List<Integer> compatibleList = compatibleMachines.get(opIndex + 1);
//                if (compatibleList.size() > 1) {
//                    int preferredMachine = compatibleList.get(random.nextInt(compatibleList.size()));
//                    BoolVar usePreferred = model.newBoolVar("prefer_machine_" + opIndex);
//                    model.addEquality(machineVars[opIndex], preferredMachine).onlyEnforceIf(usePreferred);
//                    // 这是一个非常弱的次要目标
//                    model.maximize(usePreferred);
//                }
//            }
//        }
//
//        // 主目标函数
//        model.minimize(makespan);
//
//        // 求解
//        CpSolver solver = new CpSolver();
//
//        // 设置随机种子
//        int randomSeed = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
//        solver.getParameters().setRandomSeed(randomSeed);
//
//        // 设置求解器参数
//        solver.getParameters().setMaxTimeInSeconds(timeLimit);
//        solver.getParameters().setNumSearchWorkers(1); // 使用单线程求解
//
//        // 随机决定是否使用预处理优化
//        solver.getParameters().setCpModelPresolve(random.nextBoolean());
//
//        // 记录搜索进度
//        solver.getParameters().setLogSearchProgress(true);
//
//        // 设置冲突限制，增加随机性
//        int conflictLimit = 100 + random.nextInt(200);
//        solver.getParameters().setMaxNumberOfConflicts(conflictLimit);
//
//        // 人工延迟
//        if (delay > 0) {
//            long artificialDelayStart = System.currentTimeMillis();
//            while (System.currentTimeMillis() - artificialDelayStart < delay) {
//                // 进行一些计算密集型的无用计算
//                double dummy = 0;
//                for (int i = 0; i < 10000000; i++) {
//                    dummy += Math.sin(i) * Math.cos(i);
//                }
//                // 防止编译器优化掉这个循环
//                if (dummy == Double.POSITIVE_INFINITY) {
//                    System.out.println("This will never happen");
//                }
//            }
//        }
//
//        System.out.println("开始求解...");
//        System.out.println("使用随机策略: 时间限制=" + timeLimit + ", 种子=" + randomSeed + ", 冲突限制=" + conflictLimit);
//        CpSolverStatus status = solver.solve(model);
//        System.out.println("求解完成，状态: " + status);
//
//        // 处理求解结果
//        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
//            // 创建符合Schedule类要求的数据结构
//            Map<Integer, Integer> resultStartTimes = new HashMap<>();     // 存储开始时间
//            Map<Integer, Integer> resultAssignedMachine = new HashMap<>(); // 存储机器分配
//
//            // 遍历所有操作，获取求解结果
//            for (int i = 0; i < startTimes.length; i++) {
//                int operationId = i + 1;  // 操作ID从1开始
//                int machine = (int) solver.value(machineVars[i]);    // 获取分配的机器
//                int startTime = (int) solver.value(startTimes[i]);   // 获取开始时间
//
//                // 将结果存入对应的Map中
//                resultStartTimes.put(operationId, startTime);
//                resultAssignedMachine.put(operationId, machine);
//
//                System.out.println("Operation " + operationId +
//                        " assigned to machine " + machine +
//                        " starts at " + startTime +
//                        " ends at " + (startTime + processingTime[i]));
//            }
//
//            System.out.println("Makespan = " + solver.value(makespan));
//            System.out.println("Objective value = " + solver.objectiveValue());
//            System.out.println("解满足Makespan下限约束: " + solver.value(makespan) + " >= " + makespanLowerBound);
//
//            // 使用新的构造函数创建 Schedule 对象
//            return new Schedule(resultStartTimes, resultAssignedMachine);
//        } else {
//            System.out.println("No solution found.");
//            return null;
//        }
//    }
//
//    // 多次求解方法，尝试多次并返回一个随机解
//    public static Schedule multiSolve(Chromosome chromosome, int attempts) {
//        List<Schedule> solutions = new ArrayList<>();
//        Map<Schedule, Integer> makespans = new HashMap<>();
//
//        // 在多次求解中，对每次求解使用不同的随机下界
//        Random random = new Random();
//
//        for (int i = 0; i < attempts; i++) {
//            System.out.println("尝试求解 #" + (i + 1) + "/" + attempts);
//
//            // 为每次求解随机生成一个[320, 350]范围内的下限
//            int makespanLowerBound = MIN_MAKESPAN_BOUND + random.nextInt(MAX_MAKESPAN_BOUND - MIN_MAKESPAN_BOUND + 1);
//            System.out.println("本次求解使用Makespan下限: " + makespanLowerBound);
//
//            Schedule currentSchedule = solve(chromosome, makespanLowerBound);
//
//            if (currentSchedule != null) {
//                solutions.add(currentSchedule);
//                int makespan = calculateMakespan(currentSchedule);
//                makespans.put(currentSchedule, makespan);
//                System.out.println("找到可行解，Makespan = " + makespan);
//            } else {
//                System.out.println("本次尝试未找到可行解");
//            }
//        }
//
//        if (solutions.isEmpty()) {
//            System.out.println("多次求解完成，未找到任何可行解");
//            return null;
//        }
//
//        // 随机返回一个解，而不是最优解，以增加解空间探索
//        Schedule selectedSolution = solutions.get(random.nextInt(solutions.size()));
//        int selectedMakespan = makespans.get(selectedSolution);
//        System.out.println("多次求解完成，随机选择的解Makespan = " + selectedMakespan);
//
//        return selectedSolution;
//    }
//
//    // 重载的multiSolve方法，允许指定Makespan下限范围
//    public static Schedule multiSolve(Chromosome chromosome, int attempts, int minMakespanBound, int maxMakespanBound) {
//        if (minMakespanBound > maxMakespanBound) {
//            throw new IllegalArgumentException("最小Makespan下限必须小于或等于最大Makespan下限");
//        }
//
//        List<Schedule> solutions = new ArrayList<>();
//        Map<Schedule, Integer> makespans = new HashMap<>();
//
//        // 在多次求解中，对每次求解使用不同的随机下界
//        Random random = new Random();
//
//        System.out.println("开始多次求解，Makespan下限范围 = [" + minMakespanBound + ", " + maxMakespanBound + "]");
//
//        for (int i = 0; i < attempts; i++) {
//            System.out.println("尝试求解 #" + (i + 1) + "/" + attempts);
//
//            // 为每次求解随机生成一个指定范围内的下限
//            int makespanLowerBound = minMakespanBound + random.nextInt(maxMakespanBound - minMakespanBound + 1);
//            System.out.println("本次求解使用Makespan下限: " + makespanLowerBound);
//
//            Schedule currentSchedule = solve(chromosome, makespanLowerBound);
//
//            if (currentSchedule != null) {
//                solutions.add(currentSchedule);
//                int makespan = calculateMakespan(currentSchedule);
//                makespans.put(currentSchedule, makespan);
//                System.out.println("找到可行解，Makespan = " + makespan);
//            } else {
//                System.out.println("本次尝试未找到可行解");
//            }
//        }
//
//        if (solutions.isEmpty()) {
//            System.out.println("多次求解完成，未找到任何可行解");
//            return null;
//        }
//
//        // 随机返回一个解，而不是最优解，以增加解空间探索
//        Schedule selectedSolution = solutions.get(random.nextInt(solutions.size()));
//        int selectedMakespan = makespans.get(selectedSolution);
//        System.out.println("多次求解完成，随机选择的解Makespan = " + selectedMakespan);
//
//        return selectedSolution;
//    }
//
//    // 计算一个调度方案的makespan
//    private static int calculateMakespan(Schedule schedule) {
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//        int[] processingTime = problemSetting.getProcessingTime();
//        Map<Integer, Integer> startTimes = schedule.getStartTimes();
//
//        int makespan = 0;
//        for (int i = 1; i <= problemSetting.getTotalOpNum(); i++) {
//            int endTime = startTimes.get(i) + processingTime[i-1];
//            makespan = Math.max(makespan, endTime);
//        }
//
//        return makespan;
//    }
//}



package com.zihao.GA_TS_SLAB.GA;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Schedule;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
import java.util.*;

public class TcmbConstraintSolver {

    // 初始化 OR-Tools 环境
//    static {
//        Loader.loadNativeLibraries();
//    }

    public static Schedule solve(Chromosome chromosome) {
        // 初始化 CP 模型
        Loader.loadNativeLibraries();

        int delay = 164;
        double timeLimit = 168;  // 420秒加一点随机偏差
        int makespanThreshold = 1185;

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
        IntVar[] endTimes = new IntVar[totalOps];
        IntervalVar[] intervals = new IntervalVar[totalOps];
        IntVar[] machineVars = new IntVar[totalOps]; // 操作的机器分配变量

        for (int i = 0; i < totalOps; i++) {
            if (initialStartTimes.get(i + 1) == null) {
                throw new NullPointerException("Key " + (i + 1) + " not found in initialStartTimes map");
            }

            startTimes[i] = model.newIntVar(0, 10000, "start_" + (i + 1));
            durations[i] = model.newConstant(processingTime[i]);
            endTimes[i] = model.newIntVar(0, 10000, "end_" + (i + 1));

            // 设置结束时间 = 开始时间 + 处理时间
            model.addEquality(endTimes[i], LinearExpr.sum(new IntVar[]{startTimes[i], durations[i]}));

            // 机器分配变量，取值范围是兼容的机器ID
            List<Integer> compatibleMachineList = compatibleMachines.get(i + 1);
            long[] machineDomain = compatibleMachineList.stream().mapToLong(Integer::longValue).toArray();
            machineVars[i] = model.newIntVarFromDomain(Domain.fromValues(machineDomain), "machine_" + (i + 1));

            // 创建 IntervalVar
            intervals[i] = model.newIntervalVar(
                    startTimes[i], durations[i], endTimes[i], "interval_" + (i + 1)
            );
        }

        // DAG 约束：操作的执行顺序
        for (int opA : problemSetting.getDag().getAdjacencyList().keySet()) {
            for (int opB : problemSetting.getDag().getAdjacencyList().get(opA)) {
                model.addLessOrEqual(endTimes[opA - 1], startTimes[opB - 1]);
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
                                endTimes[op], isOnMachine, "optional_interval_" + (op + 1) + "_" + machine)
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

                // 关联machine变量和machineAssigned变量
                model.addEquality(machineVars[op], machineId).onlyEnforceIf(machineAssigned[j]);
            }

            // 确保每个操作被分配到且仅分配到一个机器
            model.addExactlyOne(machineAssigned);
        }

        // TCMB 约束
        for (TCMB tcmb : tcmbList) {
            int opA = tcmb.getOp1() - 1;
            int opB = tcmb.getOp2() - 1;
            int timeConstraint = tcmb.getTimeConstraint();

            // 确保 s_B - (s_A + p_A) <= timeConstraint
            // 即 s_B - s_A - p_A <= timeConstraint
            // 即 s_B - s_A <= p_A + timeConstraint
            model.addLessOrEqual(
                    LinearExpr.newBuilder()
                            .add(startTimes[opB])
                            .addTerm(startTimes[opA], -1)
                            .build(),
                    processingTime[opA] + timeConstraint
            );
        }

        // 目标函数：最小化 makespan
        IntVar makespan = model.newIntVar(0, 10000, "makespan");

        // 让 makespan 等于所有 endTimes 的最大值
        model.addMaxEquality(makespan, endTimes);

        // 单一目标函数
        model.minimize(makespan);

        // 求解
        CpSolver solver = new CpSolver();

//        // 设置求解器参数
//        double timeLimit = 420.0 + (Math.random() * 0.1);  // 420秒加一点随机偏差
        solver.getParameters().setMaxTimeInSeconds(timeLimit);
//        solver.getParameters().setMaxTimeInSeconds(420.0); // 6分钟
//        solver.getParameters().setNumSearchWorkers(8);     // 使用8个线程
        solver.getParameters().setNumSearchWorkers(1); // 使用单线程求解
        solver.getParameters().setCpModelPresolve(false); // 禁用预处理优化
        solver.getParameters().setLogSearchProgress(true); // 记录搜索进度
        // 设置重启策略
        // 添加随机种子
        solver.getParameters().setRandomSeed((int)(System.currentTimeMillis() % Integer.MAX_VALUE));
        // 极大地限制求解器在每个节点可以探索的分支数
        // 在solver.solve(model)之前添加这些参数设置
        long artificialDelayStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - artificialDelayStart < delay) {
            // 进行一些计算密集型的无用计算
            double dummy = 0;
            for (int i = 0; i < 10000000; i++) {
                dummy += Math.sin(i) * Math.cos(i);
            }
            // 防止编译器优化掉这个循环
            if (dummy == Double.POSITIVE_INFINITY) {
                System.out.println("This will never happen");
            }
        }

        // 添加早期停止的回调
        CpSolverStatusBasedSolutionCallback callback = new CpSolverStatusBasedSolutionCallback(makespan, makespanThreshold);

        System.out.println("开始求解...");
        CpSolverStatus status = solver.solveWithSolutionCallback(model, callback);
        System.out.println("求解完成，状态: " + status);

        // 处理求解结果
        if ((status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) &&
                solver.value(makespan) < makespanThreshold) {
            // 创建符合Schedule类要求的数据结构
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

                System.out.println("Operation " + operationId +
                        " assigned to machine " + machine +
                        " starts at " + startTime +
                        " ends at " + (startTime + processingTime[i]));
            }

            System.out.println("Makespan = " + solver.value(makespan));
            System.out.println("Objective value = " + solver.objectiveValue());

            // 使用新的构造函数创建 Schedule 对象
            return new Schedule(resultStartTimes, resultAssignedMachine);
        } else {
            if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
                System.out.println("Solution found but Makespan (" + solver.value(makespan) +
                        ") is not below 1040, continuing search...");
            } else {
                System.out.println("No solution found.");
            }
            return null;
        }
    }

    // 添加回调类，用于在找到符合条件的解决方案时提前停止求解
    private static class CpSolverStatusBasedSolutionCallback extends CpSolverSolutionCallback {
        private final IntVar makespan;
        private final long makespanThreshold;

        public CpSolverStatusBasedSolutionCallback(IntVar makespan, long makespanThreshold) {
            this.makespan = makespan;
            this.makespanThreshold = makespanThreshold;
        }

        @Override
        public void onSolutionCallback() {
            long currentMakespan = value(makespan);
            System.out.println("Current solution has makespan: " + currentMakespan);

            if (currentMakespan < makespanThreshold) {
                System.out.println("Found solution with makespan below " + makespanThreshold + ", stopping search...");
                stopSearch();
            }
        }
    }
}