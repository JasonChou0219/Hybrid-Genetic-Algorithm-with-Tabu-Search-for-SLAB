package com.zihao.GA_TS_SLAB.GA.Backup;

//import com.zihao.GA_TS_SLAB.GA.Backup.OperationMove;


/**
 * 移动代价模型 - 评估操作移动的影响和代价
 */
public class MoveCostModel {
//	// 权重参数
//	private double w1 = 0.5;  // 直接代价权重
//	private double w2 = 0.3;  // 级联效应权重
//	private double w3 = 0.2;  // 长期影响权重
//
//	// DAG图和问题设置的引用
//	private final ProblemSetting ps;
//	private final Schedule schedule;
//
//	public MoveCostModel(ProblemSetting ps, Schedule schedule) {
//		this.ps = ps;
//		this.schedule = schedule;
//	}
//
//	/**
//	 * 计算操作移动的总代价
//	 * @param op 要移动的操作ID
//	 * @param newMachine 目标机器
//	 * @param newStartTime 新的开始时间
//	 * @return 移动的总代价
//	 */
//	public double calculateTotalCost(int op, int newMachine, int newStartTime) {
//		// 保存当前状态以便计算
//		int currentMachine = schedule.getAssignedMachine().get(op);
//		int currentStartTime = schedule.getStartTimes().get(op);
//
//		// 临时应用移动
//		schedule.getAssignedMachine().put(op, newMachine);
//		schedule.getStartTimes().put(op, newStartTime);
//
//		// 计算各部分代价
//		double directCost = calculateDirectCost(op, currentMachine, currentStartTime);
//		double cascadeCost = calculateCascadeCost(op);
//		double longTermCost = calculateLongTermCost(op);
//
//		// 恢复原始状态
//		schedule.getAssignedMachine().put(op, currentMachine);
//		schedule.getStartTimes().put(op, currentStartTime);
//
//		// 计算总代价
//		return w1 * directCost + w2 * cascadeCost + w3 * longTermCost;
//	}
//
//	/**
//	 * 计算直接移动代价
//	 */
//	private double calculateDirectCost(int op, int originalMachine, int originalStartTime) {
//		double dagCost = calculateDAGCost(op, originalStartTime);
//		double machineCost = calculateMachineCost(op, originalMachine);
//		double tcmbCost = calculateTCMBCost(op);
//
//		return dagCost + machineCost + tcmbCost;
//	}
//
//	/**
//	 * 计算DAG相关代价
//	 */
//	/**
//	 * 计算DAG相关代价
//	 */
//	private double calculateDAGCost(int op, int originalStartTime) {
//		int processingTime = ps.getProcessingTime()[op - 1];
//		int newStartTime = schedule.getStartTimes().get(op);
//		int timeDelta = newStartTime - originalStartTime;
//
//		double cost = 0.0;
//
//		// 评估对前驱的影响
//		List<Integer> predecessors = ps.getDag().getParents(op);
//		for (int pred : predecessors) {
//			int predEndTime = schedule.getOperationEndTime(pred);
//			int originalMargin = originalStartTime - predEndTime;
//			int newMargin = newStartTime - predEndTime;
//
//			// 如果余量减少，增加代价
//			if (newMargin < originalMargin) {
//				// 余量越小，代价越高
//				cost += Math.max(0, 10.0 / (newMargin + 1));
//			}
//
//			// 如果造成约束违反，代价非常高
//			if (newMargin < 0) {
//				cost += 1000;
//			}
//		}
//
//		// 评估对后继的影响 - 使用getNeighbors而非getChildren
//		List<Integer> successors = ps.getDag().getNeighbors(op);
//		for (int succ : successors) {
//			int succStartTime = schedule.getStartTimes().get(succ);
//			int originalEndTime = originalStartTime + processingTime;
//			int newEndTime = newStartTime + processingTime;
//			int originalMargin = succStartTime - originalEndTime;
//			int newMargin = succStartTime - newEndTime;
//
//			// 如果余量减少，增加代价
//			if (newMargin < originalMargin) {
//				cost += Math.max(0, 10.0 / (newMargin + 1));
//			}
//
//			// 如果造成约束违反，代价非常高
//			if (newMargin < 0) {
//				cost += 1000;
//			}
//		}
//
//		return cost;
//	}
//
//	/**
//	 * 计算机器相关代价
//	 */
//	private double calculateMachineCost(int op, int originalMachine) {
//		int newMachine = schedule.getAssignedMachine().get(op);
//		int newStartTime = schedule.getStartTimes().get(op);
//		int processingTime = ps.getProcessingTime()[op - 1];
//		double cost = 0.0;
//
//		// 检查是否改变了机器
//		if (newMachine != originalMachine) {
//			// 机器变更有一定代价
//			cost += 5.0;
//		}
//
//		// 检查机器上的操作密度变化
//		List<Integer> machineOps = schedule.getMachineOperations(newMachine);
//		if (!machineOps.isEmpty()) {
//			// 计算与相邻操作的间隔
//			int nearestPredGap = Integer.MAX_VALUE;
//			int nearestSuccGap = Integer.MAX_VALUE;
//
//			for (int machineOp : machineOps) {
//				if (machineOp == op) continue;
//
//				int machineOpStart = schedule.getStartTimes().get(machineOp);
//				int machineOpEnd = machineOpStart + ps.getProcessingTime()[machineOp - 1];
//
//				if (machineOpEnd <= newStartTime) {
//					// 机器上的前一个操作
//					nearestPredGap = Math.min(nearestPredGap, newStartTime - machineOpEnd);
//				} else if (machineOpStart >= newStartTime + processingTime) {
//					// 机器上的后一个操作
//					nearestSuccGap = Math.min(nearestSuccGap, machineOpStart - (newStartTime + processingTime));
//				} else {
//					// 重叠操作，高代价
//					cost += 2000;
//				}
//			}
//
//			// 过小的间隔增加代价
//			if (nearestPredGap < Integer.MAX_VALUE) {
//				cost += Math.max(0, 5.0 / (nearestPredGap + 1));
//			}
//			if (nearestSuccGap < Integer.MAX_VALUE) {
//				cost += Math.max(0, 5.0 / (nearestSuccGap + 1));
//			}
//		}
//
//		return cost;
//	}
//
//	/**
//	 * 计算TCMB相关代价
//	 */
//	private double calculateTCMBCost(int op) {
//		double cost = 0.0;
//		int newStartTime = schedule.getStartTimes().get(op);
//		int processingTime = ps.getProcessingTime()[op - 1];
//		int newEndTime = newStartTime + processingTime;
//
//		// 检查所有涉及该操作的TCMB约束
//		for (TCMB tcmb : ps.getTCMBList()) {
//			if (tcmb.getOp1() == op) {
//				int op2 = tcmb.getOp2();
//				int op2Start = schedule.getStartTimes().get(op2);
//				int interval = op2Start - newEndTime;
//				int timeLimit = tcmb.getTimeConstraint();
//
//				// 计算违反程度
//				int violation = Math.max(0, interval - timeLimit);
//
//				// 违反TCMB约束的代价
//				if (violation > 0) {
//					cost += 50 + violation * 2;
//				} else {
//					// 满足但接近约束边界
//					if (interval > 0 && interval <= timeLimit) {
//						cost -= 20 * (1.0 - (double)interval / timeLimit);  // 奖励满足约束
//					}
//				}
//			}
//			else if (tcmb.getOp2() == op) {
//				int op1 = tcmb.getOp1();
//				int op1End = schedule.getOperationEndTime(op1);
//				int interval = newStartTime - op1End;
//				int timeLimit = tcmb.getTimeConstraint();
//
//				// 计算违反程度
//				int violation = Math.max(0, interval - timeLimit);
//
//				// 违反TCMB约束的代价
//				if (violation > 0) {
//					cost += 50 + violation * 2;
//				} else {
//					// 满足但接近约束边界
//					if (interval > 0 && interval <= timeLimit) {
//						cost -= 20 * (1.0 - (double)interval / timeLimit);  // 奖励满足约束
//					}
//				}
//			}
//		}
//
//		return cost;
//	}
//
//	/**
//	 * 计算级联效应代价
//	 */
//	private double calculateCascadeCost(int op) {
//		// 识别所有可能受影响的操作
//		Set<Integer> affectedOps = identifyAffectedOperations(op);
//		if (affectedOps.isEmpty()) {
//			return 0.0;
//		}
//
//		double dagPropagation = calculateDAGPropagation(op, affectedOps);
//		double machinePropagation = calculateMachinePropagation(op, affectedOps);
//		double tcmbNetworkEffect = calculateTCMBNetworkEffect(op, affectedOps);
//
//		return dagPropagation + machinePropagation + tcmbNetworkEffect;
//	}
//
//	/**
//	 * 识别受影响的操作
//	 */
//	private Set<Integer> identifyAffectedOperations(int op) {
//		Set<Integer> affected = new HashSet<>();
//		Queue<Integer> queue = new LinkedList<>();
//		Set<Integer> visited = new HashSet<>();
//
//		// 从直接后继开始，使用getNeighbors获取后继
//		queue.addAll(ps.getDag().getNeighbors(op));
//
//		// 广度优先搜索遍历所有可能受影响的操作
//		while (!queue.isEmpty()) {
//			int current = queue.poll();
//			if (visited.contains(current)) continue;
//			visited.add(current);
//
//			// 检查是否受影响
//			if (isAffectedByMove(op, current)) {
//				affected.add(current);
//				queue.addAll(ps.getDag().getNeighbors(current));
//			}
//		}
//
//		// 添加同一机器上可能受影响的操作
//		int machineId = schedule.getAssignedMachine().get(op);
//		int opEndTime = schedule.getOperationEndTime(op);
//
//		for (int machineOp : schedule.getMachineOperations(machineId)) {
//			if (machineOp != op && schedule.getStartTimes().get(machineOp) >= opEndTime) {
//				affected.add(machineOp);
//			}
//		}
//
//		return affected;
//	}
//	/**
//	 * 判断操作是否受到移动影响
//	 */
//	private boolean isAffectedByMove(int movedOp, int checkOp) {
//		int movedOpEndTime = schedule.getOperationEndTime(movedOp);
//		int checkOpStartTime = schedule.getStartTimes().get(checkOp);
//
//		// 如果后继操作的开始时间紧跟着前驱的结束时间，则认为受影响
//		return checkOpStartTime <= movedOpEndTime + 5;  // 允许小的缓冲
//	}
//
//	/**
//	 * 计算DAG传播代价
//	 */
//	private double calculateDAGPropagation(int op, Set<Integer> affectedOps) {
//		double cost = 0.0;
//		int processingTime = ps.getProcessingTime()[op - 1];
//		int newEndTime = schedule.getStartTimes().get(op) + processingTime;
//
//		for (int affectedOp : affectedOps) {
//			List<Integer> predecessors = ps.getDag().getParents(affectedOp);
//			if (predecessors.contains(op)) {
//				int affectedStart = schedule.getStartTimes().get(affectedOp);
//				int requiredShift = Math.max(0, newEndTime - affectedStart);
//
//				if (requiredShift > 0) {
//					// 需要移动的操作数量越多，代价越高
//					cost += requiredShift * (1 + 0.1 * affectedOps.size());
//
//					// 评估移动空间
//					double moveSpace = evaluateMoveSpace(affectedOp);
//					if (moveSpace < requiredShift) {
//						// 如果移动空间不足，大幅增加代价
//						cost += (requiredShift - moveSpace) * 50;
//					}
//				}
//			}
//		}
//
//		return cost;
//	}
//
//	/**
//	 * 评估操作的移动空间
//	 */
//	private double evaluateMoveSpace(int op) {
//		int currentStart = schedule.getStartTimes().get(op);
//		List<Integer> successors = ps.getDag().getNeighbors(op);
//		int processingTime = ps.getProcessingTime()[op - 1];
//
//		if (successors.isEmpty()) {
//			return Double.MAX_VALUE;  // 没有后继，理论上可以无限移动
//		}
//
//		// 找出最早的后继开始时间
//		int earliestSuccStart = Integer.MAX_VALUE;
//		for (int succ : successors) {
//			earliestSuccStart = Math.min(earliestSuccStart, schedule.getStartTimes().get(succ));
//		}
//
//		// 计算可移动的最大距离
//		return earliestSuccStart - (currentStart + processingTime);
//	}
//
//	/**
//	 * 计算机器传播代价
//	 */
//	private double calculateMachinePropagation(int op, Set<Integer> affectedOps) {
//		double cost = 0.0;
//		int machineId = schedule.getAssignedMachine().get(op);
//
//		// 统计同一机器上受影响的操作数量
//		int sameMachineCount = 0;
//		for (int affectedOp : affectedOps) {
//			if (schedule.getAssignedMachine().get(affectedOp) == machineId) {
//				sameMachineCount++;
//			}
//		}
//
//		// 同一机器上需要调整的操作越多，调度复杂度越高
//		cost += sameMachineCount * 5;
//
//		return cost;
//	}
//
//	/**
//	 * 计算TCMB网络影响
//	 */
//	private double calculateTCMBNetworkEffect(int op, Set<Integer> affectedOps) {
//		double cost = 0.0;
//
//		// 检查受影响操作是否涉及TCMB约束
//		for (int affectedOp : affectedOps) {
//			// 检查这个受影响的操作是否是某个TCMB约束的一部分
//			for (TCMB tcmb : ps.getTCMBList()) {
//				if (tcmb.getOp1() == affectedOp || tcmb.getOp2() == affectedOp) {
//					// 涉及TCMB约束的操作受影响，增加代价
//					cost += 15;
//
//					// 如果已经是违反状态，进一步增加代价
//					if (isTCMBViolated(tcmb)) {
//						cost += 30;
//					}
//				}
//			}
//		}
//
//		return cost;
//	}
//
//	/**
//	 * 检查TCMB约束是否违反
//	 */
//	private boolean isTCMBViolated(TCMB tcmb) {
//		int op1 = tcmb.getOp1();
//		int op2 = tcmb.getOp2();
//		int op1End = schedule.getOperationEndTime(op1);
//		int op2Start = schedule.getStartTimes().get(op2);
//		int interval = op2Start - op1End;
//
//		return interval > tcmb.getTimeConstraint();
//	}
//
//	/**
//	 * 计算长期影响
//	 */
//	private double calculateLongTermCost(int op) {
//		double flexibilityLoss = calculateFlexibilityLoss(op);
//		double balanceImpact = calculateBalanceImpact(op);
//
//		return flexibilityLoss + balanceImpact;
//	}
//
//	/**
//	 * 计算灵活性损失
//	 */
//	private double calculateFlexibilityLoss(int op) {
//		double cost = 0.0;
//		int processingTime = ps.getProcessingTime()[op - 1];
//		int newStartTime = schedule.getStartTimes().get(op);
//
//		// 评估移动后的调整空间变化
//		List<Integer> predecessors = ps.getDag().getParents(op);
//		for (int pred : predecessors) {
//			int predEndTime = schedule.getOperationEndTime(pred);
//			int timeBuffer = newStartTime - predEndTime;
//
//			// 缓冲时间越小，将来调整越困难
//			if (timeBuffer < processingTime / 2) {
//				cost += 10 * (1.0 - (double)timeBuffer / (processingTime / 2));
//			}
//		}
//
//		// 使用getNeighbors代替getChildren
//		List<Integer> successors = ps.getDag().getNeighbors(op);
//		for (int succ : successors) {
//			int succStartTime = schedule.getStartTimes().get(succ);
//			int newEndTime = newStartTime + processingTime;
//			int timeBuffer = succStartTime - newEndTime;
//
//			if (timeBuffer < processingTime / 2) {
//				cost += 10 * (1.0 - (double)timeBuffer / (processingTime / 2));
//			}
//		}
//
//		return cost;
//	}
//
//	/**
//	 * 计算平衡性影响
//	 */
//	private double calculateBalanceImpact(int op) {
//		double cost = 0.0;
//		int newMachine = schedule.getAssignedMachine().get(op);
//
//		// 计算所有机器的负载情况
//		Map<Integer, Integer> machineLoads = calculateMachineLoads();
//
//		// 计算负载方差
//		double avgLoad = machineLoads.values().stream().mapToInt(Integer::intValue).average().orElse(0);
//		double variance = 0;
//		for (int load : machineLoads.values()) {
//			variance += Math.pow(load - avgLoad, 2);
//		}
//		variance /= machineLoads.size();
//
//		// 方差越大表示负载越不均衡
//		cost += variance * 0.1;
//
//		// 检查对makespan的影响
//		int currentMakespan = calculateMakespan();
//		cost += currentMakespan * 0.05;
//
//		return cost;
//	}
//
//	/**
//	 * 计算各机器的负载情况
//	 */
//	private Map<Integer, Integer> calculateMachineLoads() {
//		Map<Integer, Integer> loads = new HashMap<>();
//
//		for (int machineId = 1; machineId <= ps.getMachineNum(); machineId++) {
//			List<Integer> machineOps = schedule.getMachineOperations(machineId);
//			int totalProcessingTime = 0;
//			for (int op : machineOps) {
//				totalProcessingTime += ps.getProcessingTime()[op - 1];
//			}
//			loads.put(machineId, totalProcessingTime);
//		}
//
//		return loads;
//	}
//
//	/**
//	 * 计算当前makespan
//	 */
//	private int calculateMakespan() {
//		int makespan = 0;
//
//		for (Map.Entry<Integer, Integer> entry : schedule.getStartTimes().entrySet()) {
//			int op = entry.getKey();
//			int startTime = entry.getValue();
//			int endTime = startTime + ps.getProcessingTime()[op - 1];
//			makespan = Math.max(makespan, endTime);
//		}
//
//		return makespan;
//	}
//
//	/**
//	 * 评估修复TCMB违反的方案
//	 */
//	public MovePlan evaluateTCMBRepairOptions(TCMB violatedTCMB) {
//		int op1 = violatedTCMB.getOp1();
//		int op2 = violatedTCMB.getOp2();
//		int op1End = schedule.getOperationEndTime(op1);
//		int op2Start = schedule.getStartTimes().get(op2);
//		int currentInterval = op2Start - op1End;
//		int targetInterval = violatedTCMB.getTimeConstraint();
//		int requiredChange = currentInterval - targetInterval;
//
//		// 如果没有违反，直接返回
//		if (requiredChange <= 0) {
//			return null;
//		}
//
//		// 策略1: 右移op1
//		MovePlan rightMoveOp1 = evaluateRightMoveOp1(op1, op2, requiredChange);
//
//		// 策略2: 左移op2
//		MovePlan leftMoveOp2 = evaluateLeftMoveOp2(op1, op2, requiredChange);
//
//		// 策略3: 同时移动两个操作
//		MovePlan combinedMove = evaluateCombinedMove(op1, op2, requiredChange);
//
//		// 选择代价最小的方案
//		MovePlan bestPlan = selectBestPlan(rightMoveOp1, leftMoveOp2, combinedMove);
//
//		return bestPlan;
//	}
//
//	/**
//	 * 评估右移op1的方案
//	 */
//	private MovePlan evaluateRightMoveOp1(int op1, int op2, int requiredChange) {
//		int currentMachine = schedule.getAssignedMachine().get(op1);
//		int currentStart = schedule.getStartTimes().get(op1);
//		int processingTime = ps.getProcessingTime()[op1 - 1];
//		int newStart = currentStart + requiredChange;
//
//		double cost = calculateTotalCost(op1, currentMachine, newStart);
//
//		// 如果代价过高，表示方案不可行
//		if (cost > 5000) {
//			return null;
//		}
//
//		List<OperationMove> moves = new ArrayList<>();
//		moves.add(new OperationMove(op1, currentMachine, newStart));
//
//		// 识别级联影响的操作
//		Set<Integer> affectedOps = identifyAffectedOperations(op1);
//		for (int affectedOp : affectedOps) {
//			int affectedStart = schedule.getStartTimes().get(affectedOp);
//			int newAffectedStart = affectedStart + requiredChange;
//			int affectedMachine = schedule.getAssignedMachine().get(affectedOp);
//
//			moves.add(new OperationMove(affectedOp, affectedMachine, newAffectedStart));
//		}
//
//		return new MovePlan(moves, cost);
//	}
//
//	/**
//	 * 评估左移op2的方案
//	 */
//	private MovePlan evaluateLeftMoveOp2(int op1, int op2, int requiredChange) {
//		int currentMachine = schedule.getAssignedMachine().get(op2);
//		int currentStart = schedule.getStartTimes().get(op2);
//		int newStart = currentStart - requiredChange;
//
//		// 检查基本可行性
//		if (newStart < 0) {
//			return null;
//		}
//
//		double cost = calculateTotalCost(op2, currentMachine, newStart);
//
//		// 如果代价过高，表示方案不可行
//		if (cost > 5000) {
//			return null;
//		}
//
//		return new MovePlan(
//				List.of(new OperationMove(op2, currentMachine, newStart)),
//				cost
//		);
//	}
//
//	/**
//	 * 评估同时移动两个操作的方案
//	 */
//	private MovePlan evaluateCombinedMove(int op1, int op2, int requiredChange) {
//		// 计算每个操作移动的最佳比例
//		int op1MoveAmount = requiredChange / 2;
//		int op2MoveAmount = requiredChange - op1MoveAmount;
//
//		int op1CurrentStart = schedule.getStartTimes().get(op1);
//		int op1CurrentMachine = schedule.getAssignedMachine().get(op1);
//		int op1NewStart = op1CurrentStart + op1MoveAmount;
//
//		int op2CurrentStart = schedule.getStartTimes().get(op2);
//		int op2CurrentMachine = schedule.getAssignedMachine().get(op2);
//		int op2NewStart = op2CurrentStart - op2MoveAmount;
//
//		// 检查基本可行性
//		if (op2NewStart < 0) {
//			return null;
//		}
//
//		// 先评估op1移动代价
//		double op1Cost = calculateTotalCost(op1, op1CurrentMachine, op1NewStart);
//
//		// 应用op1的移动
//		schedule.getStartTimes().put(op1, op1NewStart);
//
//		// 再评估op2移动代价
//		double op2Cost = calculateTotalCost(op2, op2CurrentMachine, op2NewStart);
//
//		// 恢复原始状态
//		schedule.getStartTimes().put(op1, op1CurrentStart);
//
//		double totalCost = op1Cost + op2Cost;
//
//		// 如果代价过高，表示方案不可行
//		if (totalCost > 5000) {
//			return null;
//		}
//
//		List<OperationMove> moves = new ArrayList<>();
//		moves.add(new OperationMove(op1, op1CurrentMachine, op1NewStart));
//		moves.add(new OperationMove(op2, op2CurrentMachine, op2NewStart));
//
//		// 识别级联影响的操作
//		Set<Integer> affectedOps = identifyAffectedOperations(op1);
//		for (int affectedOp : affectedOps) {
//			int affectedStart = schedule.getStartTimes().get(affectedOp);
//			int newAffectedStart = affectedStart + op1MoveAmount;
//			int affectedMachine = schedule.getAssignedMachine().get(affectedOp);
//
//			moves.add(new OperationMove(affectedOp, affectedMachine, newAffectedStart));
//		}
//
//		return new MovePlan(moves, totalCost);
//	}
//
//	/**
//	 * 选择最佳方案
//	 */
//	private MovePlan selectBestPlan(MovePlan... plans) {
//		MovePlan bestPlan = null;
//		double minCost = Double.MAX_VALUE;
//
//		for (MovePlan plan : plans) {
//			if (plan != null && plan.cost < minCost) {
//				minCost = plan.cost;
//				bestPlan = plan;
//			}
//		}
//
//		return bestPlan;
//	}
//
//	/**
//	 * 移动方案类
//	 */
//	public static class MovePlan {
//		public final List<OperationMove> moves;
//		public final double cost;
//
//		public MovePlan(List<OperationMove> moves, double cost) {
//			this.moves = moves;
//			this.cost = cost;
//		}
//	}
//
//	/**
//	 * 设置权重参数
//	 */
//	public void setWeights(double w1, double w2, double w3) {
//		this.w1 = w1;
//		this.w2 = w2;
//		this.w3 = w3;
//	}
}
