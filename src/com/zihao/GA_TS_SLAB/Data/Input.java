package com.zihao.GA_TS_SLAB.Data;

import java.io.*;
import java.util.*;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

/**
 * Description: input the data from dataset
 */
public class Input {
    private final File parentDir;
    ProblemSetting problemSetting;

    public Input(File parentDir) {
        this.parentDir = parentDir;
        this.problemSetting = ProblemSetting.getInstance();
    }

    public ProblemSetting getProblemSetting(){
        return problemSetting;
    }

    private BufferedReader getBufferedReader(File file) throws IOException {
        return new BufferedReader(new FileReader(file));
    }

    /**
     * Description: read dependency data from dataset and
     * form a directed acyclic graph by using adjacencyList
     * output: a map from operation to its directed adjacent neighbour
     */
    public void inputDep(File filePath){
        try (BufferedReader reader = getBufferedReader(filePath)) {
            String line;
            // Skip the header
            line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\t");
                int from = Integer.parseInt(fields[1]);
                int to = Integer.parseInt(fields[2]);
                problemSetting.getDag().addEdge(from, to);
                problemSetting.getReverseDag().addEdge(to, from);
            }
        }   catch (IOException e) {
            e.printStackTrace();
        }
        //test output
//        problemSetting.getDag().printGraph();
    }

    /**
     * Description: read machine data from dataset
     * Input: filepath of machines.tsv
     * Output:
     * total machine number: int machineNum
     * map from type to id list: HashMap machineTypeToList
     */
    public void inputMachine(File filePath) {
        try (BufferedReader reader = getBufferedReader(filePath)) {
            String line;
            Map<Integer, List<Integer>> machineTypeToList = new HashMap<>();
            int totalMachineNum = 0;
            List<String> machineNamesList = new ArrayList<>();

            // this is for file header
            line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\t");
                int machineID = Integer.parseInt(fields[0]);
                int machineType = Integer.parseInt(fields[1]);
                String machineName = fields[2];

                totalMachineNum++;
                machineTypeToList.computeIfAbsent(machineType, k -> new ArrayList<>()).add(machineID);
                machineNamesList.add(machineName);
            }
            problemSetting.setMachineNum(totalMachineNum);
            problemSetting.setMachineTypeToList(machineTypeToList);

            String[] machineNamesArray = new String[machineNamesList.size()];
            machineNamesList.toArray(machineNamesArray);
            problemSetting.setMachineNames(machineNamesArray);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Description: read operation data from dataset
     * Input: filepath of operations.tsv
     * Output:
     * total operation number: int opNum
     * total job number: int jobNum
     * map from operation id its compatible machine list: HashMap opToCompatibleList
     * list of operation processing time: int[] processingTime
     * number of operations for each job: int[] opNumEachJob
     */
    public void inputOp(File filePath) {
        int totalOpNum = 0;
        Map<Integer, List<Integer>> opToCompatibleList = new HashMap<>();
        List<Integer> processingTimeList = new ArrayList<>();
        Map<Integer, Integer> jobToOpCountMap = new HashMap<>();
        Map<Integer, Integer> opToJob = new HashMap<>();

        try (BufferedReader reader = getBufferedReader(filePath)) {
            String line;
            // for file header
            line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\t");
                int jobID = Integer.parseInt(fields[0]);
                int operationID = Integer.parseInt(fields[1]);
                int compatibleMachineType = Integer.parseInt(fields[2]);
                int processTime = Integer.parseInt(fields[3]);

                totalOpNum++;
                processingTimeList.add(processTime);

                // count the number of operations for each job
                jobToOpCountMap.put(jobID, jobToOpCountMap.getOrDefault(jobID, 0) + 1);
                opToJob.put(operationID,jobID);
                // map compatible machine id list to operation id
                List<Integer> compatibleList = problemSetting.getMachineTypeToList().get(compatibleMachineType);

                opToCompatibleList.put(operationID,compatibleList);
            }

            int totalJobNum = jobToOpCountMap.size();


            // transform Map jobToOpCountMap into int[]
            int maxJobID = jobToOpCountMap.keySet().stream().max(Integer::compare).orElse(0);
            int[] opNumEachJob = new int[maxJobID];
            for (Map.Entry<Integer, Integer> entry : jobToOpCountMap.entrySet()) {
                opNumEachJob[entry.getKey() - 1] = entry.getValue();
            }
            // transform List processingTimeList into int[]
            int[] processingTime = processingTimeList.stream().mapToInt(i -> i).toArray();

            problemSetting.setJobNum(totalJobNum);
            problemSetting.setOpNumEachJob(opNumEachJob);
            problemSetting.setTotalOpNum(totalOpNum);
            problemSetting.setProcessingTime(processingTime);
            problemSetting.setOpToCompatibleList(opToCompatibleList);
            problemSetting.setOpToJob(opToJob);

            int[][] orderMatrix = new int[totalOpNum + 1][totalOpNum + 1];

            problemSetting.buildOrderMatrix(problemSetting.getDag(), totalOpNum);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Description: read tcmb data from dataset
     * Input: filepath of tcmb.tsv
     * Output: TCMB tuples
     */
    public void inputTCMB(File filePath) {
        List<TCMB> tcmbList = new ArrayList<>();
        try (BufferedReader reader = getBufferedReader(filePath)) {
            String line;
            // Skip file header
            line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\t");
                try {
                    int jobID = Integer.parseInt(fields[0]);
                    int op1 = Integer.parseInt(fields[1]);
                    String point1 = fields[2];
                    int op2 = Integer.parseInt(fields[3]);
                    String point2 = fields[4];
                    int timeConstraint = Integer.parseInt(fields[5]);

                    tcmbList.add(new TCMB(op1, op2, timeConstraint));
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line due to number format error: " + Arrays.toString(fields));
                }
            }
            problemSetting.setTCMBList(tcmbList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Test output function to print all problem setting information
     */
    public void testOutput() {
        System.out.println("Total operation number: " + problemSetting.getTotalOpNum());
        System.out.println("Total job number: " + problemSetting.getJobNum());
        System.out.println("Total machine number: " + problemSetting.getMachineNum());

        System.out.println("Operation to Compatible Machines:");
        for (Map.Entry<Integer, List<Integer>> entry : problemSetting.getOpToCompatibleList().entrySet()) {
            System.out.println("Operation ID: " + entry.getKey() + ", Compatible Machines: " + entry.getValue());
            System.out.println("Operation ID: " + entry.getKey() + " belong to job " +
                    problemSetting.getOpToJob().get(entry.getKey()));
        }
        System.out.println(problemSetting.getOpToCompatibleList());

        System.out.print("Processing Times: ");
        for (int time : problemSetting.getProcessingTime()) {
            System.out.print(time + " ");
        }
        System.out.println();

        System.out.print("Number of operations for each job: ");
        for (int num : problemSetting.getOpNumEachJob()) {
            System.out.print(num + " ");
        }
        System.out.println();

        System.out.println("Machine Type to Machine ID List:");
        for (Map.Entry<Integer, List<Integer>> entry : problemSetting.getMachineTypeToList().entrySet()) {
            System.out.println("Machine Type: " + entry.getKey() + ", Machine IDs: " + entry.getValue());
        }

        System.out.println("Machine Names:");
        for (int i = 0; i < problemSetting.getMachineNames().length; i++) {
            System.out.println("Machine ID: " + (i + 1) + ", Machine Name: " + problemSetting.getMachineNames()[i]);
        }

        System.out.println("TCMB List:");
        for (TCMB tcmb : problemSetting.getTCMBList()) {
            System.out.println(tcmb);
        }
    }


    /**
     * Description: get problem description from dataset
     */
    public void getProblemDesFromFile() {
        String dependencyFileName = "dependency.tsv";
        String machineFileName = "machines.tsv";
        String operationFileName = "operations.tsv";
        String TCMBFileName = "tcmb.tsv";

        inputDep(new File(parentDir, dependencyFileName));
        inputMachine(new File(parentDir, machineFileName));
        inputOp(new File(parentDir, operationFileName));
        inputTCMB(new File(parentDir, TCMBFileName));
    }


    public static void main(String[] args) {
        File parentDir = new File("src/Dataset/Gu2016/N1");
        Input input = new Input(parentDir);
        input.getProblemDesFromFile();
        input.testOutput();
    }
}