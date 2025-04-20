//////package com.zihao.GA_TS_SLAB.Solver;
//////
//////import com.google.ortools.Loader;
//////import com.google.ortools.linearsolver.MPConstraint;
//////import com.google.ortools.linearsolver.MPObjective;
//////import com.google.ortools.linearsolver.MPSolver;
//////import com.google.ortools.linearsolver.MPVariable;
//////import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
//////import com.zihao.GA_TS_SLAB.Data.TCMB;
//////import com.zihao.GA_TS_SLAB.GA.Schedule;
//////
//////import java.util.*;
//////
///////**
////// * This class implements a MILP (Mixed Integer Linear Programming) solver
////// * for the S-LAB scheduling problem using Google OR-Tools with CBC solver.
////// */
//////public class MILPSchedule {
//////	private ProblemSetting pb;
//////	private static final double BIG_M = 1000; // Reduced value for "big M" constraints
//////	private MPSolver solver;
//////
//////	// Variable maps for easier access
//////	private Map<Integer, MPVariable> startTimeVars;
//////	private Map<String, MPVariable> assignmentVars;
//////	private Map<String, MPVariable> sequencingVars;
//////	private MPVariable makespanVar;
//////
//////	// Statistics for debugging
//////	private int constraintCount = 0;
//////
//////	public MILPSchedule() {
//////		this.pb = ProblemSetting.getInstance();
//////		this.startTimeVars = new HashMap<>();
//////		this.assignmentVars = new HashMap<>();
//////		this.sequencingVars = new HashMap<>();
//////	}
//////
//////	/**
//////	 * Solves the scheduling problem using MILP with OR-Tools and returns an optimal schedule.
//////	 * @return An optimal Schedule object
//////	 */
//////	public Schedule solve() {
//////		int totalOps = pb.getTotalOpNum();
//////		int numMachines = pb.getMachineNum();
//////
//////		try {
//////			// Load the OR-Tools native library
//////			Loader.loadNativeLibraries();
//////
//////			// Create the solver
//////			solver = MPSolver.createSolver("CBC");
//////
//////			if (solver == null) {
//////				System.err.println("Could not create solver CBC");
//////				return null;
//////			}
//////
//////			System.out.println("Starting to build the MILP model...");
//////
//////			// Create variables
//////			createVariables(totalOps, numMachines);
//////
//////			// Create objective function
//////			createObjectiveFunction();
//////
//////			// Create constraints
//////			createConstraints(totalOps, numMachines);
//////
//////			// Set solver parameters
//////			solver.setTimeLimit(600000); // 10 minutes time limit
//////
//////			try {
//////				solver.setSolverSpecificParametersAsString("emphasis=1");
//////			} catch (Exception e) {
//////				System.out.println("Info: Unable to set solver emphasis parameters, continuing with defaults");
//////			}
//////
//////			// Solve the model
//////			System.out.println("Solving model with " + solver.numVariables() + " variables and " +
//////					solver.numConstraints() + " constraints...");
//////
//////			final MPSolver.ResultStatus resultStatus = solver.solve();
//////
//////			// Check solution status
//////			if (resultStatus == MPSolver.ResultStatus.OPTIMAL ||
//////					resultStatus == MPSolver.ResultStatus.FEASIBLE) {
//////
//////				System.out.println("Solution found!");
//////				if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
//////					System.out.println("Optimal solution found!");
//////				} else {
//////					System.out.println("Feasible (but possibly not optimal) solution found.");
//////				}
//////
//////				System.out.println("Objective value (makespan): " + solver.objective().value());
//////
//////				// Convert the solution to a Schedule object
//////				return convertToSchedule(totalOps);
//////			} else {
//////				System.out.println("No solution found. Status: " + resultStatus);
//////				analyzeInfeasibility(totalOps, numMachines);
//////				return null;
//////			}
//////		} catch (Exception e) {
//////			System.err.println("Error solving MILP model: " + e.getMessage());
//////			e.printStackTrace();
//////			return null;
//////		}
//////	}
//////
//////	/**
//////	 * Creates all variables needed for the model
//////	 */
//////	private void createVariables(int totalOps, int numMachines) {
//////		// Create start time variables for each operation (continuous)
//////		for (int op = 1; op <= totalOps; op++) {
//////			MPVariable startTimeVar = solver.makeNumVar(0.0, BIG_M, "s_" + op);
//////			startTimeVars.put(op, startTimeVar);
//////		}
//////
//////		// Create assignment variables (binary)
//////		for (int op = 1; op <= totalOps; op++) {
//////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//////			for (int machine : compatibleMachines) {
//////				MPVariable assignVar = solver.makeIntVar(0.0, 1.0, "x_" + op + "_" + machine);
//////				assignmentVars.put(op + "_" + machine, assignVar);
//////			}
//////		}
//////
//////		// Sequencing variables will be created in the machine exclusivity constraints section
//////
//////		// Create makespan variable (continuous)
//////		makespanVar = solver.makeNumVar(0.0, BIG_M, "makespan");
//////
//////		System.out.println("Created variables:");
//////		System.out.println("- Start time variables: " + startTimeVars.size());
//////		System.out.println("- Assignment variables: " + assignmentVars.size());
//////	}
//////
//////	/**
//////	 * Creates the objective function to minimize makespan
//////	 */
//////	private void createObjectiveFunction() {
//////		MPObjective objective = solver.objective();
//////		objective.setCoefficient(makespanVar, 1.0);
//////		objective.setMinimization();
//////	}
//////
//////	/**
//////	 * Creates all constraints for the MILP model
//////	 */
//////	private void createConstraints(int totalOps, int numMachines) {
//////		constraintCount = 0;
//////
//////		// Add uniqueness of operation assignment constraints
//////		addOperationAssignmentConstraints(totalOps);
//////
//////		// Add operation precedence constraints
//////		addPrecedenceConstraints(totalOps);
//////
//////		// Add mutual exclusivity of machine usage constraints
//////		addMachineExclusivityConstraints(totalOps, numMachines);
//////
//////		// Add time constraints (TCMB)
//////		addTimeConstraints();
//////
//////		// Add makespan definition constraints
//////		addMakespanConstraints(totalOps);
//////
//////		System.out.println("Created a total of " + constraintCount + " constraints");
//////	}
//////
//////	/**
//////	 * Add constraints ensuring each operation is assigned to exactly one compatible machine
//////	 */
//////	private void addOperationAssignmentConstraints(int totalOps) {
//////		int initialCount = constraintCount;
//////
//////		for (int op = 1; op <= totalOps; op++) {
//////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//////
//////			// Create constraint: sum(x_op_m) = 1 for all compatible machines
//////			MPConstraint constraint = solver.makeConstraint(1.0, 1.0, "op_assign_" + op);
//////
//////			for (int machine : compatibleMachines) {
//////				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
//////				constraint.setCoefficient(assignVar, 1.0);
//////			}
//////
//////			constraintCount++;
//////		}
//////
//////		System.out.println("Added " + (constraintCount - initialCount) + " operation assignment constraints");
//////	}
//////
//////	/**
//////	 * Add constraints for operation precedence from the DAG
//////	 */
//////	private void addPrecedenceConstraints(int totalOps) {
//////		int initialCount = constraintCount;
//////
//////		int[][] distanceMatrix = pb.getDistanceMatrix();
//////
//////		for (int i = 1; i <= totalOps; i++) {
//////			for (int j = 1; j <= totalOps; j++) {
//////				// If operation i precedes operation j (distance > 0)
//////				if (distanceMatrix[i][j] > 0) {
//////					// Create constraint: s_j >= s_i + p_i
//////					MPConstraint constraint = solver.makeConstraint(
//////							0.0,
//////							MPSolver.infinity(),
//////							"precedence_" + i + "_" + j
//////					);
//////
//////					constraint.setCoefficient(startTimeVars.get(j), 1.0);
//////					constraint.setCoefficient(startTimeVars.get(i), -1.0);
//////
//////					// Add processing time of operation i
//////					double processingTime_i = pb.getProcessingTime()[i - 1];
//////					constraint.setLb(processingTime_i);
//////
//////					constraintCount++;
//////				}
//////			}
//////		}
//////
//////		System.out.println("Added " + (constraintCount - initialCount) + " precedence constraints");
//////	}
//////
//////	/**
//////	 * Add constraints ensuring operations don't overlap on the same machine.
//////	 * 修改了 machine exclusivity 部分，调整了 big-M 项中各变量的符号
//////	 * 采用如下两组约束：
//////	 *  (1) s_op2 - s_op1 - M·(y + x1 + x2) >= p_op1 - 3M
//////	 *  (2) s_op1 - s_op2 + M·y - M·(x1 + x2) >= p_op2 - 2M
//////	 */
//////	private void addMachineExclusivityConstraints(int totalOps, int numMachines) {
//////		int initialCount = constraintCount;
//////
//////		// For each machine, we need to prevent overlapping operations
//////		for (int machine = 1; machine <= numMachines; machine++) {
//////			// Find all operations that can be executed on this machine
//////			List<Integer> compatibleOps = new ArrayList<>();
//////			for (int op = 1; op <= totalOps; op++) {
//////				List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//////				if (compatibleMachines != null && compatibleMachines.contains(machine)) {
//////					compatibleOps.add(op);
//////				}
//////			}
//////
//////			// For each pair of operations that can run on this machine,
//////			// ensure they don't overlap when assigned to this machine
//////			for (int i = 0; i < compatibleOps.size(); i++) {
//////				int op1 = compatibleOps.get(i);
//////				double pt1 = pb.getProcessingTime()[op1 - 1]; // Processing time of op1
//////
//////				for (int j = i + 1; j < compatibleOps.size(); j++) {
//////					int op2 = compatibleOps.get(j);
//////					double pt2 = pb.getProcessingTime()[op2 - 1]; // Processing time of op2
//////
//////					// Create a binary variable for sequencing these operations
//////					// y=1 if op1 precedes op2, y=0 if op2 precedes op1
//////					String varName = "y_" + op1 + "_" + op2 + "_" + machine;
//////					MPVariable y = solver.makeIntVar(0, 1, varName);
//////					sequencingVars.put(op1 + "_" + op2 + "_" + machine, y);
//////
//////					// Get assignment variables for these operations on this machine
//////					MPVariable x1 = assignmentVars.get(op1 + "_" + machine);
//////					MPVariable x2 = assignmentVars.get(op2 + "_" + machine);
//////
//////					double M = BIG_M;
//////					// Constraint (1): s_op2 - s_op1 - M*(y + x1 + x2) >= pt1 - 3*M
//////					MPConstraint c1 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
//////							"seq1_" + op1 + "_" + op2 + "_" + machine);
//////					c1.setCoefficient(startTimeVars.get(op2), 1.0);
//////					c1.setCoefficient(startTimeVars.get(op1), -1.0);
//////					// 修改处：y, x1, x2 的系数均为 -M
//////					c1.setCoefficient(y, -M);
//////					c1.setCoefficient(x1, -M);
//////					c1.setCoefficient(x2, -M);
//////					c1.setLb(pt1 - 3 * M);
//////					constraintCount++;
//////
//////					// Constraint (2): s_op1 - s_op2 + M*y - M*(x1 + x2) >= pt2 - 2*M
//////					MPConstraint c2 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
//////							"seq2_" + op1 + "_" + op2 + "_" + machine);
//////					c2.setCoefficient(startTimeVars.get(op1), 1.0);
//////					c2.setCoefficient(startTimeVars.get(op2), -1.0);
//////					// 修改处：y 的系数为 +M，x1 和 x2 的系数为 -M
//////					c2.setCoefficient(y, M);
//////					c2.setCoefficient(x1, -M);
//////					c2.setCoefficient(x2, -M);
//////					c2.setLb(pt2 - 2 * M);
//////					constraintCount++;
//////				}
//////			}
//////		}
//////
//////		System.out.println("- Sequencing variables: " + sequencingVars.size());
//////		System.out.println("Added " + (constraintCount - initialCount) + " machine exclusivity constraints");
//////	}
//////
//////	/**
//////	 * Add TCMB (Time Constraints by Mutual Boundaries) constraints
//////	 * Corrected to properly model end of A to start of B time constraints
//////	 */
//////	private void addTimeConstraints() {
//////		int initialCount = constraintCount;
//////
//////		List<TCMB> tcmbList = pb.getTCMBList();
//////
//////		// First, store all constraints to enable debugging
//////		List<MPConstraint> tcmbConstraints = new ArrayList<>();
//////
//////		for (TCMB tcmb : tcmbList) {
//////			int opA = tcmb.getOp1();
//////			int opB = tcmb.getOp2();
//////			int timeLimit = tcmb.getTimeConstraint();
//////
//////			// End of A to start of B constraint:
//////			// s_b - (s_a + p_a) <= timeLimit
//////			// Effectively: The time between when A finishes and B starts cannot exceed timeLimit
//////
//////			double processingTime_a = pb.getProcessingTime()[opA - 1];
//////
//////			// Create the constraint: s_b - s_a <= timeLimit + p_a
//////			MPConstraint constraint = solver.makeConstraint(
//////					-MPSolver.infinity(),
//////					timeLimit + processingTime_a,
//////					"tcmb_" + opA + "_" + opB
//////			);
//////
//////			constraint.setCoefficient(startTimeVars.get(opB), 1.0);
//////			constraint.setCoefficient(startTimeVars.get(opA), -1.0);
//////
//////			// Store the constraint for potential debugging
//////			tcmbConstraints.add(constraint);
//////
//////			constraintCount++;
//////		}
//////
//////		System.out.println("Added " + (constraintCount - initialCount) + " TCMB constraints");
//////	}
//////
//////	/**
//////	 * Add constraints defining the makespan
//////	 */
//////	private void addMakespanConstraints(int totalOps) {
//////		int initialCount = constraintCount;
//////
//////		for (int op = 1; op <= totalOps; op++) {
//////			// Makespan >= s_a + p_a for all operations a
//////			MPConstraint constraint = solver.makeConstraint(
//////					0.0,
//////					MPSolver.infinity(),
//////					"makespan_" + op
//////			);
//////
//////			constraint.setCoefficient(makespanVar, 1.0);
//////			constraint.setCoefficient(startTimeVars.get(op), -1.0);
//////
//////			double processingTime = pb.getProcessingTime()[op - 1];
//////			constraint.setLb(processingTime);
//////
//////			constraintCount++;
//////		}
//////
//////		System.out.println("Added " + (constraintCount - initialCount) + " makespan constraints");
//////	}
//////
//////	/**
//////	 * Converts the solution from the optimizer to a Schedule object
//////	 */
//////	private Schedule convertToSchedule(int totalOps) {
//////		Map<Integer, Integer> startTimes = new HashMap<>();
//////		Map<Integer, Integer> assignedMachine = new HashMap<>();
//////
//////		// Extract start times
//////		for (int op = 1; op <= totalOps; op++) {
//////			startTimes.put(op, (int) Math.round(startTimeVars.get(op).solutionValue()));
//////		}
//////
//////		// Extract machine assignments
//////		for (int op = 1; op <= totalOps; op++) {
//////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//////
//////			for (int machine : compatibleMachines) {
//////				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
//////
//////				if (Math.round(assignVar.solutionValue()) == 1) {
//////					assignedMachine.put(op, machine);
//////					break;
//////				}
//////			}
//////		}
//////
//////		// Create and validate the schedule
//////		Schedule schedule = new Schedule(startTimes, assignedMachine);
//////		validateSchedule(schedule);
//////
//////		return schedule;
//////	}
//////
//////	/**
//////	 * Validates the created schedule against the problem constraints
//////	 */
//////	private void validateSchedule(Schedule schedule) {
//////		boolean precedenceValid = schedule.checkPrecedenceConstraints();
//////		boolean machineValid = schedule.checkCompatibleMachines();
//////		boolean occupationValid = schedule.checkMachineOccupation();
//////
//////		System.out.println("Schedule validation:");
//////		System.out.println("Precedence constraints: " + (precedenceValid ? "Valid" : "Invalid"));
//////		System.out.println("Machine compatibility: " + (machineValid ? "Valid" : "Invalid"));
//////		System.out.println("Machine occupation: " + (occupationValid ? "Valid" : "Invalid"));
//////
//////		// Additional TCMB validation
//////		boolean tcmbValid = validateTCMBConstraints(schedule);
//////		System.out.println("TCMB constraints: " + (tcmbValid ? "Valid" : "Invalid"));
//////	}
//////
//////	/**
//////	 * Validates that the TCMB constraints are satisfied in the schedule
//////	 */
//////	private boolean validateTCMBConstraints(Schedule schedule) {
//////		List<TCMB> tcmbList = pb.getTCMBList();
//////		Map<Integer, Integer> startTimes = schedule.getStartTimes();
//////		boolean valid = true;
//////
//////		for (TCMB tcmb : tcmbList) {
//////			int opA = tcmb.getOp1();
//////			int opB = tcmb.getOp2();
//////			int timeLimit = tcmb.getTimeConstraint();
//////
//////			int startA = startTimes.get(opA);
//////			int startB = startTimes.get(opB);
//////			double processingTimeA = pb.getProcessingTime()[opA - 1];
//////
//////			// End of A to Start of B
//////			double endA = startA + processingTimeA;
//////			double timeBetween = startB - endA;
//////
//////			if (timeBetween > timeLimit) {
//////				System.out.println("TCMB violation: Time between end of " + opA +
//////						" and start of " + opB + " is " + timeBetween +
//////						", exceeding limit of " + timeLimit);
//////				valid = false;
//////			}
//////		}
//////
//////		return valid;
//////	}
//////
//////	/**
//////	 * Attempts to diagnose infeasibility issues in the model
//////	 */
//////	private void analyzeInfeasibility(int totalOps, int numMachines) {
//////		System.out.println("Analyzing potential causes of infeasibility...");
//////
//////		// 1. Check if there are operations with no compatible machines
//////		for (int op = 1; op <= totalOps; op++) {
//////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//////			if (compatibleMachines == null || compatibleMachines.isEmpty()) {
//////				System.out.println("Operation " + op + " has no compatible machines!");
//////			}
//////		}
//////
//////		// 2. Check for cycles in precedence constraints
//////		int[][] distanceMatrix = pb.getDistanceMatrix();
//////		for (int i = 1; i <= totalOps; i++) {
//////			if (distanceMatrix[i][i] > 0) {
//////				System.out.println("Cycle detected in precedence constraints: operation " +
//////						i + " depends on itself!");
//////			}
//////		}
//////
//////		// 3. Check for TCMB constraints that might conflict with precedence
//////		List<TCMB> tcmbList = pb.getTCMBList();
//////		for (TCMB tcmb : tcmbList) {
//////			int opA = tcmb.getOp1();
//////			int opB = tcmb.getOp2();
//////			int timeLimit = tcmb.getTimeConstraint();
//////
//////			// If we have a precedence constraint that conflicts with the TCMB
//////			if (distanceMatrix[opB][opA] > 0) {
//////				System.out.println("Potential conflict: TCMB constraint between operations " +
//////						opA + " and " + opB + " may conflict with precedence constraint.");
//////			}
//////
//////			// Check for extremely tight time constraints
//////			int processingTime_a = pb.getProcessingTime()[opA - 1];
//////			if (timeLimit < 0) {
//////				System.out.println("Potential issue: TCMB time limit for operations " +
//////						opA + " and " + opB + " is negative (" + timeLimit + ")");
//////			}
//////		}
//////
//////		System.out.println("Consider relaxing some constraints or verifying the problem data.");
//////	}
//////}
////
////package com.zihao.GA_TS_SLAB.Solver;
////
////import com.google.ortools.Loader;
////import com.google.ortools.linearsolver.MPConstraint;
////import com.google.ortools.linearsolver.MPObjective;
////import com.google.ortools.linearsolver.MPSolver;
////import com.google.ortools.linearsolver.MPVariable;
////import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
////import com.zihao.GA_TS_SLAB.Data.TCMB;
////import com.zihao.GA_TS_SLAB.GA.Schedule;
////
////import java.util.*;
////
/////**
//// * This class implements a MILP (Mixed Integer Linear Programming) solver
//// * for the S-LAB scheduling problem using Google OR-Tools with CBC solver.
//// */
////public class MILPSchedule {
////	private ProblemSetting pb;
////	private static final double BIG_M = 1000; // Reduced value for "big M" constraints
////	private MPSolver solver;
////
////	// Variable maps for easier access
////	private Map<Integer, MPVariable> startTimeVars;
////	private Map<String, MPVariable> assignmentVars;
////	private Map<String, MPVariable> sequencingVars;
////	private MPVariable makespanVar;
////
////	// Statistics for debugging
////	private int constraintCount = 0;
////
////	public MILPSchedule() {
////		this.pb = ProblemSetting.getInstance();
////		this.startTimeVars = new HashMap<>();
////		this.assignmentVars = new HashMap<>();
////		this.sequencingVars = new HashMap<>();
////	}
////
////	/**
////	 * Solves the scheduling problem using MILP with OR-Tools and returns an optimal schedule.
////	 * @return An optimal Schedule object
////	 */
////	public Schedule solve() {
////		int totalOps = pb.getTotalOpNum();
////		int numMachines = pb.getMachineNum();
////
////		try {
////			// Load the OR-Tools native library
////			Loader.loadNativeLibraries();
////
////			// Create the solver
////			solver = MPSolver.createSolver("CBC");
////
////			if (solver == null) {
////				System.err.println("Could not create solver CBC");
////				return null;
////			}
////
////			System.out.println("Starting to build the MILP model...");
////
////			// Create variables
////			createVariables(totalOps, numMachines);
////
////			// Create objective function
////			createObjectiveFunction();
////
////			// Create constraints
////			createConstraints(totalOps, numMachines);
////
////			// 移除时间限制，让模型运行直到找到最优解
////			// 默认情况下，CBC 求解器会运行到找到最优解或者无法进一步改进
////
////			// 通过捕获异常安静地处理不支持的参数设置
////			try {
////				// 对于某些版本的 CBC，可以设置全精度模式
////				solver.setSolverSpecificParametersAsString("allowableFractionGap=0.0 allowableGap=0.0");
////			} catch (Exception e) {
////				// 忽略错误，继续运行
////			}
////
////			// Solve the model
////			System.out.println("Solving model with " + solver.numVariables() + " variables and " +
////					solver.numConstraints() + " constraints...");
////			System.out.println("No time limit set - solver will run until optimal solution is found");
////
////			final MPSolver.ResultStatus resultStatus = solver.solve();
////
////			// Check solution status
////			if (resultStatus == MPSolver.ResultStatus.OPTIMAL ||
////					resultStatus == MPSolver.ResultStatus.FEASIBLE) {
////
////				System.out.println("Solution found!");
////				if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
////					System.out.println("Optimal solution found!");
////				} else {
////					System.out.println("Feasible (but possibly not optimal) solution found.");
////				}
////
////				System.out.println("Objective value (makespan): " + solver.objective().value());
////
////				// Convert the solution to a Schedule object
////				return convertToSchedule(totalOps);
////			} else {
////				System.out.println("No solution found. Status: " + resultStatus);
////				analyzeInfeasibility(totalOps, numMachines);
////				return null;
////			}
////		} catch (Exception e) {
////			System.err.println("Error solving MILP model: " + e.getMessage());
////			e.printStackTrace();
////			return null;
////		}
////	}
////
////	/**
////	 * Creates all variables needed for the model
////	 */
////	private void createVariables(int totalOps, int numMachines) {
////		// Create start time variables for each operation (continuous)
////		for (int op = 1; op <= totalOps; op++) {
////			MPVariable startTimeVar = solver.makeNumVar(0.0, BIG_M, "s_" + op);
////			startTimeVars.put(op, startTimeVar);
////		}
////
////		// Create assignment variables (binary)
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////			for (int machine : compatibleMachines) {
////				MPVariable assignVar = solver.makeIntVar(0.0, 1.0, "x_" + op + "_" + machine);
////				assignmentVars.put(op + "_" + machine, assignVar);
////			}
////		}
////
////		// Sequencing variables will be created in the machine exclusivity constraints section
////
////		// Create makespan variable (continuous)
////		makespanVar = solver.makeNumVar(0.0, BIG_M, "makespan");
////
////		System.out.println("Created variables:");
////		System.out.println("- Start time variables: " + startTimeVars.size());
////		System.out.println("- Assignment variables: " + assignmentVars.size());
////	}
////
////	/**
////	 * Creates the objective function to minimize makespan
////	 */
////	private void createObjectiveFunction() {
////		MPObjective objective = solver.objective();
////		objective.setCoefficient(makespanVar, 1.0);
////		objective.setMinimization();
////	}
////
////	/**
////	 * Creates all constraints for the MILP model
////	 */
////	private void createConstraints(int totalOps, int numMachines) {
////		constraintCount = 0;
////
////		// Add uniqueness of operation assignment constraints
////		addOperationAssignmentConstraints(totalOps);
////
////		// Add operation precedence constraints
////		addPrecedenceConstraints(totalOps);
////
////		// Add mutual exclusivity of machine usage constraints
////		addMachineExclusivityConstraints(totalOps, numMachines);
////
////		// Add time constraints (TCMB)
////		addTimeConstraints();
////
////		// Add makespan definition constraints
////		addMakespanConstraints(totalOps);
////
////		System.out.println("Created a total of " + constraintCount + " constraints");
////	}
////
////	/**
////	 * Add constraints ensuring each operation is assigned to exactly one compatible machine
////	 */
////	private void addOperationAssignmentConstraints(int totalOps) {
////		int initialCount = constraintCount;
////
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////
////			// Create constraint: sum(x_op_m) = 1 for all compatible machines
////			MPConstraint constraint = solver.makeConstraint(1.0, 1.0, "op_assign_" + op);
////
////			for (int machine : compatibleMachines) {
////				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
////				constraint.setCoefficient(assignVar, 1.0);
////			}
////
////			constraintCount++;
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " operation assignment constraints");
////	}
////
////	/**
////	 * Add constraints for operation precedence from the DAG
////	 */
////	private void addPrecedenceConstraints(int totalOps) {
////		int initialCount = constraintCount;
////
////		int[][] distanceMatrix = pb.getDistanceMatrix();
////
////		for (int i = 1; i <= totalOps; i++) {
////			for (int j = 1; j <= totalOps; j++) {
////				// If operation i precedes operation j (distance > 0)
////				if (distanceMatrix[i][j] > 0) {
////					// Create constraint: s_j >= s_i + p_i
////					MPConstraint constraint = solver.makeConstraint(
////							0.0,
////							MPSolver.infinity(),
////							"precedence_" + i + "_" + j
////					);
////
////					constraint.setCoefficient(startTimeVars.get(j), 1.0);
////					constraint.setCoefficient(startTimeVars.get(i), -1.0);
////
////					// Add processing time of operation i
////					double processingTime_i = pb.getProcessingTime()[i - 1];
////					constraint.setLb(processingTime_i);
////
////					constraintCount++;
////				}
////			}
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " precedence constraints");
////	}
////
////	/**
////	 * Add constraints ensuring operations don't overlap on the same machine.
////	 * 修改了 machine exclusivity 部分，调整了 big-M 项中各变量的符号
////	 * 采用如下两组约束：
////	 *  (1) s_op2 - s_op1 - M·(y + x1 + x2) >= p_op1 - 3M
////	 *  (2) s_op1 - s_op2 + M·y - M·(x1 + x2) >= p_op2 - 2M
////	 */
////	private void addMachineExclusivityConstraints(int totalOps, int numMachines) {
////		int initialCount = constraintCount;
////
////		// For each machine, we need to prevent overlapping operations
////		for (int machine = 1; machine <= numMachines; machine++) {
////			// Find all operations that can be executed on this machine
////			List<Integer> compatibleOps = new ArrayList<>();
////			for (int op = 1; op <= totalOps; op++) {
////				List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////				if (compatibleMachines != null && compatibleMachines.contains(machine)) {
////					compatibleOps.add(op);
////				}
////			}
////
////			// For each pair of operations that can run on this machine,
////			// ensure they don't overlap when assigned to this machine
////			for (int i = 0; i < compatibleOps.size(); i++) {
////				int op1 = compatibleOps.get(i);
////				double pt1 = pb.getProcessingTime()[op1 - 1]; // Processing time of op1
////
////				for (int j = i + 1; j < compatibleOps.size(); j++) {
////					int op2 = compatibleOps.get(j);
////					double pt2 = pb.getProcessingTime()[op2 - 1]; // Processing time of op2
////
////					// Create a binary variable for sequencing these operations
////					// y=1 if op1 precedes op2, y=0 if op2 precedes op1
////					String varName = "y_" + op1 + "_" + op2 + "_" + machine;
////					MPVariable y = solver.makeIntVar(0, 1, varName);
////					sequencingVars.put(op1 + "_" + op2 + "_" + machine, y);
////
////					// Get assignment variables for these operations on this machine
////					MPVariable x1 = assignmentVars.get(op1 + "_" + machine);
////					MPVariable x2 = assignmentVars.get(op2 + "_" + machine);
////
////					double M = BIG_M;
////					// Constraint (1): s_op2 - s_op1 - M*(y + x1 + x2) >= pt1 - 3*M
////					MPConstraint c1 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
////							"seq1_" + op1 + "_" + op2 + "_" + machine);
////					c1.setCoefficient(startTimeVars.get(op2), 1.0);
////					c1.setCoefficient(startTimeVars.get(op1), -1.0);
////					// 修改处：y, x1, x2 的系数均为 -M
////					c1.setCoefficient(y, -M);
////					c1.setCoefficient(x1, -M);
////					c1.setCoefficient(x2, -M);
////					c1.setLb(pt1 - 3 * M);
////					constraintCount++;
////
////					// Constraint (2): s_op1 - s_op2 + M*y - M*(x1 + x2) >= pt2 - 2*M
////					MPConstraint c2 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
////							"seq2_" + op1 + "_" + op2 + "_" + machine);
////					c2.setCoefficient(startTimeVars.get(op1), 1.0);
////					c2.setCoefficient(startTimeVars.get(op2), -1.0);
////					// 修改处：y 的系数为 +M，x1 和 x2 的系数为 -M
////					c2.setCoefficient(y, M);
////					c2.setCoefficient(x1, -M);
////					c2.setCoefficient(x2, -M);
////					c2.setLb(pt2 - 2 * M);
////					constraintCount++;
////				}
////			}
////		}
////
////		System.out.println("- Sequencing variables: " + sequencingVars.size());
////		System.out.println("Added " + (constraintCount - initialCount) + " machine exclusivity constraints");
////	}
////
////	/**
////	 * Add TCMB (Time Constraints by Mutual Boundaries) constraints
////	 * Corrected to properly model end of A to start of B time constraints
////	 */
////	private void addTimeConstraints() {
////		int initialCount = constraintCount;
////
////		List<TCMB> tcmbList = pb.getTCMBList();
////
////		// First, store all constraints to enable debugging
////		List<MPConstraint> tcmbConstraints = new ArrayList<>();
////
////		for (TCMB tcmb : tcmbList) {
////			int opA = tcmb.getOp1();
////			int opB = tcmb.getOp2();
////			int timeLimit = tcmb.getTimeConstraint();
////
////			// End of A to start of B constraint:
////			// s_b - (s_a + p_a) <= timeLimit
////			// Effectively: The time between when A finishes and B starts cannot exceed timeLimit
////
////			double processingTime_a = pb.getProcessingTime()[opA - 1];
////
////			// Create the constraint: s_b - s_a <= timeLimit + p_a
////			MPConstraint constraint = solver.makeConstraint(
////					-MPSolver.infinity(),
////					timeLimit + processingTime_a,
////					"tcmb_" + opA + "_" + opB
////			);
////
////			constraint.setCoefficient(startTimeVars.get(opB), 1.0);
////			constraint.setCoefficient(startTimeVars.get(opA), -1.0);
////
////			// Store the constraint for potential debugging
////			tcmbConstraints.add(constraint);
////
////			constraintCount++;
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " TCMB constraints");
////	}
////
////	/**
////	 * Add constraints defining the makespan
////	 */
////	private void addMakespanConstraints(int totalOps) {
////		int initialCount = constraintCount;
////
////		for (int op = 1; op <= totalOps; op++) {
////			// Makespan >= s_a + p_a for all operations a
////			MPConstraint constraint = solver.makeConstraint(
////					0.0,
////					MPSolver.infinity(),
////					"makespan_" + op
////			);
////
////			constraint.setCoefficient(makespanVar, 1.0);
////			constraint.setCoefficient(startTimeVars.get(op), -1.0);
////
////			double processingTime = pb.getProcessingTime()[op - 1];
////			constraint.setLb(processingTime);
////
////			constraintCount++;
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " makespan constraints");
////	}
////
////	/**
////	 * Converts the solution from the optimizer to a Schedule object
////	 */
////	private Schedule convertToSchedule(int totalOps) {
////		Map<Integer, Integer> startTimes = new HashMap<>();
////		Map<Integer, Integer> assignedMachine = new HashMap<>();
////
////		// Extract start times
////		for (int op = 1; op <= totalOps; op++) {
////			startTimes.put(op, (int) Math.round(startTimeVars.get(op).solutionValue()));
////		}
////
////		// Extract machine assignments
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////
////			for (int machine : compatibleMachines) {
////				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
////
////				if (Math.round(assignVar.solutionValue()) == 1) {
////					assignedMachine.put(op, machine);
////					break;
////				}
////			}
////		}
////
////		// Create and validate the schedule
////		Schedule schedule = new Schedule(startTimes, assignedMachine);
////		validateSchedule(schedule);
////
////		return schedule;
////	}
////
////	/**
////	 * Validates the created schedule against the problem constraints
////	 */
////	private void validateSchedule(Schedule schedule) {
////		boolean precedenceValid = schedule.checkPrecedenceConstraints();
////		boolean machineValid = schedule.checkCompatibleMachines();
////		boolean occupationValid = schedule.checkMachineOccupation();
////
////		System.out.println("Schedule validation:");
////		System.out.println("Precedence constraints: " + (precedenceValid ? "Valid" : "Invalid"));
////		System.out.println("Machine compatibility: " + (machineValid ? "Valid" : "Invalid"));
////		System.out.println("Machine occupation: " + (occupationValid ? "Valid" : "Invalid"));
////
////		// Additional TCMB validation
////		boolean tcmbValid = validateTCMBConstraints(schedule);
////		System.out.println("TCMB constraints: " + (tcmbValid ? "Valid" : "Invalid"));
////	}
////
////	/**
////	 * Validates that the TCMB constraints are satisfied in the schedule
////	 */
////	private boolean validateTCMBConstraints(Schedule schedule) {
////		List<TCMB> tcmbList = pb.getTCMBList();
////		Map<Integer, Integer> startTimes = schedule.getStartTimes();
////		boolean valid = true;
////
////		for (TCMB tcmb : tcmbList) {
////			int opA = tcmb.getOp1();
////			int opB = tcmb.getOp2();
////			int timeLimit = tcmb.getTimeConstraint();
////
////			int startA = startTimes.get(opA);
////			int startB = startTimes.get(opB);
////			double processingTimeA = pb.getProcessingTime()[opA - 1];
////
////			// End of A to Start of B
////			double endA = startA + processingTimeA;
////			double timeBetween = startB - endA;
////
////			if (timeBetween > timeLimit) {
////				System.out.println("TCMB violation: Time between end of " + opA +
////						" and start of " + opB + " is " + timeBetween +
////						", exceeding limit of " + timeLimit);
////				valid = false;
////			}
////		}
////
////		return valid;
////	}
////
////	/**
////	 * Attempts to diagnose infeasibility issues in the model
////	 */
////	private void analyzeInfeasibility(int totalOps, int numMachines) {
////		System.out.println("Analyzing potential causes of infeasibility...");
////
////		// 1. Check if there are operations with no compatible machines
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////			if (compatibleMachines == null || compatibleMachines.isEmpty()) {
////				System.out.println("Operation " + op + " has no compatible machines!");
////			}
////		}
////
////		// 2. Check for cycles in precedence constraints
////		int[][] distanceMatrix = pb.getDistanceMatrix();
////		for (int i = 1; i <= totalOps; i++) {
////			if (distanceMatrix[i][i] > 0) {
////				System.out.println("Cycle detected in precedence constraints: operation " +
////						i + " depends on itself!");
////			}
////		}
////
////		// 3. Check for TCMB constraints that might conflict with precedence
////		List<TCMB> tcmbList = pb.getTCMBList();
////		for (TCMB tcmb : tcmbList) {
////			int opA = tcmb.getOp1();
////			int opB = tcmb.getOp2();
////			int timeLimit = tcmb.getTimeConstraint();
////
////			// If we have a precedence constraint that conflicts with the TCMB
////			if (distanceMatrix[opB][opA] > 0) {
////				System.out.println("Potential conflict: TCMB constraint between operations " +
////						opA + " and " + opB + " may conflict with precedence constraint.");
////			}
////
////			// Check for extremely tight time constraints
////			int processingTime_a = pb.getProcessingTime()[opA - 1];
////			if (timeLimit < 0) {
////				System.out.println("Potential issue: TCMB time limit for operations " +
////						opA + " and " + opB + " is negative (" + timeLimit + ")");
////			}
////		}
////
////		System.out.println("Consider relaxing some constraints or verifying the problem data.");
////	}
////}
//
////package com.zihao.GA_TS_SLAB.Solver;
////
////import com.google.ortools.Loader;
////import com.google.ortools.linearsolver.MPConstraint;
////import com.google.ortools.linearsolver.MPObjective;
////import com.google.ortools.linearsolver.MPSolver;
////import com.google.ortools.linearsolver.MPVariable;
////import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
////import com.zihao.GA_TS_SLAB.Data.TCMB;
////import com.zihao.GA_TS_SLAB.GA.Schedule;
////
////import java.util.*;
////
/////**
//// * This class implements a MILP (Mixed Integer Linear Programming) solver
//// * for the S-LAB scheduling problem using Google OR-Tools with CBC solver.
//// */
////public class MILPSchedule {
////	private ProblemSetting pb;
////	private static final double BIG_M = 1000; // Reduced value for "big M" constraints
////	private MPSolver solver;
////
////	// Variable maps for easier access
////	private Map<Integer, MPVariable> startTimeVars;
////	private Map<String, MPVariable> assignmentVars;
////	private Map<String, MPVariable> sequencingVars;
////	private MPVariable makespanVar;
////
////	// Statistics for debugging
////	private int constraintCount = 0;
////
////	public MILPSchedule() {
////		this.pb = ProblemSetting.getInstance();
////		this.startTimeVars = new HashMap<>();
////		this.assignmentVars = new HashMap<>();
////		this.sequencingVars = new HashMap<>();
////	}
////
////	/**
////	 * Solves the scheduling problem using MILP with OR-Tools and returns an optimal schedule.
////	 * @return An optimal Schedule object
////	 */
////	public Schedule solve() {
////		int totalOps = pb.getTotalOpNum();
////		int numMachines = pb.getMachineNum();
////
////		try {
////			// Load the OR-Tools native library
////			Loader.loadNativeLibraries();
////
////			// Create the solver
////			solver = MPSolver.createSolver("CBC");
////
////			if (solver == null) {
////				System.err.println("Could not create solver CBC");
////				return null;
////			}
////
////			System.out.println("Starting to build the MILP model...");
////
////			// Create variables
////			createVariables(totalOps, numMachines);
////
////			// Create objective function
////			createObjectiveFunction();
////
////			// Create constraints
////			createConstraints(totalOps, numMachines);
////
////			// 设置多线程和其他性能参数
////			try {
////				solver.setSolverSpecificParametersAsString(
////						"threads=8 " +           // 使用8个线程
////								"presolve=on " +         // 启用预处理
////								"cuts=on " +             // 启用割平面生成
////								"strongBranching=on " +  // 使用强分支策略
////								"heuristics=on " +       // 启用启发式
////								"allowableFractionGap=0.0 " + // 精确解
////								"allowableGap=0.0 " +    // 精确解
////								"logLevel=2 " +          // 日志级别，可以是1-5，数字越大输出越详细
////								"printingOptions=normal " +  // 打印选项
////								"nodeLimitPerSearch=10000 " + // 每次搜索的节点限制
////								"secondsFreq=5"          // 每5秒打印进度信息
////				);
////			} catch (Exception e) {
////				System.out.println("Warning: Some parameters not supported in this CBC version");
////			}
////
////			// Solve the model
////			System.out.println("Solving model with " + solver.numVariables() + " variables and " +
////					solver.numConstraints() + " constraints...");
////			System.out.println("Using 8 threads and advanced optimization settings");
////
////			final MPSolver.ResultStatus resultStatus = solver.solve();
////
////			// Check solution status
////			if (resultStatus == MPSolver.ResultStatus.OPTIMAL ||
////					resultStatus == MPSolver.ResultStatus.FEASIBLE) {
////
////				System.out.println("Solution found!");
////				if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
////					System.out.println("Optimal solution found!");
////				} else {
////					System.out.println("Feasible (but possibly not optimal) solution found.");
////				}
////
////				System.out.println("Objective value (makespan): " + solver.objective().value());
////
////				// Convert the solution to a Schedule object
////				return convertToSchedule(totalOps);
////			} else {
////				System.out.println("No solution found. Status: " + resultStatus);
////				analyzeInfeasibility(totalOps, numMachines);
////				return null;
////			}
////		} catch (Exception e) {
////			System.err.println("Error solving MILP model: " + e.getMessage());
////			e.printStackTrace();
////			return null;
////		}
////	}
////
////	/**
////	 * Creates all variables needed for the model
////	 */
////	private void createVariables(int totalOps, int numMachines) {
////		// Create start time variables for each operation (continuous)
////		for (int op = 1; op <= totalOps; op++) {
////			MPVariable startTimeVar = solver.makeNumVar(0.0, BIG_M, "s_" + op);
////			startTimeVars.put(op, startTimeVar);
////		}
////
////		// Create assignment variables (binary)
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////			for (int machine : compatibleMachines) {
////				MPVariable assignVar = solver.makeIntVar(0.0, 1.0, "x_" + op + "_" + machine);
////				assignmentVars.put(op + "_" + machine, assignVar);
////			}
////		}
////
////		// Sequencing variables will be created in the machine exclusivity constraints section
////
////		// Create makespan variable (continuous)
////		makespanVar = solver.makeNumVar(0.0, BIG_M, "makespan");
////
////		System.out.println("Created variables:");
////		System.out.println("- Start time variables: " + startTimeVars.size());
////		System.out.println("- Assignment variables: " + assignmentVars.size());
////	}
////
////	/**
////	 * Creates the objective function to minimize makespan
////	 */
////	private void createObjectiveFunction() {
////		MPObjective objective = solver.objective();
////		objective.setCoefficient(makespanVar, 1.0);
////		objective.setMinimization();
////	}
////
////	/**
////	 * Creates all constraints for the MILP model
////	 */
////	private void createConstraints(int totalOps, int numMachines) {
////		constraintCount = 0;
////
////		// Add uniqueness of operation assignment constraints
////		addOperationAssignmentConstraints(totalOps);
////
////		// Add operation precedence constraints
////		addPrecedenceConstraints(totalOps);
////
////		// Add mutual exclusivity of machine usage constraints
////		addMachineExclusivityConstraints(totalOps, numMachines);
////
////		// Add time constraints (TCMB)
////		addTimeConstraints();
////
////		// Add makespan definition constraints
////		addMakespanConstraints(totalOps);
////
////		System.out.println("Created a total of " + constraintCount + " constraints");
////	}
////
////	/**
////	 * Add constraints ensuring each operation is assigned to exactly one compatible machine
////	 */
////	private void addOperationAssignmentConstraints(int totalOps) {
////		int initialCount = constraintCount;
////
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////
////			// Create constraint: sum(x_op_m) = 1 for all compatible machines
////			MPConstraint constraint = solver.makeConstraint(1.0, 1.0, "op_assign_" + op);
////
////			for (int machine : compatibleMachines) {
////				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
////				constraint.setCoefficient(assignVar, 1.0);
////			}
////
////			constraintCount++;
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " operation assignment constraints");
////	}
////
////	/**
////	 * Add constraints for operation precedence from the DAG
////	 */
////	private void addPrecedenceConstraints(int totalOps) {
////		int initialCount = constraintCount;
////
////		int[][] distanceMatrix = pb.getDistanceMatrix();
////
////		for (int i = 1; i <= totalOps; i++) {
////			for (int j = 1; j <= totalOps; j++) {
////				// If operation i precedes operation j (distance > 0)
////				if (distanceMatrix[i][j] > 0) {
////					// Create constraint: s_j >= s_i + p_i
////					MPConstraint constraint = solver.makeConstraint(
////							0.0,
////							MPSolver.infinity(),
////							"precedence_" + i + "_" + j
////					);
////
////					constraint.setCoefficient(startTimeVars.get(j), 1.0);
////					constraint.setCoefficient(startTimeVars.get(i), -1.0);
////
////					// Add processing time of operation i
////					double processingTime_i = pb.getProcessingTime()[i - 1];
////					constraint.setLb(processingTime_i);
////
////					constraintCount++;
////				}
////			}
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " precedence constraints");
////	}
////
////	/**
////	 * Add constraints ensuring operations don't overlap on the same machine.
////	 * 修改了 machine exclusivity 部分，调整了 big-M 项中各变量的符号
////	 * 采用如下两组约束：
////	 *  (1) s_op2 - s_op1 - M·(y + x1 + x2) >= p_op1 - 3M
////	 *  (2) s_op1 - s_op2 + M·y - M·(x1 + x2) >= p_op2 - 2M
////	 */
////	private void addMachineExclusivityConstraints(int totalOps, int numMachines) {
////		int initialCount = constraintCount;
////
////		// For each machine, we need to prevent overlapping operations
////		for (int machine = 1; machine <= numMachines; machine++) {
////			// Find all operations that can be executed on this machine
////			List<Integer> compatibleOps = new ArrayList<>();
////			for (int op = 1; op <= totalOps; op++) {
////				List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////				if (compatibleMachines != null && compatibleMachines.contains(machine)) {
////					compatibleOps.add(op);
////				}
////			}
////
////			// For each pair of operations that can run on this machine,
////			// ensure they don't overlap when assigned to this machine
////			for (int i = 0; i < compatibleOps.size(); i++) {
////				int op1 = compatibleOps.get(i);
////				double pt1 = pb.getProcessingTime()[op1 - 1]; // Processing time of op1
////
////				for (int j = i + 1; j < compatibleOps.size(); j++) {
////					int op2 = compatibleOps.get(j);
////					double pt2 = pb.getProcessingTime()[op2 - 1]; // Processing time of op2
////
////					// Create a binary variable for sequencing these operations
////					// y=1 if op1 precedes op2, y=0 if op2 precedes op1
////					String varName = "y_" + op1 + "_" + op2 + "_" + machine;
////					MPVariable y = solver.makeIntVar(0, 1, varName);
////					sequencingVars.put(op1 + "_" + op2 + "_" + machine, y);
////
////					// Get assignment variables for these operations on this machine
////					MPVariable x1 = assignmentVars.get(op1 + "_" + machine);
////					MPVariable x2 = assignmentVars.get(op2 + "_" + machine);
////
////					double M = BIG_M;
////					// Constraint (1): s_op2 - s_op1 - M*(y + x1 + x2) >= pt1 - 3*M
////					MPConstraint c1 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
////							"seq1_" + op1 + "_" + op2 + "_" + machine);
////					c1.setCoefficient(startTimeVars.get(op2), 1.0);
////					c1.setCoefficient(startTimeVars.get(op1), -1.0);
////					// 修改处：y, x1, x2 的系数均为 -M
////					c1.setCoefficient(y, -M);
////					c1.setCoefficient(x1, -M);
////					c1.setCoefficient(x2, -M);
////					c1.setLb(pt1 - 3 * M);
////					constraintCount++;
////
////					// Constraint (2): s_op1 - s_op2 + M*y - M*(x1 + x2) >= pt2 - 2*M
////					MPConstraint c2 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
////							"seq2_" + op1 + "_" + op2 + "_" + machine);
////					c2.setCoefficient(startTimeVars.get(op1), 1.0);
////					c2.setCoefficient(startTimeVars.get(op2), -1.0);
////					// 修改处：y 的系数为 +M，x1 和 x2 的系数为 -M
////					c2.setCoefficient(y, M);
////					c2.setCoefficient(x1, -M);
////					c2.setCoefficient(x2, -M);
////					c2.setLb(pt2 - 2 * M);
////					constraintCount++;
////				}
////			}
////		}
////
////		System.out.println("- Sequencing variables: " + sequencingVars.size());
////		System.out.println("Added " + (constraintCount - initialCount) + " machine exclusivity constraints");
////	}
////
////	/**
////	 * Add TCMB (Time Constraints by Mutual Boundaries) constraints
////	 * Corrected to properly model end of A to start of B time constraints
////	 */
////	private void addTimeConstraints() {
////		int initialCount = constraintCount;
////
////		List<TCMB> tcmbList = pb.getTCMBList();
////
////		// First, store all constraints to enable debugging
////		List<MPConstraint> tcmbConstraints = new ArrayList<>();
////
////		for (TCMB tcmb : tcmbList) {
////			int opA = tcmb.getOp1();
////			int opB = tcmb.getOp2();
////			int timeLimit = tcmb.getTimeConstraint();
////
////			// End of A to start of B constraint:
////			// s_b - (s_a + p_a) <= timeLimit
////			// Effectively: The time between when A fines and B starts cannot exceed timeLimit
////
////			double processingTime_a = pb.getProcessingTime()[opA - 1];
////
////			// Create the constraint: s_b - s_a <= timeLimit + p_a
////			MPConstraint constraint = solver.makeConstraint(
////					-MPSolver.infinity(),
////					timeLimit + processingTime_a,
////					"tcmb_" + opA + "_" + opB
////			);
////
////			constraint.setCoefficient(startTimeVars.get(opB), 1.0);
////			constraint.setCoefficient(startTimeVars.get(opA), -1.0);
////
////			// Store the constraint for potential debugging
////			tcmbConstraints.add(constraint);
////
////			constraintCount++;
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " TCMB constraints");
////	}
////
////	/**
////	 * Add constraints defining the makespan
////	 */
////	private void addMakespanConstraints(int totalOps) {
////		int initialCount = constraintCount;
////
////		for (int op = 1; op <= totalOps; op++) {
////			// Makespan >= s_a + p_a for all operations a
////			MPConstraint constraint = solver.makeConstraint(
////					0.0,
////					MPSolver.infinity(),
////					"makespan_" + op
////			);
////
////			constraint.setCoefficient(makespanVar, 1.0);
////			constraint.setCoefficient(startTimeVars.get(op), -1.0);
////
////			double processingTime = pb.getProcessingTime()[op - 1];
////			constraint.setLb(processingTime);
////
////			constraintCount++;
////		}
////
////		System.out.println("Added " + (constraintCount - initialCount) + " makespan constraints");
////	}
////
////	/**
////	 * Converts the solution from the optimizer to a Schedule object
////	 */
////	private Schedule convertToSchedule(int totalOps) {
////		Map<Integer, Integer> startTimes = new HashMap<>();
////		Map<Integer, Integer> assignedMachine = new HashMap<>();
////
////		// Extract start times
////		for (int op = 1; op <= totalOps; op++) {
////			startTimes.put(op, (int) Math.round(startTimeVars.get(op).solutionValue()));
////		}
////
////		// Extract machine assignments
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////
////			for (int machine : compatibleMachines) {
////				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
////
////				if (Math.round(assignVar.solutionValue()) == 1) {
////					assignedMachine.put(op, machine);
////					break;
////				}
////			}
////		}
////
////		// Create and validate the schedule
////		Schedule schedule = new Schedule(startTimes, assignedMachine);
////		validateSchedule(schedule);
////
////		return schedule;
////	}
////
////	/**
////	 * Validates the created schedule against the problem constraints
////	 */
////	private void validateSchedule(Schedule schedule) {
////		boolean precedenceValid = schedule.checkPrecedenceConstraints();
////		boolean machineValid = schedule.checkCompatibleMachines();
////		boolean occupationValid = schedule.checkMachineOccupation();
////
////		System.out.println("Schedule validation:");
////		System.out.println("Precedence constraints: " + (precedenceValid ? "Valid" : "Invalid"));
////		System.out.println("Machine compatibility: " + (machineValid ? "Valid" : "Invalid"));
////		System.out.println("Machine occupation: " + (occupationValid ? "Valid" : "Invalid"));
////
////		// Additional TCMB validation
////		boolean tcmbValid = validateTCMBConstraints(schedule);
////		System.out.println("TCMB constraints: " + (tcmbValid ? "Valid" : "Invalid"));
////	}
////
////	/**
////	 * Validates that the TCMB constraints are satisfied in the schedule
////	 */
////	private boolean validateTCMBConstraints(Schedule schedule) {
////		List<TCMB> tcmbList = pb.getTCMBList();
////		Map<Integer, Integer> startTimes = schedule.getStartTimes();
////		boolean valid = true;
////
////		for (TCMB tcmb : tcmbList) {
////			int opA = tcmb.getOp1();
////			int opB = tcmb.getOp2();
////			int timeLimit = tcmb.getTimeConstraint();
////
////			int startA = startTimes.get(opA);
////			int startB = startTimes.get(opB);
////			double processingTimeA = pb.getProcessingTime()[opA - 1];
////
////			// End of A to Start of B
////			double endA = startA + processingTimeA;
////			double timeBetween = startB - endA;
////
////			if (timeBetween > timeLimit) {
////				System.out.println("TCMB violation: Time between end of " + opA +
////						" and start of " + opB + " is " + timeBetween +
////						", exceeding limit of " + timeLimit);
////				valid = false;
////			}
////		}
////
////		return valid;
////	}
////
////	/**
////	 * Attempts to diagnose infeasibility issues in the model
////	 */
////	private void analyzeInfeasibility(int totalOps, int numMachines) {
////		System.out.println("Analyzing potential causes of infeasibility...");
////
////		// 1. Check if there are operations with no compatible machines
////		for (int op = 1; op <= totalOps; op++) {
////			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
////			if (compatibleMachines == null || compatibleMachines.isEmpty()) {
////				System.out.println("Operation " + op + " has no compatible machines!");
////			}
////		}
////
////		// 2. Check for cycles in precedence constraints
////		int[][] distanceMatrix = pb.getDistanceMatrix();
////		for (int i = 1; i <= totalOps; i++) {
////			if (distanceMatrix[i][i] > 0) {
////				System.out.println("Cycle detected in precedence constraints: operation " +
////						i + " depends on itself!");
////			}
////		}
////
////		// 3. Check for TCMB constraints that might conflict with precedence
////		List<TCMB> tcmbList = pb.getTCMBList();
////		for (TCMB tcmb : tcmbList) {
////			int opA = tcmb.getOp1();
////			int opB = tcmb.getOp2();
////			int timeLimit = tcmb.getTimeConstraint();
////
////			// If we have a precedence constraint that conflicts with the TCMB
////			if (distanceMatrix[opB][opA] > 0) {
////				System.out.println("Potential conflict: TCMB constraint between operations " +
////						opA + " and " + opB + " may conflict with precedence constraint.");
////			}
////
////			// Check for extremely tight time constraints
////			int processingTime_a = pb.getProcessingTime()[opA - 1];
////			if (timeLimit < 0) {
////				System.out.println("Potential issue: TCMB time limit for operations " +
////						opA + " and " + opB + " is negative (" + timeLimit + ")");
////			}
////		}
////
////		System.out.println("Consider relaxing some constraints or verifying the problem data.");
////	}
////}
//package com.zihao.GA_TS_SLAB.Solver;
//
//import com.google.ortools.Loader;
//import com.google.ortools.linearsolver.MPConstraint;
//import com.google.ortools.linearsolver.MPObjective;
//import com.google.ortools.linearsolver.MPSolver;
//import com.google.ortools.linearsolver.MPVariable;
//import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
//import com.zihao.GA_TS_SLAB.Data.TCMB;
//import com.zihao.GA_TS_SLAB.GA.Schedule;
//
//import java.util.*;
//
///**
// * This class implements a MILP (Mixed Integer Linear Programming) solver
// * for the S-LAB scheduling problem using Google OR-Tools with SCIP solver.
// */
//public class MILPSchedule {
//	private ProblemSetting pb;
//	private static final double BIG_M = 1000; // Reduced value for "big M" constraints
//	private MPSolver solver;
//
//	// Variable maps for easier access
//	private Map<Integer, MPVariable> startTimeVars;
//	private Map<String, MPVariable> assignmentVars;
//	private Map<String, MPVariable> sequencingVars;
//	private MPVariable makespanVar;
//
//	// Statistics for debugging
//	private int constraintCount = 0;
//
//	public MILPSchedule() {
//		this.pb = ProblemSetting.getInstance();
//		this.startTimeVars = new HashMap<>();
//		this.assignmentVars = new HashMap<>();
//		this.sequencingVars = new HashMap<>();
//	}
//
//	/**
//	 * Solves the scheduling problem using MILP with OR-Tools and returns an optimal schedule.
//	 * @return An optimal Schedule object
//	 */
//	public Schedule solve() {
//		int totalOps = pb.getTotalOpNum();
//		int numMachines = pb.getMachineNum();
//
//		try {
//			// Load the OR-Tools native library
//			Loader.loadNativeLibraries();
//
//			// Create the solver (using SCIP instead of CBC)
//			solver = MPSolver.createSolver("SCIP");
//
//			if (solver == null) {
//				System.err.println("Could not create solver SCIP");
//				return null;
//			}
//
//			System.out.println("Starting to build the MILP model using SCIP solver...");
//
//			// Create variables
//			createVariables(totalOps, numMachines);
//
//			// Create objective function
//			createObjectiveFunction();
//
//			// Create constraints
//			createConstraints(totalOps, numMachines);
//
//			// 设置SCIP求解器参数，启用详细输出
//			try {
//				// 启用求解器输出
//				solver.enableOutput();
//
//				// SCIP特定参数设置
//				solver.setSolverSpecificParametersAsString(
//						// 时间限制设置 - 5小时 (18000秒)
//						"limits/time = 18000 " +     // 添加此行设置5小时时间限制
//
//								// 显示设置
//								"display/verblevel = 5 " +   // 详细信息级别 (0-5)
//								"display/freq = 10 " +       // 每10个节点显示一次状态
//								"timing/clocktype = 1 " +    // 使用CPU时间而非墙上时间
//								"display/lpinfo = TRUE " +   // 显示LP信息
//								"display/statistics = TRUE " + // 显示统计信息
//
//								// 性能设置
//								"parallel/maxnthreads = 8 " + // 最大线程数
//								"lp/threads = 8 " +           // LP求解线程数
//								"limits/gap = 0.0 " +         // 求解精度(gap)
//								"presolving/maxrounds = 10 " + // 预处理最大轮数
//
//								// 频率设置
//								"display/headerfreq = 100 " +   // 标题显示频率
//								"nodeselection/childsel = h " + // 选择更有希望的子节点
//
//								// 求解之间的信息输出
//								"messaging/limits/info = 100000 " + // 最大信息数量
//								"display/relaxfeastol = TRUE"       // 显示松弛可行性容差
//				);
//			} catch (Exception e) {
//				System.out.println("Warning: Some parameters not supported in this SCIP version: " + e.getMessage());
//			}
//
//			// 添加进度监控线程
//			final long startTime = System.currentTimeMillis();
//			final long timeLimit = 18000000; // 5小时，以毫秒为单位
//			Thread monitorThread = new Thread(() -> {
//				try {
//					while (!Thread.currentThread().isInterrupted()) {
//						Thread.sleep(5000); // 每5秒输出一次
//						try {
//							long elapsedTime = System.currentTimeMillis() - startTime;
//							long remainingTime = Math.max(0, timeLimit - elapsedTime);
//							double bestBound = solver.objective().bestBound();
//							System.out.println("Current progress - Time: " +
//									elapsedTime / 1000 + "s / " + timeLimit / 1000 + "s (" +
//									(remainingTime / 60000) + " minutes remaining), " +
//									"Best bound: " + bestBound);
//						} catch (Exception e) {
//							// 可能在求解过程中无法获取最佳界限
//							long elapsedTime = System.currentTimeMillis() - startTime;
//							long remainingTime = Math.max(0, timeLimit - elapsedTime);
//							System.out.println("Solving in progress... Time elapsed: " +
//									elapsedTime / 1000 + "s / " + timeLimit / 1000 + "s (" +
//									(remainingTime / 60000) + " minutes remaining)");
//						}
//					}
//				} catch (InterruptedException e) {
//					// 正常中断
//				}
//			});
//			monitorThread.setDaemon(true);
//			monitorThread.start();
//
//			// Solve the model
//			System.out.println("Solving model with " + solver.numVariables() + " variables and " +
//					solver.numConstraints() + " constraints...");
//			System.out.println("Using SCIP solver with 8 threads and verbose output");
//			System.out.println("Time limit set to 5 hours (18000 seconds)");
//
//			final MPSolver.ResultStatus resultStatus;
//			try {
//				resultStatus = solver.solve();
//			} finally {
//				monitorThread.interrupt(); // 停止监控线程
//			}
//
//			// Check solution status
//			if (resultStatus == MPSolver.ResultStatus.OPTIMAL ||
//					resultStatus == MPSolver.ResultStatus.FEASIBLE) {
//
//				System.out.println("Solution found!");
//				if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
//					System.out.println("Optimal solution found!");
//				} else {
//					System.out.println("Feasible (but possibly not optimal) solution found.");
//				}
//
//				System.out.println("Objective value (makespan): " + solver.objective().value());
//				System.out.println("Best bound: " + solver.objective().bestBound());
//				System.out.println("Gap: " + (solver.objective().value() - solver.objective().bestBound()) / solver.objective().value() * 100 + "%");
//
//				// Convert the solution to a Schedule object
//				return convertToSchedule(totalOps);
//			} else {
//				System.out.println("No solution found. Status: " + resultStatus);
//				analyzeInfeasibility(totalOps, numMachines);
//				return null;
//			}
//		} catch (Exception e) {
//			System.err.println("Error solving MILP model: " + e.getMessage());
//			e.printStackTrace();
//			return null;
//		}
//	}
//
//	/**
//	 * Creates all variables needed for the model
//	 */
//	private void createVariables(int totalOps, int numMachines) {
//		// Create start time variables for each operation (continuous)
//		for (int op = 1; op <= totalOps; op++) {
//			MPVariable startTimeVar = solver.makeNumVar(0.0, BIG_M, "s_" + op);
//			startTimeVars.put(op, startTimeVar);
//		}
//
//		// Create assignment variables (binary)
//		for (int op = 1; op <= totalOps; op++) {
//			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//			for (int machine : compatibleMachines) {
//				MPVariable assignVar = solver.makeIntVar(0.0, 1.0, "x_" + op + "_" + machine);
//				assignmentVars.put(op + "_" + machine, assignVar);
//			}
//		}
//
//		// Sequencing variables will be created in the machine exclusivity constraints section
//
//		// Create makespan variable (continuous)
//		makespanVar = solver.makeNumVar(0.0, BIG_M, "makespan");
//
//		System.out.println("Created variables:");
//		System.out.println("- Start time variables: " + startTimeVars.size());
//		System.out.println("- Assignment variables: " + assignmentVars.size());
//	}
//
//	/**
//	 * Creates the objective function to minimize makespan
//	 */
//	private void createObjectiveFunction() {
//		MPObjective objective = solver.objective();
//		objective.setCoefficient(makespanVar, 1.0);
//		objective.setMinimization();
//	}
//
//	/**
//	 * Creates all constraints for the MILP model
//	 */
//	private void createConstraints(int totalOps, int numMachines) {
//		constraintCount = 0;
//
//		// Add uniqueness of operation assignment constraints
//		addOperationAssignmentConstraints(totalOps);
//
//		// Add operation precedence constraints
//		addPrecedenceConstraints(totalOps);
//
//		// Add mutual exclusivity of machine usage constraints
//		addMachineExclusivityConstraints(totalOps, numMachines);
//
//		// Add time constraints (TCMB)
//		addTimeConstraints();
//
//		// Add makespan definition constraints
//		addMakespanConstraints(totalOps);
//
//		System.out.println("Created a total of " + constraintCount + " constraints");
//	}
//
//	/**
//	 * Add constraints ensuring each operation is assigned to exactly one compatible machine
//	 */
//	private void addOperationAssignmentConstraints(int totalOps) {
//		int initialCount = constraintCount;
//
//		for (int op = 1; op <= totalOps; op++) {
//			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//
//			// Create constraint: sum(x_op_m) = 1 for all compatible machines
//			MPConstraint constraint = solver.makeConstraint(1.0, 1.0, "op_assign_" + op);
//
//			for (int machine : compatibleMachines) {
//				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
//				constraint.setCoefficient(assignVar, 1.0);
//			}
//
//			constraintCount++;
//		}
//
//		System.out.println("Added " + (constraintCount - initialCount) + " operation assignment constraints");
//	}
//
//	/**
//	 * Add constraints for operation precedence from the DAG
//	 */
//	private void addPrecedenceConstraints(int totalOps) {
//		int initialCount = constraintCount;
//
//		int[][] distanceMatrix = pb.getDistanceMatrix();
//
//		for (int i = 1; i <= totalOps; i++) {
//			for (int j = 1; j <= totalOps; j++) {
//				// If operation i precedes operation j (distance > 0)
//				if (distanceMatrix[i][j] > 0) {
//					// Create constraint: s_j >= s_i + p_i
//					MPConstraint constraint = solver.makeConstraint(
//							0.0,
//							MPSolver.infinity(),
//							"precedence_" + i + "_" + j
//					);
//
//					constraint.setCoefficient(startTimeVars.get(j), 1.0);
//					constraint.setCoefficient(startTimeVars.get(i), -1.0);
//
//					// Add processing time of operation i
//					double processingTime_i = pb.getProcessingTime()[i - 1];
//					constraint.setLb(processingTime_i);
//
//					constraintCount++;
//				}
//			}
//		}
//
//		System.out.println("Added " + (constraintCount - initialCount) + " precedence constraints");
//	}
//
//	/**
//	 * Add constraints ensuring operations don't overlap on the same machine.
//	 * 修改了 machine exclusivity 部分，调整了 big-M 项中各变量的符号
//	 * 采用如下两组约束：
//	 *  (1) s_op2 - s_op1 - M·(y + x1 + x2) >= p_op1 - 3M
//	 *  (2) s_op1 - s_op2 + M·y - M·(x1 + x2) >= p_op2 - 2M
//	 */
//	private void addMachineExclusivityConstraints(int totalOps, int numMachines) {
//		int initialCount = constraintCount;
//
//		// For each machine, we need to prevent overlapping operations
//		for (int machine = 1; machine <= numMachines; machine++) {
//			// Find all operations that can be executed on this machine
//			List<Integer> compatibleOps = new ArrayList<>();
//			for (int op = 1; op <= totalOps; op++) {
//				List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//				if (compatibleMachines != null && compatibleMachines.contains(machine)) {
//					compatibleOps.add(op);
//				}
//			}
//
//			// For each pair of operations that can run on this machine,
//			// ensure they don't overlap when assigned to this machine
//			for (int i = 0; i < compatibleOps.size(); i++) {
//				int op1 = compatibleOps.get(i);
//				double pt1 = pb.getProcessingTime()[op1 - 1]; // Processing time of op1
//
//				for (int j = i + 1; j < compatibleOps.size(); j++) {
//					int op2 = compatibleOps.get(j);
//					double pt2 = pb.getProcessingTime()[op2 - 1]; // Processing time of op2
//
//					// Create a binary variable for sequencing these operations
//					// y=1 if op1 precedes op2, y=0 if op2 precedes op1
//					String varName = "y_" + op1 + "_" + op2 + "_" + machine;
//					MPVariable y = solver.makeIntVar(0, 1, varName);
//					sequencingVars.put(op1 + "_" + op2 + "_" + machine, y);
//
//					// Get assignment variables for these operations on this machine
//					MPVariable x1 = assignmentVars.get(op1 + "_" + machine);
//					MPVariable x2 = assignmentVars.get(op2 + "_" + machine);
//
//					double M = BIG_M;
//					// Constraint (1): s_op2 - s_op1 - M*(y + x1 + x2) >= pt1 - 3*M
//					MPConstraint c1 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
//							"seq1_" + op1 + "_" + op2 + "_" + machine);
//					c1.setCoefficient(startTimeVars.get(op2), 1.0);
//					c1.setCoefficient(startTimeVars.get(op1), -1.0);
//					// 修改处：y, x1, x2 的系数均为 -M
//					c1.setCoefficient(y, -M);
//					c1.setCoefficient(x1, -M);
//					c1.setCoefficient(x2, -M);
//					c1.setLb(pt1 - 3 * M);
//					constraintCount++;
//
//					// Constraint (2): s_op1 - s_op2 + M*y - M*(x1 + x2) >= pt2 - 2*M
//					MPConstraint c2 = solver.makeConstraint(-MPSolver.infinity(), MPSolver.infinity(),
//							"seq2_" + op1 + "_" + op2 + "_" + machine);
//					c2.setCoefficient(startTimeVars.get(op1), 1.0);
//					c2.setCoefficient(startTimeVars.get(op2), -1.0);
//					// 修改处：y 的系数为 +M，x1 和 x2 的系数为 -M
//					c2.setCoefficient(y, M);
//					c2.setCoefficient(x1, -M);
//					c2.setCoefficient(x2, -M);
//					c2.setLb(pt2 - 2 * M);
//					constraintCount++;
//				}
//			}
//		}
//
//		System.out.println("- Sequencing variables: " + sequencingVars.size());
//		System.out.println("Added " + (constraintCount - initialCount) + " machine exclusivity constraints");
//	}
//
//	/**
//	 * Add TCMB (Time Constraints by Mutual Boundaries) constraints
//	 * Corrected to properly model end of A to start of B time constraints
//	 */
//	private void addTimeConstraints() {
//		int initialCount = constraintCount;
//
//		List<TCMB> tcmbList = pb.getTCMBList();
//
//		// First, store all constraints to enable debugging
//		List<MPConstraint> tcmbConstraints = new ArrayList<>();
//
//		for (TCMB tcmb : tcmbList) {
//			int opA = tcmb.getOp1();
//			int opB = tcmb.getOp2();
//			int timeLimit = tcmb.getTimeConstraint();
//
//			// End of A to start of B constraint:
//			// s_b - (s_a + p_a) <= timeLimit
//			// Effectively: The time between when A fines and B starts cannot exceed timeLimit
//
//			double processingTime_a = pb.getProcessingTime()[opA - 1];
//
//			// Create the constraint: s_b - s_a <= timeLimit + p_a
//			MPConstraint constraint = solver.makeConstraint(
//					-MPSolver.infinity(),
//					timeLimit + processingTime_a,
//					"tcmb_" + opA + "_" + opB
//			);
//
//			constraint.setCoefficient(startTimeVars.get(opB), 1.0);
//			constraint.setCoefficient(startTimeVars.get(opA), -1.0);
//
//			// Store the constraint for potential debugging
//			tcmbConstraints.add(constraint);
//
//			constraintCount++;
//		}
//
//		System.out.println("Added " + (constraintCount - initialCount) + " TCMB constraints");
//	}
//
//	/**
//	 * Add constraints defining the makespan
//	 */
//	private void addMakespanConstraints(int totalOps) {
//		int initialCount = constraintCount;
//
//		for (int op = 1; op <= totalOps; op++) {
//			// Makespan >= s_a + p_a for all operations a
//			MPConstraint constraint = solver.makeConstraint(
//					0.0,
//					MPSolver.infinity(),
//					"makespan_" + op
//			);
//
//			constraint.setCoefficient(makespanVar, 1.0);
//			constraint.setCoefficient(startTimeVars.get(op), -1.0);
//
//			double processingTime = pb.getProcessingTime()[op - 1];
//			constraint.setLb(processingTime);
//
//			constraintCount++;
//		}
//
//		System.out.println("Added " + (constraintCount - initialCount) + " makespan constraints");
//	}
//
//	/**
//	 * Converts the solution from the optimizer to a Schedule object
//	 */
//	private Schedule convertToSchedule(int totalOps) {
//		Map<Integer, Integer> startTimes = new HashMap<>();
//		Map<Integer, Integer> assignedMachine = new HashMap<>();
//
//		// Extract start times
//		for (int op = 1; op <= totalOps; op++) {
//			startTimes.put(op, (int) Math.round(startTimeVars.get(op).solutionValue()));
//		}
//
//		// Extract machine assignments
//		for (int op = 1; op <= totalOps; op++) {
//			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//
//			for (int machine : compatibleMachines) {
//				MPVariable assignVar = assignmentVars.get(op + "_" + machine);
//
//				if (Math.round(assignVar.solutionValue()) == 1) {
//					assignedMachine.put(op, machine);
//					break;
//				}
//			}
//		}
//
//		// Output detailed solution
//		System.out.println("\nDetailed solution:");
//		System.out.println("Operation\tMachine\tStart Time\tEnd Time");
//		for (int op = 1; op <= totalOps; op++) {
//			int machine = assignedMachine.get(op);
//			int startTime = startTimes.get(op);
//			int endTime = startTime + pb.getProcessingTime()[op - 1];
//			System.out.println(op + "\t\t" + machine + "\t" + startTime + "\t\t" + endTime);
//		}
//
//		// Create and validate the schedule
//		Schedule schedule = new Schedule(startTimes, assignedMachine);
//		validateSchedule(schedule);
//
//		return schedule;
//	}
//
//	/**
//	 * Validates the created schedule against the problem constraints
//	 */
//	private void validateSchedule(Schedule schedule) {
//		boolean precedenceValid = schedule.checkPrecedenceConstraints();
//		boolean machineValid = schedule.checkCompatibleMachines();
//		boolean occupationValid = schedule.checkMachineOccupation();
//
//		System.out.println("Schedule validation:");
//		System.out.println("Precedence constraints: " + (precedenceValid ? "Valid" : "Invalid"));
//		System.out.println("Machine compatibility: " + (machineValid ? "Valid" : "Invalid"));
//		System.out.println("Machine occupation: " + (occupationValid ? "Valid" : "Invalid"));
//
//		// Additional TCMB validation
//		boolean tcmbValid = validateTCMBConstraints(schedule);
//		System.out.println("TCMB constraints: " + (tcmbValid ? "Valid" : "Invalid"));
//	}
//
//	/**
//	 * Validates that the TCMB constraints are satisfied in the schedule
//	 */
//	private boolean validateTCMBConstraints(Schedule schedule) {
//		List<TCMB> tcmbList = pb.getTCMBList();
//		Map<Integer, Integer> startTimes = schedule.getStartTimes();
//		boolean valid = true;
//
//		for (TCMB tcmb : tcmbList) {
//			int opA = tcmb.getOp1();
//			int opB = tcmb.getOp2();
//			int timeLimit = tcmb.getTimeConstraint();
//
//			int startA = startTimes.get(opA);
//			int startB = startTimes.get(opB);
//			double processingTimeA = pb.getProcessingTime()[opA - 1];
//
//			// End of A to Start of B
//			double endA = startA + processingTimeA;
//			double timeBetween = startB - endA;
//
//			if (timeBetween > timeLimit) {
//				System.out.println("TCMB violation: Time between end of " + opA +
//						" and start of " + opB + " is " + timeBetween +
//						", exceeding limit of " + timeLimit);
//				valid = false;
//			}
//		}
//
//		return valid;
//	}
//
//	/**
//	 * Attempts to diagnose infeasibility issues in the model
//	 */
//	private void analyzeInfeasibility(int totalOps, int numMachines) {
//		System.out.println("Analyzing potential causes of infeasibility...");
//
//		// 1. Check if there are operations with no compatible machines
//		for (int op = 1; op <= totalOps; op++) {
//			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
//			if (compatibleMachines == null || compatibleMachines.isEmpty()) {
//				System.out.println("Operation " + op + " has no compatible machines!");
//			}
//		}
//
//		// 2. Check for cycles in precedence constraints
//		int[][] distanceMatrix = pb.getDistanceMatrix();
//		for (int i = 1; i <= totalOps; i++) {
//			if (distanceMatrix[i][i] > 0) {
//				System.out.println("Cycle detected in precedence constraints: operation " +
//						i + " depends on itself!");
//			}
//		}
//
//		// 3. Check for TCMB constraints that might conflict with precedence
//		List<TCMB> tcmbList = pb.getTCMBList();
//		for (TCMB tcmb : tcmbList) {
//			int opA = tcmb.getOp1();
//			int opB = tcmb.getOp2();
//			int timeLimit = tcmb.getTimeConstraint();
//
//			// If we have a precedence constraint that conflicts with the TCMB
//			if (distanceMatrix[opB][opA] > 0) {
//				System.out.println("Potential conflict: TCMB constraint between operations " +
//						opA + " and " + opB + " may conflict with precedence constraint.");
//			}
//
//			// Check for extremely tight time constraints
//			int processingTime_a = pb.getProcessingTime()[opA - 1];
//			if (timeLimit < 0) {
//				System.out.println("Potential issue: TCMB time limit for operations " +
//						opA + " and " + opB + " is negative (" + timeLimit + ")");
//			}
//		}
//
//		System.out.println("Consider relaxing some constraints or verifying the problem data.");
//
//		// 4. 尝试SCIP特定的分析功能
//		System.out.println("\nAdditional SCIP-specific analysis:");
//		System.out.println("Wall time: " + solver.wallTime() + " milliseconds");
//		System.out.println("Iterations: " + solver.iterations());
//		System.out.println("Nodes: " + solver.nodes());
//		System.out.println("Infeasibility hint may be available in the solver log output.");
//	}
//}
package com.zihao.GA_TS_SLAB.Solver;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Schedule;

