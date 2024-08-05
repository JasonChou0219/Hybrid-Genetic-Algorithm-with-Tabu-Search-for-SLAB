package com.zihao.GA_TS_SLAB.GA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.zihao.GA_TS_SLAB.GA.Parameters;
import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.GA.Utility;
import com.zihao.GA_TS_SLAB.GA.Operator;

public class HybridGA {

    private int popNum = Parameters.POP_NUM;

    public Schedule solve(){
        // Initialize population
        Chromosome[] parents = initPopulation();

        Chromosome currentBest = getBest(parents);
        Chromosome best = new Chromosome(currentBest);


        int curGen = 0;

        while (curGen < Parameters.MAX_GENERATION) {
            // disterpution


            // Selection
            Chromosome[] elist = Operator.ElitistSelection(parents);
            Chromosome[] children = Operator.TournamentSelection(parents);
            // Crossover
            Operator.Crossover(children);
            // Mutation
            Operator.Mutation(children);
            // Combine
            int index = 0;
            for (Chromosome o: elist){
                parents[index++] = o;
            }
            for (Chromosome o : children) {
                parents[index++] = o;
            }
            currentBest = getBest(parents);
            if (currentBest.getFitness() < best.getFitness()) {
                best = new Chromosome(currentBest);
                System.out.println("In " + curGen + " generation, get new best fitness :" + best.getFitness());
            }
            System.out.println(" After " + curGen + " generation, the best fitness is:" + best.getFitness());
            curGen++;
        }

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

    public  Chromosome getBest(Chromosome[] population){
        int bestIndex = -1;
        double minFitness = Double.POSITIVE_INFINITY;
        for (int i = 0; i < population.length; i++) {
            if (minFitness > population[i].getFitness()) {
                minFitness = population[i].getFitness();
                bestIndex = i;
            }
        }
        return  new Chromosome(population[bestIndex]);
    }
}
