package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.CompoundMove;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompoundMoveTest {

	static public ProblemSetting pb = ProblemSetting.getInstance();
	/**
	 * 测试单次compound move的效果
	 */
	public static void testSingleCompoundMove(Schedule schedule) {
		// 打印所有TCMB约束
//		printTCMBConstraints();
//		printTCMBViolations(schedule);

		// 1. 记录移动前的状态和违反情况
		System.out.println("Before compound move:");
//		System.out.println(schedule.toString());
		Map<Integer, Double> beforeViolations = calculateTCMBViolations(schedule);

		if (beforeViolations.isEmpty()) {
			System.out.println("No TCMB violations to fix!");
			return;
		}

		// 2. 选择一个违反TCMB约束的操作
		int targetOp = selectOperationForMove(schedule);
		if (targetOp == -1) {
			System.out.println("Failed to select operation for move");
			return;
		}
		System.out.println("\nSelected operation for move: " + targetOp);
		System.out.println("Total violation degree: " + beforeViolations.get(targetOp));

		// 3. 打印操作相关的时间和机器信息
		int currentMachine = schedule.getAssignedMachine().get(targetOp);
		int currentStart = schedule.getStartTimes().get(targetOp);
		System.out.printf("Current position: Machine_%d, Start_time=%d\n",
				currentMachine, currentStart);

		// 4. 执行compound move
		boolean moveSuccess = CompoundMove.atomicCompoundMove(schedule, targetOp);

		// 5. 检查移动后的状态
		System.out.println("\nAfter compound move:");
		System.out.println("Move success: " + moveSuccess);
		if (moveSuccess) {
//			System.out.println(schedule.toString());
			Map<Integer, Double> afterViolations = calculateTCMBViolations(schedule);

			// 打印优化效果
			if (beforeViolations.containsKey(targetOp)) {
				double beforeViolation = beforeViolations.get(targetOp);
				double afterViolation = afterViolations.getOrDefault(targetOp, 0.0);
				System.out.printf("Violation improvement for Op%d: %.2f -> %.2f\n",
						targetOp, beforeViolation, afterViolation);
			}
		}
	}

	/**
	 * 打印所有违反的TCMB约束
	 */
	private static void printTCMBViolations(Schedule schedule) {
		System.out.println("TCMB Violations:");
		for (TCMB tcmb : pb.getTCMBList()) {
			int op1 = tcmb.getOp1();
			int op2 = tcmb.getOp2();
			int actualInterval = schedule.getStartTimes().get(op2) -
					schedule.getOperationEndTime(op1);
			int limit = tcmb.getTimeConstraint();

			if (actualInterval > limit) {
				System.out.printf("Op%d -> Op%d: Required <= %d, Actual = %d\n",
						op1, op2, limit, actualInterval);
			}
		}
	}

	/**
	 * 选择一个违反TCMB约束的操作
	 */
	/**
	 * 随机选择一个违反TCMB约束的操作
	 */
	private static int selectOperationForMove(Schedule schedule) {
		List<Integer> violatingOps = new ArrayList<>();
		List<TCMB> tcmbList = pb.getTCMBList();

		// 收集所有违反TCMB约束的操作
		for (TCMB tcmb : tcmbList) {
			int op1 = tcmb.getOp1();
			int op2 = tcmb.getOp2();

			int actualInterval = schedule.getStartTimes().get(op2) -
					schedule.getOperationEndTime(op1);
			int limit = tcmb.getTimeConstraint();

			if (actualInterval > limit) {
				// 对于每个违反的约束，随机选择添加op1或op2
				if (Math.random() < 0.5) {
					violatingOps.add(op1);
				} else {
					violatingOps.add(op2);
				}
			}
		}

		if (violatingOps.isEmpty()) {
			System.out.println("No TCMB violations found!");
			return -1;
		}

		// 随机选择一个操作
		int randomIndex = (int)(Math.random() * violatingOps.size());
		int selectedOp = violatingOps.get(randomIndex);

		// 打印违反信息
		System.out.println("Randomly selected Op" + selectedOp + " from " +
				violatingOps.size() + " violating operations");

		return selectedOp;
	}
	/**
	 * 打印所有违反的TCMB约束并返回违反的数量
	 */
	private static Map<Integer, Double> calculateTCMBViolations(Schedule schedule) {
		Map<Integer, Double> opViolations = new HashMap<>();
		List<TCMB> tcmbList = pb.getTCMBList();

		int count = 0;
		System.out.println("TCMB Violations:");
		for (TCMB tcmb : tcmbList) {
			int op1 = tcmb.getOp1();
			int op2 = tcmb.getOp2();

			int op1End = schedule.getOperationEndTime(op1);
			int op2Start = schedule.getStartTimes().get(op2);
			int actualInterval = op2Start - op1End;
			int limit = tcmb.getTimeConstraint();


			if (actualInterval > limit) {
				double violation = actualInterval - limit;
				// 累加每个操作的违反程度
				opViolations.merge(op1, violation, Double::sum);
				opViolations.merge(op2, violation, Double::sum);

				System.out.printf("  Violation: %d\n", (int)++count);
				System.out.printf("Op%d -> Op%d : interval=%d, limit=%d\n",
						op1, op2, actualInterval, limit);
			}
		}
		return opViolations;
	}
	private static void printTCMBConstraints() {
		List<TCMB> tcmbList = pb.getTCMBList();
		System.out.println("\nAll TCMB Constraints:");
		for (TCMB tcmb : tcmbList) {
			System.out.printf("Op%d -> Op%d <= %d\n",
					tcmb.getOp1(), tcmb.getOp2(), tcmb.getTimeConstraint());
		}
		System.out.println();
	}
}
