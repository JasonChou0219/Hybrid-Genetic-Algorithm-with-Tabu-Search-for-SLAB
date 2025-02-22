package com.zihao.GA_TS_SLAB.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Schedule;
import com.zihao.GA_TS_SLAB.Data.Input;



public class TestConstraint {

	public File parentDir;
	public ProblemSetting pb;
	public File testFile;
	public File dataSetPath;

	public TestConstraint(String testName){
		this.parentDir = new File("src/com/zihao/GA_TS_SLAB/Test");
		this.pb = ProblemSetting.getInstance();
		this.testFile = new File(parentDir, testName+".tsv");
		this.dataSetPath = getDataSetPath(testName);
	}


	public Schedule getScheduleFromTestFile() throws IOException {
		// 初始化 Schedule 的成员变量
		Map<Integer, Integer> startTimes = new HashMap<>();
		Map<Integer, Integer> assignedMachine = new HashMap<>();

		// 读取 TSV 文件
		try (BufferedReader reader = new BufferedReader(new FileReader(testFile))) {
			String line;
			line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				// 解析每一行
				String[] parts = line.split("\t");
				int jobID = Integer.parseInt(parts[0]);
				int operationID = Integer.parseInt(parts[1]);
				int start = Integer.parseInt(parts[2]);
				int machineID = Integer.parseInt(parts[4]);

				// 获取全局唯一的 Operation ID
				int globalOpID = getUniqueOpID(jobID, operationID, pb.getOpNumEachJob());
				// 填充 startTimes 和 assignedMachine
				startTimes.put(globalOpID, start);
				assignedMachine.put(globalOpID, machineID);
			}
		}

