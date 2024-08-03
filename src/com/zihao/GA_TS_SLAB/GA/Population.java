package com.zihao.GA_TS_SLAB.GA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

public class Population {
    private Parameters parameters = new Parameters();
    private int popNum = parameters.popNum;
    private List<Chromosome> chromosomes;

    public Population() {
        chromosomes = new ArrayList<>();
    }

    public void init() {
        Random random = new Random();
        for (int i = 0; i < popNum; i++) {
            Chromosome chromosome = new Chromosome(random);
            chromosomes.add(chromosome);
        }
    }

    public List<Chromosome> getChromosomes() {
        return chromosomes;
    }

    public static void main(String[] args) {
        File parentDir = new File("src/Dataset/Gu2016/N1");
        Input input = new Input(parentDir);
        input.getProblemDesFromFile();

        Population population = new Population();
        population.init();
        List<Chromosome> chromosomes = population.getChromosomes();
        for (Chromosome chromosome : chromosomes) {
            System.out.println(chromosome);
        }
    }
}
