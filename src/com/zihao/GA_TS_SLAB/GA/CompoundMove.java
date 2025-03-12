package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;

import java.util.*;

/**
 * 复合移动操作的抽象基类
 * 所有具体的复合移动操作（交换、插入、重分配）都应继承此类
 */
abstract class CompoundMove {
	// 问题设置引用
	protected final ProblemSetting ps = ProblemSetting.getInstance();

	// 随机数生成器
	protected final Random random = new Random();

	/**
	 * 执行复合移动操作
	 * @param schedule 当前调度
	 * @return 操作是否成功
	 */
	public boolean execute(Schedule schedule) {
		// 保存原始状态（用于回滚）
		ScheduleState originalState = new ScheduleState(schedule);

		try {
			// 1. 选择目标操作
			if (!selectTargetOperations(schedule)) {
				return false;
			}

			// 2. 执行具体的移动操作
			if (!doMove(schedule)) {
				throw new Exception("移动操作执行失败");
			}

			// 3. 检查DAG约束
			if (!validateDAGConstraints(schedule)) {
				throw new Exception("违反DAG约束");
			}

			// 4. 解决机器冲突
			if (!resolveConflic(schedule)) {
				throw new Exception("无法解决机器冲突");
			}

			// 5. 最终验证所有约束
			if (!validateSchedule(schedule)) {
				throw new Exception("最终验证失败");
			}

			return true;
		} catch (Exception e) {
			// 操作失败，恢复原始状态
			originalState.restore(schedule);
			return false;
		}
	}

	/**
	 * 获取操作类型
	 * @return 操作类型的枚举值
	 */
	public abstract MoveType getType();

	/**
	 * 选择操作的目标（子类必须实现）
	 * @param schedule 当前调度
	 * @return 是否成功选择目标
	 */
	protected abstract boolean selectTargetOperations(Schedule schedule);

	/**
	 * 执行具体的移动操作（子类必须实现）
	 * @param schedule 当前调度
	 * @return 操作是否成功
	 */
	protected abstract boolean doMove(Schedule schedule);

	/**
	 * 解决可能的机器冲突（子类必须实现）
	 * @param schedule 当前调度
	 * @return 是否成功解决冲突
	 */
	protected abstract boolean resolveConflic(Schedule schedule);

	/**
	 * 验证DAG约束
	 * @param schedule 当前调度
	 * @return 约束是否满足
	 */
	protected boolean validateDAGConstraints(Schedule schedule) {
		Map<Integer, List<Integer>> dag = ps.getDag().getAdjacencyList();

		for (Map.Entry<Integer, List<Integer>> entry : dag.entrySet()) {
			int op = entry.getKey();
			List<Integer> successors = entry.getValue();

			if (successors != null) {
				int opEndTime = schedule.getOperationEndTime(op);

				for (int successor : successors) {
					int successorStartTime = schedule.getStartTimes().get(successor);

					if (opEndTime > successorStartTime) {
						return false;
					}
				}
			}
		}

		return true;
	}

