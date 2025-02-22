package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;

import java.util.*;

/**
 * 调度状态类，用于保存和恢复调度状态
 */
class ScheduleState {
	private final Map<Integer, Integer> machineAssignments;
	private final Map<Integer, Integer> startTimes;

	public ScheduleState(Schedule schedule) {
		this.machineAssignments = new HashMap<>(schedule.getAssignedMachine());
		this.startTimes = new HashMap<>(schedule.getStartTimes());
	}

	public void restore(Schedule schedule) {
		schedule.getAssignedMachine().clear();
		schedule.getAssignedMachine().putAll(machineAssignments);
		schedule.getStartTimes().clear();
		schedule.getStartTimes().putAll(startTimes);
	}
}

/**
 * 移动计划类，存储复合移动的各个步骤
 */
class MovePlan {
	// 为了移动op创造时间窗口
	List<OperationMove> spacePreparation;
	OperationMove targetMove;
	// 移动了
	List<OperationMove> adjustments;

	public MovePlan() {
		this.spacePreparation = new ArrayList<>();
		this.targetMove = null;
		this.adjustments = new ArrayList<>();
	}
}

/**
 * 操作移动类，存储单个操作的移动信息
 */
class OperationMove {
	int operationId;
	int newMachine;
	int newStartTime;

	public OperationMove(int operationId, int newMachine, int newStartTime) {
		this.operationId = operationId;
		this.newMachine = newMachine;
		this.newStartTime = newStartTime;
	}
}

/**
 * 复合移动操作的主类
 */
public class CompoundMove {
	// 各种类型的占比
	private static final double SHIFTING_WEIGHT = 0.4;
	private static final double MACHINE_WEIGHT = 0.3;
	private static final double IMPACT_WEIGHT = 0.3;
	private static final int MAX_ADJUSTMENT_DEPTH = 3;

	// 静态引用ProblemSetting实例
	private static final ProblemSetting ps = ProblemSetting.getInstance();

	/**
	 * 执行具有原子特性的复合移动操作
	 */
	public static boolean atomicCompoundMove(Schedule schedule, int targetOp) {
		ScheduleState initialState = new ScheduleState(schedule);
		try {
			MovePlan movePlan = prepareCompoundMove(schedule, targetOp);
			if (movePlan == null) {
				return false;
			}
			if (!executeMovePlan(schedule, movePlan)) {
				throw new RuntimeException("Move plan execution failed");
			}
			if (!validateSchedule(schedule)) {
				throw new RuntimeException("Schedule validation failed");
			}
			return true;
		} catch (Exception e) {
			initialState.restore(schedule);
			return false;
		}
	}

	/**
	 * 准备移动计划
	 */
	private static MovePlan prepareCompoundMove(Schedule schedule, int targetOp) {
		MovePlan movePlan = new MovePlan();
		int currentMachine = schedule.getAssignedMachine().get(targetOp);

		// 尝试在当前机器上移动
		if (canMoveOnCurrentMachine(schedule, targetOp)) {
			movePlan.targetMove = calculateBestPosition(schedule, targetOp, currentMachine);
			movePlan.adjustments = calculateRequiredAdjustments(schedule, targetOp, movePlan.targetMove);
			return movePlan;
		}

		// 尝试移动到其他兼容机器
		for (int newMachine : getCompatibleMachines(targetOp)) {
			if (newMachine != currentMachine) {
				List<OperationMove> spacePrep = calculateSpacePreparation(schedule, targetOp, newMachine);
				if (spacePrep != null) {
					movePlan.spacePreparation = spacePrep;
					movePlan.targetMove = calculateBestPosition(schedule, targetOp, newMachine);
					movePlan.adjustments = calculateRequiredAdjustments(schedule, targetOp, movePlan.targetMove);
					return movePlan;
				}
			}
		}
		return null;
	}

