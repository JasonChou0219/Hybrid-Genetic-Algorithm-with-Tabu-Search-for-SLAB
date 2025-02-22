package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Utility;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimulatedAnnealing {
    private static final double INITIAL_TEMPERATURE = 100.0;
    private static final double COOLING_RATE = 0.95;
    private static final double FINAL_TEMPERATURE = 0.1;
    private static final int MAX_ITERATIONS = 100;

    // 记录最优解
    private Schedule bestSchedule;
    private double bestEnergy;

    /**
     * SA算法主体
     * @param initialSchedule GA生成的初始调度方案
     * @return 优化后的调度方案
     */
    public Schedule optimize(Schedule initialSchedule) {
        double currentTemperature = INITIAL_TEMPERATURE;
        Schedule currentSchedule = new Schedule(initialSchedule);
        bestSchedule = new Schedule(initialSchedule);

        // 计算初始解的能量
        double currentEnergy = calculateEnergy(currentSchedule);
        bestEnergy = currentEnergy;

        // 温度循环
        while (currentTemperature > FINAL_TEMPERATURE) {
            // 在当前温度下的迭代
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                // 生成新解
                Schedule newSchedule = generateNeighbor(currentSchedule);

                // 在评估能量之前压缩makespan
                newSchedule = compressMakespan(newSchedule);

                // 计算新解的能量
                double newEnergy = calculateEnergy(newSchedule);

                // 计算能量差
                double deltaEnergy = newEnergy - currentEnergy;

                // 判断是否接受新解
                if (acceptNewSolution(deltaEnergy, currentTemperature)) {
                    currentSchedule = new Schedule(newSchedule);
                    currentEnergy = newEnergy;

                    // 更新最优解
                    if (currentEnergy < bestEnergy) {
                        bestSchedule = new Schedule(currentSchedule);
                        bestEnergy = currentEnergy;
                    }
                }
            }

            // 降温
            currentTemperature *= COOLING_RATE;
        }

        return bestSchedule;
    }

    /**
     * 计算调度方案的能量值（目标函数值）
     * 这里主要关注TCMB约束违反程度
     */
    private double calculateEnergy(Schedule schedule) {
        // TODO: 实现能量计算
        // 1. 计算TCMB约束违反的总时间
        // 2. 添加适当的惩罚权重
        return 0.0;
    }

    /**
     * 生成邻域解
     * 基于复合移动操作生成新的调度方案
     */
    private Schedule generateNeighbor(Schedule currentSchedule) {
        Schedule newSchedule = new Schedule(currentSchedule);
        // TODO: 实现邻域生成
        // 1. 随机选择一个操作
        // 2. 应用复合移动操作
        // 3. 如果移动失败，可以选择其他操作尝试
        return newSchedule;
    }

    /**
     * 压缩调度的makespan
     * 在SA生成新解之前调用，尝试在不违反约束的情况下减小总完工时间
     * @param currentSchedule 当前调度方案
     * @return 压缩后的调度方案
     */
    private Schedule compressMakespan(Schedule currentSchedule) {
        Schedule compressedSchedule = new Schedule(currentSchedule);
        // TODO: 实现makespan压缩
        // 1. 按开始时间对操作排序
        // 2. 尝试向前移动每个操作
        // 3. 确保移动不违反任何约束
        // 4. 迭代直到无法继续改进
        return compressedSchedule;
    }


    /**
     * 根据Metropolis准则判断是否接受新解
     */
    private boolean acceptNewSolution(double deltaEnergy, double temperature) {
        if (deltaEnergy <= 0) {
            return true;  // 更好的解直接接受
        }

        // Metropolis准则
        double acceptanceProbability = Math.exp(-deltaEnergy / temperature);
        return Math.random() < acceptanceProbability;
    }
}