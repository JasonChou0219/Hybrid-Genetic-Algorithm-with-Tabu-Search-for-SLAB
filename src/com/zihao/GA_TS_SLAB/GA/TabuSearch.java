package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;

import java.util.*;

public class TabuSearch {
    private int maxIterations;
    private int tabuListSize;
    private ProblemSetting problemSetting;
    private Random random;

    public TabuSearch(int maxIterations, int tabuListSize) {
        this.maxIterations = maxIterations;
        this.tabuListSize = tabuListSize;
        this.problemSetting = ProblemSetting.getInstance();
        this.random = new Random();
    }

    public Chromosome optimize(Chromosome initialChromosome) {
        Schedule currentSchedule = initialChromosome.getSchedule();
        Chromosome bestChromosome = new Chromosome(initialChromosome);
        double bestFitness = initialChromosome.getFitness();

        List<String> tabuList = new ArrayList<>();

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<Chromosome> neighbors = generateNeighbors(initialChromosome, tabuList);

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
            if (bestNeighborFitness < bestFitness) {
                bestChromosome = new Chromosome(bestNeighbor);
                bestFitness = bestNeighborFitness;
            }

            // Update the tabu list
            String tabuKey = generateTabuKey(bestNeighbor);
            tabuList.add(tabuKey);
            if (tabuList.size() > tabuListSize) {
                tabuList.remove(0);  //
            }

            initialChromosome = bestNeighbor;

//            System.out.println("Iteration " + iteration + ": Best Fitness = " + bestFitness);
        }

        return bestChromosome;
    }


    private List<Chromosome> generateNeighbors(Chromosome chromosome, List<String> tabuList) {
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
                double delayStdDev = Math.max(timeLag - tcmb.getTimeConstraint(), 1);
//                int delayIncrement = Math.min((int) Math.round(random.nextGaussian() * delayStdDev),
//                        timeLag - tcmb.getTimeConstraint());
                int delayIncrement = (int) Math.round(random.nextGaussian() * delayStdDev);
                // 计算新的延迟值，并确保不为负数
                int newDelay = Math.max(currentDelay + delayIncrement, 0);

                Chromosome newNeighbor = new Chromosome(chromosome);
                newNeighbor.getDelay().put(opA, newDelay);
                newNeighbor.updateScheduleAndFitness();
                String tabuKey = generateTabuKey(newNeighbor);
                if (!tabuList.contains(tabuKey)) {
                    neighbors.add(newNeighbor);
                }
            }
        }


        return neighbors;
    }


    private String generateTabuKey(Chromosome chromosome) {
        // Generate a key based on the chromosome's schedule
        return chromosome.getSchedule().getStartTimes().toString();
    }
}
