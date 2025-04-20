package com.zihao.GA_TS_SLAB.Solver;

import java.util.*;
import net.sf.jmpi.solver.cplex.SolverCPLEX;
import net.sf.jmpi.main.MpConstraint;
import net.sf.jmpi.main.MpDirection;
import net.sf.jmpi.main.MpOperator;
import net.sf.jmpi.main.MpResult;
import net.sf.jmpi.main.MpVariable;
import net.sf.jmpi.main.MpProblem;
import net.sf.jmpi.main.expression.MpExpr;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Schedule;

/**
 * This class implements a MILP (Mixed Integer Linear Programming) solver
 * for the S-LAB scheduling problem using JMPI's CPLEX solver interface.
 */
public class CPLEXSchedule {
	private ProblemSetting pb;
	private static final double BIG_M = 1000; // Reduced value for "big M" constraints
	private SolverCPLEX solver;
	private MpProblem problem;

	// Variable maps for easier access
	private Map<Integer, Object> startTimeVars;
	private Map<String, Object> assignmentVars;
	private Map<String, Object> sequencingVars;
	private Object makespanVar;

	// Statistics for debugging
	private int constraintCount = 0;

	public CPLEXSchedule() {
		this.pb = ProblemSetting.getInstance();
		this.startTimeVars = new HashMap<>();
		this.assignmentVars = new HashMap<>();
		this.sequencingVars = new HashMap<>();
	}

	/**
	 * Solves the scheduling problem using MILP with CPLEX and returns an optimal schedule.
	 * @return An optimal Schedule object
	 */
	public Schedule solve() {
		int totalOps = pb.getTotalOpNum();
		int numMachines = pb.getMachineNum();

		try {
			// Create the problem model
			problem = new MpProblem();

			// Create the CPLEX solver
			solver = new SolverCPLEX();

			if (solver == null) {
				System.err.println("Could not create CPLEX solver");
				return null;
			}

			System.out.println("Starting to build the MILP model with CPLEX...");

			// Create variables
			createVariables(totalOps, numMachines);

			// Create constraints
			createConstraints(totalOps, numMachines);

			// Create objective function
			createObjectiveFunction();

			// Configure CPLEX parameters for better performance
			configureSolver();

			// Add the problem to the solver
			solver.add(problem);

			// Solve the model
			System.out.println("Solving model with CPLEX...");
			System.out.println("Using CPLEX as solver with optimality focus...");

			MpResult result = solver.solve();

			// Check solution status
			if (result != null) {
				System.out.println("Solution found!");
				System.out.println("Objective value (makespan): " + result.getObjective());

				// Convert the solution to a Schedule object
				return convertToSchedule(totalOps, result);
			} else {
				System.out.println("No solution found.");
				analyzeInfeasibility(totalOps, numMachines);
				return null;
			}
		} catch (Exception e) {
			System.err.println("Error solving MILP model: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Configure CPLEX solver parameters for better performance
	 */
	private void configureSolver() {
		try {
			// 设置详细输出级别（1表示输出求解信息）
			solver.setVerbose(1);

			// 设置时间限制（秒），设置为0表示无限制
			solver.setTimeout(0);

			System.out.println("CPLEX solver configured for optimality");
		} catch (Exception e) {
			System.out.println("Warning: Error setting CPLEX parameters. Using defaults: " + e.getMessage());
		}
	}

	/**
	 * Creates all variables needed for the model
	 */
	private void createVariables(int totalOps, int numMachines) {
		// Create start time variables for each operation (continuous)
		for (int op = 1; op <= totalOps; op++) {
			String varName = "s_" + op;
			MpVariable startTimeVar = problem.addVar(0.0, varName, Double.MAX_VALUE, Double.class);
			startTimeVars.put(op, varName);
		}

		// Create assignment variables (binary)
		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
			for (int machine : compatibleMachines) {
				String varName = "x_" + op + "_" + machine;
				MpVariable assignVar = problem.addVar(0, varName, 1, Boolean.class);
				assignmentVars.put(op + "_" + machine, varName);
			}
		}

		// Create makespan variable (continuous)
		makespanVar = "makespan";
		problem.addVar(0.0, makespanVar, Double.MAX_VALUE, Double.class);

		System.out.println("Created variables:");
		System.out.println("- Start time variables: " + startTimeVars.size());
		System.out.println("- Assignment variables: " + assignmentVars.size());
	}

	/**
	 * Creates the objective function to minimize makespan
	 */
	private void createObjectiveFunction() {
		MpExpr objective = new MpExpr();
		objective.addTerm(makespanVar, 1.0);
		problem.setObjective(objective, MpDirection.MIN);
	}

	/**
	 * Creates all constraints for the MILP model
	 */
	private void createConstraints(int totalOps, int numMachines) {
		constraintCount = 0;

		// Add uniqueness of operation assignment constraints
		addOperationAssignmentConstraints(totalOps);

		// Add operation precedence constraints
		addPrecedenceConstraints(totalOps);

		// Add mutual exclusivity of machine usage constraints
		addMachineExclusivityConstraints(totalOps, numMachines);

		// Add time constraints (TCMB)
		addTimeConstraints();

		// Add makespan definition constraints
		addMakespanConstraints(totalOps);

		System.out.println("Created a total of " + constraintCount + " constraints");
	}

	/**
	 * Add constraints ensuring each operation is assigned to exactly one compatible machine
	 */
	private void addOperationAssignmentConstraints(int totalOps) {
		int initialCount = constraintCount;

		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);

			// Create constraint: sum(x_op_m) = 1 for all compatible machines
			MpExpr lhs = new MpExpr();
			for (int machine : compatibleMachines) {
				lhs.addTerm(assignmentVars.get(op + "_" + machine), 1.0);
			}

			MpExpr rhs = new MpExpr();
			rhs.addTerm(1.0); // 常数项1.0

			problem.add(lhs, MpOperator.EQ, rhs);
			constraintCount++;
		}

		System.out.println("Added " + (constraintCount - initialCount) + " operation assignment constraints");
	}

