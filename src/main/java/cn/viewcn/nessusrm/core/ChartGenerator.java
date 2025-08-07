package cn.viewcn.nessusrm.core;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class ChartGenerator {

    // 风险等级颜色映射
    private static final Map<String, Color> RISK_COLORS = new HashMap<>();
    static {
        RISK_COLORS.put("严重", new Color(220, 20, 60));    // 红色
        RISK_COLORS.put("高危", new Color(255, 140, 0));    // 橙色
        RISK_COLORS.put("中危", new Color(255, 215, 0));    // 黄色
        RISK_COLORS.put("低危", new Color(50, 205, 50));    // 绿色
        RISK_COLORS.put("信息", new Color(100, 149, 237));  // 蓝色
    }

    public static BufferedImage createRiskChart(List<String> labels, List<Long> values) {
        // 1. 创建数据集
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (int i = 0; i < labels.size(); i++) {
            dataset.setValue(labels.get(i), values.get(i));
        }

        // 2. 创建饼图
        JFreeChart chart = ChartFactory.createPieChart(
            "漏洞风险分布",  // 图表标题
            dataset,       // 数据集
            true,          // 显示图例
            true,          // 显示工具提示
            false          // 不生成URL链接
        );

        // 3. 自定义图表样式
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionOutlinesVisible(false);
        plot.setLabelGenerator(null); // 不显示标签
        
        // 设置自定义颜色
        for (String key : RISK_COLORS.keySet()) {
            if (dataset.getKeys().contains(key)) {
                plot.setSectionPaint(key, RISK_COLORS.get(key));
            }
        }

        // 4. 设置背景透明
        chart.setBackgroundPaint(null);
        plot.setBackgroundPaint(null);
        
        // 5. 渲染为BufferedImage
        return chart.createBufferedImage(800, 600);
    }
}