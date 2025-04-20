package com.zihao.GA_TS_SLAB.GA;


import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.*;


import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Graph.DirectedAcyclicGraph;
import com.zihao.GA_TS_SLAB.Test.TestConstraint;

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

            int searchDelayNum = (int) (popNum * Parameters.DELAY_SEARCH_RATIO);

            TabuSearchDelay tabuSearchDelay = new TabuSearchDelay(Parameters.TABU_MIN_ITERATION,
                    Parameters.TABU_MAX_ITERATION,
                    Parameters.TABU_SIZE,
                    Parameters.TABU_IMPROVEMENT);


            // Apply Tabu Search to the top searchNum individuals
//            for (int i = 0; i < searchDelayNum; i++) {
////                Chromosome optimizedChromosome = tabuSearchDelay.optimize(parents[i], best.getFitness());
////                parents[i] = optimizedChromosome;
//                parents[i] = tabuSearchDelay.optimize(parents[i], best.getFitness());
//            }

            int tournamentSize = 3;  // 锦标赛的规模，可以根据需求调整
//            for (int i = 0; i < searchDelayNum; i++) {
//                Chromosome selectedChromosome = tournamentSelection(Arrays.asList(parents), tournamentSize);  // 锦标赛选择个体
//                Chromosome optimizedChromosome = tabuSearchDelay.optimize(selectedChromosome, best.getFitness());  // 进行 Tabu Search 优化
//                // 替换原始的个体
//
//                for (int j = 0; j < parents.length; j++) {
//                    if (parents[j].equals(selectedChromosome)) {
//                        parents[j] = optimizedChromosome;  // 更新选择的个体为优化后的个体
//                        break;
//
//                    }
//                }
//            }

            int[] selectedIndices = new int[searchDelayNum];
            for (int i = 0; i < searchDelayNum; i++) {
                selectedIndices[i] = tournamentSelection(Arrays.asList(parents), tournamentSize);
                parents[selectedIndices[i]] = tabuSearchDelay.optimize(parents[selectedIndices[i]], best.getFitness());
            }

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

//        best.checkPrecedenceConstraints();


        // Create SAFM optimizer with customized parameters
//        SAFM safm = new SAFM(10000, 0.98, 5000);

        // Apply SAFM to optimize the best solution from GA
//        Schedule optimizedSchedule = safm.optimize(best);

//        Utility.printViolation(optimizedSchedule);

        // Report TCMB violations after SAFM
//        System.out.println("\nTCMB violations after SAFM:");
//

//        Utility.printViolation(best.getSchedule());
//        TestConstraint test = new TestConstraint("qPCR_RNAseq_N5");
////
//        Schedule optimizedSchedule = best.getSchedule();
//        if (!test.checkDependency(optimizedSchedule)){
//            System.out.println("The schedule has violated precedence constraint");
//        }
//        if (!test.checkMachineConstraint(optimizedSchedule)){
//            System.out.println("The schedule has violates machine constraint");
//        }
////        if (!test.checkTCMBConstraint(optimizedSchedule)){
////            System.out.println("The schedule has violated TCMB constraint");
////        }
//        if (!test.checkMachineOccupation(optimizedSchedule)){
//            System.out.println("The schedule has violated machine occupation constraint");
//        }
//        System.out.println(optimizedSchedule.printMachineSchedule());

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


        return best.getSchedule();
//        return optimizedSchedule;
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

    //todo 选择一种 tournament selection
//    private Chromosome tournamentSelection(List<Chromosome> population, int tournamentSize) {
//        List<Chromosome> tournament = new ArrayList<>();
//        Random random = new Random();
//        for (int i = 0; i < tournamentSize; i++) {
//            int randomIndex = random.nextInt(population.size());
//            tournament.add(population.get(randomIndex));
//        }
//        // 使用 compareTo 方法进行排序，并选择适应度最好的个体（最小 fitness）
//        return Collections.min(tournament);  // Collections.min 直接调用 compareTo
//    }

    /**
     * 锦标赛选择，返回选中个体在种群中的索引
     * @param population 种群
     * @param tournamentSize 锦标赛规模
     * @return 选中个体在种群中的索引
     */
    private int tournamentSelection(List<Chromosome> population, int tournamentSize) {
        List<Integer> tournamentIndices = new ArrayList<>();
        List<Chromosome> tournament = new ArrayList<>();
        Random r = new Random();

        // 随机选择锦标赛参与者并记录其索引
        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = r.nextInt(population.size());
            tournamentIndices.add(randomIndex);
            tournament.add(population.get(randomIndex));
        }

        // 找出锦标赛中适应度最好的个体
        Chromosome bestInTournament = Collections.min(tournament);

        // 返回最优个体在原始种群中的索引
        int bestIndex = tournamentIndices.get(tournament.indexOf(bestInTournament));
        return bestIndex;
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