		// 创建并返回 Schedule 对象
		return new Schedule(
				startTimes,
				assignedMachine
		);
	}


	public boolean checkDependency(Schedule schedule) {
		// 获取调度方案中的 startTimes 和 assignedMachine
		Map<Integer, Integer> startTimes = schedule.getStartTimes();
		int[][]distanceMatrix = pb.getDistanceMatrix();

		// 遍历 distanceMatrix 检查依赖关系
		for (int i = 1; i < distanceMatrix.length; i++) {
			for (int j = 1; j < distanceMatrix[i].length; j++) {
				// 如果 Distance[i][j] > 0，表示 i 是 j 的前驱
				if (distanceMatrix[i][j] > 0) {
					// 获取 operation i 和 j 的开始时间
					int startTimeI = startTimes.getOrDefault(i , -1); // 矩阵索引从 0 开始，operation ID 从 1 开始
					int startTimeJ = startTimes.getOrDefault(j , -1);

					// 如果 startTimeI 或 startTimeJ 不存在，跳过检查
					if (startTimeI == -1 || startTimeJ == -1) {
						continue;
					}

					// 检查是否违反依赖关系
					if (startTimeI >= startTimeJ) {
						System.out.println("Violation: Operation " + i + " (starts at " + startTimeI + ") "
								+ "must start before Operation " + j + " (starts at " + startTimeJ + ")");
						System.out.println("i = " + i + " j = " + j +  "  " + "distanceMatrix[i][j] = " + distanceMatrix[i][j]);
						return false;
					}
				}
			}
		}

		// 如果没有违反任何依赖关系
		return true;
	}

	public boolean checkMachineConstraint(Schedule schedule) {
		// 1. 获取调度中的 assignedMachine 映射
		Map<Integer, Integer> assignedMachineMap = schedule.getAssignedMachine();

		// 2. 防御性检查: 确保映射本身不为空且有效
		if (assignedMachineMap == null) {
			throw new IllegalArgumentException("Schedule's assignedMachine map is null");
		}

		// 3. 遍历所有已分配机器的 operation
		for (Map.Entry<Integer, Integer> entry : assignedMachineMap.entrySet()) {
			int opId = entry.getKey();
			int machineId = entry.getValue();

			// 4. 获取该 operation 的兼容机器列表 (防御性检查)

			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(opId);


			// 5. 检查兼容列表是否存在且非空
			if (compatibleMachines == null) {
				throw new IllegalStateException("No compatible machines defined for operation " + opId);
			}
			if (compatibleMachines.isEmpty()) {
				throw new IllegalStateException("Empty compatible machine list for operation " + opId);
			}

			// 6. 检查机器是否在兼容列表中 (优化查找)
			if (!isMachineCompatible(machineId, compatibleMachines)) {
				System.err.printf("Violation: Operation %d assigned to machine %d, " +
								"but compatible machines are %s%n",
						opId, machineId, compatibleMachines);
				return false;
			}
		}

		// 7. 检查未分配机器的 operation 是否有兼容性要求 (深度防御)
		Set<Integer> allOperations = pb.getOpToCompatibleList().keySet();
		for (int opId : allOperations) {
			if (!assignedMachineMap.containsKey(opId)) {
				throw new IllegalStateException("Operation " + opId + " has compatibility rules " +
						"but is not assigned to any machine");
			}
		}

		return true;
	}

	// 使用 HashSet 加速查找 (时间复杂度 O(1))
	private boolean isMachineCompatible(int machineId, List<Integer> compatibleMachines) {
		// 预检查: 确保输入有效性
		if (compatibleMachines == null || compatibleMachines.isEmpty()) {
			throw new IllegalArgumentException("Invalid compatible machines list");
		}

		// 使用临时 HashSet 优化查找性能
		Set<Integer> machineSet = new HashSet<>(compatibleMachines);
		return machineSet.contains(machineId);
	}



	public int getUniqueOpID(int jobID, int operationID, int[] opNumEachJob) {
		int globalOpID = 0;

		for (int j = 1; j < jobID; j++) {
			globalOpID += opNumEachJob[j-1];
		}

		globalOpID += operationID;
		return globalOpID;
	}

	private BufferedReader getBufferedReader(File file) throws IOException {
		return new BufferedReader(new FileReader(file));
	}

	public static File getDataSetPath(String testName){
		File parent = new File("src/Dataset");
		if (testName == "Gu2016_N1") {
			return new File(parent, "Gu2016/N1");
		}
		if (testName == "Gu2016_N5") {
			return new File(parent, "Gu2016/N5");
		}
		if (testName == "qPCR_N5") {
			return new File(parent, "qPCR.N5");
		}
		if (testName == "qPCR_RNAseq_N5"){
			return new File(parent,"qPCR_RNAseq/N5_N5");
		}
		if (testName == "RNAseq_N5"){
			return new File(parent, "RNAseq/N5");
		}
		if (testName == "RNAseq_N10"){
			return new File(parent, "RNAseq/N10_min");
		}
		return null;
	}

	public boolean checkTCMBConstraint(Schedule schedule) {
		// 1. 获取基础数据 (防御性校验)
		final List<TCMB> tcmbList = pb.getTCMBList();
		if (tcmbList == null) throw new IllegalStateException("TCMB list is null");

		final int[] processingTime = pb.getProcessingTime();
		if (processingTime == null || processingTime.length == 0) {
			throw new IllegalStateException("Processing time array is invalid");
		}

		final Map<Integer, Integer> startTimes = schedule.getStartTimes();
		if (startTimes == null) throw new IllegalArgumentException("Schedule startTimes is null");

		// 2. 预计算全局操作ID映射表 (O(1)快速访问)
		final Set<Integer> scheduledOperations = startTimes.keySet();

		// 3. 并行流加速处理 (利用多核CPU)
		return tcmbList.parallelStream().allMatch(tcmb -> {
			// 4. 原子性提取约束参数
			final int op1 = tcmb.getOp1();
			final int op2 = tcmb.getOp2();
			final int constraint = tcmb.getTimeConstraint();

			// 5. 深度参数校验
			if (op1 <= 0 || op2 <= 0) {
				throw new IllegalStateException(
						String.format("Invalid operation IDs in TCMB: op1=%d, op2=%d", op1, op2));
			}
			if (constraint < 0) {
				throw new IllegalStateException(
						String.format("Negative constraint %d for TCMB(%d->%d)", constraint, op1, op2));
			}

			// 6. 索引安全转换 (处理ID与数组索引的映射)
			final int op1Idx = op1 - 1; // 假设operation ID从1开始
			final int op2Idx = op2 - 1;

			// 7. 加工时间有效性验证
			if (op1Idx >= processingTime.length || op1Idx < 0) {
				throw new IndexOutOfBoundsException(
						String.format("op1=%d exceeds processingTime array size %d", op1, processingTime.length));
			}
			if (op2Idx >= processingTime.length || op2Idx < 0) {
				throw new IndexOutOfBoundsException(
						String.format("op2=%d exceeds processingTime array size %d", op2, processingTime.length));
			}

			// 8. 调度存在性检查
			if (!scheduledOperations.contains(op1)) {
				throw new IllegalStateException(
						String.format("TCMB requires unscheduled operation: op1=%d", op1));
			}
			if (!scheduledOperations.contains(op2)) {
				throw new IllegalStateException(
						String.format("TCMB requires unscheduled operation: op2=%d", op2));
			}

			// 9. 时间计算与约束验证
			final int start1 = startTimes.get(op1);
			final int start2 = startTimes.get(op2);
			final int duration1 = processingTime[op1Idx];
			final int end1 = start1 + duration1;
			final int interval = start2 - end1;

			// 10. 核心约束逻辑 (允许interval=0)
			if (interval < 0 || interval > constraint) {
				System.err.printf("[TCMB VIOLATION] %d->%d: end1=%d, start2=%d, interval=%d > constraint=%d%n",
						op1, op2, end1, start2, interval, constraint);
				return false;
			}

			return true;
		});
	}


	public boolean checkMachineOccupation(Schedule schedule) {
		// 0. 防御性校验基础数据结构
		final Map<Integer, Integer> assignedMachine = schedule.getAssignedMachine();
		final Map<Integer, Integer> startTimes = schedule.getStartTimes();
		final int[] processingTime = pb.getProcessingTime();

		if (assignedMachine == null) throw new IllegalArgumentException("assignedMachine is null");
		if (startTimes == null) throw new IllegalArgumentException("startTimes is null");
		if (processingTime == null || processingTime.length == 0) {
			throw new IllegalStateException("Invalid processingTime array");
		}

		// 1. 构建机器分配索引 (O(N)时间 + O(M)空间)
		final Map<Integer, List<OperationInterval>> machineTimeline = new HashMap<>();

		assignedMachine.forEach((opId, machineId) -> {
			// 2. 深度数据校验
			final Integer startTime = startTimes.get(opId);
			if (startTime == null) {
				throw new IllegalStateException("Operation " + opId + " has no start time");
			}
			if (machineId <= 0) {
				throw new IllegalStateException("Invalid machine ID " + machineId + " for operation " + opId);
			}

			// 3. 加工时间安全获取 (防御索引越界)
			final int opIndex = opId - 1; // 假设operation ID从1开始
			if (opIndex < 0 || opIndex >= processingTime.length) {
				throw new IndexOutOfBoundsException(String.format(
						"Operation %d index out of processingTime bounds [0, %d)", opId, processingTime.length));
			}
			final int duration = processingTime[opIndex];
			if (duration <= 0) {
				throw new IllegalStateException("Non-positive duration " + duration + " for operation " + opId);
			}

			// 4. 构建时间区间对象
			final OperationInterval interval = new OperationInterval(opId, startTime, startTime + duration);

			// 5. 按机器分组 (自动处理机器ID不存在的情况)
			machineTimeline.computeIfAbsent(machineId, k -> new ArrayList<>()).add(interval);
		});

		// 6. 并行化处理每台机器的时序检查 (利用多核CPU)
		return machineTimeline.entrySet().parallelStream().allMatch(entry -> {
			final int machineId = entry.getKey();
			List<OperationInterval> intervals = entry.getValue();

			// 7. 单机器优化检查 (无需排序的快速检查)
			if (intervals.size() < 2) return true; // 单操作无需检查

			// 8. 内存优化排序 (原地排序避免拷贝)
			intervals.sort(Comparator.comparingInt(OperationInterval::start));

			// 9. 滑动窗口检查重叠 (时间复杂度O(n))
			int previousEnd = intervals.get(0).end();
			for (int i = 1; i < intervals.size(); i++) {
				final OperationInterval current = intervals.get(i);

				// 10. 精确重叠检测 (处理零宽间隔)
				if (current.start() < previousEnd) { // 严格小于，等于不视为重叠
					System.err.printf("[COLLISION] Machine %d: Operation %d [%d-%d] overlaps with previous ending at %d%n",
							machineId, current.opId(), current.start(), current.end(), previousEnd);
					return false;
				}
				previousEnd = Math.max(previousEnd, current.end()); // 处理嵌套区间
			}
			return true;
		});
	}

	// 内存优化记录类 (不可变数据结构)
	private record OperationInterval(int opId, int start, int end) {
		// 防御性构造函数
		public OperationInterval {
			if (start < 0) throw new IllegalArgumentException("Negative start time: " + start);
			if (end <= start) throw new IllegalArgumentException("Invalid interval: " + start + "-" + end);
		}
	}

	//请补全main函数
	public static void main(String[] args){
		String testName = "qPCR_RNAseq_N5";
		File dataSetPath = getDataSetPath(testName);
		Input input = new Input(dataSetPath);
		input.getProblemDesFromFile();
		TestConstraint test = new TestConstraint(testName);

		try {
			Schedule schedule = test.getScheduleFromTestFile();
			System.out.println("Schedule created successfully!");
//			System.out.println(schedule);
			if (!test.checkDependency(schedule)){
				System.out.println("The schedule has violated precedence constraint");
			}
			if (!test.checkMachineConstraint(schedule)){
				System.out.println("The schedule has violates machine constraint");
			}
			if (!test.checkTCMBConstraint(schedule)){
				System.out.println("The schedule has violated TCMB constraint");
			}
			if (!test.checkMachineOccupation(schedule)){
				System.out.println("The schedule has violated machine occupation constraint");
			}
		} catch (IOException e) {
			System.err.println("Error reading file: " + e.getMessage());
		}
//		test.pb.printDistanceMatrix(test.pb.getTotalOpNum());
//		test.pb.printDag();

	}
}
