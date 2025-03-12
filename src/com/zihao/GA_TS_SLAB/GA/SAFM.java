package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;

import java.util.*;

/**
 * SAFM (Simulated Annealing with Final Modification)
 *
 * This class implements a simulated annealing algorithm similar to SAGAS.cpp
 * with additional final modification techniques to improve the solution quality.
 * It's designed to work with Schedule objects from the GA framework and specifically
 * focuses on resolving TCMB constraint violations.
 */
public class SAFM {
	private final ProblemSetting problemSetting;
	private final Random random;
	private double initialTemperature;
	private double finalTemperature;
	private final int maxIterations;
	private final double coolingRate;
	private int[] processingTimes;

	private static final boolean DEBUG_MODE = true;

	/**
	 * Constructor with default parameters
	 */
	public SAFM() {
		this(10000, 0.98, 5000);
	}

	/**
	 * Constructor with customizable parameters
	 *
	 * @param maxIterations maximum number of iterations
	 * @param coolingRate cooling rate for temperature reduction
	 * @param annealingTime annealing time in milliseconds (similar to SA_msec in SAGAS.cpp)
	 */
	public SAFM(int maxIterations, double coolingRate, int annealingTime) {
		this.problemSetting = ProblemSetting.getInstance();
		this.random = new Random();
		this.maxIterations = maxIterations;
		this.coolingRate = coolingRate;
		this.processingTimes = problemSetting.getProcessingTime();
	}

	/**
	 * Main optimization method that takes an initial solution and improves it
	 *
	 * @param initialChromosome the initial chromosome with a solution
	 * @return optimized Schedule
	 */
	public Schedule optimize(Chromosome initialChromosome) {
		// Get initial schedule
		Schedule currentSchedule = new Schedule(initialChromosome.getSchedule());
		Schedule bestSchedule = new Schedule(currentSchedule);

		// Calculate initial fitness (lower is better)
		double currentFitness = calculateFitness(currentSchedule);
		double bestFitness = currentFitness;

		// Calculate initial temperature based on the scale of the problem
		setTemperatureParameters(currentSchedule);

		// Start the annealing process
		double temperature = initialTemperature;
		int iteration = 0;
		long startTime = System.currentTimeMillis();

		// 添加初始输出
		if (DEBUG_MODE) {
			System.out.println("\n========== SAFM算法开始 ==========");
			System.out.println("初始解信息:");
			System.out.println("初始适应度: " + currentFitness);
			System.out.println("初始makespan: " + Utility.calculateMakespan(currentSchedule));
			System.out.println("DAG约束违反: " + countDagViolations(currentSchedule));
			System.out.println("TCMB约束违反: " + countTcmbViolations(currentSchedule));
			System.out.println("机器重叠违反: " + countMachineOverlaps(currentSchedule));
			System.out.println("初始温度: " + initialTemperature);
			System.out.println("最终温度: " + finalTemperature);
			System.out.println("最大迭代次数: " + maxIterations);
			System.out.println("冷却速率: " + coolingRate);
		}

		while (iteration < maxIterations && temperature > finalTemperature) {
			// Generate a neighboring solution
			if (DEBUG_MODE && iteration % 10 == 0) {
				System.out.println("\n----- 迭代 " + iteration + " -----");
				System.out.println("当前温度: " + temperature);
				System.out.println("当前适应度: " + currentFitness);
				System.out.println("最佳适应度: " + bestFitness);
				// 输出当前约束违反情况
				System.out.println("当前DAG约束违反: " + countDagViolations(currentSchedule));
				System.out.println("当前TCMB约束违反: " + countTcmbViolations(currentSchedule));
				System.out.println("当前机器重叠违反: " + countMachineOverlaps(currentSchedule));
			}
			Schedule neighborSchedule = generateNeighborSchedule(currentSchedule);

			// Calculate the fitness of the neighbor
			double neighborFitness = calculateFitness(neighborSchedule);

			// Decide whether to accept the neighbor
			boolean accept = false;
			if (neighborFitness < currentFitness) {
				// Always accept better solutions
				accept = true;
			} else {
				System.out.println("接受新的解fitness:  " + neighborFitness);
				// Probabilistically accept worse solutions based on temperature
				double acceptanceProbability = Math.exp(-(neighborFitness - currentFitness) / temperature);
				if (random.nextDouble() < acceptanceProbability) {
					accept = true;
				}
			}

			// Update current solution if accepted
			if (accept) {
				currentSchedule = neighborSchedule;
				currentFitness = neighborFitness;

				// Update best solution if current is better
				if (currentFitness < bestFitness) {
					bestSchedule = new Schedule(currentSchedule);
					bestFitness = currentFitness;

					if (DEBUG_MODE) {
						System.out.println("*** 新的最佳解! 适应度: " + bestFitness + " ***");
					}
				}
			}

			// Cool down the temperature
			temperature *= coolingRate;
			iteration++;
		}

		if (DEBUG_MODE) {
			System.out.println("\n========== SA阶段完成 ==========");
			System.out.println("总迭代次数: " + iteration);
			System.out.println("最终温度: " + temperature);
			System.out.println("最终适应度: " + bestFitness);
		}

		if (DEBUG_MODE) {
			System.out.println("====== The schedule after SA ====");
			printDetailedSchedule(bestSchedule);
		}

//		// Apply final modifications to further improve the solution
//		bestSchedule = applyFinalModification(bestSchedule);
//
//		if (DEBUG_MODE) {
//			System.out.println("====== The schedule after final modification ====");
//			printDetailedSchedule(bestSchedule);
//		}
		return bestSchedule;
	}

	/**
	 * Sets the temperature parameters based on the scale of the problem
	 */
	private void setTemperatureParameters(Schedule schedule) {
		Map<Integer, Integer> startTimes = schedule.getStartTimes();

		// Find the maximum operation completion time as a scale reference
		int maxCompletionTime = 0;
		for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
			int opId = entry.getKey();
			int startTime = entry.getValue();
			int processingTime = processingTimes[opId - 1];
			maxCompletionTime = Math.max(maxCompletionTime, startTime + processingTime);
		}