	/**
	 * 验证机器占用约束
	 * @param schedule 当前调度
	 * @return 约束是否满足
	 */
	protected boolean validateMachineOccupation(Schedule schedule) {
		// 按机器分组操作
		Map<Integer, List<Integer>> machineOps = new HashMap<>();

		for (Map.Entry<Integer, Integer> entry : schedule.getAssignedMachine().entrySet()) {
			int op = entry.getKey();
			int machine = entry.getValue();

			machineOps.computeIfAbsent(machine, k -> new ArrayList<>()).add(op);
		}

		// 检查每台机器上的操作是否有重叠
		for (List<Integer> ops : machineOps.values()) {
			// 按开始时间排序
			ops.sort(Comparator.comparingInt(op -> schedule.getStartTimes().get(op)));

			// 检查相邻操作是否重叠
			for (int i = 0; i < ops.size() - 1; i++) {
				int currentOp = ops.get(i);
				int nextOp = ops.get(i + 1);

				int currentEndTime = schedule.getOperationEndTime(currentOp);
				int nextStartTime = schedule.getStartTimes().get(nextOp);

				if (currentEndTime > nextStartTime) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 综合验证所有约束
	 * @param schedule 当前调度
	 * @return 约束是否满足
	 */
	protected boolean validateSchedule(Schedule schedule) {
		// 验证DAG约束
		if (!validateDAGConstraints(schedule)) {
			return false;
		}

		// 验证机器占用约束
		if (!validateMachineOccupation(schedule)) {
			return false;
		}

		return true;
	}

	/**
	 * 计算操作的最早可能开始时间（基于DAG约束）
	 */
	protected int calculateEarliestStartTime(Schedule schedule, int op) {
		int earliestStart = 0;
		List<Integer> predecessors = ps.getDag().getParents(op);

		if (predecessors != null) {
			for (int predecessor : predecessors) {
				int predecessorEnd = schedule.getOperationEndTime(predecessor);
				earliestStart = Math.max(earliestStart, predecessorEnd);
			}
		}

		return earliestStart;
	}

	/**
	 * 计算操作的最晚可能结束时间（基于DAG约束）
	 */
	protected int calculateLatestEndTime(Schedule schedule, int op) {
		int latestEnd = Integer.MAX_VALUE;
		List<Integer> successors = ps.getDag().getNeighbors(op);

		if (successors != null && !successors.isEmpty()) {
			for (int successor : successors) {
				int successorStart = schedule.getStartTimes().get(successor);
				latestEnd = Math.min(latestEnd, successorStart);
			}
		}

		// 如果没有后继操作，使用一个合理的上界
		if (latestEnd == Integer.MAX_VALUE) {
			//todo 这里的边界要考虑
			//todo 这里的调整范围应该根据temperature决定
			latestEnd = Utility.calculateMakespan(schedule) + 10; // 一个足够大的缓冲区
		}

		return latestEnd;
	}

	/**
	 * 判断两个操作是否有直接依赖关系（DAG约束）
	 */
	protected boolean hasDirectDependency(int op1, int op2) {
		//todo 不只是紧挨着的successor和predecessor
 		Map<Integer, List<Integer>> dag = ps.getDag().getAdjacencyList();

		// 检查op1是否是op2的前驱
		List<Integer> op1Successors = dag.get(op1);
		if (op1Successors != null && op1Successors.contains(op2)) {
			return true;
		}

		// 检查op2是否是op1的前驱
		List<Integer> op2Successors = dag.get(op2);
		if (op2Successors != null && op2Successors.contains(op1)) {
			return true;
		}

		return false;
	}

	/**
	 * 寻找机器上的空闲时段
	 * @param schedule 当前调度
	 * @param machine 机器ID
	 * @param excludeOp 要排除的操作ID（-1表示不排除任何操作）
	 * @return 空闲时段列表，每个时段为[开始时间, 结束时间]
	 */
	protected List<int[]> findIdlePeriods(Schedule schedule, int machine, int excludeOp) {
		List<int[]> idlePeriods = new ArrayList<>();

		// 获取该机器上的所有操作（排除指定操作）
		List<Integer> operations = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : schedule.getAssignedMachine().entrySet()) {
			if (entry.getValue() == machine && entry.getKey() != excludeOp) {
				operations.add(entry.getKey());
			}
		}

		// 按开始时间排序
		operations.sort(Comparator.comparingInt(op -> schedule.getStartTimes().get(op)));

		// 计算空闲时段
		int lastEndTime = 0;
		for (int op : operations) {
			int startTime = schedule.getStartTimes().get(op);

			if (startTime > lastEndTime) {
				idlePeriods.add(new int[]{lastEndTime, startTime});
			}

			lastEndTime = schedule.getOperationEndTime(op);
		}

		// 添加最后一段空闲时间（到infinity）
		idlePeriods.add(new int[]{lastEndTime, Integer.MAX_VALUE});

		return idlePeriods;
	}

	/**
	 * 根据TCMB违反程度排序操作
	 */
	protected List<Integer> rankOperationsByTCMBViolation(Schedule schedule) {
		Map<Integer, Double> opViolationScore = new HashMap<>();
		List<TCMB> tcmbConstraints = ps.getTCMBList();

		// 计算每个操作的违反程度
		for (TCMB constraint : tcmbConstraints) {
			int op1 = constraint.getOp1();
			int op2 = constraint.getOp2();
			int timeConstraint = constraint.getTimeConstraint();

			int end1 = schedule.getOperationEndTime(op1);
			int start2 = schedule.getStartTimes().get(op2);

			int actualGap = start2 - end1;
			double violation = 0;

			if (actualGap > timeConstraint) {
				violation = actualGap - timeConstraint;
				opViolationScore.put(op1, opViolationScore.getOrDefault(op1, 0.0) + violation);
				opViolationScore.put(op2, opViolationScore.getOrDefault(op2, 0.0) + violation);
			}
		}

		// 排序操作
		List<Integer> operations = new ArrayList<>(opViolationScore.keySet());
		operations.sort((op1, op2) ->
				Double.compare(opViolationScore.get(op2), opViolationScore.get(op1)));

		return operations;
	}
}

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
 * 复合交换操作
 * 交换两个操作的开始时间，并处理可能出现的约束冲突
 */
class CompoundSwap extends CompoundMove {
	// 被选中交换的两个操作
	private int operation1;
	private int operation2;

	@Override
	public MoveType getType() {
		return MoveType.SWAP;
	}

	@Override
	protected boolean selectTargetOperations(Schedule schedule) {
		// 基于TCMB违反度排序操作
		List<Integer> violatingOps = rankOperationsByTCMBViolation(schedule);
		if (violatingOps.isEmpty()) {
			return false;
		}

		// 选择最严重的违反操作
		operation1 = violatingOps.get(0);

		// 寻找潜在的交换伙伴
		List<Integer> potentialPartners = findPotentialSwapPartners(schedule, operation1);
		if (potentialPartners.isEmpty()) {
			return false;
		}

		// 随机选择一个伙伴
		operation2 = potentialPartners.get(random.nextInt(potentialPartners.size()));

		return true;
	}

	@Override
	protected boolean doMove(Schedule schedule) {
		// 交换开始时间
		int start1 = schedule.getStartTimes().get(operation1);
		int start2 = schedule.getStartTimes().get(operation2);

		schedule.getStartTimes().put(operation1, start2);
		schedule.getStartTimes().put(operation2, start1);

		return true;
	}

	@Override
	protected boolean resolveConflic(Schedule schedule) {
		// TODO: 实现机器冲突解决
		// 1. 识别交换后可能出现的机器冲突
		// 2. 尝试调整开始时间以解决冲突
		// 3. 如果无法解决，返回false
		return true; // 临时返回，实际实现需要替换
	}

	/**
	 * 寻找潜在的交换伙伴
	 */
	private List<Integer> findPotentialSwapPartners(Schedule schedule, int op1) {
		List<Integer> partners = new ArrayList<>();

		// TODO: 实现寻找潜在交换伙伴的逻辑
		// 1. 排除有DAG依赖关系的操作
		// 2. 检查交换后是否会违反DAG约束
		// 3. 返回合适的候选伙伴列表

		return partners;
	}

}

/**
 * 复合插入操作
 * 将操作移动到新的开始时间，并处理可能出现的约束冲突
 */
class CompoundInsert extends CompoundMove {
	// 被选中插入的操作
	private int targetOperation;
	// 新的开始时间
	private int newStartTime;

	@Override
	public MoveType getType() {
		return MoveType.INSERT;
	}

	@Override
	protected boolean selectTargetOperations(Schedule schedule) {
		// 基于TCMB违反度排序操作
		List<Integer> violatingOps = rankOperationsByTCMBViolation(schedule);
		if (violatingOps.isEmpty()) {
			return false;
		}

		//todo 暂时采用随机的方式
		Collections.shuffle(violatingOps);

		targetOperation = violatingOps.get(0);

		// 计算可行的时间窗口
		TimeWindow feasibleWindow = calculateFeasibleTimeWindow(schedule, targetOperation);
		if (feasibleWindow.start > feasibleWindow.end) {
			return false;
		}


		// 在可行窗口内随机选择新的开始时间
		newStartTime = feasibleWindow.start +
				random.nextInt(feasibleWindow.end - feasibleWindow.start + 1);

		return true;
	}

	protected boolean doMove(Schedule schedule) {
		schedule.getStartTimes().put(targetOperation, newStartTime);

		return true;
	}

	@Override
	protected boolean resolveConflic(Schedule schedule) {
		// TODO: 实现机器冲突解决
		// 1. 识别插入后可能出现的机器冲突
		// 2. 尝试调整其他操作的开始时间
		// 3. 如果无法解决，返回false
		return true; // 临时返回，实际实现需要替换
	}

	/**
	 * 计算操作的可行时间窗口
	 */
	private TimeWindow calculateFeasibleTimeWindow(Schedule schedule, int op) {
		// 基本计算
		int earliestStart = calculateEarliestStartTime(schedule, op);
		int latestEnd = calculateLatestEndTime(schedule, op);
		int processingTime = ps.getProcessingTime()[op-1];

		// 增加一些灵活性
		int relaxFactor = Math.max(5, processingTime / 4); // 动态缓冲区

		// 放宽窗口（但不要超过硬约束）
		earliestStart = Math.max(0, earliestStart - relaxFactor);
		latestEnd = latestEnd + relaxFactor;

		return new TimeWindow(earliestStart, latestEnd - processingTime);
	}
}



/**
 * 复合重分配操作
 * 将操作分配到新的机器，并处理可能出现的约束冲突
 */
class CompoundReassign extends CompoundMove {
	// 被选中重分配的操作
	private int targetOperation;
	// 新的机器
	private int newMachine;
	// 新的开始时间
	private int newStartTime;

	@Override
	public MoveType getType() {
		return MoveType.REASSIGN;
	}

	@Override
	protected boolean selectTargetOperations(Schedule schedule) {
		// 基于TCMB违反度排序操作
		List<Integer> violatingOps = rankOperationsByTCMBViolation(schedule);
		if (violatingOps.isEmpty()) {
			return false;
		}

		// 筛选出有多个兼容机器的操作
		List<Integer> candidates = new ArrayList<>();
		for (int op : violatingOps) {
			List<Integer> compatibleMachines = ps.getOpToCompatibleList().get(op);
			if (compatibleMachines.size() > 1) {
				candidates.add(op);
			}

			if (candidates.size() >= 3) {
				break;
			}
		}

		if (candidates.isEmpty()) {
			return false;
		}

		// 随机选择一个候选操作
		targetOperation = candidates.get(random.nextInt(candidates.size()));

		// 选择新机器
		int currentMachine = schedule.getAssignedMachine().get(targetOperation);
		List<Integer> compatibleMachines = new ArrayList<>(ps.getOpToCompatibleList().get(targetOperation));
		compatibleMachines.remove(Integer.valueOf(currentMachine));

		if (compatibleMachines.isEmpty()) {
			return false;
		}

		newMachine = compatibleMachines.get(random.nextInt(compatibleMachines.size()));

		// 选择新机器上的开始时间
		TimeWindow feasibleWindow = calculateFeasibleTimeWindow(schedule, targetOperation);
		List<int[]> idlePeriods = findIdlePeriods(schedule, newMachine, -1);

		// 找到可行的开始时间
		for (int[] period : idlePeriods) {
			int overlapStart = Math.max(period[0], feasibleWindow.start);
			int overlapEnd = Math.min(period[1], feasibleWindow.end);
			int processingTime = ps.getProcessingTime()[targetOperation-1];

			if (overlapEnd - overlapStart >= processingTime) {
				newStartTime = overlapStart;
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean doMove(Schedule schedule) {
		// 更新机器和开始时间
		schedule.getAssignedMachine().put(targetOperation, newMachine);
		schedule.getStartTimes().put(targetOperation, newStartTime);

		return true;
	}

	@Override
	protected boolean resolveConflic(Schedule schedule) {
		// 重分配到新机器通常不会直接导致机器冲突
		// 因为我们在选择新机器和开始时间时已经考虑了空闲时段
		return true;
	}

	/**
	 * 计算操作的可行时间窗口
	 */
	private TimeWindow calculateFeasibleTimeWindow(Schedule schedule, int op) {
		// 计算基于DAG约束的最早开始和最晚结束时间
		int earliestStart = calculateEarliestStartTime(schedule, op);
		int latestEnd = calculateLatestEndTime(schedule, op);
		int processingTime = ps.getProcessingTime()[op-1];

		// TODO: 考虑TCMB约束的影响

		return new TimeWindow(
				earliestStart,
				Math.max(earliestStart, latestEnd - processingTime)
		);
	}
}

/**
 * 移动操作类型枚举
 */
enum MoveType {
	SWAP,
	INSERT,
	REASSIGN
}

/**
 * 时间窗口辅助类
 */
class TimeWindow {
	int start;
	int end;

	public TimeWindow(int start, int end) {
		this.start = start;
		this.end = end;
	}
}

