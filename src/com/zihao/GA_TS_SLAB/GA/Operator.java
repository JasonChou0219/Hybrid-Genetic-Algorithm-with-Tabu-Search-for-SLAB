package com.zihao.GA_TS_SLAB.GA;

import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
        for (int i = 0; i < elitNum; i++) {
            Chromosome o = p.get(i);
            elites[i] = new Chromosome(o);
        }
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
        }
        return tournamentSelection;
    }

    /**
     * Description: precedence operation crossover(POX) and job-based crossover (JBX) for OS chromosome,
     * two-point crossover for MS chromosome.
     */
    public static void Crossover(Chromosome[] parents) {
         int num = parents.length;

        for (int i = 0; i < num; i += 2) {
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

    /**
     * Description: mutation operation for OS and MS chromosome
     */
    // remained to be done: consider different mutation rate for OS and MS
    // remained to be done: adaptive mutation rate
    public static void Mutation(Chromosome[] parents) {
        int num = parents.length;

        for (int i = 0; i < num; i++){
            Chromosome o = parents[i];
            // OS mutation
            if (r.nextDouble() < Parameters.OS_MUTATION_RATE) {
                List<Integer> newOS = new ArrayList<>(o.getOS());
                if (r.nextBoolean()) {
                    SwappingMutation(newOS);
                } else {
                    InsertionMutation(newOS);
                }
                o.setOS(newOS);
            }
            // MS mutation
            if (r.nextDouble() < Parameters.MS_MUTATION_RATE) {
                List<Integer> newMS = new ArrayList<>(o.getMS());
                MachineReassignmentMutation(newMS, o.getOS());
                o.setMS(newMS);
            }
            // update schedule and fitness after crossover and mutation
            parents[i] = new Chromosome(o);
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
