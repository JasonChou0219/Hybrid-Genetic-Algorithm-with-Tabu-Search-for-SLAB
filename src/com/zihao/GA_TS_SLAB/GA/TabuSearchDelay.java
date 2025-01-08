package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;

import java.util.*;

public class TabuSearchDelay {
    private int minIterations; // 新增: 最小迭代次数
    private int maxIterations; // 新增: 最大迭代次数
    private int tabuListSize;
    private ProblemSetting problemSetting;
    private Random random;
    private int maxNoImprovement;

    public TabuSearchDelay(int minIterations, int maxIterations, int tabuListSize, int maxNoImprovement) {
        this.minIterations = minIterations;
        this.maxIterations = maxIterations;  // 将最大迭代次数作为可变参数
        this.tabuListSize = tabuListSize;
        this.problemSetting = ProblemSetting.getInstance();
        this.random = new Random();
        this.maxNoImprovement = maxNoImprovement;
    }


    public Chromosome optimize(Chromosome currentChromosome, double bestFitness) {
        Schedule currentSchedule = currentChromosome.getSchedule();
        Chromosome bestChromosome = new Chromosome(currentChromosome);
        double currentFitness = currentChromosome.getFitness();

        List<Integer> tabuList = new ArrayList<>();
        int noImprovement = 0;

        // 动态设置最大迭代次数，基于 fitness 与 bestFitness 的比例
        int dynamicIterations = calculateIterations(currentFitness, bestFitness);
//        int dynamicIterations = Parameters.TABU_MAX_ITERATION;
        for (int iteration = 0; iteration < dynamicIterations; iteration++) {
            List<Chromosome> neighbors = generateNeighbors(currentChromosome, iteration, tabuList, noImprovement);

            Chromosome bestNeighbor = null;
            double bestNeighborFitness = Double.POSITIVE_INFINITY;

            for (Chromosome neighbor : neighbors) {
                double neighborFitness = neighbor.getFitness();
                if (neighborFitness < bestNeighborFitness) {
                    bestNeighbor = neighbor;
                    bestNeighborFitness = neighborFitness;
                }
            }

            if (bestNeighbor == null) {
                break;
            }

            // Check if the best neighbor is better than the current best solution
            if (bestNeighborFitness < currentFitness) {
                noImprovement = 0;
                bestChromosome = new Chromosome(bestNeighbor);
                currentFitness = bestNeighborFitness;
            } else {
                noImprovement++;
            }

            // Update the tabu list
            Integer tabuKey = generateTabuKey(bestNeighbor);
            tabuList.add(tabuKey);
            if (tabuList.size() > tabuListSize) {
                tabuList.remove(0);
            }

            currentChromosome = bestNeighbor;
        }
        if (Utility.checkViolation(bestChromosome)) {
//            propagateDelay(bestChromosome, tabuList);
        }

        return bestChromosome;
    }
//    public Chromosome optimize(Chromosome currentChromosome, double bestFitness) {
//        Schedule currentSchedule = currentChromosome.getSchedule();
//        Chromosome bestChromosome = new Chromosome(currentChromosome);
//        double currentFitness = currentChromosome.getFitness();
//
//        List<Integer> tabuList = new ArrayList<>();
//        int noImprovementCount = 0; // 记录连续未找到改进解的次数
//
//        // 设置固定的最大迭代次数
//        int dynamicIterations = Parameters.TABU_MAX_ITERATION;
//
//        for (int iteration = 0; iteration < dynamicIterations; iteration++) {
//            // 仅改变一个 op 的 delay
//            Chromosome neighbor = generateSingleNeighbor(currentChromosome, iteration, tabuList, noImprovementCount);
//
//            if (neighbor == null) {
//                noImprovementCount++; // 增加无改进计数
//                continue; // 跳过本次迭代，继续下一次
//            }
//
//            double neighborFitness = neighbor.getFitness();
//
//            // Check if the neighbor is better than the current best solution
//            if (neighborFitness < currentFitness) {
//                noImprovementCount = 0; // 重置无改进计数
//                bestChromosome = new Chromosome(neighbor);
//                currentFitness = neighborFitness;
//            } else {
//                noImprovementCount++;
//            }
//
//            // Update the tabu list
//            Integer tabuKey = generateTabuKey(neighbor);
//            tabuList.add(tabuKey);
//            if (tabuList.size() > tabuListSize) {
//                tabuList.remove(0);
//            }
//
//            currentChromosome = neighbor;
//        }
//
//        return bestChromosome;
//    }

//    private Chromosome generateSingleNeighbor(Chromosome chromosome, int iteration, List<Integer> tabuList, int noImprovement) {
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//
//        // 随机选择一个 TCMB
//        TCMB selectedTcmb = tcmbList.get(random.nextInt(tcmbList.size()));
//        int opA = selectedTcmb.getOp1();
//        int opB = selectedTcmb.getOp2();
//
//        Schedule schedule = chromosome.getSchedule();
//        int endA = schedule.getStartTimes().get(opA) + problemSetting.getProcessingTime()[opA - 1];
//        int startB = schedule.getStartTimes().get(opB);
//
//        int timeLag = startB - endA;
//
//        // 检查是否满足条件
//        if (timeLag > selectedTcmb.getTimeConstraint()) {
//            // 获取当前的延迟
//            int currentDelay = chromosome.getDelay().getOrDefault(opA, 0);
//
//            // 增大标准差，使搜索幅度更大
//            double delayStdDev = timeLag - selectedTcmb.getTimeConstraint();
//            int delayIncrement = (int) Math.round(random.nextGaussian() * delayStdDev);
//
//            // 计算新的延迟值，并确保不为负数
//            int newDelay = Math.max(currentDelay + delayIncrement, 0);
//
//            Chromosome newNeighbor = new Chromosome(chromosome);
//            newNeighbor.getDelay().put(opA, newDelay);
//            newNeighbor.updateScheduleAndFitness();
//
//            int tabuKey = generateTabuKey(newNeighbor);
//            if (!tabuList.contains(tabuKey)) {
//                return newNeighbor;
//            }
//        }
////        else if (timeLag <= selectedTcmb.getTimeConstraint()) {
////            if (random.nextDouble() < Parameters.DELAY_WITHDRAW_PROBABILITY){
////
////            }
////        }
//
//        // 如果无法找到合适的邻居，返回 null
//        return null;
//    }

//    private List<Chromosome> generateNeighbors(Chromosome chromosome, int iteration, List<Integer> tabuList, int noImprovement) {
//        List<Chromosome> neighbors = new ArrayList<>();
//        for (TCMB tcmb : problemSetting.getTCMBList()) {
//            int opA = tcmb.getOp1();
//            int opB = tcmb.getOp2();
//
//            Schedule schedule = chromosome.getSchedule();
//            int endA = schedule.getStartTimes().get(opA) + problemSetting.getProcessingTime()[opA - 1];
//            int startB = schedule.getStartTimes().get(opB);
//
//            int timeLag = startB - endA;
//
//            if (timeLag > tcmb.getTimeConstraint()) {
//                // 获取当前的延迟
//                int currentDelay = chromosome.getDelay().getOrDefault(opA, 0);
//
//                // 增大标准差，使搜索幅度更大
//                double delayStdDev = timeLag - tcmb.getTimeConstraint();
////                int delayIncrement = Math.min((int) Math.round(random.nextGaussian() * delayStdDev),
////                        timeLag - tcmb.getTimeConstraint());
//
////                int delayIncrement = adaptiveDelayIncrement(delayStdDev, iteration, maxIterations, noImprovement, maxNoImprovement);
//                int delayIncrement = (int) Math.round( random.nextGaussian() * delayStdDev);
//
//                int newDelay = Math.max(currentDelay + delayIncrement, 0);
//
//                Chromosome newNeighbor = new Chromosome(chromosome);
//                newNeighbor.getDelay().put(opA, newDelay);
//                newNeighbor.updateScheduleAndFitness();
//                int tabuKey = generateTabuKey(newNeighbor);
//                if (!tabuList.contains(tabuKey)) {
//                    neighbors.add(newNeighbor);
//                }
//            }
//        }
//        return neighbors;
//    }

