package cn.viewcn.nessusrm.core;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import java.util.Arrays;
import java.util.List;

public class MakeReport {
    public static void main(String[] args) {
        // 读取CSV文件（确保路径正确）
        Table table = Table.read().csv("src/main/resources/test.csv");

        // 基于所有列去重
        Table uniqueTable = table.dropDuplicateRows();

        // 正确创建风险等级集合，并直接传递List给isIn()
        List<String> riskLevels = Arrays.asList("High", "Medium", "Low");
        StringColumn riskColumn = uniqueTable.stringColumn("Risk"); // 确保列名完全匹配
        Table highRiskTable = uniqueTable.where(riskColumn.isIn(riskLevels)); // 移除toString()

        // 打印结果
        System.out.println("Filtered rows: " + highRiskTable.rowCount());
        System.out.println(highRiskTable.print());


    }
}
