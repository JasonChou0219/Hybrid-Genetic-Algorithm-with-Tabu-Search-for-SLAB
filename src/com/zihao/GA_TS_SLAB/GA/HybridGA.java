package com.zihao.GA_TS_SLAB.GA;

import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Map;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.*;


import java.util.stream.Collectors;


import com.zihao.GA_TS_SLAB.GA.Parameters;
import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.GA.Utility;
import com.zihao.GA_TS_SLAB.GA.Operator;
import com.zihao.GA_TS_SLAB.GA.TabuSearch;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
import com.zihao.GA_TS_SLAB.GA.SimulatedAnnealing;

import javax.swing.*;


public class HybridGA {

    private int popNum = Parameters.POP_NUM;

    public Schedule solve() {
        // Initialize population

        System.out.println(System.getProperty("java.library.path"));
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


//            TabuSearchInsert tabuSearchInsert = new TabuSearchInsert(100);
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
//        for (int i = 0; i < 500; i++) {
////            System.out.println(i + ": Fitness = " + parents[i].getFitness() + "; OS = " + parents[i].getOS()  + "; MS = " +
////                    parents[i].getMS() + "; Delay :" + parents[i].getDelay());
//            System.out.println(i + ": Fitness = " + parents[i].getFitness() + "; OS = " + parents[i].getOS()  + "; MS = " +
//                    parents[i].getMS()) ;
//        }
//        System.out.println("The info of best, OS: " + best.getOS() + "; MS :" + best.getMS() + "; delay : " + best.getDelay());
//        ProblemSetting problemSetting = ProblemSetting.getInstance();
//        Chromosome revised = simulatedAnnealingAdjust(best, problemSetting);
//        Chromosome revised = adjustDelayBasedOnViolations(best, problemSetting);
//        checkChromosome(revised);

//        saveFeatureVectors(parents);

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

//        conjuncitvePlot(best);


//        return best.getSchedule();
//        ACO_TcmbSolver solver = new ACO_TcmbSolver();

//        Schedule adjust = solver.solve(best);
//        Utility.printViolation(adjust);
//        return adjust;
//        Schedule opt = SimulatedAnnealing.optimize(best, 500, 0.97, 10000);

//        Utility.printViolation(opt);
        return best.getSchedule();
    }


