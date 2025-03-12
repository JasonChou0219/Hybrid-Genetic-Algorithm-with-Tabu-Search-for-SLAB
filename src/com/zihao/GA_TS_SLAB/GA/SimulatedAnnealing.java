package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.util.Random;

public class SimulatedAnnealing {
    private static final double INITIAL_TEMPERATURE = 100.0;
    private static final double COOLING_RATE = 0.95;
    private static final double FINAL_TEMPERATURE = 0.1;
    private static final int MAX_ITERATIONS = 100;
    private static final int MAX_CONSECUTIVE_FAILURES = 10;
    private static final double REHEATING_FACTOR = 1.5;

    // 记录最优解
    private Schedule bestSchedule;
    private double bestEnergy;


    // 复合操作权重
    private double swapWeight = 0.4;
    private double insertWeight = 0.4;
    private double reassignWeight = 0.2;

    // 能量函数权重
    private double makespanWeight = 0.4;
    private double tcmbViolationWeight = 0.5;
    private double balanceWeight = 0.1;


    // 问题设置引用
    private final ProblemSetting ps = ProblemSetting.getInstance();

    // 复合操作管理器
    private CompoundMoveManager moveManager;

    // 随机数生成器
    private final Random random = new Random();


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

        int iteration = 0;
        int failureCounter = 0;

        // 温度循环
        while (currentTemperature > FINAL_TEMPERATURE) {
            // 在当前温度下的迭代
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                // 生成新解
                Schedule newSchedule =  generateNeighbor(currentSchedule);


                if (newSchedule != null) {

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
                    } else {
                        failureCounter++;
                    }
                } else {
                    failureCounter++;
                }
                if (failureCounter >= MAX_CONSECUTIVE_FAILURES) {
                    currentTemperature *= REHEATING_FACTOR; // 升温
                    failureCounter = 0;
                    System.out.println("重加热: 温度升至 " + currentTemperature);
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
    /**
     * 计算调度方案的能量值（目标函数值）
     */
    private double calculateEnergy(Schedule schedule) {
        // 1. 计算makespan
        int makespan = Utility.calculateMakespan(schedule);

        // 2. 计算TCMB约束违反程度
        // todo TCMB vioaltion calculation for Schedule
        // todo 考虑程度degree 和 范围的数量
//        double tcmbViolation = calculateTCMBViolation(schedule);

//        // 3. 计算负载平衡度
//        double balance = calculateLoadBalance(schedule);

        // 加权求和
//        return makespanWeight * makespan +
//                tcmbViolationWeight * tcmbViolation +
//                balanceWeight * balance;
        return makespanWeight * makespan;
    }

    /**
     * 生成邻域解
     * 基于复合移动操作生成新的调度方案
     */
    private Schedule generateNeighbor(Schedule currentSchedule) {
        Schedule newSchedule = new Schedule(currentSchedule);
        // 根据权重随机选择操作类型
        double r = random.nextDouble();

//        if (r < swapWeight) {
//            return moveManager.applyCompoundSwap(schedule);
//        } else if (r < swapWeight + insertWeight) {
//            return moveManager.applyCompoundInsert(schedule);
//        } else {
//            return moveManager.applyCompoundReassign(schedule);
//        }

//        if (r < swapWeight) {
//            return moveManager.applyCompoundSwap(schedule);
//        } else if (r < swapWeight + insertWeight) {
//            return moveManager.applyCompoundInsert(schedule);
//        } else {
//            return moveManager.applyCompoundReassign(schedule);
//        }
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