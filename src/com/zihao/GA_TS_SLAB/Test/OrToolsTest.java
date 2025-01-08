package com.zihao.GA_TS_SLAB.Test;


import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;



public class OrToolsTest {
    public static void main(String[] args) {
        // 加载 OR-Tools 库
        Loader.loadNativeLibraries();

        // 创建模型
        CpModel model = new CpModel();

        // 创建变量
        int numVals = 3;
        IntVar x = model.newIntVar(0, numVals - 1, "x");
        IntVar y = model.newIntVar(0, numVals - 1, "y");
        IntVar z = model.newIntVar(0, numVals - 1, "z");

        // 添加约束: x != y
        model.addDifferent(x, y);

        // 目标函数: 最大化 x + 2*y + 3*z
        model.maximize(LinearExpr.weightedSum(new IntVar[]{x, y, z}, new long[]{1, 2, 3}));

        // 创建求解器并解决问题
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        // 检查解的状态
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Solution found!");
            System.out.println("x = " + solver.value(x));
            System.out.println("y = " + solver.value(y));
            System.out.println("z = " + solver.value(z));
            System.out.println("Objective value = " + solver.objectiveValue());
        } else {
            System.out.println("No solution found.");
        }
    }
}
