package com.zihao.GA_TS_SLAB.Test;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;

public class JFreeChartTest {
    public static void main(String[] args) {
        // 创建数据集
        DefaultCategoryDataset dataset = createDataset();

        // 创建折线图
        JFreeChart lineChart = ChartFactory.createLineChart(
                "折线图示例",         // 图表标题
                "年份",              // X 轴标签
                "数量",              // Y 轴标签
                dataset,             // 数据集
                PlotOrientation.VERTICAL,
                true,                // 是否包含图例
                true,                // 是否生成工具提示
                false                // 是否生成 URL
        );

        // 保存图表为文件
        saveChartAsPNG(lineChart, "LineChartExample.png", 800, 600);
    }

    private static DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 添加数据到数据集
        dataset.addValue(1.0, "系列1", "2015");
        dataset.addValue(4.0, "系列1", "2016");
        dataset.addValue(3.0, "系列1", "2017");
        dataset.addValue(5.0, "系列1", "2018");
        dataset.addValue(5.0, "系列1", "2019");

        return dataset;
    }

    private static void saveChartAsPNG(JFreeChart chart, String filePath, int width, int height) {
        File file = new File(filePath);
        try {
            ChartUtilities.saveChartAsPNG(file, chart, width, height);
            System.out.println("图表已成功保存到: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存图表时出错: " + e.getMessage());
        }
    }
}

