package com.zihao.GA_TS_SLAB.GA;


import java.util.*;

/**
 * 复合移动操作管理器
 * 负责创建和执行各种类型的复合移动操作
 */
class CompoundMoveManager {
	// 操作权重
	private double swapWeight = 0.4;
	private double insertWeight = 0.4;
	private double reassignWeight = 0.2;

	// 随机数生成器
	private final Random random = new Random();

	/**
	 * 执行随机选择的复合移动操作
	 * @param schedule 当前调度
	 * @return 操作是否成功
	 */
	public boolean applyRandomMove(Schedule schedule) {
		// 根据权重随机选择操作类型
		double r = random.nextDouble();

		if (r < swapWeight) {
			return applyCompoundSwap(schedule);
		} else if (r < swapWeight + insertWeight) {
			return applyCompoundInsert(schedule);
		} else {
			return applyCompoundReassign(schedule);
		}
	}

	/**
	 * 应用复合交换操作
	 * @param schedule 当前调度
	 * @return 操作是否成功
	 */
	public boolean applyCompoundSwap(Schedule schedule) {
		CompoundMove move = new CompoundSwap();
		return move.execute(schedule);
	}

	/**
	 * 应用复合插入操作
	 * @param schedule 当前调度
	 * @return 操作是否成功
	 */
	public boolean applyCompoundInsert(Schedule schedule) {
		CompoundMove move = new CompoundInsert();
		return move.execute(schedule);
	}

	/**
	 * 应用复合重分配操作
	 * @param schedule 当前调度
	 * @return 操作是否成功
	 */
	public boolean applyCompoundReassign(Schedule schedule) {
		CompoundMove move = new CompoundReassign();
		return move.execute(schedule);
	}

	/**
	 * 更新操作权重
	 * @param successfulType 成功执行的操作类型
	 */
	public void updateWeights(MoveType successfulType) {
		// TODO: 实现动态权重调整
		// 根据成功的操作类型微调权重
		switch (successfulType) {
			case SWAP:
				swapWeight = Math.min(0.6, swapWeight + 0.05);
				insertWeight = Math.max(0.2, insertWeight - 0.025);
				reassignWeight = Math.max(0.1, reassignWeight - 0.025);
				break;
			case INSERT:
				insertWeight = Math.min(0.6, insertWeight + 0.05);
				swapWeight = Math.max(0.2, swapWeight - 0.025);
				reassignWeight = Math.max(0.1, reassignWeight - 0.025);
				break;
			case REASSIGN:
				reassignWeight = Math.min(0.4, reassignWeight + 0.05);
				swapWeight = Math.max(0.2, swapWeight - 0.025);
				insertWeight = Math.max(0.2, insertWeight - 0.025);
				break;
		}

		// 确保权重总和为1
		double sum = swapWeight + insertWeight + reassignWeight;
		swapWeight /= sum;
		insertWeight /= sum;
		reassignWeight /= sum;
	}
}