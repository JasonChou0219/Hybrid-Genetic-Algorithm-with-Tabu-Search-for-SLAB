package com.zihao.GA_TS_SLAB.Test;
import com.gurobi.gurobi.*;

public class MipExample {

	public static void main(String[] args) {
		try {
			// 创建环境
			GRBEnv env = new GRBEnv();

			// 创建模型
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "mip1");

			// 创建变量
			GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
			GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
			GRBVar z = model.addVar(0.0, 10.0, 0.0, GRB.INTEGER, "z");

			// 设置目标函数：最大化 x + y + 2 * z
			GRBLinExpr expr = new GRBLinExpr();
			expr.addTerm(1.0, x);
			expr.addTerm(1.0, y);
			expr.addTerm(2.0, z);
			model.setObjective(expr, GRB.MAXIMIZE);

			// 添加约束条件：x + 2 * y + 3 * z <= 4
			expr = new GRBLinExpr();
			expr.addTerm(1.0, x);
			expr.addTerm(2.0, y);
			expr.addTerm(3.0, z);
			model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");

			// 添加约束条件：x + y >= 1
			expr = new GRBLinExpr();
			expr.addTerm(1.0, x);
			expr.addTerm(1.0, y);
			model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

			// 优化模型
			model.optimize();

			// 输出结果
			System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));
			System.out.println("x = " + x.get(GRB.DoubleAttr.X));
			System.out.println("y = " + y.get(GRB.DoubleAttr.X));
			System.out.println("z = " + z.get(GRB.DoubleAttr.X));

			// 清理资源
			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
		}
	}
}


