package com.zihao.GA_TS_SLAB.Plot;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.GA.Calculation;
import com.zihao.GA_TS_SLAB.GA.Chromosome;
import com.zihao.GA_TS_SLAB.GA.Schedule;
import com.zihao.GA_TS_SLAB.GA.Calculation;

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

        File parentDir = new File("src/Dataset/Gu2016/N1");
        Input input = new Input(parentDir);
        input.getProblemDesFromFile();

        List<Integer> OS = new ArrayList<>(List.of(1, 3, 5, 4, 10, 2, 7, 8, 9, 6, 11, 12, 13, 14, 16, 15, 17));
        List<Integer> MS = new ArrayList<>(List.of(5, 5, 5, 3, 6, 2, 6, 5, 1, 4, 6, 2, 5, 4, 5, 6, 2));

        Chromosome chromosome = new Chromosome(OS, MS);
        Schedule schedule = chromosome.decode();
        Calculation calculation = Calculation.getInstance();
        System.out.println("The fitness is " + calculation.calculateFitness(schedule));


        File scheduelFile = new File("src/com/zihao/GA_TS_SLAB/Plot/schedule.csv");
        try (FileWriter writer = new FileWriter(scheduelFile)) {
            writer.append("Operation,Machine,Start,End\n");
            Map<Integer, Integer> startTimes = schedule.getStartTimes();
            Map<Integer, List<Integer>> machineAssignments = schedule.getMachineAssignments();
            int[] processingTimes = ProblemSetting.getInstance().getProcessingTime();

            for (Map.Entry<Integer, List<Integer>> entry : machineAssignments.entrySet()) {
                int machine = entry.getKey();
                for (int operation : entry.getValue()) {
                    int start = startTimes.get(operation);
                    int end = start + processingTimes[operation - 1];
                    writer.append(operation + "," + machine + "," + start + "," + end + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Schedule exported to " + scheduelFile.getAbsolutePath());


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



        // Call python script
        try {
            String pythonScriptPath = "src/com/zihao/GA_TS_SLAB/Plot/plot_gantt.py"; // 确保路径正确
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", "import sys; print(sys.executable)");
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
    }
}