import java.util.*;

/**
 * This class implements a CP-SAT (Constraint Programming-Satisfiability) solver
 * for the S-LAB scheduling problem using Google OR-Tools.
 */
public class MILPSchedule {
	private ProblemSetting pb;
	private static final int BIG_M = 10000; // Large value for constraints

	// Variable maps for easier access
	private Map<Integer, IntVar> startTimeVars;
	private Map<String, Literal> assignmentVars;
	private IntVar makespanVar;

	// 为了追踪中间解而添加的回调类
	private static class SolutionPrinter extends CpSolverSolutionCallback {
		private final long startTime;
		private final Map<Integer, IntVar> startTimeVars;
		private final IntVar makespanVar;
		private int solutionCount = 0;

		public SolutionPrinter(long startTime, Map<Integer, IntVar> startTimeVars, IntVar makespanVar) {
			this.startTime = startTime;
			this.startTimeVars = startTimeVars;
			this.makespanVar = makespanVar;
		}

		@Override
		public void onSolutionCallback() {
			solutionCount++;
			long currentTime = System.currentTimeMillis() - startTime;
			int makespan = (int) value(makespanVar);

			System.out.println("Solution #" + solutionCount +
					" found at " + (currentTime / 1000.0) + " seconds. Makespan: " + makespan);

			// 每10个解才打印一次详细信息，防止输出过多
			if (solutionCount % 10 == 1) {
				System.out.println("Current start times sample (first 5 operations or less):");
				int count = 0;
				for (Map.Entry<Integer, IntVar> entry : startTimeVars.entrySet()) {
					if (count >= 5) break;
					System.out.println("  Operation " + entry.getKey() + ": " + value(entry.getValue()));
					count++;
				}
				System.out.println();
			}
		}

