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

    public static Chromosome[] ElitistSelection(Chromosome[] parents) {
        int popNum = Parameters.POP_NUM;
        double elitRate = Parameters.ELIT_RATIO;

        int elitNum = (int) (elitRate * popNum);
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
    public static Chromosome[] TournamentSelection(Chromosome[] parents) {
        int popNum = Parameters.POP_NUM;
        double elitRate = Parameters.ELIT_RATIO;

        int elitNum = (int) (elitRate * popNum);
        Chromosome[] tournamentSelection = new Chromosome[popNum - elitNum];


        for (int i = 0; i < popNum - elitNum; i++) {
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


    public static Chromosome[] RouletteWheelSelection(Chromosome[] parents) {

        int popNum = Parameters.POP_NUM;
        double elitRate = Parameters.ELIT_RATIO;

        int elitNum = (int) (elitRate * popNum);
        Chromosome[] wheelSelection = new Chromosome[popNum - elitNum];

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
        }

        // Selection of individuals based on roulette wheel
        for (int i = 0; i < popNum - elitNum; i++) {
            double rand = r.nextDouble();
            for (int j = 0; j < popNum; j++) {
                if (rand <= cumulativeProbabilities[j]) {
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

                if (r.nextDouble() < Parameters.OS_CROSSOVER_RATIO) {
                    POXCrossover(OS1, OS2);
                } else {
                    JBXCrossover(OS1, OS2);
                }

                // Perform two-point crossover for MS
                TwoPointCrossover(MS1, MS2);

                // replace parent by crossover offspring
                // adjust MS by compatibility
                MS1 = Utility.compatibleAdjust(MS1, OS1);
                MS2 = Utility.compatibleAdjust(MS2, OS2);
                parents[i].setOS(OS1);
                parents[i].setMS(MS1);
                parents[i + 1].setOS(OS2);
                parents[i + 1].setMS(MS2);
//                System.out.println("Performed crossover between chromosome " + i + " and " + (i + 1));
            }
        }
    }

    /**
     * Description: precedence operation crossover(POX)
     */
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

        List<Integer> Off1 = new ArrayList<>(Collections.nCopies(OS1.size(), -1));
        List<Integer> Off2 = new ArrayList<>(Collections.nCopies(OS2.size(), -1));

        // Allocate operations belonging to J1 in P1 to O1 and J1 in P2 to O2
        for (int i = 0; i < OS1.size(); i++) {
            if (J1.contains(problemSetting.getOpToJob().get(OS1.get(i)))) {
                Off1.set(i, OS1.get(i));
            }
            if (J1.contains(ProblemSetting.getInstance().getOpToJob().get(OS2.get(i)))) {
                Off2.set(i, OS2.get(i));
            }
        }

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

        OS1.clear();
        OS1.addAll(Off1);
        OS2.clear();
        OS2.addAll(Off2);
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

        List<Integer> Off1 = new ArrayList<>(MS1);
        List<Integer> Off2 = new ArrayList<>(MS2);

        // Swap the segments between p1 and p2
        for (int i = p1; i <= p2; i++) {
            Off1.set(i, MS2.get(i));
            Off2.set(i, MS1.get(i));
        }

        MS1.clear();
        MS1.addAll(Off1);
        MS2.clear();
        MS2.addAll(Off2);
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

    private static double calculateDiversity(List<Chromosome> population, boolean isOS) {
        int popSize = population.size();
        int seqLength = isOS ? population.get(0).getOS().size() : population.get(0).getMS().size();
        int totalDistance = 0;

        for (int i = 0; i < popSize; i++) {
            for (int j = i + 1; j < popSize; j++) {
                List<Integer> seq1 = isOS ? population.get(i).getOS() : population.get(i).getMS();
                List<Integer> seq2 = isOS ? population.get(j).getOS() : population.get(j).getMS();
                totalDistance += hammingDistance(seq1, seq2);
            }
        }

        int maxPossibleDistance = seqLength * (popSize * (popSize - 1)) / 2;
        return totalDistance / (double) maxPossibleDistance;
    }


    /**
     * Description: mutation operation for OS and MS chromosome
     */
    // remained to be done: consider different mutation rate for OS and MS
    // remained to be done: adaptive mutation rate
    public static void Mutation(Chromosome[] parents) {
        int num = parents.length;
        List<Chromosome> parentList = Arrays.asList(parents);

        double osDiversity = calculateDiversity(parentList, true);
        double msDiversity = calculateDiversity(parentList, false);

        double osBaseRate = Parameters.OS_MUTATION_RATE;
        double osMaxRate = Parameters.OS_MAX_MUTATION_RATE;
        double msBaseRate = Parameters.MS_MUTATION_RATE;
        double msMaxRate = Parameters.MS_MAX_MUTATION_RATE;

        double osMutationRate = osBaseRate + (1 - osDiversity) * (osMaxRate - osBaseRate);
        double msMutationRate = msBaseRate + (1 - msDiversity) * (msMaxRate - msBaseRate);


        for (int i = 0; i < num; i++){
            Chromosome o = parents[i];
            // OS mutation
//            if (r.nextDouble() < Parameters.OS_MUTATION_RATE) {
            if (r.nextDouble() < osMutationRate) {
                List<Integer> newOS = new ArrayList<>(o.getOS());
                if (r.nextBoolean()) {
                    SwappingMutation(newOS);
                } else {
                    InsertionMutation(newOS);
                }
                o.setOS(newOS);
            }
            // MS mutation
//            if (r.nextDouble() < Parameters.MS_MUTATION_RATE) {
            if (r.nextDouble() < msMutationRate) {
                    List<Integer> newMS = new ArrayList<>(o.getMS());
                    MachineReassignmentMutation(newMS, o.getOS());
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
                        int newDelay = (int) Math.round(r.nextGaussian() * delayStdDev + delayMean);
                        if (newDelay > 0) {
                            int curDelay = maxDelay.getOrDefault(opA, 0);
                            if (curDelay != 0){
                                maxDelay.put(opA, Math.min(curDelay, newDelay));
                            }
                        }
                    }
                }

                for (Map.Entry<Integer, Integer> entry : maxDelay.entrySet()) {
                    int opA = entry.getKey();
                    int delay = entry.getValue();
                    o.getDelay().put(opA, delay);
                }
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
        r = new Random();

        for (int i = 0; i < h; i++) {
            int p = r.nextInt(length);
            int operation = OS.get(p);
            List<Integer> compatibleMachines = ProblemSetting.getInstance().getOpToCompatibleList().get(operation);
            MS.set(p, compatibleMachines.get(r.nextInt(compatibleMachines.size())));
        }
    }

}