		// Set initial temperature to a fraction of the maximum completion time
		// Similar to Temp_S in SAGAS.cpp
		initialTemperature = maxCompletionTime;

		// Calculate machine loads to determine final temperature
		// Similar to Temp_L in SAGAS.cpp
		Map<Integer, Integer> machineTypeLoads = new HashMap<>();
		Map<Integer, List<Integer>> machineTypeToList = problemSetting.getMachineTypeToList();

		for (Map.Entry<Integer, Integer> entry : schedule.getAssignedMachine().entrySet()) {
			int opId = entry.getKey();
			int machineId = entry.getValue();

			// Find the machine type for this machine
			for (Map.Entry<Integer, List<Integer>> typeEntry : machineTypeToList.entrySet()) {
				if (typeEntry.getValue().contains(machineId)) {
					int machineType = typeEntry.getKey();
					int currentLoad = machineTypeLoads.getOrDefault(machineType, 0);
					machineTypeLoads.put(machineType, currentLoad + processingTimes[opId - 1]);
					break;
				}
			}
		}

		// Find the machine type with the highest load per machine
		double highestLoadPerMachine = 0;
		for (Map.Entry<Integer, Integer> entry : machineTypeLoads.entrySet()) {
			int machineType = entry.getKey();
			int totalLoad = entry.getValue();
			int machineCount = machineTypeToList.get(machineType).size();
			double loadPerMachine = (double) totalLoad / machineCount;
			highestLoadPerMachine = Math.max(highestLoadPerMachine, loadPerMachine);
		}