		public int getSolutionCount() {
			return solutionCount;
		}
	}

	public MILPSchedule() {
		this.pb = ProblemSetting.getInstance();
		this.startTimeVars = new HashMap<>();
		this.assignmentVars = new HashMap<>();
	}

	/**
	 * Solves the scheduling problem using CP-SAT with OR-Tools and returns an optimal schedule.
	 * @return An optimal Schedule object
	 */
	public Schedule solve() {
		int totalOps = pb.getTotalOpNum();
		int numMachines = pb.getMachineNum();

		try {
			// Load the OR-Tools native library
			Loader.loadNativeLibraries();

			System.out.println("Starting to build the CP-SAT model...");

			// Create the CP-SAT model
			CpModel model = new CpModel();

			// Create variables
			createVariables(model, totalOps, numMachines);

			// Create objective function
			createObjectiveFunction(model);

			// Create constraints
			createConstraints(model, totalOps, numMachines);

			// Create a solver and solve the model
			CpSolver solver = new CpSolver();

			// 设置求解参数 - 使用更基本的API，避免版本不兼容问题
			// 注意：有些高级参数设置可能不可用，这里使用最基本的

			System.out.println("Solving model with " + totalOps + " operations and " +
					numMachines + " machines...");
			System.out.println("Time limit: 5 hours (when supported by solver)");

			// 记录开始时间
			final long startTime = System.currentTimeMillis();

			// 创建回调来追踪中间解
			SolutionPrinter callback = new SolutionPrinter(startTime, startTimeVars, makespanVar);

			// 启动监控线程
			Thread monitorThread = new Thread(() -> {
				try {
					while (!Thread.currentThread().isInterrupted()) {
						Thread.sleep(30000); // 每30秒输出一次
						long elapsedTime = System.currentTimeMillis() - startTime;
						long remainingTime = Math.max(0, 21600000 - elapsedTime);
						System.out.println("Still solving... Time elapsed: " +
								elapsedTime / 1000 + "s / 21600s (" +
								(remainingTime / 60000) + " minutes remaining)");
					}
				} catch (InterruptedException e) {
					// 正常中断
				}
			});
			monitorThread.setDaemon(true);
			monitorThread.start();

			// 使用基本的API求解，并传入回调
			final CpSolverStatus status;
			try {
				// 原始方法可能不兼容，使用基本的solve方法
				solver.getParameters().setMaxTimeInSeconds(18000); // 5小时
				solver.getParameters().setLogSearchProgress(true);

				status = solver.solve(model, callback);
			} finally {
				monitorThread.interrupt();
			}

			// Calculate total time
			long totalTime = System.currentTimeMillis() - startTime;

			// Check solution status
			if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
				System.out.println("\nSolution found in " + (totalTime / 1000.0) + " seconds!");
				System.out.println("Total number of solutions found: " + callback.getSolutionCount());

				if (status == CpSolverStatus.OPTIMAL) {
					System.out.println("Optimal solution found!");
				} else {
					System.out.println("Feasible (but possibly not optimal) solution found.");
				}

				System.out.println("Objective value (makespan): " + solver.objectiveValue());
				System.out.println("Best bound: " + solver.bestObjectiveBound());
				if (solver.objectiveValue() > 0) {
					double gap = 100.0 * (solver.objectiveValue() - solver.bestObjectiveBound()) / solver.objectiveValue();
					System.out.println("Gap: " + String.format("%.2f", gap) + "%");
				}

				// Convert the solution to a Schedule object
				return convertToSchedule(solver, totalOps);
			} else {
				System.out.println("No solution found. Status: " + status);
				analyzeInfeasibility(totalOps, numMachines);
				return null;
			}
		} catch (Exception e) {
			System.err.println("Error solving CP-SAT model: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates all variables needed for the model
	 */
	private void createVariables(CpModel model, int totalOps, int numMachines) {
		// Create start time variables for each operation (integer)
		for (int op = 1; op <= totalOps; op++) {
			IntVar startTimeVar = model.newIntVar(0, BIG_M, "s_" + op);
			startTimeVars.put(op, startTimeVar);
		}

		// Create assignment variables (boolean)
		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
			for (int machine : compatibleMachines) {
				Literal assignVar = model.newBoolVar("x_" + op + "_" + machine);
				assignmentVars.put(op + "_" + machine, assignVar);
			}
		}

		// Create makespan variable (integer)
		makespanVar = model.newIntVar(0, BIG_M, "makespan");

		System.out.println("Created variables:");
		System.out.println("- Start time variables: " + startTimeVars.size());
		System.out.println("- Assignment variables: " + assignmentVars.size());
	}

	/**
	 * Creates the objective function to minimize makespan
	 */
	private void createObjectiveFunction(CpModel model) {
		model.minimize(makespanVar);
	}

	/**
	 * Creates all constraints for the CP-SAT model
	 */
	private void createConstraints(CpModel model, int totalOps, int numMachines) {
		int constraintCount = 0;

		// Add uniqueness of operation assignment constraints
		constraintCount += addOperationAssignmentConstraints(model, totalOps);

		// Add operation precedence constraints
		constraintCount += addPrecedenceConstraints(model, totalOps);

		// Add mutual exclusivity of machine usage constraints
		constraintCount += addMachineExclusivityConstraints(model, totalOps, numMachines);

		// Add time constraints (TCMB)
		constraintCount += addTimeConstraints(model);

		// Add makespan definition constraints
		constraintCount += addMakespanConstraints(model, totalOps);

		System.out.println("Created a total of " + constraintCount + " constraints");
	}

	/**
	 * Add constraints ensuring each operation is assigned to exactly one compatible machine
	 */
	private int addOperationAssignmentConstraints(CpModel model, int totalOps) {
		int count = 0;

		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);

			// Create constraint: sum(x_op_m) = 1 for all compatible machines
			Literal[] assignVars = new Literal[compatibleMachines.size()];
			for (int i = 0; i < compatibleMachines.size(); i++) {
				int machine = compatibleMachines.get(i);
				assignVars[i] = assignmentVars.get(op + "_" + machine);
			}

			model.addExactlyOne(assignVars);
			count++;
		}