    private List<Chromosome> generateNeighbors(Chromosome chromosome, int iteration, List<Integer> tabuList, int noImprovement) {
        List<Chromosome> neighbors = new ArrayList<>();
        int[][] distanceMatrix = problemSetting.getDistanceMatrix();
        Set<Integer> nonTcmbOps = problemSetting.getNonTcmbOps();
        Set<Integer> tcmbOps = problemSetting.getTcmbOps();

        Random r = new Random();
        // 存储违反了 constraint 的 opA
        Set<Integer> violatingOpsA = new HashSet<>();
//        Chromosome currentChromosome = new Chromosome(chromosome);

        // 首先处理 TCMB 相关的操作
        // 每次都单独操作
        Schedule schedule = chromosome.getSchedule();
        for (TCMB tcmb : problemSetting.getTCMBList()) {
            int opA = tcmb.getOp1();
            int opB = tcmb.getOp2();


            int endA = schedule.getStartTimes().get(opA) + problemSetting.getProcessingTime()[opA - 1];
            int startB = schedule.getStartTimes().get(opB);

            int timeLag = startB - endA;

            if (timeLag > tcmb.getTimeConstraint()) {
                // 标记违反了 constraint 的 opA
                violatingOpsA.add(opA);

                // 获取当前的延迟
                int currentDelay = chromosome.getDelay().getOrDefault(opA, 0);

                // 增大标准差，使搜索幅度更大
                double delayStdDev = timeLag - tcmb.getTimeConstraint();
                int delayIncrement = (int) Math.round(random.nextGaussian() * delayStdDev);

                int newDelay = Math.max(currentDelay + delayIncrement, 0);

                Chromosome newNeighbor = new Chromosome(chromosome);
                newNeighbor.getDelay().put(opA, newDelay);
                newNeighbor.updateScheduleAndFitness();
                int tabuKey = generateTabuKey(newNeighbor);
                if (!tabuList.contains(tabuKey)) {
                    neighbors.add(newNeighbor);
                }
            }
        }

        // 接着处理非 TCMB 操作
//        for (int nonTcmbOp : nonTcmbOps) {
//            // 寻找与当前 nonTcmbOp 最近的违反 constraint 的 opA
//            int minDistance = Integer.MAX_VALUE;
//            for (int violatingOpA : violatingOpsA) {
//                int distanceToOpA = distanceMatrix[nonTcmbOp][violatingOpA];
//
//                // 判断是否是前置节点
//                if (distanceToOpA > 0 && distanceToOpA < minDistance) {
//                    minDistance = distanceToOpA;
//                }
//            }
//
//            // 如果 minDistance 是 MAX_VALUE，说明 nonTcmbOp 与任何违反 constraint 的 opA 无依赖关系
//            if (minDistance == Integer.MAX_VALUE) {
//                continue;
//            }
//
//            // 根据最小距离计算调整概率（距离越小，概率越大）
//            double adjustmentProbability = 1.0 / Math.exp(minDistance);
//
//            // 使用该概率决定是否调整非 TCMB 操作
//            if (r.nextDouble() < adjustmentProbability) {
//                // 如果非 TCMB 操作已经有延迟，对其进行微调
//                int nonTcmbCurrentDelay = chromosome.getDelay().getOrDefault(nonTcmbOp, 0);
//                int randomAdjustment = r.nextInt(3) - 1; // -1, 0, or 1
//                int newNonTcmbDelay = Math.max(0, nonTcmbCurrentDelay + randomAdjustment); // 确保延迟不为负数
//
//                // 如果当前非 TCMB 操作没有延迟，则随机引入新的延迟
//                if (!chromosome.getDelay().containsKey(nonTcmbOp)) {
//                    newNonTcmbDelay = r.nextInt(2); // 生成 0 到 3 的随机整数
//                }
//
//                Chromosome nonTcmbNeighbor = new Chromosome(chromosome);
//                nonTcmbNeighbor.getDelay().put(nonTcmbOp, newNonTcmbDelay);
//                nonTcmbNeighbor.updateScheduleAndFitness();
//                int tabuKey = generateTabuKey(nonTcmbNeighbor);
//                if (!tabuList.contains(tabuKey)) {
//                    neighbors.add(nonTcmbNeighbor);
//                }
//            }
//        }

        return neighbors;
    }



//    private int adaptiveDelayIncrement(double delayStdDev, int iteration, int maxIterations, int noImprove, int maxNoImprovement) {
//        double alpha = 10.0; // 控制衰减速率
//        double progress = (double) iteration / maxIterations;  // 当前迭代进度
//
//        // 使用 Sigmoid 函数控制步长衰减
//        double T = 1.0 / (1.0 + Math.exp(alpha * (progress - 0.5)));
//
//        // 结合 noImprove 的影响，如果没有改进次数较多，恢复较大步长
//        if (noImprove >= maxNoImprovement) {
//            T = 1.0;  // 恢复大步长
//        }
//
//        // 调整步长
//        int delayIncrement = (int) Math.round(random.nextGaussian() * delayStdDev * T);
//        return delayIncrement;
//    }

