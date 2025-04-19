package cn.viewcn.nessusrm.core;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;
import tech.tablesaw.api.ColumnType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.text.StringEscapeUtils;

public class MakeReport {
    public static void main(String[] args) {

        // 配置 CSV 解析选项
        CsvReadOptions options = CsvReadOptions.builder("src/main/resources/test2.csv")
                .maxCharsPerColumn(65535) // 设置每列的最大字符数
                .lineEnding("\n")
                .columnTypes(columnName -> ColumnType.STRING)
                .build();
        Table table = Table.read().usingOptions(options);

        // 读取CSV文件（确保路径正确）
//        Table table = Table.read().csv("src/main/resources/test2.csv");

        // 筛选条件：Plugin ID = 10114 且 Host = 101.201.57.137
        Selection selection = table.stringColumn("Plugin ID").isEqualTo("10114")
                .and(table.stringColumn("Host").isEqualTo("101.201.57.137"));

        // 获取符合条件的子表
        Table filteredTable = table.where(selection);

        // 打印 Description 字段内容
        StringColumn descriptionColumn = filteredTable.stringColumn("Description");
        for (String description : descriptionColumn) {

            String rawContent = StringEscapeUtils.escapeJava(description);
            System.out.println(rawContent);
        }


//        // 基于所有列去重
//        Table uniqueTable = table.dropDuplicateRows();
//
//        // 正确创建风险等级集合，并直接传递List给isIn()
//        List<String> riskLevels = Arrays.asList("High");
//        StringColumn riskColumn = uniqueTable.stringColumn("Risk"); // 确保列名完全匹配
//        Table highRiskTable = uniqueTable.where(riskColumn.isIn(riskLevels)); // 移除toString()

        // 打印结果
//        System.out.println("Filtered rows: " + highRiskTable.rowCount());
//        System.out.println(highRiskTable.print());


    }
}
