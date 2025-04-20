package com.zihao.GA_TS_SLAB.Plot;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: Generate Gantt chart from solution file without running the solver
 */
public class SolutionGantt {
	public static void main(String[] args) {
		// Check if arguments are provided
		if (args.length == 0) {
			// Example usage with default values
			String algorithmName = "Genetic Algorithm and Simulated Annealing"; // Algorithm name
			String datasetName = "RNAseq_N5"; // Dataset name without the path structure
			String solutionFileName = "Genetic Algorithm and Simulated Annealing-" + datasetName + ".tsv"; // Following the naming pattern from the image

			// Determine the actual dataset path based on the dataset name
			String datasetPath = determineDatasetPath(datasetName);

			// Set up the data directory for the problem instance
			File parentDir = new File("src/Dataset/" + datasetPath);

			// Run with the solution file
			String solutionFilePath = "src/Solution/" + solutionFileName;
			generateGanttFromSolution(parentDir, solutionFilePath, algorithmName, datasetName);
		} else {
			// Use command line arguments if provided
			String solutionFileName = args[0]; // e.g., "SAGAS-Gu2016_N1.tsv"
			String algorithmName = "SAGAS"; // Default algorithm name

			// Extract dataset name from the solution filename
			String datasetName = solutionFileName.substring(solutionFileName.indexOf('-') + 1, solutionFileName.lastIndexOf('.'));

			// Determine the actual dataset path based on the dataset name
			String datasetPath = determineDatasetPath(datasetName);

			// Set up the data directory for the problem instance
			File parentDir = new File("src/Dataset/" + datasetPath);

			// Run with the solution file
			String solutionFilePath = "src/Solution/" + solutionFileName;
			generateGanttFromSolution(parentDir, solutionFilePath, algorithmName, datasetName);
		}
	}

	/**
	 * Determine the dataset path based on the dataset name
	 * This maps the filename format to the actual directory structure
	 *
	 * @param datasetName The dataset name from the solution filename
	 * @return The path to the dataset directory
	 */
	private static String determineDatasetPath(String datasetName) {
		// Map from solution filename format to directory structure
		if (datasetName.equals("Gu2016_N1")) {
			return "Gu2016/N1";
		} else if (datasetName.equals("Gu2016_N5")) {
			return "Gu2016/N5";
		} else if (datasetName.equals("qPCR_N5")) {
			return "qPCR/N5";
		} else if (datasetName.equals("qPRC_RNAseq_N5") || datasetName.equals("qPCR_RNAseq_N5")) {
			return "qPCR_RNAseq/N5_N5";
		} else if (datasetName.equals("RNAseq_N5")) {
			return "RNAseq/N5";
		} else {
			// Default fallback if no matching pattern
			return datasetName.replace('_', '/');
		}
	}