    private int calculateIterations(double currentFitness, double bestFitness) {
        double fitnessRatio = currentFitness / bestFitness;

        // 确保 fitnessRatio 不会小于1，避免比最优解更优的情况
        if (fitnessRatio < 1) {
            fitnessRatio = 1;
        }

        // 通过倒数关系来控制迭代次数
        double scalingFactor = 1 / fitnessRatio;  // 比例越接近1，scalingFactor越大
        return (int) (minIterations + (maxIterations - minIterations) * scalingFactor);
    }


//
//    private void propagateDelayCorrections(Chromosome chromosome, List<Integer> tabuList) {
//        Set<Integer> violatingOpsA = new HashSet<>();
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//        Schedule schedule = chromosome.getSchedule();
//        Map<Integer, Integer> startTimes = schedule.getStartTimes();
//        int[] processingTimes = problemSetting.getProcessingTime();
//
//        // 找出所有违反约束的操作对
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1();
//            int opB = tcmb.getOp2();
//            int endA = startTimes.get(opA) + processingTimes[opA - 1];
//            int startB = startTimes.get(opB);
//
//            int timeLag = startB - endA;
//
//            if (timeLag > tcmb.getTimeConstraint()) {
//                violatingOpsA.add(opA);
//            }
//        }
//
//        // 逐步对违反约束的操作进行延迟传播调整
//        for (int opA : violatingOpsA) {
//            propagateDelay(chromosome, opA, tabuList);
//        }
//    }

//    private void propagateDelay(Chromosome chromosome, int opA, List<Integer> tabuList) {
//        Schedule schedule = chromosome.getSchedule();
//        Map<Integer, Integer> startTimes = schedule.getStartTimes();
//        int[] processingTimes = problemSetting.getProcessingTime();
//
//        for (int opB : startTimes.keySet()) {
//            // 如果 opB 是 opA 的后续操作
//            if (problemSetting.getDistanceMatrix()[opA][opB] > 0) {
//                int endA = startTimes.get(opA) + processingTimes[opA - 1];
//                int startB = startTimes.get(opB);
//
//                int timeLag = startB - endA;
//
//                if (timeLag > problemSetting.getTimeConstraint(opA, opB)) {
//                    // 获取当前的延迟
//                    int currentDelay = chromosome.getDelay().getOrDefault(opB, 0);
//
//                    // 增大标准差，使搜索幅度更大
//                    double delayStdDev = timeLag - problemSetting.getTimeConstraint(opA, opB);
//                    int delayIncrement = (int) Math.round(random.nextGaussian() * delayStdDev);
//
//                    int newDelay = Math.max(currentDelay + delayIncrement, 0);
//
//                    chromosome.getDelay().put(opB, newDelay);
//                    chromosome.updateScheduleAndFitness();
//
//                    int tabuKey = generateTabuKey(chromosome);
//                    if (!tabuList.contains(tabuKey)) {
//                        tabuList.add(tabuKey);
//                    }
//
//                    // 递归传播延迟到下一个相关操作
//                    propagateDelay(chromosome, opB, tabuList);
//                }
//            }
//        }
//    }