	/**
	 * 执行移动计划
	 */
	private static boolean executeMovePlan(Schedule schedule, MovePlan plan) {
		// 执行空间准备移动
		for (OperationMove move : plan.spacePreparation) {
			if (!moveOperation(schedule, move)) {
				return false;
			}
		}

		// 执行目标操作移动
		if (!moveOperation(schedule, plan.targetMove)) {
			return false;
		}

		// 执行调整移动
		for (OperationMove adjustment : plan.adjustments) {
			if (!moveOperation(schedule, adjustment)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 移动单个操作
	 */
	private static boolean moveOperation(Schedule schedule, OperationMove move) {
		if (schedule.canMoveOperation(move.operationId, move.newMachine, move.newStartTime)) {
			schedule.getAssignedMachine().put(move.operationId, move.newMachine);
			schedule.getStartTimes().put(move.operationId, move.newStartTime);
			return true;
		}
		return false;
	}

	private static boolean validateSchedule(Schedule schedule) {
		// 1. 验证机器分配的基本约束
		if (!validateMachineAssignments(schedule)) {
			return false;
		}

		// 2. 验证时间重叠
		if (!validateTimeOverlaps(schedule)) {
			return false;
		}

		// 3. 验证工序前后依赖关系(DAG约束)
		if (!validatePrecedenceConstraints(schedule)) {
			return false;
		}

		return true;
	}

	private static boolean validateMachineAssignments(Schedule schedule) {
		Map<Integer, Integer> assignments = schedule.getAssignedMachine();

		// 确保每个操作都被分配到机器
		for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
			int operationId = entry.getKey();
			int machineId = entry.getValue();

			// 机器ID必须有效
			if (machineId < 0 || machineId >= ps.getMachineNum()) {
				return false;
			}

			// 检查机器是否在该操作的兼容机器集合中
			List<Integer> compatibleMachines = ps.getOpToCompatibleList().get(operationId);
			if (!compatibleMachines.contains(machineId)) {
				return false;
			}
		}

		return true;
	}

	private static boolean validateTimeOverlaps(Schedule schedule) {
		// 获取每个机器的操作列表
		Map<Integer, List<Integer>> machineAssignments = schedule.getMachineAssignments();

		// 检查每个机器上的操作是否有时间重叠
		for (List<Integer> operations : machineAssignments.values()) {
			// 按开始时间排序
			operations.sort(Comparator.comparingInt(op -> schedule.getStartTimes().get(op)));

			// 检查相邻操作是否重叠
			for (int i = 0; i < operations.size() - 1; i++) {
				int currentOp = operations.get(i);
				int nextOp = operations.get(i + 1);

				int currentEnd = schedule.getOperationEndTime(currentOp);
				int nextStart = schedule.getStartTimes().get(nextOp);

				if (currentEnd > nextStart) {
					return false;  // 发现时间重叠
				}
			}
		}

		return true;
	}

	private static boolean validatePrecedenceConstraints(Schedule schedule) {
		Map<Integer, Integer> startTimes = schedule.getStartTimes();
		Map<Integer, List<Integer>> dag = ps.getDag().getAdjacencyList();

		// 遍历DAG中的所有边
		for (Map.Entry<Integer, List<Integer>> entry : dag.entrySet()) {
			int predecessor = entry.getKey();
			List<Integer> successors = entry.getValue();

			if (successors != null && !successors.isEmpty()) {
				// 前驱操作的结束时间
				int predecessorEnd = schedule.getOperationEndTime(predecessor);

				for (int successor : successors) {
					// 后继操作的开始时间
					int successorStart = startTimes.get(successor);

					// 验证前后依赖关系
					if (predecessorEnd > successorStart) {
						return false;
					}
				}
			}
		}

		return true;
	}

	// 获取兼容机器列表
	private static List<Integer> getCompatibleMachines(int operationId) {
		return ps.getOpToCompatibleList().get(operationId);
	}

	/**
	 * 计算最佳位置
	 */
	/**
	 * 为操作计算最佳位置
	 * @param schedule 当前调度
	 * @param operationId 目标操作ID
	 * @param machineId 目标机器ID
	 * @return 移动方案，如果找不到合适位置返回null
	 */
	private static OperationMove calculateBestPosition(Schedule schedule, int operationId, int machineId) {
		// 1. 计算时间窗口约束
		TimeWindow timeWindow = calculateFeasibleTimeWindow(schedule, operationId, machineId);
		if (timeWindow == null) {
			return null;  // 无可行时间窗口
		}

		// 2. 在可行时间窗口内采样多个候选位置
		List<Integer> candidatePositions = generateCandidatePositions(timeWindow, ps.getProcessingTime()[operationId - 1]);

		// 3. 评估每个候选位置
		int bestPosition = -1;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (int position : candidatePositions) {
			double score = evaluatePosition(schedule, operationId, machineId, position);
			if (score > bestScore) {
				bestScore = score;
				bestPosition = position;
			}
		}

		return bestPosition == -1 ? null : new OperationMove(operationId, machineId, bestPosition);
	}

	/**
	 * 计算操作的可行时间窗口
	 */
	private static TimeWindow calculateFeasibleTimeWindow(Schedule schedule, int operationId, int machineId) {
		// 1. 计算DAG约束导致的时间窗口
		int earliestStart = calculateEarliestStartTime(schedule, operationId);
		int latestEnd = calculateLatestEndTime(schedule, operationId);

		// 2. 考虑机器上已有操作的限制
		List<int[]> idlePeriods = schedule.getIdleTimePeriods(machineId);

		// 3. 在idle periods中找到满足DAG约束的时间窗口
		int procTime = ps.getProcessingTime()[operationId - 1];
		for (int[] period : idlePeriods) {
			int start = Math.max(period[0], earliestStart);
			int end = Math.min(period[1], latestEnd);

			if (end - start >= procTime) {
				return new TimeWindow(start, end);
			}
		}

		return null;
	}

	/**
	 * 生成候选位置
	 */
	private static List<Integer> generateCandidatePositions(TimeWindow window, int processingTime) {
		List<Integer> positions = new ArrayList<>();

		// 1. 添加时间窗口的起始位置
		positions.add(window.start);

		// 2. 添加时间窗口的结束位置(考虑处理时间)
		positions.add(window.end - processingTime);

		// 3. 在时间窗口内均匀采样几个位置
		int windowSize = window.end - window.start - processingTime;
		int sampleCount = Math.min(5, windowSize);  // 最多采样5个点

		if (windowSize > 0) {
			for (int i = 1; i < sampleCount; i++) {
				int pos = window.start + (windowSize * i) / sampleCount;
				positions.add(pos);
			}
		}

		return positions;
	}

	/**
	 * 评估某个位置的得分
	 */
	private static double evaluatePosition(Schedule schedule, int operationId, int machineId, int position) {
		double score = evaluateTCMBImprovement(schedule, operationId, position)
				+ evaluateTimeRelationship(schedule, operationId, machineId, position);

		return score;
	}

	// 支持类
	private static class TimeWindow {
		final int start;
		final int end;

		TimeWindow(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}


	/**
	 * 计算操作的最早可能开始时间
	 * 基于DAG约束，考虑所有前驱操作的完成时间
	 */
	private static int calculateEarliestStartTime(Schedule schedule, int operationId) {
		int earliestStart = 0;
		List<Integer> predecessors = ps.getDag().getParents(operationId);

		for (int predecessor : predecessors) {
			int predecessorEnd = schedule.getOperationEndTime(predecessor);
			earliestStart = Math.max(earliestStart, predecessorEnd);
		}

		return earliestStart;
	}

	/**
	 * 计算操作的最晚可能结束时间
	 * 基于DAG约束，考虑所有后继操作的开始时间
	 */
	private static int calculateLatestEndTime(Schedule schedule, int operationId) {
		int latestEnd = Integer.MAX_VALUE;
		List<Integer> successors = ps.getDag().getNeighbors(operationId);

		if (successors != null) {
			for (int successor : successors) {
				int successorStart = schedule.getStartTimes().get(successor);
				latestEnd = Math.min(latestEnd, successorStart);
			}
		}

		// 如果没有后继操作，使用一个合理的上界
		if (latestEnd == Integer.MAX_VALUE) {
			latestEnd = calculateScheduleUpperBound(schedule);
		}

		return latestEnd;
	}

	/**
	 * 评估TCMB约束的改善程度
	 */
	private static double evaluateTCMBImprovement(Schedule schedule, int operationId, int newStartTime) {
		double improvement = 0.0;
		List<TCMB> tcmbList = ps.getTCMBList();

		for (TCMB tcmb : tcmbList) {
			if (tcmb.getOp1() == operationId) {
				int op2 = tcmb.getOp2();
				int op2Start = schedule.getStartTimes().get(op2);

				// 计算新旧时间间隔
				int oldInterval = op2Start - schedule.getOperationEndTime(operationId);
				int newInterval = op2Start - (newStartTime + ps.getProcessingTime()[operationId - 1]);
				int timeLimit = tcmb.getTimeConstraint();

				// 计算违反程度的变化
				int oldViolation = Math.max(0, oldInterval - timeLimit);
				int newViolation = Math.max(0, newInterval - timeLimit);
				improvement += (oldViolation - newViolation);
			}
			else if (tcmb.getOp2() == operationId) {
				int op1 = tcmb.getOp1();
				// 计算新旧时间间隔
				int oldInterval = schedule.getStartTimes().get(operationId) - schedule.getOperationEndTime(op1);
				int newInterval = newStartTime - schedule.getOperationEndTime(op1);
				int timeLimit = tcmb.getTimeConstraint();

				// 计算违反程度的变化
				int oldViolation = Math.max(0, oldInterval - timeLimit);
				int newViolation = Math.max(0, newInterval - timeLimit);
				improvement += (oldViolation - newViolation);
			}
		}

		return improvement;
	}
	/**
	 * 评估操作在新位置与其他操作的时间关系
	 * 目标：
	 * 1. 保持适度的时间间隔，既提高机器利用率，又保留调整空间
	 * 2. 如果机器为空，鼓励早开始以提高整体利用率
	 */
	private static double evaluateTimeRelationship(Schedule schedule, int operationId, int machineId, int newStartTime) {
		double score = 0.0;
		List<Integer> machineOps = schedule.getMachineOperations(machineId);
		int procTime = ps.getProcessingTime()[operationId - 1];

		if (machineOps.isEmpty()) {
			// 机器为空时，鼓励早开始
			score = -newStartTime * 0.1;
		} else {
			// 计算与前后操作的间隔并评估
			for (int otherOp : machineOps) {
				if (otherOp != operationId) {
					int otherStart = schedule.getStartTimes().get(otherOp);
					int otherEnd = schedule.getOperationEndTime(otherOp);

					if (newStartTime < otherStart) {
						// 当前操作在其他操作之前
						int gap = otherStart - (newStartTime + procTime);
						score += evaluateGap(gap);
					} else {
						// 当前操作在其他操作之后
						int gap = newStartTime - otherEnd;
						score += evaluateGap(gap);
					}
				}
			}
		}

		return score;
	}

	/**
	 * 评估时间间隔的合理性
	 * 使用单峰函数，在特定间隔值时达到最高分
	 * 间隔过大或过小都会降低得分
	 */
	private static double evaluateGap(int gap) {
		if (gap < 0) {
			return Double.NEGATIVE_INFINITY;  // 时间重叠，不可行
		}

		final int IDEAL_GAP = 5;  // 理想间隔值，可以根据实际情况调整
		// 使用高斯函数计算得分
		return Math.exp(-Math.pow(gap - IDEAL_GAP, 2) / (2 * IDEAL_GAP));
	}

	/**
	 * 计算调度的上界
	 * 用于没有后继操作时的最晚结束时间估计
	 */
	private static int calculateScheduleUpperBound(Schedule schedule) {
		int maxEnd = 0;
		for (Map.Entry<Integer, Integer> entry : schedule.getStartTimes().entrySet()) {
			int opId = entry.getKey();
			int endTime = schedule.getOperationEndTime(opId);
			maxEnd = Math.max(maxEnd, endTime);
		}
		// 添加一个合理的缓冲区
		return maxEnd + 30;  // 缓冲区大小可以根据实际情况调整
	}

	/**
	 * 检查是否可以在当前机器上移动
	 */
	/**
	 * 检查操作是否可以在当前机器上移动到更好的位置
	 * @param schedule 当前调度方案
	 * @param operationId 待移动的操作ID
	 * @return 是否可以在当前机器上移动
	 */
	/**
	 * 检查操作是否可以在当前机器上移动到更好的位置
	 */
	private static boolean canMoveOnCurrentMachine(Schedule schedule, int operationId) {
		int currentMachine = schedule.getAssignedMachine().get(operationId);
		int currentStart = schedule.getStartTimes().get(operationId);

		// 直接复用calculateFeasibleTimeWindow
		TimeWindow feasibleWindow = calculateFeasibleTimeWindow(schedule, operationId, currentMachine);

		// 如果存在可行时间窗口，且窗口起始时间与当前时间不同，说明可以移动
		return feasibleWindow != null && feasibleWindow.start != currentStart;
	}

	/**
	 * 计算所需的调整
	 */
	private static List<OperationMove> calculateRequiredAdjustments(Schedule schedule, int operationId, OperationMove targetMove) {
		// TODO: 实现调整计算逻辑
		return new ArrayList<>();
	}

	/**
	 * 计算空间准备移动
	 */
	private static List<OperationMove> calculateSpacePreparation(Schedule schedule, int operationId, int newMachine) {
		// TODO: 实现空间准备计算逻辑
		return new ArrayList<>();
	}
}