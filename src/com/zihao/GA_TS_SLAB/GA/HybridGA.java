package com.zihao.GA_TS_SLAB.GA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import java.util.Collections;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

import com.zihao.GA_TS_SLAB.GA.Parameters;
import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.GA.Utility;
import com.zihao.GA_TS_SLAB.GA.Operator;
import com.zihao.GA_TS_SLAB.GA.TabuSearch;


public class HybridGA {

    private int popNum = Parameters.POP_NUM;

    public Schedule solve() {
        // Initialize population
        Chromosome[] parents = initPopulation();

        Chromosome initBest = parents[getBestIndex(parents)];
        Chromosome currentBest = parents[getBestIndex(parents)];
        Chromosome best = new Chromosome(currentBest);
        Random r = new Random();

//        for (int i = 0; i < parents.length; i++) {
//            System.out.println(i + ": Fitness = " + parents[i].getFitness() + "Delay :" + parents[i].getDelay());
//        }

        int curGen = 0;
        int remain = 0;

        while (curGen < Parameters.MAX_GENERATION) {
            // Selection
            int popNum = Parameters.POP_NUM;
            double elitRate = Parameters.ELIT_RATIO;

            int elitNum = (int) (elitRate * popNum);


            Chromosome[] elist = Operator.ElitistSelection(parents, elitNum);
            int childrenNum = popNum - elist.length;
            Chromosome[] children = new Chromosome[childrenNum];

            // Random disturbance
            children = Operator.RouletteWheelSelection(parents, childrenNum);

//            if (curGen == 0) {
//                for (int i = 0; i < children.length; i++) {
//                    System.out.println(i + ": Fitness = " + children[i].getFitness() + "Delay :" + children[i].getDelay());
//                }
//            }

            if (curGen - remain > Parameters.REMAIN_LOOP) {
                int disturbNum = (int) (popNum * Parameters.DISTURB_RATIO);
                for (int i = 0; i < disturbNum; i++) {
                    int index = r.nextInt(disturbNum);
                    children[index] = new Chromosome(r);
                }
                remain = curGen;
            }


            // Crossover
            Operator.Crossover(children);

            // Mutation
            Operator.Mutation(children);

//            if (curGen == 0) {
//                for (int i = 0; i < children.length; i++) {
//                    System.out.println(i + ": Fitness = " + children[i].getFitness() + "Delay :" + children[i].getDelay());
//                }
//            }

            // Combine
            int index = 0;
            for (Chromosome o : elist) {
                parents[index++] = new Chromosome(o);
            }
            for (Chromosome o : children) {
                parents[index++] = new Chromosome(o);
            }

//            if (curGen == 0) {
//                for (int i = 0; i < parents.length; i++) {
//                    System.out.println(i + ": Fitness = " + parents[i].getFitness() + "Delay :" + parents[i].getDelay());
//                }
//            }


            // Sort the population by fitness to select top individuals for Tabu Search
            Arrays.sort(parents);

//            if (curGen == 0) {
//                for (int i = 0; i < parents.length; i++) {
//                    System.out.println(i + ": Fitness = " + parents[i].getFitness() + "Delay :" + parents[i].getDelay());
//                }
//            }

            int searchInsertNum = (int) (popNum * Parameters.INSERT_SEARCH_RATIO);
            int searchDelayNum = (int) (popNum * Parameters.DELAY_SEARCH_RATIO);
//            TabuSearch tabuSearch = new TabuSearch(100, 15);


            TabuSearchInsert tabuSearchInsert = new TabuSearchInsert(100);
            TabuSearchDelay tabuSearchDelay = new TabuSearchDelay(Parameters.TABU_MIN_ITERATION,
                    Parameters.TABU_MAX_ITERATION,
                    Parameters.TABU_SIZE,
                    Parameters.TABU_IMPROVEMENT);


//            for (int i = 0; i < searchInsertNum; i++) {
//                parents[i] = tabuSearchInsert.optimize(parents[i]);  // 执行插入禁忌搜索，返回优化后的个体
//            }

            // Apply Tabu Search to the top searchNum individuals
//            for (int i = 0; i < searchDelayNum; i++) {
////                Chromosome optimizedChromosome = tabuSearchDelay.optimize(parents[i], best.getFitness());
////                parents[i] = optimizedChromosome;
//                parents[i] = tabuSearchDelay.optimize(parents[i], best.getFitness());
//            }

            int tournamentSize = 3;  // 锦标赛的规模，可以根据需求调整
            for (int i = 0; i < searchDelayNum; i++) {
                Chromosome selectedChromosome = tournamentSelection(Arrays.asList(parents), tournamentSize);  // 锦标赛选择个体
                Chromosome optimizedChromosome = tabuSearchDelay.optimize(selectedChromosome, best.getFitness());  // 进行 Tabu Search 优化
                // 替换原始的个体
                for (int j = 0; j < parents.length; j++) {
                    if (parents[j].equals(selectedChromosome)) {
                        parents[j] = optimizedChromosome;  // 更新选择的个体为优化后的个体
//                        System.out.println("GO here");
                        break;

                    }
                }
            }

//            if (curGen == 30) {
//                for (int i = 0; i < parents.length; i++) {
//                    System.out.println(i + ": Fitness = " + parents[i].getFitness() + "Delay :" + parents[i].getDelay());
//                }
//            }

            currentBest = parents[getBestIndex(parents)];

            List<Chromosome> parentList = Arrays.asList(parents);
            Collections.shuffle(parentList);
            parents = parentList.toArray(new Chromosome[0]);

            System.out.println(" After " + curGen + " generation, the current best fitness is:" + currentBest.getFitness());
            if (currentBest.getFitness() < best.getFitness()) {
                remain = curGen;
                best = new Chromosome(currentBest);
            }

            curGen++;
        }

//        System.out.println("The initial best fitness is " + initBest.getFitness());
//        System.out.println("The best fitness is " + best.getFitness());
        Utility.printViolation(best.getSchedule());
        best.checkPrecedenceConstraints();
//
//        System.out.println("Final population fitness values:");
        for (int i = 0; i < 500; i++) {
//            System.out.println(i + ": Fitness = " + parents[i].getFitness() + "; OS = " + parents[i].getOS()  + "; MS = " +
//                    parents[i].getMS() + "; Delay :" + parents[i].getDelay());
            System.out.println(i + ": Fitness = " + parents[i].getFitness() + "; OS = " + parents[i].getOS()  + "; MS = " +
                    parents[i].getMS()) ;
        }

        saveFeatureVectors(parents);

//        File outputFile = new File("src/com/zihao/GA_TS_SLAB/Plot/population_data.csv");
//        exportPopulationData(outputFile, parents);
//        File dbscanDataFile = new File("src/com/zihao/GA_TS_SLAB/Plot/dbscan_data.csv");
//        try (FileWriter writer = new FileWriter(dbscanDataFile)) {
//            writer.append("Fitness,OS,MS\n");
//            for (Chromosome individual : parents) {
//                // Join OS and MS into comma-separated strings
//                String osString = String.join(",", individual.getOS().stream().map(Object::toString).collect(Collectors.toList()));
//                String msString = String.join(",", individual.getMS().stream().map(Object::toString).collect(Collectors.toList()));
//                writer.append(individual.getFitness() + "," + osString + "," + msString + "\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return best.getSchedule();
    }