		// Set final temperature based on the highest load per machine
//		finalTemperature = highestLoadPerMachine * 0.1;
		finalTemperature = 0.01;
	}




	/**
	 * Gets the minimum possible start time for an operation based on its predecessors
	 */
	private int getMinimumStartTime(int opId, Map<Integer, Integer> startTimes) {
		int minStart = 0;

		// Check all predecessors from the DAG
		for (int predOp : problemSetting.getDag().getParents(opId)) {
			int predStart = startTimes.get(predOp);
			int predEnd = predStart + processingTimes[predOp - 1];
			minStart = Math.max(minStart, predEnd);
		}

		return minStart;
	}

	/**
	 * Gets the maximum possible start time for an operation based on TCMB constraints
	 * and successors in the DAG
	 */
	private int getMaximumStartTime(int opId, Map<Integer, Integer> startTimes) {
		int maxStart = Integer.MAX_VALUE;

		// Check all TCMB constraints where this operation is op1
		for (TCMB tcmb : problemSetting.getTCMBList()) {
			if (tcmb.getOp1() == opId) {
				int op2 = tcmb.getOp2();
				int startTime2 = startTimes.get(op2);
				int timeConstraint = tcmb.getTimeConstraint();

				// op1 must finish at least timeConstraint before op2 starts
				int latestEnd = startTime2 - timeConstraint;
				int latestStart = latestEnd - processingTimes[opId - 1];

				maxStart = Math.min(maxStart, latestStart);
			}
		}

		// Check all successors in the DAG
		for (int succOp : problemSetting.getReverseDag().getNeighbors(opId)) {
			int succStart = startTimes.get(succOp);
			int latestStart = succStart - processingTimes[opId - 1];
			maxStart = Math.min(maxStart, latestStart);
		}

		return maxStart;
	}

	/**
	 * 生成一个尊重约束的邻居解
	 */
	private Schedule generateNeighborSchedule(Schedule currentSchedule) {
		// 创建当前调度的副本
		Schedule neighborSchedule = new Schedule(currentSchedule);
		Map<Integer, Integer> startTimes = new HashMap<>(neighborSchedule.getStartTimes());

		// 获取TCMB约束
		List<TCMB> tcmbList = problemSetting.getTCMBList();
		int totalOperations = problemSetting.getTotalOpNum();

		// 随机决定修改多少个操作（在1和所有操作的20%之间）
		int opsToModify = 1 + random.nextInt(Math.max(1, totalOperations / 5));

		// 构建概率分布，偏向于TCMB约束中的操作
		Map<Integer, Double> opProbabilities = new HashMap<>();

		// 首先，给每个操作一个基础概率
		for (int opId = 1; opId <= totalOperations; opId++) {
			opProbabilities.put(opId, 1.0);
		}

		// 增加TCMB约束中操作的概率
		for (TCMB tcmb : tcmbList) {
			int op1 = tcmb.getOp1();
			int op2 = tcmb.getOp2();

			// 检查这个约束是否被违反
			int startTime1 = startTimes.get(op1);
			int endTime1 = startTime1 + processingTimes[op1 - 1];
			int startTime2 = startTimes.get(op2);
			int timeLag = startTime2 - endTime1;

			// 对于违反的约束，增加更多的概率
			double multiplier = (timeLag > tcmb.getTimeConstraint() || timeLag < 0) ? 5.0 : 2.0;

			opProbabilities.put(op1, opProbabilities.get(op1) * multiplier);
			opProbabilities.put(op2, opProbabilities.get(op2) * multiplier);
		}

		// 归一化概率
		double totalProbability = opProbabilities.values().stream().mapToDouble(Double::doubleValue).sum();
		for (int opId : opProbabilities.keySet()) {
			opProbabilities.put(opId, opProbabilities.get(opId) / totalProbability);
		}

		// 根据概率分布选择要修改的操作
		List<Integer> selectedOps = new ArrayList<>();
		for (int i = 0; i < opsToModify; i++) {
			double rand = random.nextDouble();
			double cumulativeProbability = 0.0;

			for (Map.Entry<Integer, Double> entry : opProbabilities.entrySet()) {
				cumulativeProbability += entry.getValue();
				if (rand <= cumulativeProbability) {
					selectedOps.add(entry.getKey());
					break;
				}
			}
		}

		// 按拓扑顺序排序选定的操作
		selectedOps.sort((a, b) -> {
			int[][] distanceMatrix = problemSetting.getDistanceMatrix();
			if (distanceMatrix[a][b] > 0) return -1; // a 在 b 之前
			if (distanceMatrix[b][a] > 0) return 1;  // b 在 a 之前
			return 0; // 没有依赖关系
		});

		// 修改选定操作的开始时间并保持DAG约束
		for (int opId : selectedOps) {
			// 获取当前开始时间
			int currentStart = startTimes.get(opId);

			// 确定新开始时间的有效范围
			int minStart = getMinimumStartTime(opId, startTimes);
			int maxStart = getMaximumStartTime(opId, startTimes);

			// 如果没有灵活性则跳过
			if (minStart >= maxStart) continue;

			// 生成一个新的开始时间
			int newStart;

			// 使用温度因子来控制改变的大小
			double temperatureFactor = 0.3;
			if (random.nextBoolean()) {
				// 向后移动（延迟）
				int range = maxStart - currentStart;
				int delta = (int)(range * (1 - Math.exp(-random.nextDouble() / temperatureFactor)));
				newStart = Math.min(maxStart, currentStart + delta);
			} else {
				// 向前移动（提前）
				int range = currentStart - minStart;
				int delta = (int)(range * (1 - Math.exp(-random.nextDouble() / temperatureFactor)));
				newStart = Math.max(minStart, currentStart - delta);
			}

			// 更新开始时间
			if (newStart != currentStart) {
				startTimes.put(opId, newStart);

				// 将变更传播到所有依赖的操作
				propagateChange(opId, startTimes, neighborSchedule.getAssignedMachine());
			}
		}

		// 创建一个具有修改后开始时间的新调度
		return new Schedule(startTimes, neighborSchedule.getAssignedMachine());
	}

	/**
	 * 将变更传播到依赖图中，并处理机器重叠
	 * 当一个操作的开始时间改变时，确保所有依赖的操作也相应更新，并避免机器重叠
	 */
	private void propagateChange(int opId, Map<Integer, Integer> startTimes, Map<Integer, Integer> assignedMachine) {
		// 获取所有依赖于这个操作的操作
		for (int succOp : problemSetting.getDag().getNeighbors(opId)) {
			// 计算后继操作的新最小开始时间
			int newMinStart = startTimes.get(opId) + processingTimes[opId - 1];
			int currentSuccStart = startTimes.get(succOp);

			// 如果后继操作现在违反了约束，更新它
			if (currentSuccStart < newMinStart) {
				startTimes.put(succOp, newMinStart);

				// 递归地传播这个变更
				propagateChange(succOp, startTimes, assignedMachine);
			}
		}

		// 检查TCMB约束
		for (TCMB tcmb : problemSetting.getTCMBList()) {
			if (tcmb.getOp1() == opId) {
				int op2 = tcmb.getOp2();
				int endTime1 = startTimes.get(opId) + processingTimes[opId - 1];
				int startTime2 = startTimes.get(op2);
				int timeConstraint = tcmb.getTimeConstraint();

				// 检查是否违反TCMB约束
				if (startTime2 - endTime1 > timeConstraint) {
					// 重新调整op2的开始时间
					int newStart2 = endTime1 + timeConstraint;
					startTimes.put(op2, newStart2);
					// 递归传播
					propagateChange(op2, startTimes, assignedMachine);
				}
			}
		}

		// 添加机器重叠处理
		int machineId = assignedMachine.get(opId);
		int opStart = startTimes.get(opId);
		int opEnd = opStart + processingTimes[opId - 1];

		// 获取同一机器上的所有操作并按开始时间排序
		List<int[]> machineOps = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
			int otherOpId = entry.getKey();
			if (otherOpId != opId && entry.getValue() == machineId) {
				int otherStart = startTimes.get(otherOpId);
				int otherEnd = otherStart + processingTimes[otherOpId - 1];
				machineOps.add(new int[]{otherStart, otherEnd, otherOpId});
			}
		}
		machineOps.sort(Comparator.comparingInt(a -> a[0]));

		// 检查并解决重叠
		for (int[] otherOp : machineOps) {
			int otherOpId = otherOp[2];
			int otherStart = startTimes.get(otherOpId);
			int otherEnd = otherStart + processingTimes[otherOpId - 1];

			// 检测重叠
			if ((opStart < otherEnd && opEnd > otherStart)) {
				// 如果此操作在拓扑顺序中较早，则移动另一个操作
				if (isBeforeInTopologicalOrder(opId, otherOpId)) {
					startTimes.put(otherOpId, opEnd);
					// 递归传播
					propagateChange(otherOpId, startTimes, assignedMachine);
				} else {
					// 如果此操作在拓扑顺序中较晚，则移动此操作
					startTimes.put(opId, otherEnd);
					// 递归传播
					propagateChange(opId, startTimes, assignedMachine);
					break; // 跳出循环，因为此操作已经移动了
				}
			}
		}
	}

	/**
	 * 确定操作在拓扑顺序中的先后关系
	 */
	private boolean isBeforeInTopologicalOrder(int op1, int op2) {
		int[][] distanceMatrix = problemSetting.getDistanceMatrix();
		// 若op1是op2的前驱，则op1在拓扑顺序中更早
		if (distanceMatrix[op1][op2] > 0) return true;
		// 若op2是op1的前驱，则op2在拓扑顺序中更早
		if (distanceMatrix[op2][op1] > 0) return false;
		// 无依赖关系时，按ID排序
		return op1 < op2;
	}

	/**
	 * 计算调度的适应度，考虑到makespan、DAG约束、TCMB约束和机器重叠
	 */
	private double calculateFitness(Schedule schedule) {
		Map<Integer, Integer> startTimes = schedule.getStartTimes();

		// 计算makespan
		int makespan = 0;
		for (int opId = 1; opId <= problemSetting.getTotalOpNum(); opId++) {
			int startTime = startTimes.get(opId);
			int endTime = startTime + processingTimes[opId - 1];
			makespan = Math.max(makespan, endTime);
		}

		// 计算DAG约束违反
		double dagViolation = 0.0;
		for (int opId = 1; opId <= problemSetting.getTotalOpNum(); opId++) {
			int startTime = startTimes.get(opId);

			// 检查所有前驱
			for (int predOp : problemSetting.getReverseDag().getParents(opId)) {
				int predStartTime = startTimes.get(predOp);
				int predEndTime = predStartTime + processingTimes[predOp - 1];

				if (startTime < predEndTime) {
					// 平方违反值以更多地惩罚较大的违反
					double violation = Math.pow(predEndTime - startTime, 2);
					dagViolation += violation;
				}
			}
		}

		// 计算TCMB约束违反
		double tcmbViolation = 0.0;
		for (TCMB tcmb : problemSetting.getTCMBList()) {
			int op1 = tcmb.getOp1();
			int op2 = tcmb.getOp2();

			int startTime1 = startTimes.get(op1);
			int endTime1 = startTime1 + processingTimes[op1 - 1];
			int startTime2 = startTimes.get(op2);

			int timeLag = startTime2 - endTime1;
			int timeConstraint = tcmb.getTimeConstraint();

			if (timeLag > timeConstraint) {
				// 平方违反值
				double violation = Math.pow(timeLag - timeConstraint, 2);
				tcmbViolation += violation;
			}

			// 确保op1在op2之前完成（DAG约束）
			if (timeLag < 0) {
				double violation = Math.pow(-timeLag, 2);
				dagViolation += violation;
			}
		}

		// 检查机器重叠违反
		double machineOverlapViolation = calculateMachineOverlapViolation(schedule);

		// 最终适应度是makespan和违反的加权和
//		double scheduleLimitWeight = 3000; // 增加权重以更严格地强制执行约束

		double scheduleLimitWeight = 100000.0;
		return makespan + (scheduleLimitWeight * (dagViolation + tcmbViolation + machineOverlapViolation));
	}

	private double calculateMachineOverlapViolation(Schedule schedule) {
		// 按机器分组操作
		Map<Integer, List<int[]>> machineOperations = new HashMap<>();

		for (Map.Entry<Integer, Integer> entry : schedule.getAssignedMachine().entrySet()) {
			int opId = entry.getKey();
			int machineId = entry.getValue();
			int startTime = schedule.getStartTimes().get(opId);
			int endTime = startTime + processingTimes[opId - 1];

			machineOperations.computeIfAbsent(machineId, k -> new ArrayList<>())
					.add(new int[]{startTime, endTime, opId});
		}

		// 检查重叠
		double totalViolation = 0.0;
		for (List<int[]> operations : machineOperations.values()) {
			// 按开始时间排序
			operations.sort(Comparator.comparingInt(op -> op[0]));

			// 检查重叠
			for (int i = 0; i < operations.size() - 1; i++) {
				int[] currentOp = operations.get(i);
				int[] nextOp = operations.get(i + 1);

				int currentEnd = currentOp[1];
				int nextStart = nextOp[0];

				if (currentEnd > nextStart) {
					// 平方违反以更多地惩罚较大的重叠
					double violation = Math.pow(currentEnd - nextStart, 2);
					totalViolation += violation;
				}
			}
		}

		return totalViolation;
	}

	/**
	 * Applies final modification techniques to improve the solution further
	 * Based on the final modification in SAGAS.cpp
	 */
	private Schedule applyFinalModification(Schedule schedule) {
		if (DEBUG_MODE) {
			System.out.println("Starting final modification...");
			System.out.println("Initial fitness: " + calculateFitness(schedule));
		}

		// Apply local fitting
		schedule = applyLocalFitting(schedule);

		// Apply alignment
		schedule = applyAlignment(schedule);

		// Apply strict adjustment
		schedule = applyStrictAdjustment(schedule);

		if (DEBUG_MODE) {
			System.out.println("Final fitness after modifications: " + calculateFitness(schedule));
		}

		return schedule;
	}

	/**
	 * 应用局部拟合修改
	 * 基于SAGAS.cpp中的Local_fitting_J1和Local_fitting_J2
	 */
	private Schedule applyLocalFitting(Schedule initialSchedule) {
		if (DEBUG_MODE) {
			System.out.println("\n--- 开始局部拟合修改 ---");
			System.out.println("初始适应度: " + calculateFitness(initialSchedule));
			System.out.println("DAG约束违反: " + countDagViolations(initialSchedule));
			System.out.println("TCMB约束违反: " + countTcmbViolations(initialSchedule));
			System.out.println("机器重叠违反: " + countMachineOverlaps(initialSchedule));
		}

		Schedule bestSchedule = new Schedule(initialSchedule);
		double bestFitness = calculateFitness(bestSchedule);
		boolean improved;

		do {
			improved = false;

			// 第一轮：尝试将所有操作向前移动相同的量
			int lastStartTime = getLastStartTime(bestSchedule);

			for (int timeCriteria = 0; timeCriteria < lastStartTime; timeCriteria++) {
				Schedule candidateSchedule = new Schedule(bestSchedule);
				Map<Integer, Integer> startTimes = new HashMap<>(candidateSchedule.getStartTimes());
				Map<Integer, Integer> assignedMachine = candidateSchedule.getAssignedMachine();
				boolean validMove = true;

				// 按拓扑顺序对操作进行排序
				List<Integer> sortedOps = new ArrayList<>(startTimes.keySet());
				sortedOps.sort((a, b) -> {
					int[][] distanceMatrix = problemSetting.getDistanceMatrix();
					if (distanceMatrix[a][b] > 0) return -1; // a 在 b 之前
					if (distanceMatrix[b][a] > 0) return 1;  // b 在 a 之前
					return 0; // 没有依赖关系
				});

				// 按拓扑顺序移动操作
				for (int opId : sortedOps) {
					int startTime = startTimes.get(opId);

					if (startTime > timeCriteria) {
						int newStart = startTime - 1;

						// 检查这个移动是否会违反DAG约束
						int minStart = getMinimumStartTime(opId, startTimes);
						if (newStart < minStart) {
							validMove = false;
							break;
						}

						// 暂时设置新的开始时间
						int oldStart = startTimes.get(opId);
						startTimes.put(opId, newStart);

						// 检查移动后是否导致机器重叠
						if (hasOverlapsAfterMove(opId, startTimes, assignedMachine)) {
							// 如果导致重叠，恢复原始开始时间并标记为无效移动
							startTimes.put(opId, oldStart);
							validMove = false;
							break;
						}
					}
				}

				if (!validMove) continue;

				// 如果移动有效，重新处理所有操作以确保DAG约束和机器重叠约束得到满足
				for (int opId : sortedOps) {
					propagateChange(opId, startTimes, assignedMachine);
				}

				candidateSchedule = new Schedule(startTimes, assignedMachine);
				double candidateFitness = calculateFitness(candidateSchedule);

				if (candidateFitness < bestFitness) {
					bestSchedule = candidateSchedule;
					bestFitness = candidateFitness;
					improved = true;
					break;
				}
			}

			if (!improved) {
				// 第二轮：尝试单独移动每个作业的操作
				int jobNum = problemSetting.getJobNum();
				Map<Integer, Integer> opToJob = problemSetting.getOpToJob();

				for (int jobId = 1; jobId <= jobNum; jobId++) {
					lastStartTime = getLastStartTime(bestSchedule);

					for (int timeCriteria = 0; timeCriteria < lastStartTime; timeCriteria++) {
						Schedule candidateSchedule = new Schedule(bestSchedule);
						Map<Integer, Integer> startTimes = new HashMap<>(candidateSchedule.getStartTimes());
						Map<Integer, Integer> assignedMachine = candidateSchedule.getAssignedMachine();
						boolean validMove = true;

						// 获取此作业的所有操作并按拓扑顺序排序
						List<Integer> jobOps = new ArrayList<>();
						for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
							int opId = entry.getKey();
							if (opToJob.get(opId) == jobId) {
								jobOps.add(opId);
							}
						}

						jobOps.sort((a, b) -> {
							int[][] distanceMatrix = problemSetting.getDistanceMatrix();
							if (distanceMatrix[a][b] > 0) return -1;
							if (distanceMatrix[b][a] > 0) return 1;
							return 0;
						});

						// 按拓扑顺序移动作业的操作
						for (int opId : jobOps) {
							int startTime = startTimes.get(opId);

							if (startTime > timeCriteria) {
								int newStart = startTime - 1;

								// 检查这个移动是否会违反DAG约束
								int minStart = getMinimumStartTime(opId, startTimes);
								if (newStart < minStart) {
									validMove = false;
									break;
								}

								// 暂时设置新的开始时间
								int oldStart = startTimes.get(opId);
								startTimes.put(opId, newStart);

								// 检查移动后是否导致机器重叠
								if (hasOverlapsAfterMove(opId, startTimes, assignedMachine)) {
									// 如果导致重叠，恢复原始开始时间并标记为无效移动
									startTimes.put(opId, oldStart);
									validMove = false;
									break;
								}
							}
						}

						if (!validMove) continue;

						// 如果移动有效，重新处理所有操作以确保DAG约束和机器重叠约束得到满足
						for (int opId : jobOps) {
							propagateChange(opId, startTimes, assignedMachine);
						}

						candidateSchedule = new Schedule(startTimes, assignedMachine);
						double candidateFitness = calculateFitness(candidateSchedule);

						if (candidateFitness < bestFitness) {
							bestSchedule = candidateSchedule;
							bestFitness = candidateFitness;
							improved = true;
							break;
						}
					}

					if (improved) break;
				}
			}
		} while (improved);

		if (DEBUG_MODE) {
			System.out.println("局部拟合后的适应度: " + bestFitness);
			System.out.println("改进量: " + (calculateFitness(initialSchedule) - bestFitness));
			System.out.println("局部拟合后DAG约束违反: " + countDagViolations(bestSchedule));
			System.out.println("局部拟合后TCMB约束违反: " + countTcmbViolations(bestSchedule));
			System.out.println("局部拟合后机器重叠违反: " + countMachineOverlaps(bestSchedule));
			System.out.println("--- 局部拟合修改完成 ---");
		}

		return bestSchedule;
	}

	/**
	 * 应用对齐修改
	 * 基于SAGAS.cpp中的Aligning部分
	 */
	private Schedule applyAlignment(Schedule initialSchedule) {
		if (DEBUG_MODE) {
			System.out.println("\n--- 开始对齐修改 ---");
			System.out.println("初始适应度: " + calculateFitness(initialSchedule));
			System.out.println("DAG约束违反: " + countDagViolations(initialSchedule));
			System.out.println("TCMB约束违反: " + countTcmbViolations(initialSchedule));
			System.out.println("机器重叠违反: " + countMachineOverlaps(initialSchedule));
		}

		Schedule bestSchedule = new Schedule(initialSchedule);
		double bestFitness = calculateFitness(bestSchedule);
		boolean aligned;

		do {
			aligned = false;
			Schedule keepBest = new Schedule(bestSchedule);
			int lastStartTime = getLastStartTime(bestSchedule);

			// 尝试通过移动操作来对齐它们
			for (int jobId = 1; jobId <= problemSetting.getJobNum(); jobId++) {
				for (int timeCriteria = 0; timeCriteria <= lastStartTime; timeCriteria++) {
					Schedule candidateSchedule = new Schedule(bestSchedule);
					Map<Integer, Integer> startTimes = new HashMap<>(candidateSchedule.getStartTimes());
					Map<Integer, Integer> assignedMachine = candidateSchedule.getAssignedMachine();
					Map<Integer, Integer> opToJob = problemSetting.getOpToJob();
					boolean validMove = true;

					// 获取此作业的操作
					List<Integer> jobOps = new ArrayList<>();
					for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
						int opId = entry.getKey();
						if (opToJob.get(opId) == jobId) {
							jobOps.add(opId);
						}
					}

					// 按拓扑顺序排序
					jobOps.sort((a, b) -> {
						int[][] distanceMatrix = problemSetting.getDistanceMatrix();
						if (distanceMatrix[a][b] > 0) return -1;
						if (distanceMatrix[b][a] > 0) return 1;
						return 0;
					});

					// 移动操作
					for (int opId : jobOps) {
						int startTime = startTimes.get(opId);

						if (startTime <= lastStartTime - timeCriteria) {
							int newStart = Math.max(startTime - 1, 0);

							// 检查这个移动是否会违反DAG约束
							int minStart = getMinimumStartTime(opId, startTimes);
							if (newStart < minStart) {
								validMove = false;
								break;
							}

							// 暂时设置新的开始时间
							int oldStart = startTimes.get(opId);
							startTimes.put(opId, newStart);

							// 检查移动后是否导致机器重叠
							if (hasOverlapsAfterMove(opId, startTimes, assignedMachine)) {
								// 如果导致重叠，恢复原始开始时间并标记为无效移动
								startTimes.put(opId, oldStart);
								validMove = false;
								break;
							}
						}
					}

					if (!validMove) continue;

					// 如果移动有效，重新处理所有操作以确保DAG约束和机器重叠约束得到满足
					for (int opId : jobOps) {
						propagateChange(opId, startTimes, assignedMachine);
					}

					candidateSchedule = new Schedule(startTimes, assignedMachine);
					double candidateFitness = calculateFitness(candidateSchedule);

					if (candidateFitness <= bestFitness) {
						bestSchedule = candidateSchedule;
						bestFitness = candidateFitness;
					}
				}
			}

			if (!keepBest.getStartTimes().equals(bestSchedule.getStartTimes())) {
				aligned = true;

				// 对齐后再次应用局部拟合
				bestSchedule = applyLocalFitting(bestSchedule);
				bestFitness = calculateFitness(bestSchedule);
			}
		} while (aligned);

		if (DEBUG_MODE) {
			System.out.println("对齐后的适应度: " + bestFitness);
			System.out.println("改进量: " + (calculateFitness(initialSchedule) - bestFitness));
			System.out.println("对齐后DAG约束违反: " + countDagViolations(bestSchedule));
			System.out.println("对齐后TCMB约束违反: " + countTcmbViolations(bestSchedule));
			System.out.println("对齐后机器重叠违反: " + countMachineOverlaps(bestSchedule));
			System.out.println("--- 对齐修改完成 ---");
		}

		return bestSchedule;
	}

	/**
	 * 应用严格调整修改
	 * 基于SAGAS.cpp中的Strict_adjustment部分
	 */
	private Schedule applyStrictAdjustment(Schedule initialSchedule) {
		if (DEBUG_MODE) {
			System.out.println("\n--- 开始严格调整修改 ---");
			System.out.println("初始适应度: " + calculateFitness(initialSchedule));
			System.out.println("DAG约束违反: " + countDagViolations(initialSchedule));
			System.out.println("TCMB约束违反: " + countTcmbViolations(initialSchedule));
			System.out.println("机器重叠违反: " + countMachineOverlaps(initialSchedule));
		}

		Schedule bestSchedule = new Schedule(initialSchedule);
		double bestFitness = calculateFitness(bestSchedule);
		boolean improved;

		// 粗调整
		do {
			improved = false;
			Schedule keepBest = new Schedule(bestSchedule);
			int lastStartTime = getLastStartTime(bestSchedule);
			int presence = 0;

			// 尝试移动所有操作
			for (int timeCriteria = 0; timeCriteria < lastStartTime; timeCriteria++) {
				Schedule candidateSchedule = new Schedule(bestSchedule);
				Map<Integer, Integer> startTimes = new HashMap<>(candidateSchedule.getStartTimes());
				Map<Integer, Integer> assignedMachine = candidateSchedule.getAssignedMachine();
				boolean validMove = true;

				// 按拓扑顺序对操作排序
				List<Integer> sortedOps = new ArrayList<>(startTimes.keySet());
				sortedOps.sort((a, b) -> {
					int[][] distanceMatrix = problemSetting.getDistanceMatrix();
					if (distanceMatrix[a][b] > 0) return -1;
					if (distanceMatrix[b][a] > 0) return 1;
					return 0;
				});

				// 移动操作
				for (int opId : sortedOps) {
					int startTime = startTimes.get(opId);

					if (startTime > timeCriteria) {
						int newStart = startTime - 1;

						// 检查此移动是否会违反DAG约束
						int minStart = getMinimumStartTime(opId, startTimes);
						if (newStart < minStart) {
							validMove = false;
							break;
						}

						// 暂时设置新的开始时间
						int oldStart = startTimes.get(opId);
						startTimes.put(opId, newStart);

						// 检查移动后是否导致机器重叠
						if (hasOverlapsAfterMove(opId, startTimes, assignedMachine)) {
							// 如果导致重叠，恢复原始开始时间并标记为无效移动
							startTimes.put(opId, oldStart);
							validMove = false;
							break;
						}

						presence = Math.max(timeCriteria + 1, presence);
					}
				}

				if (!validMove) continue;

				// 如果移动有效，重新处理所有操作以确保DAG约束和机器重叠约束得到满足
				for (int opId : sortedOps) {
					propagateChange(opId, startTimes, assignedMachine);
				}

				candidateSchedule = new Schedule(startTimes, assignedMachine);
				double candidateFitness = calculateFitness(candidateSchedule);

				if (candidateFitness <= bestFitness) {
					bestSchedule = candidateSchedule;
					bestFitness = candidateFitness;
				}
			}

			lastStartTime = presence;

			// 尝试单独移动每个作业的操作
			for (int jobId = 1; jobId <= problemSetting.getJobNum(); jobId++) {
				for (int timeCriteria = 0; timeCriteria < lastStartTime; timeCriteria++) {
					Schedule candidateSchedule = new Schedule(bestSchedule);
					Map<Integer, Integer> startTimes = new HashMap<>(candidateSchedule.getStartTimes());
					Map<Integer, Integer> assignedMachine = candidateSchedule.getAssignedMachine();
					Map<Integer, Integer> opToJob = problemSetting.getOpToJob();
					boolean validMove = true;

					// 获取此作业的操作
					List<Integer> jobOps = new ArrayList<>();
					for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
						int opId = entry.getKey();
						if (opToJob.get(opId) == jobId) {
							jobOps.add(opId);
						}
					}

					// 按拓扑顺序排序
					jobOps.sort((a, b) -> {
						int[][] distanceMatrix = problemSetting.getDistanceMatrix();
						if (distanceMatrix[a][b] > 0) return -1;
						if (distanceMatrix[b][a] > 0) return 1;
						return 0;
					});

					// 移动操作
					for (int opId : jobOps) {
						int startTime = startTimes.get(opId);

						if (startTime > timeCriteria) {
							int newStart = startTime - 1;

							// 检查此移动是否会违反DAG约束
							int minStart = getMinimumStartTime(opId, startTimes);
							if (newStart < minStart) {
								validMove = false;
								break;
							}

							// 暂时设置新的开始时间
							int oldStart = startTimes.get(opId);
							startTimes.put(opId, newStart);

							// 检查移动后是否导致机器重叠
							if (hasOverlapsAfterMove(opId, startTimes, assignedMachine)) {
								// 如果导致重叠，恢复原始开始时间并标记为无效移动
								startTimes.put(opId, oldStart);
								validMove = false;
								break;
							}
						}
					}

					if (!validMove) continue;

					// 如果移动有效，重新处理所有操作以确保DAG约束和机器重叠约束得到满足
					for (int opId : jobOps) {
						propagateChange(opId, startTimes, assignedMachine);
					}

					candidateSchedule = new Schedule(startTimes, assignedMachine);
					double candidateFitness = calculateFitness(candidateSchedule);

					if (candidateFitness <= bestFitness) {
						bestSchedule = candidateSchedule;
						bestFitness = candidateFitness;
					}
				}
			}

			if (!keepBest.getStartTimes().equals(bestSchedule.getStartTimes())) {
				improved = true;
			}
		} while (improved);

		// 精细调整
		do {
			improved = false;
			Schedule keepBest = new Schedule(bestSchedule);

			// 尝试移动单个操作
			for (int opId = 1; opId <= problemSetting.getTotalOpNum(); opId++) {
				Schedule candidateSchedule = new Schedule(bestSchedule);
				Map<Integer, Integer> startTimes = new HashMap<>(candidateSchedule.getStartTimes());
				Map<Integer, Integer> assignedMachine = candidateSchedule.getAssignedMachine();

				int startTime = startTimes.get(opId);

				if (startTime != 0) {
					// 尝试将此操作提前
					int newStart = startTime - 1;

					// 检查此移动是否会违反DAG约束
					int minStart = getMinimumStartTime(opId, startTimes);
					if (newStart < minStart) {
						continue;
					}

					// 暂时设置新的开始时间
					startTimes.put(opId, newStart);

					// 检查移动后是否导致机器重叠
					if (hasOverlapsAfterMove(opId, startTimes, assignedMachine)) {
						continue;
					}

					// 传播更改以保持DAG约束和避免机器重叠
					propagateChange(opId, startTimes, assignedMachine);

					candidateSchedule = new Schedule(startTimes, assignedMachine);
					double candidateFitness = calculateFitness(candidateSchedule);

					if (candidateFitness <= bestFitness) {
						bestSchedule = candidateSchedule;
						bestFitness = candidateFitness;
						improved = true;
					}
				}
			}

			if (improved) {
				// 如果有改进，重新运行粗调整
				Schedule roughSchedule = applyStrictAdjustment(bestSchedule);
				double roughFitness = calculateFitness(roughSchedule);

				if (roughFitness < bestFitness) {
					bestSchedule = roughSchedule;
					bestFitness = roughFitness;
				}
			}
		} while (improved);

		if (DEBUG_MODE) {
			System.out.println("严格调整后的适应度: " + bestFitness);
			System.out.println("改进量: " + (calculateFitness(initialSchedule) - bestFitness));
			System.out.println("严格调整后DAG约束违反: " + countDagViolations(bestSchedule));
			System.out.println("严格调整后TCMB约束违反: " + countTcmbViolations(bestSchedule));
			System.out.println("严格调整后机器重叠违反: " + countMachineOverlaps(bestSchedule));
			System.out.println("--- 严格调整修改完成 ---");
		}

		return bestSchedule;
	}

	/**
	 * Gets the latest start time of any operation in the schedule
	 */
	private int getLastStartTime(Schedule schedule) {
		return schedule.getStartTimes().values().stream()
				.mapToInt(Integer::intValue)
				.max()
				.orElse(0);
	}

	// 计算DAG约束违反数量
	private int countDagViolations(Schedule schedule) {
		Map<Integer, Integer> startTimes = schedule.getStartTimes();
		int violations = 0;

		for (int opId = 1; opId <= problemSetting.getTotalOpNum(); opId++) {
			int startTime = startTimes.get(opId);

			for (int predOp : problemSetting.getReverseDag().getNeighbors(opId)) {
				int predStartTime = startTimes.get(predOp);
				int predEndTime = predStartTime + processingTimes[predOp - 1];

				if (startTime < predEndTime) {
					violations++;
				}
			}
		}

		return violations;
	}

	// 计算TCMB约束违反数量
	private int countTcmbViolations(Schedule schedule) {
		Map<Integer, Integer> startTimes = schedule.getStartTimes();
		int violations = 0;

		for (TCMB tcmb : problemSetting.getTCMBList()) {
			int op1 = tcmb.getOp1();
			int op2 = tcmb.getOp2();

			int startTime1 = startTimes.get(op1);
			int endTime1 = startTime1 + processingTimes[op1 - 1];
			int startTime2 = startTimes.get(op2);

			int timeLag = startTime2 - endTime1;
			int timeConstraint = tcmb.getTimeConstraint();

			if (timeLag > timeConstraint || timeLag < 0) {
				violations++;
			}
		}

		return violations;
	}

	// 计算机器重叠违反数量
	private int countMachineOverlaps(Schedule schedule) {
		Map<Integer, Integer> startTimes = schedule.getStartTimes();
		Map<Integer, Integer> assignedMachine = schedule.getAssignedMachine();
		int overlaps = 0;

		// 按机器分组操作
		Map<Integer, List<int[]>> machineOperations = new HashMap<>();

		for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
			int opId = entry.getKey();
			int machineId = entry.getValue();
			int startTime = startTimes.get(opId);
			int endTime = startTime + processingTimes[opId - 1];

			machineOperations.computeIfAbsent(machineId, k -> new ArrayList<>())
					.add(new int[]{startTime, endTime, opId});
		}

		// 检查重叠
		for (List<int[]> operations : machineOperations.values()) {
			// 按开始时间对操作排序
			operations.sort(Comparator.comparingInt(op -> op[0]));

			// 检查重叠
			for (int i = 0; i < operations.size() - 1; i++) {
				int[] currentOp = operations.get(i);
				int[] nextOp = operations.get(i + 1);

				int currentEnd = currentOp[1];
				int nextStart = nextOp[0];

				if (currentEnd > nextStart) {
					overlaps++;
				}
			}
		}

		return overlaps;
	}

	/**
	 * 检查指定操作移动后是否会导致机器重叠
	 */
	private boolean hasOverlapsAfterMove(int opId, Map<Integer, Integer> startTimes, Map<Integer, Integer> assignedMachine) {
		int machineId = assignedMachine.get(opId);
		int opStart = startTimes.get(opId);
		int opEnd = opStart + processingTimes[opId - 1];

		// 检查同一机器上的其它操作
		for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
			int otherOpId = entry.getKey();
			if (otherOpId != opId && entry.getValue() == machineId) {
				int otherStart = startTimes.get(otherOpId);
				int otherEnd = otherStart + processingTimes[otherOpId - 1];

				// 检查重叠
				if (opStart < otherEnd && opEnd > otherStart) {
					return true; // 有重叠
				}
			}
		}

		return false; // 无重叠
	}


	/**
	 * 打印完整的调度方案
	 * 包括每个操作的开始时间和分配的机器
	 * 并且按机器分组显示操作执行区间
	 */
	private void printDetailedSchedule(Schedule schedule) {
		Map<Integer, Integer> startTimes = schedule.getStartTimes();
		Map<Integer, Integer> assignedMachine = schedule.getAssignedMachine();

		System.out.println("\n========== 完整调度方案 ==========");

//		// 1. 按操作ID打印详细信息
//		System.out.println("\n--- 按操作ID排序的调度信息 ---");
//		System.out.println("操作ID\t作业ID\t开始时间\t结束时间\t分配机器\t处理时间");
//		System.out.println("--------------------------------------------------");
//
//		// 按操作ID排序
//		List<Integer> sortedOps = new ArrayList<>(startTimes.keySet());
//		Collections.sort(sortedOps);
//
//		Map<Integer, Integer> opToJob = problemSetting.getOpToJob();
//
//		for (int opId : sortedOps) {
//			int jobId = opToJob.get(opId);
//			int startTime = startTimes.get(opId);
//			int processingTime = processingTimes[opId - 1];
//			int endTime = startTime + processingTime;
//			int machineId = assignedMachine.get(opId);
//
//			System.out.printf("%d\t%d\t%d\t%d\t%d\t%d\n",
//					opId, jobId, startTime, endTime, machineId, processingTime);
//		}

		// 2. 按机器分组打印操作执行区间
		System.out.println("\n--- 按机器分组的操作执行顺序 ---");

		// 按机器分组操作
		Map<Integer, List<int[]>> machineOperations = new HashMap<>();

		for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
			int opId = entry.getKey();
			int machineId = entry.getValue();
			int startTime = startTimes.get(opId);
			int endTime = startTime + processingTimes[opId - 1];

			machineOperations.computeIfAbsent(machineId, k -> new ArrayList<>())
					.add(new int[]{startTime, endTime, opId});
		}

		// 获取所有机器ID并排序
		List<Integer> machineIds = new ArrayList<>(machineOperations.keySet());
		Collections.sort(machineIds);

		// 对每台机器，按开始时间排序其上的操作，并打印执行区间
		for (int machineId : machineIds) {
			List<int[]> operations = machineOperations.get(machineId);

			// 按开始时间排序
			operations.sort(Comparator.comparingInt(op -> op[0]));

			StringBuilder sb = new StringBuilder();
			sb.append("机器 ").append(machineId).append(": ");

			for (int[] op : operations) {
				int startTime = op[0];
				int endTime = op[1];
				int opId = op[2];

				sb.append("[").append(startTime).append(",").append(endTime);
				sb.append("](op").append(opId).append(") ");
			}

			System.out.println(sb.toString());
		}

