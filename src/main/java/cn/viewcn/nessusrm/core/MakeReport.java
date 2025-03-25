package cn.viewcn.nessusrm.core;
import tech.tablesaw.api.Table;
import java.util.Arrays;
import java.util.List;

public class MakeReport {
    public static void main(String[] args) {
        // 读取CSV文件
        Table table = Table.read().csv("src/main/resources/test.csv");

        // 基于所有列去重
        Table uniqueTable = table.dropDuplicateRows();

        //
        List<String> riskLevels = Arrays.asList("High","Low");
        Table highRiskTable = uniqueTable.where(table.column("Risk").asStringColumn().isIn(riskLevels.toString()));

        // 打印结果或进行其他操作
        System.out.println(highRiskTable);

        // 如果你想将去重后的表格保存回CSV
        // uniqueTable.write().csv("path/to/your/unique_file.csv");
    }
}