    private void generateDBSCANDataFile(Chromosome[] parents) {
        File dbscanFile = new File("src/com/zihao/GA_TS_SLAB/Plot/dbscan_data.csv");
        try (FileWriter writer = new FileWriter(dbscanFile)) {
            writer.append("Fitness,OS,MS\n");
            for (Chromosome chromosome : parents) {
                writer.append(chromosome.getFitness() + "," +
                        chromosome.getOS().toString().replace("[", "").replace("]", "").replace(" ", "") + "," +
                        chromosome.getMS().toString().replace("[", "").replace("]", "").replace(" ", "") + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("DBSCAN data exported to " + dbscanFile.getAbsolutePath());
    }

    private void saveFeatureVectors(Chromosome[] chromosomes) {
        try (FileWriter writer = new FileWriter("population_features.csv")) {
            for (Chromosome chrom : chromosomes) {
                double[] features = chrom.getFeatureVector();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < features.length; i++) {
                    sb.append(features[i]);
                    if (i < features.length - 1) {
                        sb.append(",");
                    }
                }
                writer.write(sb.toString());
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportPopulationData(File outputFile, Chromosome[] parents) {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.append("Fitness,OS,MS\n");  // Header
            for (Chromosome chromosome : parents) {  // Assuming 'parents' is the final population
                // Convert OS and MS to comma-separated strings
                String osString = chromosome.getOS().stream().map(String::valueOf).collect(Collectors.joining(","));
                String msString = chromosome.getMS().stream().map(String::valueOf).collect(Collectors.joining(","));
                writer.append(chromosome.getFitness() + "," + osString + "," + msString + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Population data exported to " + outputFile.getAbsolutePath());
    }


    public Chromosome[] initPopulation() {
        Chromosome[] population = new Chromosome[popNum];
        Random random = new Random();
        for (int i = 0; i < popNum; i++) {
            population[i] = new Chromosome(random);
        }
        return population;
    }

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

    private Chromosome tournamentSelection(List<Chromosome> population, int tournamentSize) {
        List<Chromosome> tournament = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = random.nextInt(population.size());
            tournament.add(population.get(randomIndex));
        }
        // 使用 compareTo 方法进行排序，并选择适应度最好的个体（最小 fitness）
        return Collections.min(tournament);  // Collections.min 直接调用 compareTo
    }


}
