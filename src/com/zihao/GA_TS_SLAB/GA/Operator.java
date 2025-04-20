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
    private static final boolean DEBUG = true; // 调试开关，可以方便地打开或关闭调试输出

    /**
     * 辅助方法：用于调试输出
     */
    private static void debug(String message) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + message);
        }
    }


    /**
     * Description: elitist selection
     */

    public static Chromosome[] ElitistSelection(Chromosome[] parents, int elitNum) {
//        debug("执行精英选择，选择数量: " + elitNum);
        Chromosome[] elites = new Chromosome[elitNum];

        ArrayList<Chromosome> p = new ArrayList<>();
        Collections.addAll(p, parents);
        Collections.sort(p);

        for (int i = 0; i < elitNum; i++) {
            Chromosome o = p.get(i);
            elites[i] = new Chromosome(o);
//            debug("选中精英 #" + i + ": fitness=" + o.getFitness());
        }
//        debug("精英选择完成");
        return elites;
    }

    /**
     * Description: tournament selection
     */
    public static Chromosome[] TournamentSelection(Chromosome[] parents, int tourNum) {
//    	debug("执行锦标赛选择，选择数量: " + tourNum);
        Chromosome[] tournamentSelection = new Chromosome[tourNum];
        int popNum = parents.length;

        for (int i = 0; i < tourNum; i++) {
            int n1 = r.nextInt(popNum);
            int n2 = r.nextInt(popNum);
//            debug("锦标赛 #" + i + ": 个体 " + n1 + " (fitness=" + parents[n1].getFitness() +
//                  ") vs 个体 " + n2 + " (fitness=" + parents[n2].getFitness() + ")");
            if (parents[n1].getFitness() < parents[n2].getFitness()){
                Chromosome o = parents[n2];
                tournamentSelection[i] = new Chromosome(o);
//                debug("锦标赛 #" + i + ": 选择个体 " + n1 + " (fitness=" + o.getFitness() + ")");
            }
            else{
                Chromosome o = parents[n1];
                tournamentSelection[i] = new Chromosome(o);
//                debug("锦标赛 #" + i + ": 选择个体 " + n2 + " (fitness=" + o.getFitness() + ")");
            }
        }
//		debug("锦标赛选择完成");
        return tournamentSelection;
    }


    public static Chromosome[] RouletteWheelSelection(Chromosome[] parents, int rouletteNum) {
//		debug("执行轮盘赌选择，选择数量: " + rouletteNum);
        Chromosome[] wheelSelection = new Chromosome[rouletteNum];
        int popNum = parents.length;

        double[] fitnessValues = new double[popNum];
        double totalFitness = 0;

        // Calculate total fitness and individual fitness values
        for (int i = 0; i < popNum; i++) {
            fitnessValues[i] = 1.0 / (parents[i].getFitness() + 1); //
            totalFitness += fitnessValues[i];
        }
//		debug("总适应度反比值: " + totalFitness);
        // Normalize fitness values and create cumulative probability distribution
        double[] cumulativeProbabilities = new double[popNum];
        cumulativeProbabilities[0] = fitnessValues[0] / totalFitness;

        for (int i = 1; i < popNum; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1] + fitnessValues[i] / totalFitness;
        }

        cumulativeProbabilities[popNum - 1] = 1.0;

        // Selection of individuals based on roulette wheel
        for (int i = 0; i < rouletteNum; i++) {
            double rand = r.nextDouble();
//            debug("轮盘赌 #" + i + ": 随机值 = " + rand);
            for (int j = 0; j < popNum; j++) {
                if (rand <= cumulativeProbabilities[j]) {
                    wheelSelection[i] = new Chromosome(parents[j]); // Create a copy of the selected chromosome
//                    debug("轮盘赌 #" + i + ": 选择个体 " + j + " (fitness=" + parents[j].getFitness() + ")");
                    break;
                }
            }
        }
