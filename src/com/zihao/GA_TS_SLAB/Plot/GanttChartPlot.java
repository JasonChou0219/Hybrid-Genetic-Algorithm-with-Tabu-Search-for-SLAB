package com.zihao.GA_TS_SLAB.Plot;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import com.zihao.GA_TS_SLAB.GA.Schedule;
import com.zihao.GA_TS_SLAB.GA.HybridGA;

import com.zihao.GA_TS_SLAB.Solver.MILPSchedule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Description: Plot Gantt chart for a schedule solution
 */
public class GanttChartPlot {
    public static void main(String[] args) {
//        String algorithmName = "Genetic Algorithm and Simulated Annealing";
        String algorithmName = "Branch and Bound";
//        File parentDir = new File("src/Dataset/Gu2016/N1");
//        File parentDir = new File("src/Dataset/Gu2016/N5");
//        File parentDir = new File("src/Dataset/qPCR/N5");
//        File parentDir = new File("src/Dataset/Test");
        File parentDir = new File("src/Dataset/qPCR_RNAseq/N5_N5");
//        File parentDir = new File("src/Dataset/RNAseq/N5");


        // 从parentDir获取数据集名称
        String dataset = extractDatasetName(parentDir);
        System.out.println("Using dataset: " + dataset);

        // 创建Output目录下的package
        String outputPackage = "src/Output/" + algorithmName + "-" + dataset;
        File outputDir = new File(outputPackage);
        if (!outputDir.exists()) {
            if (outputDir.mkdirs()) {
                System.out.println("Created output directory: " + outputDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create output directory: " + outputDir.getAbsolutePath());
            }
        }

        Input input = new Input(parentDir);
        input.getProblemDesFromFile();
//        input.testOutput();
//        HybridGA hybridGA = new HybridGA();
        // 利用cp solver
//        Schedule schedule = hybridGA.solve();
//        Schedule schedule = hybridGA.CPsolve();
//        Map<Integer, Integer>map =  schedule.getStartTimes();
//        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
//            System.out.println(
//                    "Operation " + entry.getKey() + " starts at " + entry.getValue() + ".");
//        }


        MILPSchedule milpSolver = new MILPSchedule();
        // 记录算法开始时间
        long startTime = System.currentTimeMillis();
//        // 求解问题，获取最优调度方案
        Schedule schedule = milpSolver.solve();

        // 记录算法结束时间
        long endTime = System.currentTimeMillis();
        long executionTimeMs = endTime - startTime;
        long executionTimeSec = executionTimeMs / 1000;


        // 创建调度文件
        File solutionFile = new File(outputPackage + "/" + dataset + "-schedule.tsv");
        int makespan = 0; // 用于记录调度方案的最大完成时间

        try (FileWriter writer = new FileWriter(solutionFile)) {
            writer.append("Job_ID\tOperation_ID\tStart\tEnd\tMachine_ID\n");
            Map<Integer, Integer> startTimes = schedule.getStartTimes();
            Map<Integer, Integer> assignedMachine = schedule.getAssignedMachine();
            int[] processingTimes = ProblemSetting.getInstance().getProcessingTime();
            Map<Integer, Integer> opToJob = ProblemSetting.getInstance().getOpToJob();

            // 创建按工作和操作排序的列表
            List<Map.Entry<Integer, Integer>> operations = new ArrayList<>(startTimes.entrySet());
            operations.sort((a, b) -> {
                int jobA = opToJob.getOrDefault(a.getKey(), 0);
                int jobB = opToJob.getOrDefault(b.getKey(), 0);
                if (jobA != jobB) {
                    return Integer.compare(jobA, jobB);
                }
                return Integer.compare(a.getKey(), b.getKey());
            });

            // 写入调度数据
            for (Map.Entry<Integer, Integer> entry : operations) {
                int operation = entry.getKey();
                int start = entry.getValue();
                int end = start + processingTimes[operation - 1];
                int jobId = opToJob.getOrDefault(operation, 0);
                int machine = assignedMachine.get(operation);

                // 更新makespan
                makespan = Math.max(makespan, end);

                writer.append(jobId + "\t" + operation + "\t" + start + "\t" + end + "\t" + machine + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        // 创建结果文件
        File resultFile = new File(outputPackage + "/" + dataset + "-result.tsv");
        try (FileWriter writer = new FileWriter(resultFile)) {
            // 时间戳
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            String timestamp = dateFormat.format(new Date());

            // 表头
            writer.append("Timestamp\tAlgorithm\tDataset\tMakespan\tExecution_Time_Sec\tExecution_Time_Ms\n");

            // 数据行
            writer.append(timestamp + "\t" +
                    algorithmName + "\t" +
                    dataset + "\t" +
                    makespan + "\t" +
                    executionTimeSec + "\t" +
                    executionTimeMs + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }


        File scheduleFile = new File("src/com/zihao/GA_TS_SLAB/Plot/schedule.csv");
        try (FileWriter writer = new FileWriter(scheduleFile)) {
//            writer.append("Operation,Machine,Start,End\n");
//            writer.append("Operation,Job,Machine,Start,End\n");
            writer.append("Operation,Job,Machine,MachineName,Start,End\n");
            Map<Integer, Integer> startTimes = schedule.getStartTimes();
            Map<Integer, Integer> assignedMachine = schedule.getAssignedMachine();  // 改用getAssignedMachine
            int[] processingTimes = ProblemSetting.getInstance().getProcessingTime();
            Map<Integer, Integer> opToJob = ProblemSetting.getInstance().getOpToJob();
            String[] machineNames = ProblemSetting.getInstance().getMachineNames();

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
                    //writer.append(operation + "," + machine + "," + start + "," + end + "\n");
                    int jobId = opToJob.getOrDefault(operation, 0);  // Default to 0 if not found
                    String machineName = "Unknown";
                    if (machine > 0 && machine <= machineNames.length) {
                        machineName = machineNames[machine - 1];
                    }
                    writer.append(operation + "," + jobId + "," + machine + "," + machineName + "," + start + "," + end + "\n");
//                    writer.append(operation + "," + jobId + "," + machine + "," + start + "," + end + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            String pythonScriptPath = "src/com/zihao/GA_TS_SLAB/Plot/plot_gantt.py";

            // 从parentDir变量提取数据集名称
            String datasetName = "Unknown";
            if (parentDir != null) {
                String path = parentDir.getPath();
                if (path.contains("Dataset/")) {
                    datasetName = path.substring(path.indexOf("Dataset/") + 8);
                    datasetName = datasetName.replace('/', '_').replace('\\', '_');
                } else {
                    datasetName = parentDir.getName();
                }
            }

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
                System.out.println("Using algorithm: " + algorithmName);
                System.out.println("Using dataset: " + datasetName);
            } else {
                System.out.println("Failed to get Python executable with exit code: " + exitCode);
            }

            // 传递算法名称和数据集名称作为参数
            pb = new ProcessBuilder(pythonPath, pythonScriptPath, algorithmName, datasetName);
            Map<String, String> env = pb.environment();
            env.put("PYTHONPATH", "/usr/bin/python3");
            pb.directory(new File("."));
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
                System.out.println("Gantt chart saved as: " + algorithmName.toLowerCase().replace(' ', '_') + "-" + datasetName + ".svg");
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

//            System.out.println("Standard output:");
//            while ((line = stdInput.readLine()) != null) {
//                System.out.println(line);
//            }

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
    /*
         * 从目录路径中提取数据集名称
     */
    private static String extractDatasetName(File parentDir) {
        if (parentDir != null) {
            String path = parentDir.getPath();
            if (path.contains("Dataset/")) {
                String datasetName = path.substring(path.indexOf("Dataset/") + 8);
                // 替换路径分隔符为下划线
                datasetName = datasetName.replace('/', '-').replace('\\', '-');
                return datasetName;
            } else {
                return parentDir.getName();
            }
        }
        return "Unknown";
    }
}





