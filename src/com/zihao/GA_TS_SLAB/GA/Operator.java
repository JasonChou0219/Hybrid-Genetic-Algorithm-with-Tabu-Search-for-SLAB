package com.zihao.GA_TS_SLAB.GA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Description: including selection, crossover and mutation for OS and MS chromosome
 */

public class Operator {

    private Parameters parameters;
    private Calculation calculation;
    private Random r;

    public Operator() {
        parameters = new Parameters();
        calculation = Calculation.getInstance();
        r = new Random();
    }

    /**
     * Description: elitist selection and tournament selection
     */
    public Chromosome[] Selection(Chromosome[] parents) {
        int popNum = parameters.popNum;
        double elitRate = parameters.elitRate;

        Chromosome[] children = new Chromosome[popNum];

        //elitst selection
        int elitNum = (int) (elitRate * popNum);
        ArrayList<Chromosome> p = new ArrayList<>();
        Collections.addAll(p, parents);
        Collections.sort(p);
        for (int i = 0; i < elitNum; i++) {
            children[i] = p.get(i);
        }

        // tournament selection
        for (int i = elitNum; i < popNum; i++) {
            int n1 = r.nextInt(popNum);
            int n2 = r.nextInt(popNum);
            if (parents[n1].fitness < parents[n2].fitness)
                children[i] = parents[n2];
            else
                children[i] = parents[n1];
        }
        return children;
    }

}