//        debug("轮盘赌选择完成");

        return wheelSelection;
    }

    /**
     * Description: precedence operation crossover(POX) and job-based crossover (JBX) for OS chromosome,
     * two-point crossover for MS chromosome.
     * //todo corssover for delay
     */
    public static void Crossover(Chromosome[] parents) {
//    	debug("开始交叉操作");
         int num = parents.length;

         // Might be odd if elist is odd
        for (int i = 0; i < num & i + 1 < num; i += 2) {

            if (r.nextDouble() < Parameters.CROSSOVER_RATE) {
//				debug("交叉对 #" + i/2 + ": 个体 " + i + " 和 " + (i+1));
                Chromosome parent1 = parents[i];
                Chromosome parent2 = parents[i + 1];

                List<Integer> OS1 = new ArrayList<>(parent1.getOS());
                List<Integer> OS2 = new ArrayList<>(parent2.getOS());
                List<Integer> MS1 = new ArrayList<>(parent1.getMS());
                List<Integer> MS2 = new ArrayList<>(parent2.getMS());

//				debug("交叉前 OS1: " + OS1);
//                debug("交叉前 OS2: " + OS2);
//                debug("交叉前 MS1: " + MS1);
//                debug("交叉前 MS2: " + MS2);

                if (r.nextDouble() < Parameters.OS_CROSSOVER_RATIO) {
//                	debug("执行 POX 交叉");
                    POXCrossover(OS1, OS2);
                } else {
//                	debug("执行 JBX 交叉");
                    JBXCrossover(OS1, OS2);
                }

                // Perform two-point crossover for MS
//                debug("执行 MS 两点交叉");
                TwoPointCrossover(MS1, MS2);

                // replace parent by crossover offspring
                // adjust MS by compatibility
//                debug("调整 MS 以确保兼容性");
                MS1 = Utility.compatibleAdjust(MS1, OS1);
                MS2 = Utility.compatibleAdjust(MS2, OS2);
                parents[i].setOS(OS1);
                parents[i].setMS(MS1);
                parents[i + 1].setOS(OS2);
                parents[i + 1].setMS(MS2);
//				debug("交叉后 OS1: " + OS1);
//                debug("交叉后 OS2: " + OS2);
//                debug("交叉后 MS1: " + MS1);
//                debug("交叉后 MS2: " + MS2);
            }
        }
//        debug("交叉操作完成");
    }

    /**
     * Description: precedence operation crossover(POX)
     */
    // 对于只有单个job的crossover该如何解决呢?
    private static void POXCrossover(List<Integer> OS1, List<Integer> OS2) {

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

//		debug("POX 划分工作集 J1: " + J1);
//        debug("POX 划分工作集 J2: " + J2);
        // Log the contents of J1 and J2

        List<Integer> Off1 = new ArrayList<>(Collections.nCopies(OS1.size(), -1));
        List<Integer> Off2 = new ArrayList<>(Collections.nCopies(OS2.size(), -1));

        // Allocate operations belonging to J1 in P1 to O1 and J1 in P2 to O2
        for (int i = 0; i < OS1.size(); i++) {
            int job1 = problemSetting.getOpToJob().get(OS1.get(i));
            int job2 = problemSetting.getOpToJob().get(OS2.get(i));

            if (J1.contains(job1)) {
                Off1.set(i, OS1.get(i));
//                debug("从 P1 保持位置 " + i + " 的 J1 操作: " + OS1.get(i) + " (工作 " + job1 + ")");
            }

            if (J1.contains(job2)) {
                Off2.set(i, OS2.get(i));
//                debug("从 P2 保持位置 " + i + " 的 J1 操作: " + OS2.get(i) + " (工作 " + job2 + ")");
            }
        }


//		debug("J1 操作分配后 Off1: " + Off1);
//        debug("J1 操作分配后 Off2: " + Off2);
        // Log the intermediate offspring after placing J1 operations

        // Fill the empty positions of O1 with operations belonging to J2 in P2 sequentially
        int index1 = 0;
        for (int op : OS2) {
            int job = problemSetting.getOpToJob().get(op);
            if (J2.contains(job)) {
                while (index1 < Off1.size() && Off1.get(index1) != -1) {
                    index1++;
                }
                if (index1 < Off1.size()) {
                    Off1.set(index1, op);
//                    debug("从 P2 添加 J2 操作到 Off1 位置 " + index1 + ": " + op + " (工作 " + job + ")");
                }
            }
        }

        // Log the offspring after filling with J2 operations from OS2
//        System.out.println("Off1 after filling with J2 operations from Parent 2: " + Off1.toString());

        // Fill the empty positions of O2 with operations belonging to J2 in P1 sequentially
        int index2 = 0;
        for (int op : OS1) {
            int job = problemSetting.getOpToJob().get(op);
            if (J2.contains(job)) {
                while (index2 < Off2.size() && Off2.get(index2) != -1) {
                    index2++;
                }
                if (index2 < Off2.size()) {
                    Off2.set(index2, op);
//                    debug("从 P1 添加 J2 操作到 Off2 位置 " + index2 + ": " + op + " (工作 " + job + ")");
                }
            }
        }

//        debug("最终 Off1: " + Off1);
//        debug("最终 Off2: " + Off2);

        // Log the offspring after filling with J2 operations from OS1

        // Update the original parents with the new offspring
        OS1.clear();
        OS1.addAll(Off1);
        OS2.clear();
        OS2.addAll(Off2);

        // Final log
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

//        debug("JBX 划分工作集 J1: " + J1);
//        debug("JBX 划分工作集 J2: " + J2);

        List<Integer> O1 = new ArrayList<>(Collections.nCopies(OS1.size(), -1));
        List<Integer> O2 = new ArrayList<>(Collections.nCopies(OS2.size(), -1));

        // Allocate operations belonging to J1 in P1 to O1 and J2 in P2 to O2
        for (int i = 0; i < OS1.size(); i++) {
            int job1 = problemSetting.getOpToJob().get(OS1.get(i));
            int job2 = problemSetting.getOpToJob().get(OS2.get(i));

            if (J1.contains(job1)) {
                O1.set(i, OS1.get(i));
            }

            if (J2.contains(job2)) {
                O2.set(i, OS2.get(i));
            }
        }

//        debug("初步分配后 O1: " + O1);
//        debug("初步分配后 O2: " + O2);

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


//		debug("填充后 O1: " + O1);
//        debug("填充后 O2: " + O2);
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

//        debug("MS 两点交叉开始");
//        debug("交叉前 MS1: " + MS1);
//        debug("交叉前 MS2: " + MS2);

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
//        double maxPossibleDistance = (isOS ? population.get(0).getOS().size() : population.get(0).getMS().size());
        // 更精确的归一化方式
        double maxPossibleDistance = Math.max(population.get(0).getOS().size(), population.get(0).getMS().size()) * 2;
        double normalizedDiversity = avgDistance / maxPossibleDistance;


        // 确保归一化后的多样性在 [0, 1] 范围内
        return Math.max(0.0, Math.min(1.0, normalizedDiversity));
    }
    /**
     * Description: mutation operation for OS and MS chromosome
     */
    public static void Mutation(Chromosome[] parents) {
        int num = parents.length;
        List<Chromosome> parentList = Arrays.asList(parents);
        Set<Integer> nonTcmbOps = problemSetting.getNonTcmbOps();
        Set<Integer> tcmbOps = problemSetting.getTcmbOps();
        int[][] distanceMatrix = problemSetting.getDistanceMatrix();

        double osDiversity = calculateSampledDiversity(parentList, true, (int)(num * Parameters.DIVERSITY_SAMPLE_RATIO));
        double msDiversity = calculateSampledDiversity(parentList, false, (int)(num * Parameters.DIVERSITY_SAMPLE_RATIO));

        double osBaseRate = Parameters.OS_MUTATION_RATE;
        double osMaxRate = Parameters.OS_MAX_MUTATION_RATE;
        double msBaseRate = Parameters.MS_MUTATION_RATE;
        double msMaxRate = Parameters.MS_MAX_MUTATION_RATE;


        double osMutationRate = calculateSechMutationRate(osDiversity, osBaseRate, osMaxRate, 6.0, 0.5);
        double msMutationRate = calculateSechMutationRate(msDiversity, msBaseRate, msMaxRate, 18.0, 0.54);

//        System.out.println("OS 变异率: " + osMutationRate);
//        System.out.println("MS 变异率: " + msMutationRate);


        for (int i = 0; i < num; i++){
            Chromosome o = parents[i];
            // OS mutation
//            if (r.nextDouble() < Parameters.OS_MUTATION_RATE) {
            if (r.nextDouble() < osMutationRate) {
                List<Integer> newOS = new ArrayList<>(o.getOS());
//                System.out.println("Before mutation: " + newOS);
                if (r.nextBoolean()) {
                    SwappingMutation(newOS);
//                    System.out.println("染色体 " + i + " 执行 SwappingMutation.");
                } else {
                    InsertionMutation(newOS);
//                    System.out.println("染色体 " + i + " 执行 InsertionMutation.");
                }
//                System.out.println("After mutation: " + newOS);

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
            Map<Integer, Integer> origin = o.getDelay();
            if (r.nextDouble() < Parameters.DELAY_MUTATION_RATE){
                Map<Integer,Integer> maxDelay = new HashMap<>();
                for (TCMB tcmb : problemSetting.getTCMBList()) {
                    int opA = tcmb.getOp1();
                    int opB = tcmb.getOp2();

                    int endA = o.getSchedule().getStartTimes().get(opA) + problemSetting.getProcessingTime()[opA - 1];
                    int startB = o.getSchedule().getStartTimes().get(opB);

                    int timeLag = startB - endA;

                    int originDelay = o.getDelay().getOrDefault(opA, 0);

                    if (timeLag <= tcmb.getTimeConstraint()) {
                        if (r.nextDouble() < Parameters.DELAY_WITHDRAW_PROBABILITY) {
                            // 以一定概率回溯
                            maxDelay.put(opA, 0);
//                            maxDelay.put(opA, Math.max(0, r.nextInt(Math.max(1, originDelay))));
//                            maxDelay.put(opA, Math.max(0, originDelay - r.nextInt(5)));
                        } else {

                            // 保持原有延迟
                            if(!maxDelay.containsKey(opA)){
                                maxDelay.put(opA, originDelay);
                            }
                        }
                    }
                    if (timeLag > tcmb.getTimeConstraint()) {
//                    else {
                        // 叠加策略或直接覆盖
//                        int originDelay = o.getDelay().getOrDefault(opA, 0);
                        double delayStdDev = (timeLag - tcmb.getTimeConstraint()) / 2.0;
//                        double delayStdDev = (timeLag - tcmb.getTimeConstraint()) / 3.0;
                        int newDelay = Math.min((int) Math.round((1 - r.nextGaussian()) * delayStdDev), timeLag - tcmb.getTimeConstraint());

                        if (newDelay > 0) {
                            // 叠加延迟
                            int curDelay = maxDelay.getOrDefault(opA, Integer.MAX_VALUE);
                            maxDelay.put(opA, Math.min(originDelay + newDelay,curDelay));
                        } else {
                            // 保持原始延迟
                            maxDelay.put(opA, originDelay);
                        }
                    }

                }

                // 这里需要考虑原先的非TCMB op
                o.setDelay(maxDelay);
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

    }


    private static void MachineReassignmentMutation(List<Integer> MS, List<Integer> OS) {
        int length = MS.size();
        double msMutationRatio = Parameters.MACHINE_MUTATION_RATIO;
        int h = (int) (msMutationRatio * length);

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
