package com.zihao.GA_TS_SLAB.Plot;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.GA.Chromosome;
import com.zihao.GA_TS_SLAB.GA.Schedule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class GanttChartPlot {
    public static void main(String[] args) {

        File parentDir = new File("src/Dataset/Gu2016/N1");
        Input input = new Input(parentDir);
        input.getProblemDesFromFile();


        Random random = new Random();
        Chromosome chromosome = new Chromosome(random);
        Schedule schedule = chromosome.decode();


        File csvFile = new File("schedule.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
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

        System.out.println("Schedule exported to " + csvFile.getAbsolutePath());

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



