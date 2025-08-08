package cn.viewcn.nessusrm.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class MakeReport {
    private static final Logger logger = LoggerFactory.getLogger(MakeReport.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final Map<String, Object> formData; // 新增成员变量存储formData

    // 新增构造函数，接收formData
    public MakeReport(Map<String, Object> formData) {
        this.formData = formData;
    }

    // 合并CSV文件
    public Table mergeCsvFiles(List<File> fileList) throws IOException {

        Table mergedTable = null;

        for (File file : fileList) {
            // 为每个文件动态构建选项
            CsvReadOptions options = CsvReadOptions.builder(file)  // 直接传入当前文件
                    .maxCharsPerColumn(65535)
                    .lineEnding("\n")
                    .columnTypes(columnName -> ColumnType.STRING)  // 所有列作为字符串处理
                    .missingValueIndicator("NA", "n/a", "N/A", "null", "NULL", "NaN")  // 声明缺失值标记,字符串列中出现缺省值则自动转为""
                    .build();
            Table currentTable = Table.read().usingOptions(options);

            if (mergedTable == null) {
                mergedTable = currentTable;
            } else {
                // 检查列结构是否一致
                if (!tablesHaveSameStructure(mergedTable, currentTable)) {
                    throw new IOException("文件结构不一致，无法合并: " + file.getName());
                }
                mergedTable = mergedTable.append(currentTable);
            }
        }
        return mergedTable;
    }

    // 检查两个表的列结构是否相同
    private static boolean tablesHaveSameStructure(Table table1, Table table2) {
        if (table1.columnCount() != table2.columnCount()) {
            return false;
        }

        for (int i = 0; i < table1.columnCount(); i++) {
            String colName1 = table1.column(i).name();
            String colName2 = table2.column(i).name();
            ColumnType type1 = table1.column(i).type();
            ColumnType type2 = table2.column(i).type();

            if (!colName1.equals(colName2) || !type1.equals(type2)) {
                return false;
            }
        }
        return true;
    }

    // 数据清洗和处理
    public Table cleanCsvFiles(Table table) {
        //1. 去除完全重复的行
        Table cleanedTable = table.dropDuplicateRows();
        //2. 过滤选择的漏洞等级
        @SuppressWarnings("unchecked")
        List<String> riskList = (List<String>) formData.get("risk");
        Table selectRiskTable = cleanedTable.where(cleanedTable.stringColumn("Risk").isIn(riskList));
        //3. 老版本nessus报告CVSS字段修改为新字段名
        if (selectRiskTable.containsColumn("CVSS")) selectRiskTable.column("CVSS").setName("CVSS v2.0 Base Score");
        //4. 遍历所有列，将空值替换为空字符串，读取csv时已配置缺省值自动替换为""，此处不需要替换了
        //for (Column<?> column : cleanedTable.columns()) {
        //    StringColumn stringColumn = (StringColumn) column;
        //    stringColumn.set(stringColumn.isMissing(), "");
        //}
        return selectRiskTable;
    }

    // 翻译漏洞数据
    public Table translateVulnerabilities(Table table) {
        // 添加翻译后的列
        StringColumn pluginNameCn = StringColumn.create("plugin_name_cn");
        StringColumn descriptionCn = StringColumn.create("description_cn");
        StringColumn solutionCn = StringColumn.create("solution_cn");
        StringColumn riskCn = StringColumn.create("risk_cn");

        // 遍历每一行进行翻译
        for (Row row : table) {
            Map<String, String> rowData = new HashMap<>();
            for (Column<?> column : table.columns()) {
                rowData.put(column.name(), row.getString(column.name()));
            }

            try {
                // 使用NessusTrans进行翻译（这里简化为示例）
                Map<String, Object> transResult = new HashMap<>();
                transResult.put("plugin_name_cn", "中文名称: " + row.getString("Name"));
                transResult.put("description_cn", "中文描述: " + row.getString("Description"));
                transResult.put("solution_cn", "中文解决方案: " + row.getString("Solution"));
                transResult.put("risk_cn", translateRiskLevel(row.getString("Risk")));

                pluginNameCn.append(transResult.get("plugin_name_cn").toString());
                descriptionCn.append(transResult.get("description_cn").toString());
                solutionCn.append(transResult.get("solution_cn").toString());
                riskCn.append(transResult.get("risk_cn").toString());
            } catch (Exception e) {
                // 翻译失败时使用原始数据
                pluginNameCn.append(row.getString("Name"));
                descriptionCn.append(row.getString("Description"));
                solutionCn.append(row.getString("Solution"));
                riskCn.append(translateRiskLevel(row.getString("Risk")));
            }
        }

        // 添加翻译后的列到表
        table.addColumns(pluginNameCn, descriptionCn, solutionCn, riskCn);
        return table;
    }

    private static String translateRiskLevel(String risk) {
        if (risk == null) return "未知";
        switch (risk.toLowerCase()) {
            case "critical": return "严重";
            case "high": return "高危";
            case "medium": return "中危";
            case "low": return "低危";
            case "info": return "信息";
            default: return "未知";
        }
    }


    // 分析漏洞数据
    public static Map<String, Object> analyzeVulnerabilityData(Table table) {
        Map<String, Object> analysis = new LinkedHashMap<>();

        // 1. 漏洞总数
        analysis.put("total_vulnerabilities", table.rowCount());

        // 2. 风险等级分布
        if (table.containsColumn("risk_cn")) {
            Map<String, Long> riskDistribution = new LinkedHashMap<>();
            StringColumn riskColumn = table.stringColumn("risk_cn");

            // 初始化风险等级
            List<String> riskLevels = Arrays.asList("严重", "高危", "中危", "低危", "信息");
            for (String level : riskLevels) {
                riskDistribution.put(level, 0L);
            }

            // 统计风险分布
            for (String risk : riskColumn) {
                if (riskDistribution.containsKey(risk)) {
                    riskDistribution.put(risk, riskDistribution.get(risk) + 1);
                } else {
                    riskDistribution.put("其他", riskDistribution.getOrDefault("其他", 0L) + 1);
                }
            }
            analysis.put("risk_distribution", riskDistribution);
        }

        // 3. 按主机统计风险分布
        Map<String, Map<String, Integer>> hostRiskMap = new TreeMap<>();
        if (table.containsColumn("Host") && table.containsColumn("risk_cn")) {
            StringColumn hostColumn = table.stringColumn("Host");
            StringColumn riskColumn = table.stringColumn("risk_cn");

            for (int i = 0; i < table.rowCount(); i++) {
                String host = hostColumn.get(i);
                String risk = riskColumn.get(i);

                hostRiskMap.computeIfAbsent(host, k -> new HashMap<>())
                        .merge(risk, 1, Integer::sum);
            }
            analysis.put("host_risk_distribution", hostRiskMap);
        }

        // 4. 主机数量统计
        if (table.containsColumn("Host")) {
            Set<String> uniqueHosts = new HashSet<>(table.stringColumn("Host").asList());
            analysis.put("host_count", uniqueHosts.size());
        }

        return analysis;
    }

    // 生成Word报告
    public static byte[] generateWordReport(Table translatedTable,
                                            Map<String, Object> analysis,
                                            Map<String, String> formData) throws Exception {

        // 1. 加载Word模板
        try (InputStream templateStream = MakeReport.class.getResourceAsStream("/templates/report_template.docx");
             XWPFDocument doc = new XWPFDocument(templateStream)) {

            // 2. 准备模板数据
            Map<String, Object> templateData = prepareTemplateData(formData, analysis);

            // 3. 填充文本内容
            fillTemplateText(doc, templateData);

            // 4. 生成并插入风险分布图表
            insertRiskChart(doc, (Map<String, Long>) analysis.get("risk_distribution"));

            // 5. 添加漏洞详情表格
            createVulnerabilityTable(doc, translatedTable);

            // 6. 保存到字节数组
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                doc.write(out);
                return out.toByteArray();
            }
        }
    }

    // 准备模板数据
    private static Map<String, Object> prepareTemplateData(Map<String, String> formData,
                                                           Map<String, Object> analysis) {
        Map<String, Object> templateData = new HashMap<>(formData);

        // 日期格式化
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        templateData.put("create_date", formatDate(formData.get("create_date")));
        templateData.put("audit_date", formatDate(formData.get("audit_date")));
        templateData.put("permit_date", formatDate(formData.get("permit_date")));
        templateData.put("start_date", formatDate(formData.get("start_date")));
        templateData.put("end_date", formatDate(formData.get("end_date")));

        // 添加分析结果
        templateData.put("vul_counts", analysis.get("total_vulnerabilities"));
        templateData.put("host_count", analysis.get("host_count"));

        // 风险分布数据
        Map<String, Long> riskDistribution = (Map<String, Long>) analysis.get("risk_distribution");
        templateData.putAll(riskDistribution);

        return templateData;
    }

    private static String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日");
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }

    // 填充模板文本
    private static void fillTemplateText(XWPFDocument doc, Map<String, Object> data) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            for (XWPFRun run : p.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        String placeholder = "{{" + entry.getKey() + "}}";
                        if (text.contains(placeholder)) {
                            text = text.replace(placeholder,
                                    entry.getValue() != null ? entry.getValue().toString() : "");
                            run.setText(text, 0);
                        }
                    }
                }
            }
        }

        // 处理表格中的占位符
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun run : p.getRuns()) {
                            String text = run.getText(0);
                            if (text != null) {
                                for (Map.Entry<String, Object> entry : data.entrySet()) {
                                    String placeholder = "{{" + entry.getKey() + "}}";
                                    if (text.contains(placeholder)) {
                                        text = text.replace(placeholder,
                                                entry.getValue() != null ? entry.getValue().toString() : "");
                                        run.setText(text, 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 插入风险图表
    private static void insertRiskChart(XWPFDocument doc, Map<String, Long> riskDistribution) {
        try {
            // 1. 生成风险分布图表
            List<String> labels = new ArrayList<>(riskDistribution.keySet());
            List<Long> values = new ArrayList<>(riskDistribution.values());

            BufferedImage chartImage = ChartGenerator.createRiskChart(labels, values);

            // 2. 在文档中找到图表占位符位置
            XWPFParagraph chartParagraph = null;
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getText().contains("{{risk_chart}}")) {
                    chartParagraph = p;
                    break;
                }
            }

            if (chartParagraph != null) {
                // 3. 删除占位符文本
                for (XWPFRun run : chartParagraph.getRuns()) {
                    run.setText("", 0);
                }

                // 4. 插入图表图片
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(chartImage, "png", baos);
                    try (InputStream is = new ByteArrayInputStream(baos.toByteArray())) {
                        chartParagraph.createRun().addPicture(
                                is,
                                XWPFDocument.PICTURE_TYPE_PNG,
                                "risk_chart",
                                Units.toEMU(400),  // 宽度
                                Units.toEMU(300)   // 高度
                        );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 创建漏洞详情表格
    private static void createVulnerabilityTable(XWPFDocument doc, Table table) {
        // 1. 找到表格占位符位置
        XWPFParagraph tablePlaceholder = null;
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (p.getText().contains("{{vulnerability_table}}")) {
                tablePlaceholder = p;
                break;
            }
        }

        if (tablePlaceholder == null) return;

        // 2. 删除占位符段落
        int pos = doc.getPosOfParagraph(tablePlaceholder);
        doc.removeBodyElement(pos);

        // 3. 创建新表格
        XWPFTable vulTable = doc.createTable();

        // 4. 添加表头
        String[] headers = {"漏洞名称", "风险等级", "主机", "端口", "描述", "解决方案"};
        XWPFTableRow headerRow = vulTable.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            if (i == 0) {
                headerRow.getCell(0).setText(headers[i]);
            } else {
                headerRow.addNewTableCell().setText(headers[i]);
            }
        }

        // 5. 添加表格数据
        StringColumn nameCol = table.stringColumn("plugin_name_cn");
        StringColumn riskCol = table.stringColumn("risk_cn");
        StringColumn hostCol = table.stringColumn("Host");
        StringColumn portCol = table.stringColumn("Port");
        StringColumn descCol = table.stringColumn("description_cn");
        StringColumn solutionCol = table.stringColumn("solution_cn");

        for (int i = 0; i < table.rowCount(); i++) {
            XWPFTableRow row = vulTable.createRow();
            row.getCell(0).setText(truncateText(nameCol.get(i), 50));
            row.getCell(1).setText(riskCol.get(i));
            row.getCell(2).setText(hostCol.get(i));
            row.getCell(3).setText(portCol.get(i));
            row.getCell(4).setText(truncateText(descCol.get(i), 100));
            row.getCell(5).setText(truncateText(solutionCol.get(i), 100));
        }
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // 生成CSV报告
    public static byte[] generateCsvReport(Table table) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"))) {

            // 写入CSV头部
            writer.println("Plugin ID,Name (CN),Risk (CN),Host,Port,Description (CN),Solution (CN)");

            // 写入数据行
            for (Row row : table) {
                writer.printf("%s,\"%s\",%s,%s,%s,\"%s\",\"%s\"%n",
                        row.getString("Plugin ID"),
                        row.getString("plugin_name_cn"),
                        row.getString("risk_cn"),
                        row.getString("Host"),
                        row.getString("Port"),
                        row.getString("description_cn"),
                        row.getString("solution_cn")
                );
            }

            writer.flush();
            return out.toByteArray();
        }
    }

    // 生成ZIP报告
    public static byte[] generateZipReport(Table translatedTable,
                                           Map<String, Object> analysis,
                                           Map<String, String> formData) throws Exception {
        try (ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(zipOut)) {

            // 1. 生成Word报告
            byte[] wordReport = generateWordReport(translatedTable, analysis, formData);
            zos.putNextEntry(new ZipEntry(formData.get("system_name") + "_安全扫描报告.docx"));
            zos.write(wordReport);
            zos.closeEntry();

            // 2. 生成CSV报告
            byte[] csvReport = generateCsvReport(translatedTable);
            zos.putNextEntry(new ZipEntry("漏洞详情.csv"));
            zos.write(csvReport);
            zos.closeEntry();

            // 3. 添加README
            zos.putNextEntry(new ZipEntry("README.txt"));
            zos.write("本报告包含安全扫描结果和漏洞详情".getBytes("UTF-8"));
            zos.closeEntry();

            return zipOut.toByteArray();
        }
    }

    public static Path saveReport(byte[] zipBytes, String systemName) throws IOException {
        // 1. 创建安全的文件名
        String safeSystemName = createSafeFilename(systemName);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = String.format("%s_安全扫描报告_%s.zip", safeSystemName, timestamp);

        // 2. 确定保存路径（用户下载目录）
        Path downloadDir = Paths.get(System.getProperty("user.home"), "Downloads");

        // 3. 确保下载目录存在
        if (!Files.exists(downloadDir)) {
            Files.createDirectories(downloadDir);
        }

        // 4. 构建完整文件路径
        Path outputPath = downloadDir.resolve(fileName);

        // 5. 保存文件
        Files.write(outputPath, zipBytes);

        // 6. 记录日志
        logger.info("报告已保存到: {}", outputPath.toAbsolutePath());

        return outputPath;
    }

    /**
     * 创建安全的文件名称（替换非法字符）
     *
     * @param originalName 原始系统名称
     * @return 安全的文件名称
     */
    private static String createSafeFilename(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "UntitledSystem";
        }

        // 替换文件名中的非法字符
        String safeName = originalName
                .replaceAll("[\\\\/:*?\"<>|]", "_")  // 替换Windows文件名中的非法字符
                .replaceAll("\\s+", " ")             // 合并连续空格
                .trim();                             // 去除首尾空格

        // 限制文件名长度
        int maxLength = 100;
        if (safeName.length() > maxLength) {
            safeName = safeName.substring(0, maxLength);
        }

        return safeName;
    }

}



//// 配置 CSV 解析选项
//CsvReadOptions options = CsvReadOptions.builder("src/main/resources/test2.csv")
//        .maxCharsPerColumn(65535) // 设置每列的最大字符数
//        .lineEnding("\n")
//        .columnTypes(columnName -> ColumnType.STRING)
//        .build();
//Table table = Table.read().usingOptions(options);
//
////        // 筛选条件：Plugin ID = 10114 且 Host = 101.201.57.137
////        Selection selection = table.stringColumn("Plugin ID").isEqualTo("10114")
////                .and(table.stringColumn("Host").isEqualTo("101.201.57.137"));
////
////        // 获取符合条件的子表
////        Table filteredTable = table.where(selection);
//
//// 打印 Description 字段内容
////        StringColumn descriptionColumn = filteredTable.stringColumn("Description");
////        for (String description : descriptionColumn) {
////
////            String rawContent = StringEscapeUtils.escapeJava(description);
////            System.out.println(rawContent);
////        }
//
//
//// 基于所有列去重
//Table uniqueTable = table.dropDuplicateRows();
//
//// 正确创建风险等级集合，并直接传递List给isIn()
//List<String> riskLevels = Arrays.asList("High");
//StringColumn riskColumn = uniqueTable.stringColumn("Risk"); // 确保列名完全匹配
//Table highRiskTable = uniqueTable.where(riskColumn.isIn(riskLevels)); // 移除toString()
//
////打印结果
//        System.out.println("Filtered rows: " + highRiskTable.rowCount());
//        System.out.println(highRiskTable.print());