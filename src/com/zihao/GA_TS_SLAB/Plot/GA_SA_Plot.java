package com.zihao.GA_TS_SLAB.Plot;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.GA.HybridGA;
import com.zihao.GA_TS_SLAB.GA.Schedule;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Description: Plot Gantt chart for a schedule solution
 * Modified to run the algorithm one time per execution
 */
public class GA_SA_Plot {
    public static void main(String[] args) {
        // 运行编号将根据result.tsv文件中的记录数自动确定
        String algorithmName = "Genetic Algorithm and Simulated Annealing";
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

        // 创建或追加汇总结果文件
        File summaryResultFile = new File(outputPackage + "/" + dataset + "-result.tsv");
        boolean newFile = !summaryResultFile.exists();

        // 确定当前运行编号
        int runNumber = 1;
        if (!newFile) {
            // 读取已有的结果文件，计算当前应该是第几次运行
            try (BufferedReader reader = new BufferedReader(new FileReader(summaryResultFile))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    if (count > 0) { // 跳过表头
                        runNumber++;
                    }
                    count++;
                }
            } catch (IOException e) {
                System.err.println("Error reading result file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Determined current run number: " + runNumber);

        try (FileWriter writer = new FileWriter(summaryResultFile, true)) { // true表示追加模式
            // 如果文件是新文件，需要写入表头
            if (newFile) {
                writer.append("Run_Number\tTimestamp\tAlgorithm\tDataset\tMakespan\tExecution_Time_Sec\tExecution_Time_Ms\n");
            }

            System.out.println("Starting execution #" + runNumber);

            // 记录算法开始时间
            long startTime = System.currentTimeMillis();

            // 利用CP solver
            HybridGA hybridGA = new HybridGA();
//            Schedule schedule = hybridGA.CPsolve();
            Schedule schedule = hybridGA.solve();

            // 记录算法结束时间
            long endTime = System.currentTimeMillis();
            long executionTimeMs = endTime - startTime;
            long executionTimeSec = executionTimeMs / 1000;

            // 创建当前运行的调度文件
            File solutionFile = new File(outputPackage + "/" + dataset + "-schedule-" + runNumber + ".tsv");
            int makespan = 0; // 用于记录调度方案的最大完成时间

            try (FileWriter scheduleWriter = new FileWriter(solutionFile)) {
                scheduleWriter.append("Job_ID\tOperation_ID\tStart\tEnd\tMachine_ID\n");
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

                    scheduleWriter.append(jobId + "\t" + operation + "\t" + start + "\t" + end + "\t" + machine + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 时间戳
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            String timestamp = dateFormat.format(new Date());

            // 将结果写入汇总文件
            writer.append(runNumber + "\t" +
                    timestamp + "\t" +
                    algorithmName + "\t" +
                    dataset + "\t" +
                    makespan + "\t" +
                    executionTimeSec + "\t" +
                    executionTimeMs + "\n");

            // 保证写入磁盘
            writer.flush();

            System.out.println("Execution #" + runNumber + " completed. Makespan: " + makespan + ", Execution time: " + executionTimeSec + " sec");

            // 如果当前是第5次运行，生成最佳调度方案的Gantt图
            if (runNumber == 5) {
                // 查找所有运行中的最佳结果并生成Gantt图
                findBestRunAndGenerateGanttChart(outputPackage, dataset, algorithmName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查找最佳运行并生成Gantt图
     */
    private static void findBestRunAndGenerateGanttChart(String outputPackage, String dataset, String algorithmName) {
        try {
            File resultFile = new File(outputPackage + "/" + dataset + "-result.tsv");
            if (!resultFile.exists()) {
                System.err.println("Result file not found. Cannot generate Gantt chart.");
                return;
            }

            // 找出最佳运行
            int bestRun = -1;
            int bestMakespan = Integer.MAX_VALUE;

            try (BufferedReader reader = new BufferedReader(new FileReader(resultFile))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }

                    String[] parts = line.split("\t");
                    if (parts.length >= 5) {
                        int run = Integer.parseInt(parts[0]);
                        int makespan = Integer.parseInt(parts[4]);
                        if (makespan < bestMakespan) {
                            bestMakespan = makespan;
                            bestRun = run;
                        }
                    }
                }
            }

            if (bestRun == -1) {
                System.err.println("Could not determine best run from results file.");
                return;
            }

            System.out.println("Best run identified: Run #" + bestRun + " with makespan: " + bestMakespan);

            // 读取最佳运行的调度文件
            File scheduleFile = new File(outputPackage + "/" + dataset + "-schedule-" + bestRun + ".tsv");
            if (!scheduleFile.exists()) {
                System.err.println("Schedule file for best run not found.");
                return;
            }

            // 构建最佳运行的Schedule对象
            Schedule bestSchedule = readScheduleFromFile(scheduleFile);
            if (bestSchedule == null) {
                System.err.println("Failed to read schedule from file.");
                return;
            }

            // 创建用于绘图的CSV文件
            createGanttChartData(bestSchedule, outputPackage + "/schedule.csv");

            // 标记这是最佳运行，并保存最佳运行的编号
            try (FileWriter bestRunWriter = new FileWriter(outputPackage + "/best_run.txt")) {
                bestRunWriter.write("Best Run: " + bestRun + "\n");
                bestRunWriter.write("Makespan: " + bestMakespan + "\n");
            }

            // 调用Python脚本生成Gantt图
            try {
                callPythonScript("src/com/zihao/GA_TS_SLAB/Plot/plot_gantt.py", algorithmName, dataset);
            } catch (Exception e) {
                System.err.println("Failed to generate Gantt chart: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件中读取Schedule对象
     */
    private static Schedule readScheduleFromFile(File file) {
        Map<Integer, Integer> startTimes = new HashMap<>();
        Map<Integer, Integer> assignedMachine = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length >= 5) {
                    int operationId = Integer.parseInt(parts[1]);
                    int start = Integer.parseInt(parts[2]);
                    int machineId = Integer.parseInt(parts[4]);

                    startTimes.put(operationId, start);
                    assignedMachine.put(operationId, machineId);
                }
            }

            return new Schedule(startTimes, assignedMachine);
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建用于Gantt图的CSV数据文件
     */
    private static void createGanttChartData(Schedule schedule, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Operation,Job,Machine,MachineName,Start,End\n");
            Map<Integer, Integer> startTimes = schedule.getStartTimes();
            Map<Integer, Integer> assignedMachine = schedule.getAssignedMachine();
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
                    int end = start + processingTimes[operation - 1];
                    int jobId = opToJob.getOrDefault(operation, 0);
                    String machineName = "Unknown";
                    if (machine > 0 && machine <= machineNames.length) {
                        machineName = machineNames[machine - 1];
                    }
                    writer.append(operation + "," + jobId + "," + machine + "," + machineName + "," + start + "," + end + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        File scheduleFile = new File("src/com/zihao/GA_TS_SLAB/Plot/schedule.csv");
        try (FileWriter writer = new FileWriter(scheduleFile)) {
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
                    int jobId = opToJob.getOrDefault(operation, 0);  // Default to 0 if not found
                    String machineName = "Unknown";
                    if (machine > 0 && machine <= machineNames.length) {
                        machineName = machineNames[machine - 1];
                    }
                    writer.append(operation + "," + jobId + "," + machine + "," + machineName + "," + start + "," + end + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 调用Python脚本生成Gantt图
     */
    private static void callPythonScript(String pythonScriptPath, String algorithmName, String datasetName) {
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
                System.out.println("Using algorithm: " + algorithmName);
                System.out.println("Using dataset: " + datasetName);
            } else {
                System.out.println("Failed to get Python executable with exit code: " + exitCode);
                return;
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
    }

    /**
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