    private int adaptiveDelayIncrement(double delayStdDev, int iteration, int maxIterations, int noImprove, int maxNoImprovement) {
        double alpha = 10.0; // 控制步长衰减速率
        double progress = (double) iteration / maxIterations;  // 当前迭代进度

        // Sigmoid 控制步长衰减，保证随着进展，步长逐渐减小
        double T = 1.0 / (1.0 + Math.exp(alpha * (progress - 0.5)));

        // 将 noImprove 的影响加入步长衰减：无改进次数越多，步长逐渐减小
        if (noImprove >= maxNoImprovement) {
            double penalty = 1.0 / (1.0 + (double) noImprove / maxNoImprovement);
            T *= penalty;  // 随着无改进次数增大，步长额外减小
        }

        // 调整步长，乘以 delayStdDev 控制搜索的范围
        int delayIncrement = (int) Math.round(random.nextGaussian() * delayStdDev * T);

        // 确保步长不为0，避免没有变化
        return Math.max(delayIncrement, 1);
    }

    private int generateTabuKey(Chromosome chromosome) {
        Schedule schedule = chromosome.getSchedule();
        int hash = 7;
        for (int op : schedule.getStartTimes().keySet()) {
            hash = 31 * hash + schedule.getStartTimes().get(op); // Hash for start time
            hash = 31 * hash + schedule.getAssignedMachine().get(op); // Hash for machine assignment
        }
        return hash;
    }
//    private String generateTabuKey(Chromosome chromosome) {
//        // Generate a key based on the chromosome's schedule
//        return chromosome.getSchedule().getStartTimes().toString();
//    }
}