//		// 3. 按作业分组打印操作信息
//		System.out.println("\n--- 按作业分组的操作信息 ---");
//
//		// 按作业分组操作
//		Map<Integer, List<int[]>> jobOperations = new HashMap<>();
//
//		for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
//			int opId = entry.getKey();
//			int jobId = opToJob.get(opId);
//			int startTime = entry.getValue();
//			int endTime = startTime + processingTimes[opId - 1];
//			int machineId = assignedMachine.get(opId);
//
//			jobOperations.computeIfAbsent(jobId, k -> new ArrayList<>())
//					.add(new int[]{startTime, endTime, opId, machineId});
//		}

//		// 获取所有作业ID并排序
//		List<Integer> jobIds = new ArrayList<>(jobOperations.keySet());
//		Collections.sort(jobIds);
//
//		// 对每个作业，按操作ID排序其包含的操作，并打印执行信息
//		for (int jobId : jobIds) {
//			List<int[]> operations = jobOperations.get(jobId);
//
//			// 按操作ID排序
//			operations.sort(Comparator.comparingInt(op -> op[2]));
//
//			System.out.println("作业 " + jobId + ":");
//			System.out.println("操作ID\t开始时间\t结束时间\t机器ID");
//			System.out.println("--------------------------------");
//
//			for (int[] op : operations) {
//				int startTime = op[0];
//				int endTime = op[1];
//				int opId = op[2];
//				int machineId = op[3];
//
//				System.out.printf("%d\t%d\t%d\t%d\n", opId, startTime, endTime, machineId);
//			}
//
//			System.out.println();
//		}

		// 4. 统计调度信息
		int makespan = Utility.calculateMakespan(schedule);
		int numOperations = startTimes.size();
		int numMachines = machineOperations.size();

		System.out.println("\n--- 调度统计信息 ---");
		System.out.println("操作数量: " + numOperations);
		System.out.println("使用机器数量: " + numMachines);
		System.out.println("Makespan: " + makespan);

		// 计算平均机器利用率
		double totalProcessingTime = 0;
		for (int opId : startTimes.keySet()) {
			totalProcessingTime += processingTimes[opId - 1];
		}

		double averageUtilization = (totalProcessingTime / (makespan * numMachines)) * 100;
		System.out.printf("平均机器利用率: %.2f%%\n", averageUtilization);

		System.out.println("========== 调度方案结束 ==========\n");
	}
}