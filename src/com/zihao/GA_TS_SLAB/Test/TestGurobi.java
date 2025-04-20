package com.zihao.GA_TS_SLAB.Test;

// TestGurobi.java
public class TestGurobi {
	public static void main(String[] args) {
		try {
			// 尝试加载 Gurobi 类
			Class.forName("gurobi.GRBEnv");
			System.out.println("成功加载 gurobi.GRBEnv");
		} catch (ClassNotFoundException e) {
			System.out.println("找不到 gurobi.GRBEnv 类");
			try {
				// 尝试加载另一个可能的包名
				Class.forName("com.gurobi.GRBEnv");
				System.out.println("成功加载 com.gurobi.GRBEnv");
			} catch (ClassNotFoundException e2) {
				System.out.println("找不到 com.gurobi.GRBEnv 类");
			}
		}
	}
}