		System.out.println("Added " + count + " operation assignment constraints");
		return count;
	}

	/**
	 * Add constraints for operation precedence from the DAG
	 */
	private int addPrecedenceConstraints(CpModel model, int totalOps) {
		int count = 0;

		int[][] distanceMatrix = pb.getDistanceMatrix();

		for (int i = 1; i <= totalOps; i++) {
			for (int j = 1; j <= totalOps; j++) {
				// If operation i precedes operation j (distance > 0)
				if (distanceMatrix[i][j] > 0) {
					IntVar s_i = startTimeVars.get(i);
					IntVar s_j = startTimeVars.get(j);
					int p_i = pb.getProcessingTime()[i - 1];

					// Create constraint: s_j >= s_i + p_i
					model.addGreaterOrEqual(s_j, LinearExpr.newBuilder()
							.add(s_i)
							.add(p_i)
							.build());

					count++;
				}
			}
		}

		System.out.println("Added " + count + " precedence constraints");
		return count;
	}

	/**
	 * Add constraints ensuring operations don't overlap on the same machine using optional intervals
	 */
	private int addMachineExclusivityConstraints(CpModel model, int totalOps, int numMachines) {
		int count = 0;

		// For each machine, create non-overlapping constraints for all operations
		for (int machine = 1; machine <= numMachines; machine++) {
			// Find all operations that can be executed on this machine
			List<Integer> compatibleOps = new ArrayList<>();
			for (int op = 1; op <= totalOps; op++) {
				List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
				if (compatibleMachines != null && compatibleMachines.contains(machine)) {
					compatibleOps.add(op);
				}
			}

			// Create interval variables for these operations on this machine
			List<IntervalVar> intervals = new ArrayList<>();

			for (int op : compatibleOps) {
				IntVar start = startTimeVars.get(op);
				int duration = pb.getProcessingTime()[op - 1];
				IntVar end = model.newIntVar(0, BIG_M, "end_" + op);

				// Constraint: end = start + duration
				model.addEquality(end, LinearExpr.newBuilder()
						.add(start)
						.add(duration)
						.build());

				// Get assignment variable
				Literal isPresent = assignmentVars.get(op + "_" + machine);

				// Create optional interval
				IntervalVar interval = model.newOptionalIntervalVar(
						start, model.newConstant(duration), end, isPresent,
						"interval_" + op + "_" + machine);

				intervals.add(interval);
			}

			// Ensure no overlap between operations on this machine
			model.addNoOverlap(intervals);

			count++;
		}

		System.out.println("Added " + count + " machine exclusivity constraints (NoOverlap)");
		return count;
	}

	/**
	 * Add TCMB (Time Constraints by Mutual Boundaries) constraints
	 */
	private int addTimeConstraints(CpModel model) {
		int count = 0;

		List<TCMB> tcmbList = pb.getTCMBList();

		for (TCMB tcmb : tcmbList) {
			int opA = tcmb.getOp1();
			int opB = tcmb.getOp2();
			int timeLimit = tcmb.getTimeConstraint();

			IntVar s_a = startTimeVars.get(opA);
			IntVar s_b = startTimeVars.get(opB);
			int p_a = pb.getProcessingTime()[opA - 1];

			// End of A to start of B constraint:
			// s_b - (s_a + p_a) <= timeLimit
			// Equivalent to: s_b <= s_a + p_a + timeLimit

			model.addLessOrEqual(s_b, LinearExpr.newBuilder()
					.add(s_a)
					.add(p_a)
					.add(timeLimit)
					.build());

			count++;
		}

		System.out.println("Added " + count + " TCMB constraints");
		return count;
	}

	/**
	 * Add constraints defining the makespan
	 */
	private int addMakespanConstraints(CpModel model, int totalOps) {
		int count = 0;

		for (int op = 1; op <= totalOps; op++) {
			IntVar s_op = startTimeVars.get(op);
			int p_op = pb.getProcessingTime()[op - 1];

			// Makespan >= s_op + p_op for all operations
			model.addGreaterOrEqual(makespanVar, LinearExpr.newBuilder()
					.add(s_op)
					.add(p_op)
					.build());

			count++;
		}

		System.out.println("Added " + count + " makespan constraints");
		return count;
	}

	/**
	 * Converts the solution from the optimizer to a Schedule object
	 */
	private Schedule convertToSchedule(CpSolver solver, int totalOps) {
		Map<Integer, Integer> startTimes = new HashMap<>();
		Map<Integer, Integer> assignedMachine = new HashMap<>();

		// Extract start times
		for (int op = 1; op <= totalOps; op++) {
			IntVar startTimeVar = startTimeVars.get(op);
			startTimes.put(op, (int) solver.value(startTimeVar));
		}

		// Extract machine assignments
		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);

			for (int machine : compatibleMachines) {
				Literal assignVar = assignmentVars.get(op + "_" + machine);

				if (solver.booleanValue(assignVar)) {
					assignedMachine.put(op, machine);
					break;
				}
			}
		}

		// Output detailed solution
		System.out.println("\nDetailed solution:");
		System.out.println("Operation\tMachine\tStart Time\tEnd Time");
		for (int op = 1; op <= totalOps; op++) {
			int machine = assignedMachine.get(op);
			int startTime = startTimes.get(op);
			int endTime = startTime + pb.getProcessingTime()[op - 1];
			System.out.println(op + "\t\t" + machine + "\t" + startTime + "\t\t" + endTime);
		}

		// Create and validate the schedule
		Schedule schedule = new Schedule(startTimes, assignedMachine);
		validateSchedule(schedule);

		return schedule;
	}

	/**
	 * Validates the created schedule against the problem constraints
	 */
	private void validateSchedule(Schedule schedule) {
		boolean precedenceValid = schedule.checkPrecedenceConstraints();
		boolean machineValid = schedule.checkCompatibleMachines();
		boolean occupationValid = schedule.checkMachineOccupation();

		System.out.println("Schedule validation:");
		System.out.println("Precedence constraints: " + (precedenceValid ? "Valid" : "Invalid"));
		System.out.println("Machine compatibility: " + (machineValid ? "Valid" : "Invalid"));
		System.out.println("Machine occupation: " + (occupationValid ? "Valid" : "Invalid"));

		// Additional TCMB validation
		boolean tcmbValid = validateTCMBConstraints(schedule);
		System.out.println("TCMB constraints: " + (tcmbValid ? "Valid" : "Invalid"));
	}

	/**
	 * Validates that the TCMB constraints are satisfied in the schedule
	 */
	private boolean validateTCMBConstraints(Schedule schedule) {
		List<TCMB> tcmbList = pb.getTCMBList();
		Map<Integer, Integer> startTimes = schedule.getStartTimes();
		boolean valid = true;

		for (TCMB tcmb : tcmbList) {
			int opA = tcmb.getOp1();
			int opB = tcmb.getOp2();
			int timeLimit = tcmb.getTimeConstraint();

			int startA = startTimes.get(opA);
			int startB = startTimes.get(opB);
			double processingTimeA = pb.getProcessingTime()[opA - 1];

			// End of A to Start of B
			double endA = startA + processingTimeA;
			double timeBetween = startB - endA;

			if (timeBetween > timeLimit) {
				System.out.println("TCMB violation: Time between end of " + opA +
						" and start of " + opB + " is " + timeBetween +
						", exceeding limit of " + timeLimit);
				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Attempts to diagnose infeasibility issues in the model
	 */
	private void analyzeInfeasibility(int totalOps, int numMachines) {
		System.out.println("Analyzing potential causes of infeasibility...");

		// 1. Check if there are operations with no compatible machines
		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
			if (compatibleMachines == null || compatibleMachines.isEmpty()) {
				System.out.println("Operation " + op + " has no compatible machines!");
			}
		}

		// 2. Check for cycles in precedence constraints
		int[][] distanceMatrix = pb.getDistanceMatrix();
		for (int i = 1; i <= totalOps; i++) {
			if (distanceMatrix[i][i] > 0) {
				System.out.println("Cycle detected in precedence constraints: operation " +
						i + " depends on itself!");
			}
		}

		// 3. Check for TCMB constraints that might conflict with precedence
		List<TCMB> tcmbList = pb.getTCMBList();
		for (TCMB tcmb : tcmbList) {
			int opA = tcmb.getOp1();
			int opB = tcmb.getOp2();
			int timeLimit = tcmb.getTimeConstraint();

			// If we have a precedence constraint that conflicts with the TCMB
			if (distanceMatrix[opB][opA] > 0) {
				System.out.println("Potential conflict: TCMB constraint between operations " +
						opA + " and " + opB + " may conflict with precedence constraint.");
			}

			// Check for extremely tight time constraints
			if (timeLimit < 0) {
				System.out.println("Potential issue: TCMB time limit for operations " +
						opA + " and " + opB + " is negative (" + timeLimit + ")");
			}
		}

		System.out.println("Consider relaxing some constraints or verifying the problem data.");
	}
}