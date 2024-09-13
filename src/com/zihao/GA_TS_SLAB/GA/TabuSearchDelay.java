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

//    public Chromosome optimize(Chromosome initialChromosome) {
//        Schedule currentSchedule = initialChromosome.getSchedule();
//        Chromosome bestChromosome = new Chromosome(initialChromosome);
//        double bestFitness = initialChromosome.getFitness();
//
//        List<Integer> tabuList = new ArrayList<>();
//        int noImprovement = 0;
//        for (int iteration = 0; iteration < maxIterations; iteration++) {
//
//            List<Chromosome> neighbors = generateNeighbors(initialChromosome, iteration, tabuList, noImprovement);
//
//            Chromosome bestNeighbor = null;
//            double bestNeighborFitness = Double.POSITIVE_INFINITY;
//
//            for (Chromosome neighbor : neighbors) {
//                double neighborFitness = neighbor.getFitness();
//                if (neighborFitness < bestNeighborFitness) {
//                    bestNeighbor = neighbor;
//                    bestNeighborFitness = neighborFitness;
//                }
//            }
//
//            if (bestNeighbor == null) {
//                break;
//            }
//
//            // Check if the best neighbor is better than the current best solution
//            if (bestNeighborFitness < bestFitness) {
//                noImprovement = 0;
//                bestChromosome = new Chromosome(bestNeighbor);
//                bestFitness = bestNeighborFitness;
//            }
//            else {
//                noImprovement++;
//            }
//            // Update the tabu list
//            Integer tabuKey = generateTabuKey(bestNeighbor);
//            tabuList.add(tabuKey);
//            if (tabuList.size() > tabuListSize) {
//                tabuList.remove(0);  //
//            }
//
//            initialChromosome = bestNeighbor;
//
////            System.out.println("Iteration " + iteration + ": Best Fitness = " + bestFitness);
//        }
//
//        return bestChromosome;
//    }


    public Chromosome optimize(Chromosome currentChromosome, double bestFitness) {
        Schedule currentSchedule = currentChromosome.getSchedule();
        Chromosome bestChromosome = new Chromosome(currentChromosome);
        double currentFitness = currentChromosome.getFitness();

        List<Integer> tabuList = new ArrayList<>();
        int noImprovement = 0;

        // 动态设置最大迭代次数，基于 fitness 与 bestFitness 的比例
        int dynamicIterations = calculateIterations(currentFitness, bestFitness);

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

        return bestChromosome;
    }

    private List<Chromosome> generateNeighbors(Chromosome chromosome, int iteration, List<Integer> tabuList, int noImprovement) {
        List<Chromosome> neighbors = new ArrayList<>();
        for (TCMB tcmb : problemSetting.getTCMBList()) {
            int opA = tcmb.getOp1();
            int opB = tcmb.getOp2();

            int endA = chromosome.getSchedule().getStartTimes().get(opA) + problemSetting.getProcessingTime()[opA - 1];
            int startB = chromosome.getSchedule().getStartTimes().get(opB);

            int timeLag = startB - endA;

            if (timeLag > tcmb.getTimeConstraint()) {
                // 获取当前的延迟
                int currentDelay = chromosome.getDelay().getOrDefault(opA, 0);

                // 增大标准差，使搜索幅度更大
                double delayStdDev = Math.max(timeLag - tcmb.getTimeConstraint(), 1) ;
//                int delayIncrement = Math.min((int) Math.round(random.nextGaussian() * delayStdDev),
//                        timeLag - tcmb.getTimeConstraint());

//                int delayIncrement = adaptiveDelayIncrement(delayStdDev, iteration, maxIterations, noImprovement, maxNoImprovement);
                int delayIncrement = (int) Math.round( random.nextGaussian() * delayStdDev);

//                int delayIncrement = 0;
//                if (iteration < maxIterations * 0.7) {
//                    // 前70%代数，进行较大的全局搜索
//                    delayIncrement = (int) Math.round(random.nextGaussian() * delayStdDev);
//                } else {
//                    // 后30%代数，缩小步长以进行局部搜索
//                    delayIncrement = adaptiveDelayIncrement(delayStdDev, iteration, maxIterations, noImprovement, maxNoImprovement);
//                }
                // 计算新的延迟值，并确保不为负数
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
