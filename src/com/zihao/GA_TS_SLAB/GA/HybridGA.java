package com.zihao.GA_TS_SLAB.GA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;

import com.zihao.GA_TS_SLAB.GA.Parameters;
import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.GA.Utility;
import com.zihao.GA_TS_SLAB.GA.Operator;
import com.zihao.GA_TS_SLAB.GA.TabuSearch;

//public class HybridGA {
//
//    private int popNum = Parameters.POP_NUM;
//
//    public Schedule solve(){
//        // Initialize population
//        Chromosome[] parents = initPopulation();
//
//        Chromosome initBest = parents[getBestIndex(parents)];
//        Chromosome currentBest = parents[getBestIndex(parents)];
//        Chromosome best = new Chromosome(currentBest);
//        Random r = new Random();
//
//
////
////        // 输出最终结果
////        System.out.println("Optimized Fitness: " + optimizedChromosome.getFitness());
////        System.out.println("Final Schedule: " + optimizedChromosome.getSchedule());
////
////
//        int curGen = 0;
//        int remain = 0;
//
//
//        while (curGen < Parameters.MAX_GENERATION) {
////            // Selection
//            Chromosome[] elist = Operator.ElitistSelection(parents);
//            int childrenNum = popNum - elist.length;
//            Chromosome[] children = new Chromosome[childrenNum];
//            // random disturbance
//            children = Operator.RouletteWheelSelection(parents);
//
//            if (curGen - remain > Parameters.REMAIN_LOOP) {
//                int disturbNum = (int) (popNum * Parameters.DISTURB_RATIO);
//                for (int i = 0; i < disturbNum ; i++){
//                    int index = r.nextInt(disturbNum);
//                    children[i] = new Chromosome(r);
//                }
//                remain = curGen;
//            }
////            Chromosome[] children = Operator.TournamentSelection(parents);
////            // Crossover
//            Operator.Crossover(children);
////            // Mutation
//            Operator.Mutation(children);
////            // Combine
//            int index = 0;
//            for (Chromosome o: elist){
//                parents[index++] = new Chromosome(o);
//            }
//            for (Chromosome o : children) {
//                parents[index++] = new Chromosome(o);
//            }
//            int bestIndex = getBestIndex(parents);
////            currentBest = getBest(parents);
//
//            TabuSearch tabuSearch = new TabuSearch(100, 15);
//            Chromosome optimizedChromosome = tabuSearch.optimize(parents[bestIndex]);
//            parents[bestIndex] = optimizedChromosome;
//
//            currentBest = parents[bestIndex];
//
//            System.out.println(" After " + curGen + " generation, the current  best fitness is:" + currentBest.getFitness());
//            if (currentBest.getFitness() < best.getFitness()) {
//                remain = curGen;
//                best = new Chromosome(currentBest);
////                System.out.println("In " + curGen + " generation, get new best fitness :" + best.getFitness());
//            }
////            System.out.println(" After " + curGen + " generation, the best fitness is:" + best.getFitness());
//            curGen++;
//
//
//        }
//        System.out.println("The initial best fitness is " + initBest.getFitness());
//        System.out.println("The best fitness is " + best.getFitness());
//        Utility.printViolation(best.getSchedule());
//        best.checkPrecedenceConstraints();
//
//        return best.getSchedule();
//    }
public class HybridGA {

    private int popNum = Parameters.POP_NUM;

    public Schedule solve() {
        // Initialize population
        Chromosome[] parents = initPopulation();

        Chromosome initBest = parents[getBestIndex(parents)];
        Chromosome currentBest = parents[getBestIndex(parents)];
        Chromosome best = new Chromosome(currentBest);
        Random r = new Random();

        int curGen = 0;
        int remain = 0;

        while (curGen < Parameters.MAX_GENERATION) {
            // Selection
            Chromosome[] elist = Operator.ElitistSelection(parents);
            int childrenNum = popNum - elist.length;
            Chromosome[] children = new Chromosome[childrenNum];

            // Random disturbance
            children = Operator.RouletteWheelSelection(parents);

            if (curGen - remain > Parameters.REMAIN_LOOP) {
                int disturbNum = (int) (popNum * Parameters.DISTURB_RATIO);
                for (int i = 0; i < disturbNum; i++) {
                    int index = r.nextInt(disturbNum);
                    children[i] = new Chromosome(r);
                }
                remain = curGen;
            }

            // Crossover
            Operator.Crossover(children);
            // Mutation
            Operator.Mutation(children);

            // Combine
            int index = 0;
            for (Chromosome o : elist) {
                parents[index++] = new Chromosome(o);
            }
            for (Chromosome o : children) {
                parents[index++] = new Chromosome(o);
            }

            // Sort the population by fitness to select top individuals for Tabu Search
            Arrays.sort(parents);

            int searchNum = (int) (popNum * Parameters.DELAY_SEARCH_RATIO);
            TabuSearch tabuSearch = new TabuSearch(100, 15);

            // Apply Tabu Search to the top searchNum individuals
            for (int i = 0; i < searchNum; i++) {
                Chromosome optimizedChromosome = tabuSearch.optimize(parents[i]);
                parents[i] = optimizedChromosome;
            }

            currentBest = parents[getBestIndex(parents)];

            System.out.println(" After " + curGen + " generation, the current best fitness is:" + currentBest.getFitness());
            if (currentBest.getFitness() < best.getFitness()) {
                remain = curGen;
                best = new Chromosome(currentBest);
            }

            curGen++;
        }

        System.out.println("The initial best fitness is " + initBest.getFitness());
        System.out.println("The best fitness is " + best.getFitness());
        Utility.printViolation(best.getSchedule());
        best.checkPrecedenceConstraints();

        return best.getSchedule();
    }


    public Chromosome[] initPopulation() {
        Chromosome[] population = new Chromosome[popNum];
        Random random = new Random();
        for (int i = 0; i < popNum; i++) {
            population[i] = new Chromosome(random);
        }
        return population;
    }

//    public  Chromosome getBest(Chromosome[] population){
//        int bestIndex = -1;
//        double minFitness = Double.POSITIVE_INFINITY;
//        for (int i = 0; i < population.length; i++) {
//            if (minFitness > population[i].getFitness()) {
//                minFitness = population[i].getFitness();
//                bestIndex = i;
//            }
//        }
//        return  new Chromosome(population[bestIndex]);
//    }
    public  int getBestIndex(Chromosome[] population){
        int bestIndex = -1;
        double minFitness = Double.POSITIVE_INFINITY;
        for (int i = 0; i < population.length; i++) {
            if (minFitness > population[i].getFitness()) {
                minFitness = population[i].getFitness();
                bestIndex = i;
            }
        }
        return  bestIndex;
    }
}
