package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;

import java.sql.ParameterMetaData;
import java.util.*;

/**
 * Description: including selection, crossover and mutation for OS and MS chromosome
 */

public class Operator {

    private static ProblemSetting problemSetting = ProblemSetting.getInstance();
    private static Random r = new Random();

    /**
     * Description: elitist selection
     */

    public static Chromosome[] ElitistSelection(Chromosome[] parents, int elitNum) {
//        int popNum = Parameters.POP_NUM;
//        double elitRate = Parameters.ELIT_RATIO;
//
//        int elitNum = (int) (elitRate * popNum);
        Chromosome[] elites = new Chromosome[elitNum];

        ArrayList<Chromosome> p = new ArrayList<>();
        Collections.addAll(p, parents);
        Collections.sort(p);

//        System.out.println("All chromosomes after sorting by fitness (ascending):");
//        for (int i = 0; i < p.size(); i++) {
//            System.out.println("Chromosome " + i + " with fitness: " + p.get(i).getFitness());
//        }

        for (int i = 0; i < elitNum; i++) {
            Chromosome o = p.get(i);
            elites[i] = new Chromosome(o);
//            System.out.println("Selected elite chromosome " + i + " with fitness: " + elites[i].getFitness());
        }
//        for (int i = 0; i < elitNum; i++) {
//            Chromosome o = p.get(i);
//            elites[i] = new Chromosome(o);
//            System.out.println("Selected elite chromosome " + i + " with fitness: " + elites[i].getFitness());
//        }
        return elites;
    }

    /**
     * Description: tournament selection
     */
    public static Chromosome[] TournamentSelection(Chromosome[] parents, int tourNum) {
//        int popNum = Parameters.POP_NUM;
//        double elitRate = Parameters.ELIT_RATIO;
//
//        int elitNum = (int) (elitRate * popNum);
        Chromosome[] tournamentSelection = new Chromosome[tourNum];
        int popNum = parents.length;

        for (int i = 0; i < tourNum; i++) {
            int n1 = r.nextInt(popNum);
            int n2 = r.nextInt(popNum);
            if (parents[n1].getFitness() < parents[n2].getFitness()){
                Chromosome o = parents[n2];
                tournamentSelection[i] = new Chromosome(o);
            }
            else{
                Chromosome o = parents[n1];
                tournamentSelection[i] = new Chromosome(o);
            }
//            System.out.println("Selected chromosome " + i + " with fitness: " + tournamentSelection[i].getFitness());
        }

        return tournamentSelection;
    }


    public static Chromosome[] RouletteWheelSelection(Chromosome[] parents, int rouletteNum) {

//        int popNum = Parameters.POP_NUM;
//        double elitRate = Parameters.ELIT_RATIO;
//
//        int elitNum = (int) (elitRate * popNum);
        Chromosome[] wheelSelection = new Chromosome[rouletteNum];
        int popNum = parents.length;

        double[] fitnessValues = new double[popNum];
        double totalFitness = 0;

        // Calculate total fitness and individual fitness values
        for (int i = 0; i < popNum; i++) {
            fitnessValues[i] = 1.0 / (parents[i].getFitness() + 1); //
            totalFitness += fitnessValues[i];
        }

        // Normalize fitness values and create cumulative probability distribution
        double[] cumulativeProbabilities = new double[popNum];
        cumulativeProbabilities[0] = fitnessValues[0] / totalFitness;

        for (int i = 1; i < popNum; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1] + fitnessValues[i] / totalFitness;
//            if (i == popNum - 1) {
//                System.out.println("The last cumulativeP is "+ cumulativeProbabilities[i]);
//            }
        }

        cumulativeProbabilities[popNum - 1] = 1.0;

