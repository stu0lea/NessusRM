package cn.viewcn.nessusrm.core;

import cn.viewcn.nessusrm.api.TenableTransApi;
import cn.viewcn.nessusrm.api.TxTransSplitApi;
import cn.viewcn.nessusrm.orm.DatabaseConnect;
import cn.viewcn.nessusrm.orm.PluginTranslation;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NessusTrans {
    private static final Gson gson = new Gson();
    private final Map<String, String> csvRow;
    private final Map<String, String> transResult = new HashMap<>();
    private static final Map<String, String> riskMap = new HashMap<String, String>() {{
        put("Critical", "严重");
        put("High", "高危");
        put("Medium", "中危");
        put("Low", "低危");
        put("Info", "信息");
        put("None", "信息");
    }};
//    private static final Logger logger = LoggerFactory.getLogger(TenableTransApi.class);?
    public NessusTrans(Map<String, String> csvRow) {
        this.csvRow = csvRow;
    }

    private String delNewlines(String text) {
        return text.replace("\n\n", "[@]")
                .replace("\n", "")
                .replace("[@]", "\r\n")
                .replace("\\xa0", "");
    }

    private Map<String, Object> toMap(ResultSet rs) throws SQLException {
        // 数据库转为map
        Map<String, Object> map = new HashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            map.put(meta.getColumnName(i), rs.getObject(i));
        }
        return map;
    }

    public void transUseDb() throws SQLException {
        PluginTranslation translation = DatabaseConnect.getByPluginId(csvRow.get("Plugin ID"));

        if (translation != null) {
            // 将PluginTranslation对象转换为Map
            transResult.put("plugin_id", translation.getPluginId());
            transResult.put("cve", translation.getCve());
            transResult.put("cvss", translation.getCvss());
            transResult.put("risk", translation.getRisk());
            transResult.put("plugin_name", translation.getPluginName());
            transResult.put("synopsis", translation.getSynopsis());
            transResult.put("description", translation.getDescription());
            transResult.put("solution", translation.getSolution());
            transResult.put("upload_date", translation.getUploadDate());
            transResult.put("plugin_name_cn", translation.getPluginNameCn());
            transResult.put("risk_cn", translation.getRiskCn());
            transResult.put("synopsis_cn", translation.getSynopsisCn());
            transResult.put("description_cn", translation.getDescriptionCn());
            transResult.put("solution_cn", translation.getSolutionCn());

            String dbRisk = translation.getRisk();
            String csvRisk = csvRow.get("Risk");

            if (!dbRisk.equals(csvRisk)) {
                transResult.put("risk_cn", riskMap.getOrDefault(csvRisk, ""));
            }
        } else {
            throw new SQLException("Plugin ID在数据中未查询到！");
        }
    }

    public void transUseTenable() throws IOException {
        // 请求Tenable官方中文
        String pluginId = csvRow.get("Plugin ID");
        JsonObject tenableData = TenableTransApi.translate(pluginId);
        // 以上传的漏洞级别为准，不翻译漏洞级别。
        JsonObject script_name_i18n = tenableData.getAsJsonObject("script_name_i18n");
        JsonObject synopsis_i18n = tenableData.getAsJsonObject("synopsis_i18n");
        JsonObject description_i18n = tenableData.getAsJsonObject("description_i18n");
        JsonObject solution_i18n = tenableData.getAsJsonObject("solution_i18n");

        transResult.put("plugin_name_cn", script_name_i18n.get("zh_CN").getAsString());
        transResult.put("synopsis_cn", delNewlines(synopsis_i18n.get("zh_CN").getAsString()));
        transResult.put("description_cn", delNewlines(description_i18n.get("zh_CN").getAsString()));
        transResult.put("solution_cn", delNewlines(solution_i18n.get("zh_CN").getAsString()));
    }

    public void transUseTx() {
        // 使用腾讯翻译api翻译
        transResult.put("plugin_name_cn", TxTransSplitApi.translate(csvRow.get("Name")));
        transResult.put("risk_cn", riskMap.get(csvRow.get("Risk")));
        transResult.put("synopsis_cn", delNewlines(TxTransSplitApi.translate(csvRow.get("Synopsis"))));
        transResult.put("description_cn", delNewlines(TxTransSplitApi.translate(csvRow.get("Description"))));
        transResult.put("solution_cn", delNewlines(TxTransSplitApi.translate(csvRow.get("Solution"))));
    }

    public Map<String, String> transMain() {
        // 翻译执行顺序：本地库 or 官方中文API or 腾讯翻译API -> 翻译结果存储到本地库
        try {
            // 1.本地库翻译
             transUseDb();
        } catch (Exception e) {
            System.err.println("数据库翻译错误: " + e.getMessage());
            // 以下是原始英文漏洞信息，原封不动传递给transResult，准备存储到本地库中。
            transResult.put("plugin_id", csvRow.get("Plugin ID"));
            transResult.put("cve", csvRow.get("CVE"));
            transResult.put("cvss", csvRow.get("CVSS v2.0 Base Score"));
            transResult.put("risk", csvRow.get("Risk"));
            transResult.put("risk_cn", riskMap.get(csvRow.get("Risk"))); // 无需翻译直接从riskMap获取
            transResult.put("plugin_name",csvRow.get("Name"));
            transResult.put("synopsis", csvRow.get("Synopsis"));
            transResult.put("description", csvRow.get("Description"));
            transResult.put("solution",csvRow.get("Solution"));
            transResult.put("upload_date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            try {
                // 2.Nessus官方API翻译
                transUseTenable();
                Thread.sleep(500);
            } catch (Exception ex) {
                System.err.println("官方API错误: " + ex.getMessage());
                try {
                    // 3.腾讯翻译API, 无论翻译是否成功均会返回原文
                    transUseTx();
                    Thread.sleep(1000);
                } catch (Exception exc) {
                    System.err.println("腾讯翻译错误: " + exc.getMessage());
                }
            }
            // 将翻译后的数据存储到本地数据库
            try {
                // 创建实体对象
                PluginTranslation translation = new PluginTranslation(transResult);
                // 保存到数据库
                DatabaseConnect.saveOrUpdate(translation);
            } catch (Exception ex) {
                System.err.println("保存到数据库失败: " + ex.getMessage());
            }
        }
        return transResult;
    }

    public static void main(String[] args) {
    // 测试 中文官方id：95633
        Map<String, String> testData = new HashMap<String, String>() {{
                put("Plugin ID", "93144");
                put("CVE", "CVE-2005-1794");
                put("CVSS v2.0 Base Score", "5.1");
                put("Risk", "High");
                put("Name", "Microsoft Windows Remote Desktop Protocol Server Man-in-the-Middle Weakness");
                put("Synopsis", "It may be possible to get access to the remote host.");
                put("Description", "The remote version of the Remote Desktop Protocol Server (Terminal Service) is vulnerable to a man-in-the-middle (MiTM) attack.\nThe RDP  client makes no effort to validate the identity of the server when  setting up encryption.\n\nAn attacker with the ability to intercept  traffic from the RDP server can establish encryption with the client  and server without being detected. A MiTM attack of this nature would  allow the attacker to obtain any sensitive information transmitted,  including authentication credentials.  This flaw exists because the RDP server stores a hard-coded RSA private key in the mstlsapi.dll library. Any local user with access to this file (on any Windows system) can retrieve the key and use it for this attack.");
                put("Solution", "- Force the use of SSL as a transport layer for this service if supported, or/and  - Select the 'Allow connections only from computers running Remote  Desktop with Network Level Authentication' setting if it is available.");
            }};
        NessusTrans translator = new NessusTrans(testData);
        try {
            Map<String, String> result = translator.transMain();
            System.out.println("最终翻译结果: " + gson.toJsonTree(result).getAsJsonObject());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}