	/**
	 * Add constraints for operation precedence from the DAG
	 */
	private void addPrecedenceConstraints(int totalOps) {
		int initialCount = constraintCount;

		int[][] distanceMatrix = pb.getDistanceMatrix();

		for (int i = 1; i <= totalOps; i++) {
			for (int j = 1; j <= totalOps; j++) {
				// If operation i precedes operation j (distance > 0)
				if (distanceMatrix[i][j] > 0) {
					// Create constraint: s_j - s_i >= p_i
					MpExpr lhs = new MpExpr();
					lhs.addTerm(startTimeVars.get(j), 1.0);
					lhs.addTerm(startTimeVars.get(i), -1.0);

					MpExpr rhs = new MpExpr();
					// Add processing time of operation i
					double processingTime_i = pb.getProcessingTime()[i - 1];
					rhs.addTerm(processingTime_i);

					problem.add(lhs, MpOperator.GE, rhs);
					constraintCount++;
				}
			}
		}

		System.out.println("Added " + (constraintCount - initialCount) + " precedence constraints");
	}

	/**
	 * Add constraints ensuring operations don't overlap on the same machine.
	 * Uses the Big-M method to model disjunctive constraints:
	 *  (1) s_op2 - s_op1 - M·(y + x1 + x2) >= p_op1 - 3M
	 *  (2) s_op1 - s_op2 + M·y - M·(x1 + x2) >= p_op2 - 2M
	 */
	private void addMachineExclusivityConstraints(int totalOps, int numMachines) {
		int initialCount = constraintCount;

		// For each machine, we need to prevent overlapping operations
		for (int machine = 1; machine <= numMachines; machine++) {
			// Find all operations that can be executed on this machine
			List<Integer> compatibleOps = new ArrayList<>();
			for (int op = 1; op <= totalOps; op++) {
				List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
				if (compatibleMachines != null && compatibleMachines.contains(machine)) {
					compatibleOps.add(op);
				}
			}

			// For each pair of operations that can run on this machine,
			// ensure they don't overlap when assigned to this machine
			for (int i = 0; i < compatibleOps.size(); i++) {
				int op1 = compatibleOps.get(i);
				double pt1 = pb.getProcessingTime()[op1 - 1]; // Processing time of op1

				for (int j = i + 1; j < compatibleOps.size(); j++) {
					int op2 = compatibleOps.get(j);
					double pt2 = pb.getProcessingTime()[op2 - 1]; // Processing time of op2

					// Create a binary variable for sequencing these operations
					// y=1 if op1 precedes op2, y=0 if op2 precedes op1
					String varName = "y_" + op1 + "_" + op2 + "_" + machine;
					problem.addVar(0, varName, 1, Boolean.class);
					sequencingVars.put(op1 + "_" + op2 + "_" + machine, varName);

					// Get assignment variables for these operations on this machine
					Object x1 = assignmentVars.get(op1 + "_" + machine);
					Object x2 = assignmentVars.get(op2 + "_" + machine);

					double M = BIG_M;

					// Constraint (1): s_op2 - s_op1 - M*(y + x1 + x2) >= pt1 - 3*M
					MpExpr lhs1 = new MpExpr();
					lhs1.addTerm(startTimeVars.get(op2), 1.0);
					lhs1.addTerm(startTimeVars.get(op1), -1.0);
					lhs1.addTerm(sequencingVars.get(op1 + "_" + op2 + "_" + machine), -M);
					lhs1.addTerm(x1, -M);
					lhs1.addTerm(x2, -M);

					MpExpr rhs1 = new MpExpr();
					rhs1.addTerm(pt1 - 3 * M);

					problem.add(lhs1, MpOperator.GE, rhs1);
					constraintCount++;

					// Constraint (2): s_op1 - s_op2 + M*y - M*(x1 + x2) >= pt2 - 2*M
					MpExpr lhs2 = new MpExpr();
					lhs2.addTerm(startTimeVars.get(op1), 1.0);
					lhs2.addTerm(startTimeVars.get(op2), -1.0);
					lhs2.addTerm(sequencingVars.get(op1 + "_" + op2 + "_" + machine), M);
					lhs2.addTerm(x1, -M);
					lhs2.addTerm(x2, -M);

					MpExpr rhs2 = new MpExpr();
					rhs2.addTerm(pt2 - 2 * M);

					problem.add(lhs2, MpOperator.GE, rhs2);
					constraintCount++;
				}
			}
		}

		System.out.println("- Sequencing variables: " + sequencingVars.size());
		System.out.println("Added " + (constraintCount - initialCount) + " machine exclusivity constraints");
	}