	/**
	 * Generate Gantt chart from a solution TSV file
	 *
	 * @param parentDir The directory containing the problem instance data
	 * @param solutionFilePath Path to the solution TSV file
	 * @param algorithmName Name of the algorithm used
	 * @param datasetName Name of the dataset
	 */
	public static void generateGanttFromSolution(File parentDir, String solutionFilePath, String algorithmName, String datasetName) {
		File solutionFile = new File(solutionFilePath);

		if (!solutionFile.exists()) {
			System.err.println("Solution file not found: " + solutionFilePath);
			return;
		}

		System.out.println("Loading solution file: " + solutionFilePath);
		System.out.println("Using algorithm: " + algorithmName);
		System.out.println("Using dataset: " + datasetName);

		// Initialize the problem settings from the data files
		Input input = new Input(parentDir);
		input.getProblemDesFromFile();
		System.out.println("Problem description loaded from: " + parentDir.getAbsolutePath());

		// Create a schedule.csv file from the solution TSV
		try {
			createScheduleCsvFromSolution(solutionFile);
			System.out.println("Schedule CSV created successfully.");

			// Call the Python script to generate the Gantt chart
			callPythonGanttScript(algorithmName, datasetName);

		} catch (IOException e) {
			System.err.println("Error processing solution file: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Create schedule.csv file from a solution TSV file
	 *
	 * @param solutionFile The solution TSV file
	 * @throws IOException If file operations fail
	 */
	private static void createScheduleCsvFromSolution(File solutionFile) throws IOException {
		// Get problem settings instance that has been initialized by Input
		ProblemSetting problemSetting = ProblemSetting.getInstance();

		// Get operation to job mapping and machine names from problem settings
		Map<Integer, Integer> opToJob = problemSetting.getOpToJob();
		String[] machineNames = problemSetting.getMachineNames();

		// Data structures to hold schedule information
		Map<Integer, Integer> startTimes = new HashMap<>();
		Map<Integer, Integer> endTimes = new HashMap<>();
		Map<Integer, Integer> machineIds = new HashMap<>();

		// Read the solution TSV file
		try (BufferedReader reader = new BufferedReader(new FileReader(solutionFile))) {
			String line;
			// Skip header
			reader.readLine();

			while ((line = reader.readLine()) != null) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length >= 5) {
					int jobId = Integer.parseInt(parts[0]);
					int operationId = Integer.parseInt(parts[1]);
					int start = Integer.parseInt(parts[2]);
					int end = Integer.parseInt(parts[3]);
					int machineId = Integer.parseInt(parts[4]);

					startTimes.put(operationId, start);
					endTimes.put(operationId, end);
					machineIds.put(operationId, machineId);
				}
			}
		}

		// Create a schedule.csv file for the Python script
		File scheduleFile = new File("src/com/zihao/GA_TS_SLAB/Plot/schedule.csv");
		try (FileWriter writer = new FileWriter(scheduleFile)) {
			// Write header
			writer.append("Operation,Job,Machine,MachineName,Start,End\n");

			// Create a machine groups map for operations
			Map<Integer, List<Integer>> machineGroups = new HashMap<>();
			for (Map.Entry<Integer, Integer> entry : machineIds.entrySet()) {
				int operation = entry.getKey();
				int machine = entry.getValue();
				machineGroups.computeIfAbsent(machine, k -> new ArrayList<>()).add(operation);
			}

			// Process each machine group
			for (Map.Entry<Integer, List<Integer>> entry : machineGroups.entrySet()) {
				int machine = entry.getKey();
				List<Integer> operations = entry.getValue();

				// Sort operations by start time
				operations.sort(Comparator.comparingInt(startTimes::get));

				for (int operation : operations) {
					int start = startTimes.get(operation);
					int end = endTimes.get(operation);
					int jobId = opToJob.getOrDefault(operation, 0);  // Get job ID from problem setting

					// Get machine name from problem setting if available
					String machineName = "Unknown";
					if (machine > 0 && machine <= machineNames.length) {
						machineName = machineNames[machine - 1];
					}

					writer.append(operation + "," + jobId + "," + machine + "," + machineName + "," + start + "," + end + "\n");
				}
			}
		}
	}

	/**
	 * Call the Python script to generate the Gantt chart
	 *
	 * @param algorithmName Name of the algorithm
	 * @param datasetName Name of the dataset
	 */
	private static void callPythonGanttScript(String algorithmName, String datasetName) {
		try {
			String pythonScriptPath = "src/com/zihao/GA_TS_SLAB/Plot/plot_gantt.py";

			// Get Python executable path
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

			// Format dataset name for use in the script
			String formattedDatasetName = datasetName.replace('/', '_').replace('\\', '_');

			// Call the Python script with algorithm and dataset parameters
			pb = new ProcessBuilder(pythonPath, pythonScriptPath, algorithmName, formattedDatasetName);
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
				System.out.println("Gantt chart saved as: " + algorithmName.toLowerCase().replace(' ', '_') + "-" + formattedDatasetName + ".svg");
			} else {
				System.out.println("Python script execution failed with exit code: " + exitCode);
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}