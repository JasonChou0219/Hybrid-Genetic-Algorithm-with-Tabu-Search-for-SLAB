package com.zihao.GA_TS_SLAB.Plot;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import com.zihao.GA_TS_SLAB.GA.Chromosome;
import com.zihao.GA_TS_SLAB.GA.Schedule;
import com.zihao.GA_TS_SLAB.GA.Utility;
import com.zihao.GA_TS_SLAB.GA.HybridGA;

import javax.print.attribute.standard.RequestingUserName;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;


/**
 * Description: Plot Gantt chart for a schedule solution
 */
public class GanttChartPlot {
    public static void main(String[] args) {

//        File parentDir = new File("src/Dataset/Gu2016/N1");
        File parentDir = new File("src/Dataset/Gu2016/N5");
//        File parentDir = new File("src/Dataset/qPCR/N5");
//        File parentDir = new File("src/Dataset/Test");
//        File parentDir = new File("src/Dataset/qPCR_RNAseq/N5_N5");
//        File parentDir = new File("src/Dataset/RNAseq/N5");


        Input input = new Input(parentDir);
        input.getProblemDesFromFile();
//        input.testOutput();
        HybridGA hybridGA = new HybridGA();
        // 利用cp solver
        Schedule schedule = hybridGA.solve();
//        Schedule schedule = hybridGA.CPsolve();
//        Map<Integer, Integer>map =  schedule.getStartTimes();
//        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
//            System.out.println(
//                    "Operation " + entry.getKey() + " starts at " + entry.getValue() + ".");
//        }

//        List<Integer> OS = new ArrayList<>(List.of(1, 3, 5, 4, 10, 2, 7, 8, 9, 6, 11, 12, 13, 14, 16, 15, 17));
//        List<Integer> MS = new ArrayList<>(List.of(5, 5, 5, 3, 6, 2, 6, 5, 1, 4, 6, 2, 5, 4, 5, 6, 2));
//        Chromosome chromosome = new Chromosome(OS, MS);
//        Chromosome chromosome = new Chromosome(new Random());
//        Schedule schedule = chromosome.getSchedule();
//        Schedule schedule = chromosome.decode();
//        System.out.println("The fitness is " + chromosome.getFitness());


        File scheduleFile = new File("src/com/zihao/GA_TS_SLAB/Plot/schedule.csv");
        try (FileWriter writer = new FileWriter(scheduleFile)) {
            writer.append("Operation,Machine,Start,End\n");
            Map<Integer, Integer> startTimes = schedule.getStartTimes();
            Map<Integer, Integer> assignedMachine = schedule.getAssignedMachine();  // 改用getAssignedMachine
            int[] processingTimes = ProblemSetting.getInstance().getProcessingTime();

            // 创建一个按机器分组的Map
            Map<Integer, List<Integer>> machineGroups = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : assignedMachine.entrySet()) {
                int operation = entry.getKey();
                int machine = entry.getValue();
                machineGroups.computeIfAbsent(machine, k -> new ArrayList<>()).add(operation);
            }

            // 对每个机器组的操作按开始时间排序并写入文件
            for (Map.Entry<Integer, List<Integer>> entry : machineGroups.entrySet()) {
                int machine = entry.getKey();
                List<Integer> operations = entry.getValue();
                // 按开始时间排序
                operations.sort(Comparator.comparingInt(startTimes::get));

                for (int operation : operations) {
                    int start = startTimes.get(operation);
                    int end = start + processingTimes[operation - 1];  // 注意这里使用operation-1
                    writer.append(operation + "," + machine + "," + start + "," + end + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Schedule exported to " + scheduleFile.getAbsolutePath());
//
//
        File adjacencyCsvFile = new File("src/com/zihao/GA_TS_SLAB/Plot/adjacency_list.csv");
        try (FileWriter writer = new FileWriter(adjacencyCsvFile)) {
            writer.append("Operation,AdjacencyList\n");
            Map<Integer, List<Integer>> adjacencyList = ProblemSetting.getInstance().getDag().getAdjacencyList();

            for (Map.Entry<Integer, List<Integer>> entry : adjacencyList.entrySet()) {
                int operation = entry.getKey();
                List<Integer> neighbors = entry.getValue();
                writer.append(operation + "," + neighbors.toString().replace("[", "").replace("]", "").replace(" ", "") + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//
//
//        // Call python script
        try {
            String pythonScriptPath = "src/com/zihao/GA_TS_SLAB/Plot/plot_gantt.py"; // 确保路径正确
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/python3", "-c", "import sys; print(sys.executable)");
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String pythonPath = "";
            while ((line = reader.readLine()) != null) {
                pythonPath += line;
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Python executable: " + pythonPath);
            } else {
                System.out.println("Failed to get Python executable with exit code: " + exitCode);
            }

            pb = new ProcessBuilder(pythonPath, pythonScriptPath);
            Map<String, String> env = pb.environment();
            env.put("PYTHONPATH", "/usr/bin/python3");  // 根据需要设置路径
            pb.directory(new File(".")); // 设置工作目录为当前目录
            process = pb.start();


            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            System.out.println("Standard output:");
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Standard error:");
            while ((line = stdError.readLine()) != null) {
                System.err.println(line);
            }

            exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Python script executed successfully.");
            } else {
                System.out.println("Python script execution failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
//
//
//        callPythonScript("src/com/zihao/GA_TS_SLAB/Plot/plot_clustering.py");
//        callPythonScript("src/com/zihao/GA_TS_SLAB/Plot/DBSCAN.py");

    }

//    private static void callPythonScript(String pythonScriptPath) {
//        try {
//            // 获取Python解释器的路径
//            ProcessBuilder pb = new ProcessBuilder("python3", "-c", "import sys; print(sys.executable)");
//            Process process = pb.start();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            String pythonPath = "";
//            while ((line = reader.readLine()) != null) {
//                pythonPath += line;
//            }
//
//            int exitCode = process.waitFor();
//            if (exitCode == 0) {
//                System.out.println("Python executable: " + pythonPath);
//            } else {
//                System.out.println("Failed to get Python executable with exit code: " + exitCode);
//                return;
//            }
//
//            // 调用 plot_clustering.py
//            pb = new ProcessBuilder(pythonPath, pythonScriptPath);
//            pb.directory(new File(".")); // 设置工作目录为当前目录
//            process = pb.start();
//
//            // 读取标准输出和标准错误
//            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//
//            System.out.println("Standard output:");
//            while ((line = stdInput.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            System.out.println("Standard error:");
//            while ((line = stdError.readLine()) != null) {
//                System.err.println(line);
//            }
//
//            exitCode = process.waitFor();
//            if (exitCode == 0) {
//                System.out.println("Python script executed successfully.");
//            } else {
//                System.out.println("Python script execution failed with exit code: " + exitCode);
//            }
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
    private static void callPythonScript(String pythonScriptPath) {
        try {
            // 获取Python解释器的路径
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/python3", "-c", "import sys; print(sys.executable)");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String pythonPath = "";
            while ((line = reader.readLine()) != null) {
                pythonPath += line;
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Python executable: " + pythonPath);
            } else {
                System.out.println("Failed to get Python executable with exit code: " + exitCode);
                return;
            }

            // 调用 DBSCAN.py
            pb = new ProcessBuilder(pythonPath, pythonScriptPath);
            pb.directory(new File(".")); // 设置工作目录为当前目录
            process = pb.start();

            // 读取标准输出和标准错误
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            System.out.println("Standard output:");
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }

            System.out.println("Standard error:");
            while ((line = stdError.readLine()) != null) {
                System.err.println(line);
            }

            exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Python script executed successfully.");
            } else {
                System.out.println("Python script execution failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}





