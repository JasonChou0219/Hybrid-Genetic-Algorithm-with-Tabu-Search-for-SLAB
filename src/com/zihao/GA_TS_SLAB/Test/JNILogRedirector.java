package com.zihao.GA_TS_SLAB.Test;

import java.io.IOException;
import java.util.logging.*;

public class JNILogRedirector {
	public static void setupJNILogging(String logFile) {
		try {
			// 创建 JNI 日志记录器
			Logger jniLogger = Logger.getLogger("java.library.load");

			// 创建文件处理器
			FileHandler fileHandler = new FileHandler(logFile);
			fileHandler.setFormatter(new SimpleFormatter());

			// 添加处理器到日志记录器
			jniLogger.addHandler(fileHandler);

			// 设置日志级别
			jniLogger.setLevel(Level.ALL);

			// 禁止继承父处理器，避免日志同时输出到控制台
			jniLogger.setUseParentHandlers(false);

			System.out.println("JNI 日志将重定向到: " + logFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}