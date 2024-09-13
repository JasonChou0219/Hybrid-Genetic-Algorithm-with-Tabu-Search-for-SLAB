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


    private static int hammingDistance(List<Integer> seq1, List<Integer> seq2) {
        int distance = 0;
        for (int i = 0; i < seq1.size(); i++) {
            if (!seq1.get(i).equals(seq2.get(i))) {
                distance++;
            }
        }
        return distance;
    }

//    private static double calculateDiversity(List<Chromosome> population, boolean isOS) {
//        int popSize = population.size();
//        int seqLength = isOS ? population.get(0).getOS().size() : population.get(0).getMS().size();
//        int totalDistance = 0;
//
//        for (int i = 0; i < popSize; i++) {
//            for (int j = i + 1; j < popSize; j++) {
//                List<Integer> seq1 = isOS ? population.get(i).getOS() : population.get(i).getMS();
//                List<Integer> seq2 = isOS ? population.get(j).getOS() : population.get(j).getMS();
//                totalDistance += hammingDistance(seq1, seq2);
//            }
//        }
//
//        int maxPossibleDistance = seqLength * (popSize * (popSize - 1)) / 2;
//        return totalDistance / (double) maxPossibleDistance;
//    }


    /**
     * Description: mutation operation for OS and MS chromosome
     */
    // remained to be done: consider different mutation rate for OS and MS
    // remained to be done: adaptive mutation rate
    public static void Mutation(Chromosome[] parents) {
        int num = parents.length;
        List<Chromosome> parentList = Arrays.asList(parents);

        double osDiversity = calculateClusterDiversity(parentList, true, 5); // 5 表示聚类的数量
        double msDiversity = calculateClusterDiversity(parentList, false, 5);

//        double osDiversity = calculateDiversity(parentList, true);
//        double msDiversity = calculateDiversity(parentList, false);

        System.out.println("OS 多样性: " + osDiversity);
        System.out.println("MS 多样性: " + msDiversity);

        double osBaseRate = Parameters.OS_MUTATION_RATE;
        double osMaxRate = Parameters.OS_MAX_MUTATION_RATE;
        double msBaseRate = Parameters.MS_MUTATION_RATE;
        double msMaxRate = Parameters.MS_MAX_MUTATION_RATE;


        double osMutationRate = osBaseRate + (1 - osDiversity) * (osMaxRate - osBaseRate);
        double msMutationRate = msBaseRate + (1 - msDiversity) * (msMaxRate - msBaseRate);

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

    private static double calculateClusterDiversity(List<Chromosome> population, boolean isOS, int k) {
        // 将种群转换为聚类所需的特征向量列表
        List<List<Integer>> features = new ArrayList<>();
        for (Chromosome chrom : population) {
            features.add(isOS ? chrom.getOS() : chrom.getMS());
        }

        // 执行 K-means 聚类
        List<Integer> labels = kMeansClustering(features, k);

        // 计算每个聚类的大小
        Map<Integer, Integer> clusterSizes = new HashMap<>();
        for (int label : labels) {
            clusterSizes.put(label, clusterSizes.getOrDefault(label, 0) + 1);
        }

        // 计算多样性，使用基尼指数（Gini Index）衡量
        double diversity = 0.0;
        int popSize = population.size();
        for (int size : clusterSizes.values()) {
            double proportion = (double) size / popSize;
            diversity += proportion * proportion;
        }

        return 1 - diversity; // 多样性 = 1 - 基尼指数
    }

    private static List<Integer> kMeansClustering(List<List<Integer>> data, int k) {
        // 初始化聚类中心
        List<List<Integer>> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            centroids.add(new ArrayList<>(data.get(i)));
        }

        List<Integer> labels = new ArrayList<>(Collections.nCopies(data.size(), -1));
        boolean changed = true;
        int maxIterations = 100;

        while (changed && maxIterations-- > 0) {
            changed = false;

            // 分配数据点到最近的聚类中心
            for (int i = 0; i < data.size(); i++) {
                int nearestCentroid = -1;
                double nearestDistance = Double.MAX_VALUE;

                for (int j = 0; j < k; j++) {
                    double distance = euclideanDistance(data.get(i), centroids.get(j));
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestCentroid = j;
                    }
                }

                if (labels.get(i) != nearestCentroid) {
                    labels.set(i, nearestCentroid);
                    changed = true;
                }
            }

            // 计算新的聚类中心
            List<List<Integer>> newCentroids = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                newCentroids.add(new ArrayList<>(Collections.nCopies(data.get(0).size(), 0)));
            }
            int[] counts = new int[k];

            for (int i = 0; i < data.size(); i++) {
                int label = labels.get(i);
                List<Integer> point = data.get(i);
                List<Integer> centroid = newCentroids.get(label);

                for (int d = 0; d < point.size(); d++) {
                    centroid.set(d, centroid.get(d) + point.get(d));
                }
                counts[label]++;
            }

            for (int j = 0; j < k; j++) {
                if (counts[j] > 0) {
                    List<Integer> centroid = newCentroids.get(j);
                    for (int d = 0; d < centroid.size(); d++) {
                        centroid.set(d, centroid.get(d) / counts[j]);
                    }
                }
            }

            centroids = newCentroids;
        }

        return labels;
    }

    private static double euclideanDistance(List<Integer> point1, List<Integer> point2) {
        double sum = 0.0;
        for (int i = 0; i < point1.size(); i++) {
            sum += Math.pow(point1.get(i) - point2.get(i), 2);
        }
        return Math.sqrt(sum);
    }



}