	/**
	 * Add TCMB (Time Constraints by Mutual Boundaries) constraints
	 * Models the end of A to start of B time constraints
	 */
	private void addTimeConstraints() {
		int initialCount = constraintCount;

		List<TCMB> tcmbList = pb.getTCMBList();

		for (TCMB tcmb : tcmbList) {
			int opA = tcmb.getOp1();
			int opB = tcmb.getOp2();
			int timeLimit = tcmb.getTimeConstraint();

			// End of A to start of B constraint:
			// s_b - (s_a + p_a) <= timeLimit
			// Effectively: The time between when A finishes and B starts cannot exceed timeLimit
			double processingTime_a = pb.getProcessingTime()[opA - 1];

			// Create the constraint: s_b - s_a <= timeLimit + p_a
			MpExpr lhs = new MpExpr();
			lhs.addTerm(startTimeVars.get(opB), 1.0);
			lhs.addTerm(startTimeVars.get(opA), -1.0);

			MpExpr rhs = new MpExpr();
			rhs.addTerm(timeLimit + processingTime_a);

			problem.add(lhs, MpOperator.LE, rhs);
			constraintCount++;
		}

		System.out.println("Added " + (constraintCount - initialCount) + " TCMB constraints");
	}

	/**
	 * Add constraints defining the makespan
	 */
	private void addMakespanConstraints(int totalOps) {
		int initialCount = constraintCount;

		for (int op = 1; op <= totalOps; op++) {
			// Makespan >= s_a + p_a for all operations a
			MpExpr lhs = new MpExpr();
			lhs.addTerm(makespanVar, 1.0);
			lhs.addTerm(startTimeVars.get(op), -1.0);

			MpExpr rhs = new MpExpr();
			double processingTime = pb.getProcessingTime()[op - 1];
			rhs.addTerm(processingTime);

			problem.add(lhs, MpOperator.GE, rhs);
			constraintCount++;
		}

		System.out.println("Added " + (constraintCount - initialCount) + " makespan constraints");
	}

	/**
	 * Converts the solution from the optimizer to a Schedule object
	 */
	private Schedule convertToSchedule(int totalOps, MpResult result) {
		Map<Integer, Integer> startTimes = new HashMap<>();
		Map<Integer, Integer> assignedMachine = new HashMap<>();

		// Extract start times
		for (int op = 1; op <= totalOps; op++) {
			startTimes.put(op, result.get(startTimeVars.get(op)).intValue());
		}

		// Extract machine assignments
		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);

			for (int machine : compatibleMachines) {
				if (result.getBoolean(assignmentVars.get(op + "_" + machine))) {
					assignedMachine.put(op, machine);
					break;
				}
			}
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