    public void conjuncitvePlot (Chromosome chromosome){
        //通过 chromosome的OS MS序列,确定在同一机器的operation执行顺序用于绘制conjunctive graph的path

        //通过 ProblemSetting.getInstance ,通过getDAG()获取临街表,绘制precedence关系

        //通过Problem setting getTCMBList 获取 TCMB关系

        //将需要的数据保存进文件中,为利用python绘制图像作准备

        Schedule schedule = chromosome.getSchedule();
        ProblemSetting problemSetting = ProblemSetting.getInstance();
        DirectedAcyclicGraph dag = problemSetting.getDag();
        List<TCMB> tcmbList = problemSetting.getTCMBList();
        Map<Integer, Integer> opToJob = ProblemSetting.getInstance().getOpToJob();


        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/com/zihao/GA_TS_SLAB/Plot/conjunctive_plot_data.csv"))) {
            // Writing the precedence relations from DAG
            writer.write("# Precedence Relationships\n");

            Map<Integer, List<Integer>> adjList = dag.getAdjacencyList();
            for (Map.Entry<Integer, List<Integer>> entry : adjList.entrySet()) {
                int operation = entry.getKey();
                List<Integer> successors = entry.getValue();
                for (int successor : successors) {
                    writer.write(operation + ", " + successor + "\n");
                }
            }

            writer.write("# Machine Paths\n");
            List<Integer> osSequence = chromosome.getOS();
            List<Integer> msSequence = chromosome.getMS();
            Map<Integer, List<Integer>> machineOperations = new HashMap<>();

            for (int i = 0; i < msSequence.size(); i++) {
                int machine = msSequence.get(i);
                int operation = osSequence.get(i);
                machineOperations.putIfAbsent(machine, new ArrayList<>());
                machineOperations.get(machine).add(operation);
            }

            for (Map.Entry<Integer, List<Integer>> entry : machineOperations.entrySet()) {
                int machine = entry.getKey();
                List<Integer> operations = entry.getValue();
                writer.write(machine + "," + String.join(",", operations.stream().map(String::valueOf).toArray(String[]::new)) + "\n");
            }

            // Writing the TCMB constraints
            writer.write("# TCMB Constraints\n");
            for (TCMB tcmb : tcmbList) {
                writer.write(tcmb.getOp1() + ", " + tcmb.getOp2() + ", " + tcmb.getTimeConstraint() + "\n");
            }
//            writer.write("# Operation to Job Mapping\n");
//            for (Map.Entry<Integer, Integer> entry : opToJob.entrySet()) {
//                writer.write(entry.getKey() + ", " + entry.getValue() + "\n");
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/python3", "src/com/zihao/GA_TS_SLAB/Plot/plot_conjunctive_graph.py", "conjunctive_plot_data.csv");
            pb.inheritIO(); // To see the output of the Python script in console
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

//    private void generateDBSCANDataFile(Chromosome[] parents) {
//        File dbscanFile = new File("src/com/zihao/GA_TS_SLAB/Plot/dbscan_data.csv");
//        try (FileWriter writer = new FileWriter(dbscanFile)) {
//            writer.append("Fitness,OS,MS\n");
//            for (Chromosome chromosome : parents) {
//                writer.append(chromosome.getFitness() + "," +
//                        chromosome.getOS().toString().replace("[", "").replace("]", "").replace(" ", "") + "," +
//                        chromosome.getMS().toString().replace("[", "").replace("]", "").replace(" ", "") + "\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("DBSCAN data exported to " + dbscanFile.getAbsolutePath());
//    }

//    private void saveFeatureVectors(Chromosome[] chromosomes) {
//        try (FileWriter writer = new FileWriter("population_features.csv")) {
//            for (Chromosome chrom : chromosomes) {
//                double[] features = chrom.getFeatureVector();
//                StringBuilder sb = new StringBuilder();
//                for (int i = 0; i < features.length; i++) {
//                    sb.append(features[i]);
//                    if (i < features.length - 1) {
//                        sb.append(",");
//                    }
//                }
//                writer.write(sb.toString());
//                writer.write("\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public void exportPopulationData(File outputFile, Chromosome[] parents) {
//        try (FileWriter writer = new FileWriter(outputFile)) {
//            writer.append("Fitness,OS,MS\n");  // Header
//            for (Chromosome chromosome : parents) {  // Assuming 'parents' is the final population
//                // Convert OS and MS to comma-separated strings
//                String osString = chromosome.getOS().stream().map(String::valueOf).collect(Collectors.joining(","));
//                String msString = chromosome.getMS().stream().map(String::valueOf).collect(Collectors.joining(","));
//                writer.append(chromosome.getFitness() + "," + osString + "," + msString + "\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Population data exported to " + outputFile.getAbsolutePath());
//    }


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

    void checkChromosome(Chromosome o){
        Utility.printViolation(o.getSchedule());
        o.checkPrecedenceConstraints();
        System.out.println("The fitness of chromosome is "+ o.getFitness());
    }

    public Chromosome adjustDelayBasedOnViolations(Chromosome chromosome, ProblemSetting problemSetting) {
        List<TCMB> listTCMB = problemSetting.getTCMBList();
        Schedule schedule = chromosome.getSchedule();
        Map<Integer, Integer> startTimes = schedule.getStartTimes();
        Map<Integer, Integer> delayMap = chromosome.getDelay(); // 获取当前的 delay 映射
        int[] processingTimes = problemSetting.getProcessingTime();

        for (TCMB tcmb : listTCMB) {
            int opA = tcmb.getOp1();
            int opB = tcmb.getOp2();

            int s_a = startTimes.getOrDefault(opA, 0);
            int s_b = startTimes.getOrDefault(opB, 0);
            int pi_a = processingTimes[opA - 1];

            int timeLag = s_b - (s_a + pi_a);

            // 如果违反了约束
            if (timeLag > tcmb.getTimeConstraint()) {
                // 计算 violation 的时间量
                int violation = timeLag - tcmb.getTimeConstraint();

                // 获取当前 opA 的 delay，并加上 violation
                int currentDelay = delayMap.getOrDefault(opA, 0);
                int newDelay = currentDelay + violation;

                // 更新 opA 的 delay
                delayMap.put(opA, newDelay);
            }
        }

        // 设置更新后的 delay 映射
        chromosome.setDelay(delayMap);
        chromosome.updateScheduleAndFitness(); // 更新调度和适应度

        return chromosome;
    }


//    public class Pair<K, V> {
//        private final K key;
//        private final V value;
//
//        public Pair(K key, V value) {
//            this.key = key;
//            this.value = value;
//        }
//
//        public K getKey() {
//            return key;
//        }
//
//        public V getValue() {
//            return value;
//        }
//
//        @Override
//        public String toString() {
//            return "(" + key + ", " + value + ")";
//        }
//    }


//    public Chromosome simulatedAnnealingAdjust(Chromosome chromosome, ProblemSetting problemSetting) {
//        // 初始化参数
//        Random random = new Random();
//        double temperature = 1000;
//        double coolingRate = 0.95;
//        int maxIterations = 1000;
//        Chromosome currentChromosome = new Chromosome(chromosome);
//        Chromosome bestChromosome = new Chromosome(chromosome);
//        double currentFitness = currentChromosome.getFitness();
//
//        List<TCMB> tcmbList = problemSetting.getTCMBList();
//
//        // 迭代过程
//        for (int iter = 0; iter < maxIterations; iter++) {
//            // 记录违反TCMB约束的操作对
//            List<Pair<Integer, Integer>> violatingPairs = getViolatingPairs(currentChromosome, problemSetting);
//
//            // 如果没有违反约束的操作对，返回最优解
//            if (violatingPairs.isEmpty()) {
//                return bestChromosome;
//            }
//
//            // 随机选择一个违反约束的操作对
//            Pair<Integer, Integer> selectedPair = violatingPairs.get(random.nextInt(violatingPairs.size()));
//            int opA = selectedPair.getKey();
//            int opB = selectedPair.getValue();
//
//            // 生成邻域解
//            Chromosome neighborChromosome = new Chromosome(currentChromosome);
//            generateNeighbor(neighborChromosome, opA, opB);
//
//            // 更新调度和适应度
//            neighborChromosome.updateScheduleAndFitness();
//            double neighborFitness = neighborChromosome.getFitness();
//
//            // 违反约束数量
//            int currentViolations = countViolations(currentChromosome, problemSetting);
//            int neighborViolations = countViolations(neighborChromosome, problemSetting);
//
//            // 接受准则
//            if (neighborViolations < currentViolations ||
//                    (neighborViolations == currentViolations && neighborFitness < currentFitness)) {
//                // 如果邻域解更优或等效但适应度更好，接受此解
//                currentChromosome = neighborChromosome;
//                currentFitness = neighborFitness;
//
//                // 更新最优解
//                if (neighborViolations < countViolations(bestChromosome, problemSetting) ||
//                        (neighborViolations == countViolations(bestChromosome, problemSetting) && neighborFitness < bestChromosome.getFitness())) {
//                    bestChromosome = neighborChromosome;
//                }
//            } else {
//                // 以一定概率接受更差的解
//                double acceptanceProbability = Math.exp((currentViolations - neighborViolations) / temperature);
//                if (Math.random() < acceptanceProbability) {
//                    currentChromosome = neighborChromosome;
//                    currentFitness = neighborFitness;
//                }
//            }
//
//            // 冷却过程
//            temperature *= coolingRate;
//        }
//
//        return bestChromosome;
//    }

//    // 获取违反约束的操作对
//    private List<Pair<Integer, Integer>> getViolatingPairs(Chromosome chromosome, ProblemSetting problemSetting) {
//        List<Pair<Integer, Integer>> violatingPairs = new ArrayList<>();
//        List<TCMB>tcmbList = problemSetting.getTCMBList();
//        for (TCMB tcmb : tcmbList) {
//            int opA = tcmb.getOp1();
//            int opB = tcmb.getOp2();
//            Schedule s = chromosome.decode();
//            int startA = s.getStartTimes().get(opA);
//            int endA = startA + problemSetting.getProcessingTime()[opA - 1];
//            int startB = s.getStartTimes().get(opB);
//            int timeLag = startB - endA;
//
//            if (timeLag < tcmb.getTimeConstraint()) {
//                violatingPairs.add(new Pair<>(opA, opB));
//            }
//        }
//        return violatingPairs;
//    }

//    // 生成邻域解
//    private void generateNeighbor(Chromosome chromosome, int opA, int opB) {
//        // 获取当前的延迟
//        int currentDelay = chromosome.getDelay().getOrDefault(opA, 0);
//
//        // 动态步长调整
//        int step = (int) (Math.random() * 5 + 1);  // 可以根据需要调整步长
//        boolean increaseDelay = Math.random() > 0.5;
//
//        if (increaseDelay) {
//            // 增加opA的延迟
//            chromosome.getDelay().put(opA, currentDelay + step);
//        } else {
//            // 减少opA的延迟
//            chromosome.getDelay().put(opA, Math.max(0, currentDelay - step));
//        }
//    }

//    public int countViolations(Chromosome chromosome, ProblemSetting problemSetting) {
//        int violationCount = 0;
//        // 遍历 Chromosome，检查违反的约束
//        for (TCMB tcmb : problemSetting.getTCMBList()) {
//            int opA = tcmb.getOp1();
//            int opB = tcmb.getOp2();
//
//            Schedule schedule = chromosome.getSchedule();
//            int endA = schedule.getStartTimes().get(opA) + problemSetting.getProcessingTime()[opA - 1];
//            int startB = schedule.getStartTimes().get(opB);
//
//            int timeLag = startB - endA;
//            if (timeLag < tcmb.getTimeConstraint()) {
//                violationCount++;
//            }
//        }
//        return violationCount;
//    }


    public Schedule CPsolve(){
        System.out.println(System.getProperty("java.library.path"));
        TcmbConstraintSolver cp = new TcmbConstraintSolver();
        Chromosome[] parents = initPopulation();

        Chromosome initBest = parents[getBestIndex(parents)];
        Chromosome currentBest = parents[getBestIndex(parents)];
        Chromosome best = new Chromosome(currentBest);
        Schedule result = cp.solve(best);
        Utility.checkViolation(result);
        return result;
    }

}
