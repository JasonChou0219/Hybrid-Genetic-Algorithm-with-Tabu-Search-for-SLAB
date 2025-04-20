


package com.zihao.GA_TS_SLAB.Solver;

import com.gurobi.gurobi.*;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import com.zihao.GA_TS_SLAB.Data.TCMB;
import com.zihao.GA_TS_SLAB.GA.Schedule;

import java.util.*;

/**
 * This class implements a MILP (Mixed Integer Linear Programming) solver
 * for the S-LAB scheduling problem using Gurobi optimizer.
 */
public class GurobiSchedule {
	private ProblemSetting pb;
	private static final double BIG_M = 1000; // Reduced value for "big M" constraints
	private GRBEnv env;
	private GRBModel model;

	// Variable maps for easier access
	private Map<Integer, GRBVar> startTimeVars;
	private Map<String, GRBVar> assignmentVars;
	private Map<String, GRBVar> sequencingVars;
	private GRBVar makespanVar;

	// Statistics for debugging
	private int constraintCount = 0;

	public GurobiSchedule() {
		this.pb = ProblemSetting.getInstance();
		this.startTimeVars = new HashMap<>();
		this.assignmentVars = new HashMap<>();
		this.sequencingVars = new HashMap<>();
	}

	/**
	 * Solves the scheduling problem using MILP with Gurobi and returns an optimal schedule.
	 * @return An optimal Schedule object
	 */
	public Schedule solve() {
		int totalOps = pb.getTotalOpNum();
		int numMachines = pb.getMachineNum();

		try {
			// Create Gurobi environment
			env = new GRBEnv();

			// Create Gurobi model
			model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "SLAB_Scheduling");

			System.out.println("Starting to build the MILP model with Gurobi...");

			// Create variables
			createVariables(totalOps, numMachines);

			// Create objective function
			createObjectiveFunction();

			// Create constraints
			createConstraints(totalOps, numMachines);

			// Set solver parameters
			model.set(GRB.IntParam.Threads, 8);               // 使用8个线程
			model.set(GRB.IntParam.Presolve, 2);              // 激进的预处理
			model.set(GRB.IntParam.MIPFocus, 1);              // 专注于找到可行解
			model.set(GRB.DoubleParam.MIPGap, 0.05);          // 接受13%的间隙
//			model.set(GRB.DoubleParam.TimeLimit, 3600);       // 1小时时间限制
			model.set(GRB.DoubleParam.TimeLimit, 36000);
			model.set(GRB.IntParam.OutputFlag, 1);            // 输出求解日志


			// Solve the model
			System.out.println("Solving model with " + model.get(GRB.IntAttr.NumVars) + " variables and " +
					model.get(GRB.IntAttr.NumConstrs) + " constraints...");
			System.out.println("Using 8 threads and advanced optimization settings");
			System.out.println("Target MIP Gap: 13%");

			model.optimize();

			// Check solution status
			int status = model.get(GRB.IntAttr.Status);
			if (status == GRB.Status.OPTIMAL || status == GRB.Status.SUBOPTIMAL ||
					status == GRB.Status.TIME_LIMIT && model.get(GRB.IntAttr.SolCount) > 0) {

				double objVal = model.get(GRB.DoubleAttr.ObjVal);
				double bestBound = model.get(GRB.DoubleAttr.ObjBound);
				double mipGap = model.get(GRB.DoubleAttr.MIPGap);

				System.out.println("Solution found!");
				if (status == GRB.Status.OPTIMAL) {
					System.out.println("Optimal solution found!");
				} else if (status == GRB.Status.SUBOPTIMAL) {
					System.out.println("Suboptimal solution found.");
				} else {
					System.out.println("Time limit reached, but a feasible solution was found.");
				}

				System.out.println("Objective value (makespan): " + objVal);
				System.out.println("Best bound: " + bestBound);
				System.out.println("MIP Gap: " + (mipGap * 100) + "%");

				// Convert the solution to a Schedule object
				return convertToSchedule(totalOps);
			} else {
				System.out.println("No solution found. Status: " + status);
				analyzeInfeasibility(totalOps, numMachines);
				return null;
			}
		} catch (GRBException e) {
			System.err.println("Error solving MILP model with Gurobi: " + e.getMessage());
			e.printStackTrace();
			return null;
		} finally {
			// Clean up Gurobi resources
			try {
				if (model != null) model.dispose();
				if (env != null) env.dispose();
			} catch (GRBException e) {
				System.err.println("Error disposing Gurobi resources: " + e.getMessage());
			}
		}
	}

	/**
	 * Creates all variables needed for the model
	 */
	private void createVariables(int totalOps, int numMachines) throws GRBException {
		// Create start time variables for each operation (continuous)
		for (int op = 1; op <= totalOps; op++) {
			GRBVar startTimeVar = model.addVar(0.0, BIG_M, 0.0, GRB.CONTINUOUS, "s_" + op);
			startTimeVars.put(op, startTimeVar);
		}

		// Create assignment variables (binary)
		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);
			for (int machine : compatibleMachines) {
				GRBVar assignVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + op + "_" + machine);
				assignmentVars.put(op + "_" + machine, assignVar);
			}
		}

		// Sequencing variables will be created in the machine exclusivity constraints section

		// Create makespan variable (continuous)
		makespanVar = model.addVar(0.0, BIG_M, 0.0, GRB.CONTINUOUS, "makespan");

		// Update model to integrate new variables
		model.update();

		System.out.println("Created variables:");
		System.out.println("- Start time variables: " + startTimeVars.size());
		System.out.println("- Assignment variables: " + assignmentVars.size());
	}

	/**
	 * Creates the objective function to minimize makespan
	 */
	private void createObjectiveFunction() throws GRBException {
		GRBLinExpr objective = new GRBLinExpr();
		objective.addTerm(1.0, makespanVar);
		model.setObjective(objective, GRB.MINIMIZE);
	}

	/**
	 * Creates all constraints for the MILP model
	 */
	private void createConstraints(int totalOps, int numMachines) throws GRBException {
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
	private void addOperationAssignmentConstraints(int totalOps) throws GRBException {
		int initialCount = constraintCount;

		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);

			// Create constraint: sum(x_op_m) = 1 for all compatible machines
			GRBLinExpr expr = new GRBLinExpr();
			for (int machine : compatibleMachines) {
				GRBVar assignVar = assignmentVars.get(op + "_" + machine);
				expr.addTerm(1.0, assignVar);
			}
			model.addConstr(expr, GRB.EQUAL, 1.0, "op_assign_" + op);
			constraintCount++;
		}

		System.out.println("Added " + (constraintCount - initialCount) + " operation assignment constraints");
	}

	/**
	 * Add constraints for operation precedence from the DAG
	 */
	private void addPrecedenceConstraints(int totalOps) throws GRBException {
		int initialCount = constraintCount;

		int[][] distanceMatrix = pb.getDistanceMatrix();

		for (int i = 1; i <= totalOps; i++) {
			for (int j = 1; j <= totalOps; j++) {
				// If operation i precedes operation j (distance > 0)
				if (distanceMatrix[i][j] > 0) {
					// Create constraint: s_j >= s_i + p_i
					GRBLinExpr expr = new GRBLinExpr();
					expr.addTerm(1.0, startTimeVars.get(j));
					expr.addTerm(-1.0, startTimeVars.get(i));

					// Add processing time of operation i
					double processingTime_i = pb.getProcessingTime()[i - 1];
					model.addConstr(expr, GRB.GREATER_EQUAL, processingTime_i, "precedence_" + i + "_" + j);

					constraintCount++;
				}
			}
		}

		System.out.println("Added " + (constraintCount - initialCount) + " precedence constraints");
	}

	/**
	 * Add constraints ensuring operations don't overlap on the same machine.
	 * 采用如下两组约束：
	 *  (1) s_op2 - s_op1 - M·(y + x1 + x2) >= p_op1 - 3M
	 *  (2) s_op1 - s_op2 + M·y - M·(x1 + x2) >= p_op2 - 2M
	 */
	private void addMachineExclusivityConstraints(int totalOps, int numMachines) throws GRBException {
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
					GRBVar y = model.addVar(0, 1, 0, GRB.BINARY, varName);
					sequencingVars.put(op1 + "_" + op2 + "_" + machine, y);
					model.update();

					// Get assignment variables for these operations on this machine
					GRBVar x1 = assignmentVars.get(op1 + "_" + machine);
					GRBVar x2 = assignmentVars.get(op2 + "_" + machine);

					double M = BIG_M;
					// Constraint (1): s_op2 - s_op1 - M*(y + x1 + x2) >= pt1 - 3*M
					GRBLinExpr c1 = new GRBLinExpr();
					c1.addTerm(1.0, startTimeVars.get(op2));
					c1.addTerm(-1.0, startTimeVars.get(op1));
					c1.addTerm(-M, y);
					c1.addTerm(-M, x1);
					c1.addTerm(-M, x2);

					model.addConstr(c1, GRB.GREATER_EQUAL, pt1 - 3 * M, "seq1_" + op1 + "_" + op2 + "_" + machine);
					constraintCount++;

					// Constraint (2): s_op1 - s_op2 + M*y - M*(x1 + x2) >= pt2 - 2*M
					GRBLinExpr c2 = new GRBLinExpr();
					c2.addTerm(1.0, startTimeVars.get(op1));
					c2.addTerm(-1.0, startTimeVars.get(op2));
					c2.addTerm(M, y);
					c2.addTerm(-M, x1);
					c2.addTerm(-M, x2);

					model.addConstr(c2, GRB.GREATER_EQUAL, pt2 - 2 * M, "seq2_" + op1 + "_" + op2 + "_" + machine);
					constraintCount++;
				}
			}
		}

		model.update();
		System.out.println("- Sequencing variables: " + sequencingVars.size());
		System.out.println("Added " + (constraintCount - initialCount) + " machine exclusivity constraints");
	}

	/**
	 * Add TCMB (Time Constraints by Mutual Boundaries) constraints
	 * Correctly models end of A to start of B time constraints
	 */
	private void addTimeConstraints() throws GRBException {
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
			GRBLinExpr expr = new GRBLinExpr();
			expr.addTerm(1.0, startTimeVars.get(opB));
			expr.addTerm(-1.0, startTimeVars.get(opA));

			model.addConstr(expr, GRB.LESS_EQUAL, timeLimit + processingTime_a, "tcmb_" + opA + "_" + opB);
			constraintCount++;
		}

		System.out.println("Added " + (constraintCount - initialCount) + " TCMB constraints");
	}

	/**
	 * Add constraints defining the makespan
	 */
	private void addMakespanConstraints(int totalOps) throws GRBException {
		int initialCount = constraintCount;

		for (int op = 1; op <= totalOps; op++) {
			// Makespan >= s_a + p_a for all operations a
			GRBLinExpr expr = new GRBLinExpr();
			expr.addTerm(1.0, makespanVar);
			expr.addTerm(-1.0, startTimeVars.get(op));

			double processingTime = pb.getProcessingTime()[op - 1];
			model.addConstr(expr, GRB.GREATER_EQUAL, processingTime, "makespan_" + op);

			constraintCount++;
		}

		System.out.println("Added " + (constraintCount - initialCount) + " makespan constraints");
	}

	/**
	 * Converts the solution from the Gurobi optimizer to a Schedule object
	 */
	private Schedule convertToSchedule(int totalOps) throws GRBException {
		Map<Integer, Integer> startTimes = new HashMap<>();
		Map<Integer, Integer> assignedMachine = new HashMap<>();

		// Extract start times
		for (int op = 1; op <= totalOps; op++) {
			startTimes.put(op, (int) Math.round(startTimeVars.get(op).get(GRB.DoubleAttr.X)));
		}

		// Extract machine assignments
		for (int op = 1; op <= totalOps; op++) {
			List<Integer> compatibleMachines = pb.getOpToCompatibleList().get(op);

			for (int machine : compatibleMachines) {
				GRBVar assignVar = assignmentVars.get(op + "_" + machine);

				if (Math.round(assignVar.get(GRB.DoubleAttr.X)) == 1) {
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
			int processingTime_a = pb.getProcessingTime()[opA - 1];
			if (timeLimit < 0) {
				System.out.println("Potential issue: TCMB time limit for operations " +
						opA + " and " + opB + " is negative (" + timeLimit + ")");
			}
		}

		try {
			// 4. 使用Gurobi的IIS(Irreducible Inconsistent Subsystem)功能
			// 注意：需要先计算IIS
			model.computeIIS();
			System.out.println("IIS computed. An IIS is a subset of constraints and bounds that make the model infeasible.");

			// 输出不可行子系统中的约束
			int iisConstraints = 0;
			for (GRBConstr c : model.getConstrs()) {
				if (c.get(GRB.IntAttr.IISConstr) == 1) {
					System.out.println("Constraint in IIS: " + c.get(GRB.StringAttr.ConstrName));
					iisConstraints++;
				}
			}
			System.out.println("Number of constraints in IIS: " + iisConstraints);
		} catch (GRBException e) {
			System.out.println("Unable to compute IIS: " + e.getMessage());
		}

		System.out.println("Consider relaxing some constraints or verifying the problem data.");
	}
}