        // Selection of individuals based on roulette wheel
        for (int i = 0; i < rouletteNum; i++) {
            double rand = r.nextDouble();
            for (int j = 0; j < popNum; j++) {
                if (rand <= cumulativeProbabilities[j]) {
//                    System.out.println("THe random number is "+ rand);
//                    System.out.println("The accumulate p is "+ cumulativeProbabilities[j]);
                    wheelSelection[i] = new Chromosome(parents[j]); // Create a copy of the selected chromosome
                    break;
                }
            }
        }

        return wheelSelection;
    }

    /**
     * Description: precedence operation crossover(POX) and job-based crossover (JBX) for OS chromosome,
     * two-point crossover for MS chromosome.
     */
    public static void Crossover(Chromosome[] parents) {
         int num = parents.length;

         // Might be odd if elist is odd
        for (int i = 0; i < num & i + 1 < num; i += 2) {

            if (r.nextDouble() < Parameters.CROSSOVER_RATE) {

                Chromosome parent1 = parents[i];
                Chromosome parent2 = parents[i + 1];

                List<Integer> OS1 = new ArrayList<>(parent1.getOS());
                List<Integer> OS2 = new ArrayList<>(parent2.getOS());
                List<Integer> MS1 = new ArrayList<>(parent1.getMS());
                List<Integer> MS2 = new ArrayList<>(parent2.getMS());

//                System.out.println("交叉前 Chromosome " + i + " 和 " + (i + 1) + ":");
//                System.out.println("OS1: " + OS1);
//                System.out.println("OS2: " + OS2);
//                System.out.println("MS1: " + MS1);
//                System.out.println("MS2: " + MS2);

                if (r.nextDouble() < Parameters.OS_CROSSOVER_RATIO) {
                    POXCrossover(OS1, OS2);
                } else {
                    JBXCrossover(OS1, OS2);
                }

                // Perform two-point crossover for MS
                TwoPointCrossover(MS1, MS2);

//                System.out.println("交叉后 Chromosome " + i + " 和 " + (i + 1) + ":");
//                System.out.println("OS1: " + OS1);
//                System.out.println("OS2: " + OS2);
//                System.out.println("MS1: " + MS1);
//                System.out.println("MS2: " + MS2);

                // replace parent by crossover offspring
                // adjust MS by compatibility
                MS1 = Utility.compatibleAdjust(MS1, OS1);
                MS2 = Utility.compatibleAdjust(MS2, OS2);
                parents[i].setOS(OS1);
                parents[i].setMS(MS1);
                parents[i + 1].setOS(OS2);
                parents[i + 1].setMS(MS2);

//                System.out.println("调整后 Chromosome " + i + " 和 " + (i + 1) + ":");
//                System.out.println("MS1: " + MS1);
//                System.out.println("MS2: " + MS2);
//                System.out.println("====================================");
//                System.out.println("Performed crossover between chromosome " + i + " and " + (i + 1));
            }
        }
    }

    /**
     * Description: precedence operation crossover(POX)
     */
    // 对于只有单个job的crossover该如何解决呢?
    private static void POXCrossover(List<Integer> OS1, List<Integer> OS2) {
//        System.out.println("Performing POX Crossover: =============================");
//        System.out.println("Crossover between:");
//        System.out.println("Parent 1: " + OS1.toString());
//        System.out.println("Parent 2: " + OS2.toString());

        int totalJobs = problemSetting.getJobNum();
        Set<Integer> J1 = new HashSet<>();
        Set<Integer> J2 = new HashSet<>();

        // Randomly divide job set into two subsets J1 and J2
        for (int i = 1; i <= totalJobs; i++) {
            if (r.nextBoolean()) {
                J1.add(i);
            } else {
                J2.add(i);
            }
        }

        // Log the contents of J1 and J2
//        System.out.println("J1 contains jobs: " + J1.toString());
//        System.out.println("J2 contains jobs: " + J2.toString());

        List<Integer> Off1 = new ArrayList<>(Collections.nCopies(OS1.size(), -1));
        List<Integer> Off2 = new ArrayList<>(Collections.nCopies(OS2.size(), -1));

        // Allocate operations belonging to J1 in P1 to O1 and J1 in P2 to O2
        for (int i = 0; i < OS1.size(); i++) {
            if (J1.contains(problemSetting.getOpToJob().get(OS1.get(i)))) {
                Off1.set(i, OS1.get(i));
            }
            if (J1.contains(problemSetting.getOpToJob().get(OS2.get(i)))) {
                Off2.set(i, OS2.get(i));
            }
        }

        // Log the intermediate offspring after placing J1 operations
//        System.out.println("Off1 after placing J1 operations: " + Off1.toString());
//        System.out.println("Off2 after placing J1 operations: " + Off2.toString());

        // Fill the empty positions of O1 with operations belonging to J2 in P2 sequentially
        int index1 = 0;
        for (int op : OS2) {
            if (J2.contains(ProblemSetting.getInstance().getOpToJob().get(op))) {
                while (Off1.get(index1) != -1) {
                    index1++;
                }
                Off1.set(index1, op);
            }
        }

        // Log the offspring after filling with J2 operations from OS2
//        System.out.println("Off1 after filling with J2 operations from Parent 2: " + Off1.toString());

        // Fill the empty positions of O2 with operations belonging to J2 in P1 sequentially
        int index2 = 0;
        for (int op : OS1) {
            if (J2.contains(ProblemSetting.getInstance().getOpToJob().get(op))) {
                while (Off2.get(index2) != -1) {
                    index2++;
                }
                Off2.set(index2, op);
            }
        }

        // Log the offspring after filling with J2 operations from OS1
//        System.out.println("Off2 after filling with J2 operations from Parent 1: " + Off2.toString());

        // Update the original parents with the new offspring
        OS1.clear();
        OS1.addAll(Off1);
        OS2.clear();
        OS2.addAll(Off2);

        // Final log
//        System.out.println("Final Offspring 1: " + OS1.toString());
//        System.out.println("Final Offspring 2: " + OS2.toString());
    }
    /**
     * Description: job-based crossover (JBX)
     */

    private static void JBXCrossover(List<Integer> OS1, List<Integer> OS2) {
        int totalJobs = problemSetting.getJobNum();
        Set<Integer> J1 = new HashSet<>();
        Set<Integer> J2 = new HashSet<>();

        // Randomly divide job set into two subsets J1 and J2
        for (int i = 1; i <= totalJobs; i++) {
            if (r.nextBoolean()) {
                J1.add(i);
            } else {
                J2.add(i);
            }
        }

        List<Integer> O1 = new ArrayList<>(Collections.nCopies(OS1.size(), -1));
        List<Integer> O2 = new ArrayList<>(Collections.nCopies(OS2.size(), -1));

        // Allocate operations belonging to J1 in P1 to O1 and J2 in P2 to O2
        for (int i = 0; i < OS1.size(); i++) {
            if (J1.contains(ProblemSetting.getInstance().getOpToJob().get(OS1.get(i)))) {
                O1.set(i, OS1.get(i));
            }
            if (J2.contains(ProblemSetting.getInstance().getOpToJob().get(OS2.get(i)))) {
                O2.set(i, OS2.get(i));
            }
        }

        // Fill the empty positions of O1 with operations belonging to J2 in P2 sequentially
        int index1 = 0;
        for (int op : OS2) {
            if (J2.contains(ProblemSetting.getInstance().getOpToJob().get(op))) {
                while (O1.get(index1) != -1) {
                    index1++;
                }
                O1.set(index1, op);
            }
        }

        // Fill the empty positions of O2 with operations belonging to J1 in P1 sequentially
        int index2 = 0;
        for (int op : OS1) {
            if (J1.contains(ProblemSetting.getInstance().getOpToJob().get(op))) {
                while (O2.get(index2) != -1) {
                    index2++;
                }
                O2.set(index2, op);
            }
        }

        OS1.clear();
        OS1.addAll(O1);
        OS2.clear();
        OS2.addAll(O2);
    }
    /**
     * Description: two-point crossover for MS
     */

    private static void TwoPointCrossover(List<Integer> MS1, List<Integer> MS2) {
        // 输出初始的 MS1 和 MS2
//        System.out.println("执行 TwoPointCrossover: =============================");
//        System.out.println("初始 MS1: " + MS1.toString());
//        System.out.println("初始 MS2: " + MS2.toString());

        int length = MS1.size();
        int p1 = r.nextInt(length);
        int p2 = r.nextInt(length);
        while (p2 == p1) {
            p2 = r.nextInt(length);
        }
        if (p1 > p2) {
            int temp = p1;
            p1 = p2;
            p2 = temp;
        }

        // 输出选定的交叉点
//        System.out.println("选定的交叉点 p1: " + p1 + ", p2: " + p2);

        List<Integer> Off1 = new ArrayList<>(MS1);
        List<Integer> Off2 = new ArrayList<>(MS2);

        // 交换 p1 和 p2 之间的片段
        for (int i = p1; i <= p2; i++) {
            Off1.set(i, MS2.get(i));
            Off2.set(i, MS1.get(i));
        }

        // 输出交叉后的片段
//        System.out.println("交叉后的 Off1: " + Off1.toString());
//        System.out.println("交叉后的 Off2: " + Off2.toString());

        // 更新 MS1 和 MS2
        MS1.clear();
        MS1.addAll(Off1);
        MS2.clear();
        MS2.addAll(Off2);

//        // 输出最终的 MS1 和 MS2
//        System.out.println("最终的 MS1: " + MS1.toString());
//        System.out.println("最终的 MS2: " + MS2.toString());
    }




    public static class EditDistance {
        public int calculateEditDistance(int[] a, int[] b) {
            int n = a.length;
            int m = b.length;
            int[][] dp = new int[n + 1][m + 1];

            // 初始化dp数组
            for (int i = 0; i <= n; i++) {
                dp[i][0] = i;
            }
            for (int j = 0; j <= m; j++) {
                dp[0][j] = j;
            }

            // 计算编辑距离
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= m; j++) {
                    if (a[i - 1] == b[j - 1]) {
                        dp[i][j] = dp[i - 1][j - 1];
                    } else {
                        dp[i][j] = Math.min(dp[i - 1][j - 1], // 替换
                                Math.min(dp[i - 1][j],   // 删除
                                        dp[i][j - 1])   // 插入
                        ) + 1;
                    }
                }
            }

            return dp[n][m];
        }
    }

    /**
     * 使用采样的方式计算种群的多样性
     * @param population 种群
     * @param isOS 表示是 OS 还是 MS
     * @param sampleSize 采样的对数
     * @return 近似多样性
     */
    private static double calculateSampledDiversity(List<Chromosome> population, boolean isOS, int sampleSize) {
        int popSize = population.size();
        EditDistance editDistance = new EditDistance();
        double totalDistance = 0.0;

        // 进行采样
        for (int i = 0; i < sampleSize; i++) {
            // 随机选择两个不同的个体
            int index1 = r.nextInt(popSize);
            int index2 = r.nextInt(popSize);
            while (index1 == index2) {
                index2 = r.nextInt(popSize);
            }

            // 获取个体的 OS 或 MS
            List<Integer> seq1 = isOS ? population.get(index1).getOS() : population.get(index1).getMS();
            List<Integer> seq2 = isOS ? population.get(index2).getOS() : population.get(index2).getMS();

            // 将 List<Integer> 转换为 int[] 以计算编辑距离
            int[] arr1 = seq1.stream().mapToInt(Integer::intValue).toArray();
            int[] arr2 = seq2.stream().mapToInt(Integer::intValue).toArray();

            // 计算编辑距离并累加
            totalDistance += editDistance.calculateEditDistance(arr1, arr2);
        }

        // 返回平均距离，作为多样性指标
        double avgDistance = totalDistance / sampleSize;

        // 归一化多样性到 [0, 1]
        double maxPossibleDistance = (isOS ? population.get(0).getOS().size() : population.get(0).getMS().size());
        double normalizedDiversity = avgDistance / maxPossibleDistance;

        // 确保归一化后的多样性在 [0, 1] 范围内
        return Math.max(0.0, Math.min(1.0, normalizedDiversity));
    }
    /**
     * Description: mutation operation for OS and MS chromosome
     */
    // remained to be done: consider different mutation rate for OS and MS
    // remained to be done: adaptive mutation rate
    public static void Mutation(Chromosome[] parents) {
        int num = parents.length;
        List<Chromosome> parentList = Arrays.asList(parents);

//        double osDiversity = calculateClusterDiversity(parentList, true, 5); // 5 表示聚类的数量
//        double msDiversity = calculateClusterDiversity(parentList, false, 5);
        // 55 60 65
//        double osDiversity = calculateDBSCANDiversity(parentList, true, 65.0, 1); // 调整 eps 和 minPts
////        double osDiversity = 1;
//        double msDiversity = calculateDBSCANDiversity(parentList, false, 40.0, 1);
////        double msDiversity = 1;
//
//
//        double osDiversity = calculateAverageEditDistanceDiversity(parentList, true);
//        double msDiversity = calculateAverageEditDistanceDiversity(parentList, false);
//        double osDiversity = calculateDiversity(parentList, true);
//        double msDiversity = calculateDiversity(parentList, false);

//        System.out.println("OS 多样性: " + osDiversity);
//        System.out.println("MS 多样性: " + msDiversity);

        double osDiversity = calculateSampledDiversity(parentList, true, 50); // 调整采样对数，例如50
        double msDiversity = calculateSampledDiversity(parentList, false, 50);




        System.out.println("OS 多样性: " + osDiversity);
        System.out.println("MS 多样性: " + msDiversity);

        double osBaseRate = Parameters.OS_MUTATION_RATE;
        double osMaxRate = Parameters.OS_MAX_MUTATION_RATE;
        double msBaseRate = Parameters.MS_MUTATION_RATE;
        double msMaxRate = Parameters.MS_MAX_MUTATION_RATE;


//        double osMutationRate = osBaseRate + (1 - osDiversity) * (osMaxRate - osBaseRate);
//        double msMutationRate = msBaseRate + (1 - msDiversity) * (msMaxRate - msBaseRate);


        double osMutationRate = calculateSechMutationRate(osDiversity, osBaseRate, osMaxRate, 6.0, 0.5);
        double msMutationRate = calculateSechMutationRate(msDiversity, msBaseRate, msMaxRate, 18.0, 0.54);
//        double osMutationRate = osMaxRate;
//        double msMutationRate = msMaxRate;

        System.out.println("OS 变异率: " + osMutationRate);
        System.out.println("MS 变异率: " + msMutationRate);


        for (int i = 0; i < num; i++){
            Chromosome o = parents[i];
            // OS mutation
//            if (r.nextDouble() < Parameters.OS_MUTATION_RATE) {
            if (r.nextDouble() < osMutationRate) {
                List<Integer> newOS = new ArrayList<>(o.getOS());
                if (r.nextBoolean()) {
                    SwappingMutation(newOS);
//                    System.out.println("染色体 " + i + " 执行 SwappingMutation.");
                } else {
                    InsertionMutation(newOS);
//                    System.out.println("染色体 " + i + " 执行 InsertionMutation.");
                }
                o.setOS(newOS);
            }
            // MS mutation
//            if (r.nextDouble() < Parameters.MS_MUTATION_RATE) {
            if (r.nextDouble() < msMutationRate) {
                    List<Integer> newMS = new ArrayList<>(o.getMS());
                    MachineReassignmentMutation(newMS, o.getOS());
//                    System.out.println("染色体 " + i + " 执行 MachineReassignmentMutation.");
                    o.setMS(newMS);

            }
            // update schedule and fitness after crossover and mutation
            parents[i] = new Chromosome(o);
            if (r.nextDouble() < Parameters.DELAY_MUTATION_RATE){
                Map<Integer,Integer> maxDelay = new HashMap<>();
                for (TCMB tcmb : problemSetting.getTCMBList()) {
                    int opA = tcmb.getOp1();
                    int opB = tcmb.getOp2();

                    int endA = o.getSchedule().getStartTimes().get(opA) + problemSetting.getProcessingTime()[opA - 1];
                    int startB = o.getSchedule().getStartTimes().get(opB);

                    int timeLag = startB - endA;

                    if (timeLag > tcmb.getTimeConstraint()) {
                        double delayMean = 0;
                        double delayStdDev = Math.max(timeLag - tcmb.getTimeConstraint(), 1) / 2.0;
//                        System.out.println(timeLag - tcmb.getTimeConstraint());
                        int newDelay = (int) Math.round( (1 - r.nextGaussian()) * delayStdDev + delayMean);
                        if (newDelay > 0) {
                            int curDelay = maxDelay.getOrDefault(opA, 0);
                            if (curDelay != 0){
                                maxDelay.put(opA, Math.min(curDelay, newDelay));
                            }
                        }
                    }
                }

//                for (Map.Entry<Integer, Integer> entry : maxDelay.entrySet()) {
//                    int opA = entry.getKey();
//                    int delay = entry.getValue();
//                    o.getDelay().put(opA, delay);
//                }
                o.setDelay(maxDelay);
//                System.out.println("染色体 " + i + " 执行延迟变异，更新延迟.");
            }
        }
    }


    private static void SwappingMutation(List<Integer> OS) {
        int length = OS.size();
        int p1 = r.nextInt(length);
        int p2 = r.nextInt(length);
        while (p2 == p1) {
            p2 = r.nextInt(length);
        }

        // Swap elements
        Collections.swap(OS, p1, p2);

        // Perform topological sort
        Utility.topologicalSort(OS);

//        List<Integer> sortedOperations = Utility.kahnTopologicalSort(OS);
//
//        // Update the original OS with the sorted list
//        OS.clear();
//        OS.addAll(sortedOperations);

    }

    private static void InsertionMutation(List<Integer> OS) {
        int length = OS.size();
        int p1 = r.nextInt(length);
        int p2 = r.nextInt(length);
        while (p2 == p1) {
            p2 = r.nextInt(length);
        }

        int element = OS.remove(p1);
        OS.add(p2, element);

        // Perform topological sort
        Utility.topologicalSort(OS);

//        List<Integer> sortedOperations = Utility.kahnTopologicalSort(OS);
//
//        // Update the original OS with the sorted list
//        OS.clear();
//        OS.addAll(sortedOperations);
    }


    private static void MachineReassignmentMutation(List<Integer> MS, List<Integer> OS) {
        int length = MS.size();
        double msMutationRatio = Parameters.MACHINE_MUTATION_RATIO;
        int h = (int) (msMutationRatio * length);
        r = new Random();

        for (int i = 0; i < h; i++) {
            int p = r.nextInt(length);
            int operation = OS.get(p);
            List<Integer> compatibleMachines = ProblemSetting.getInstance().getOpToCompatibleList().get(operation);
            MS.set(p, compatibleMachines.get(r.nextInt(compatibleMachines.size())));
        }
    }

    private static double calculateSechMutationRate(double diversity, double baseRate, double maxRate, double alpha, double beta) {
        // 调整参数 a 以控制曲线的陡峭程度
//        double a = 6.0; // 增大 a 的值使曲线在中间更陡峭
        double mutationRate = baseRate + (maxRate - baseRate) * (1.0 / Math.cosh(alpha * (diversity - beta)));

        // 确保变异率在 [baseRate, maxRate] 范围内
        mutationRate = Math.max(baseRate, Math.min(mutationRate, maxRate));

        return mutationRate;
    }

}
