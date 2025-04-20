package cn.viewcn.nessusrm.core;

import cn.viewcn.nessusrm.api.TenableTransApi;
import cn.viewcn.nessusrm.api.TxTransApi;
import cn.viewcn.nessusrm.api.TxTransSplitApi;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
    private Map<String, Object> transResult = new HashMap<>();
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
        // 使用本地数据库存储的中文漏洞库翻译

        String sql = "SELECT * FROM nessus_trans_plugin WHERE plugin_id = ?";

        // try-with-resources 自动关闭连接、语句和结果集
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:nessus_trans_plugin.sqlite");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, csvRow.get("Plugin ID"));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                transResult = toMap(rs); // 直接转换结果集为 Map
                String dbRisk = (String) transResult.get("risk");
                String csvRisk = csvRow.get("Risk");

                if (!dbRisk.equals(csvRisk)) {
                    transResult.put("risk_cn", riskMap.getOrDefault(csvRisk, ""));
                }
            }
            else {
                throw new SQLException("Plugin ID在数据中未查询到！");
            }

        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        }

    }

    public void transUseTenable() throws IOException {
        // 请求Tenable官方中文
        String pluginId = csvRow.get("Plugin ID");
        Map<String, Object> tenableData = gson.fromJson(TenableTransApi.translate(pluginId), new TypeToken<Map<String, Object>>() {}.getType()
        );
        transResult.put("plugin_name_cn", tenableData.get("script_name"));
        transResult.put("risk_cn", riskMap.get((String) tenableData.get("risk_factor")));
        transResult.put("synopsis_cn", delNewlines((String) tenableData.get("synopsis")));
        transResult.put("description_cn", delNewlines((String) tenableData.get("description")));
        transResult.put("solution_cn", delNewlines((String) tenableData.get("solution")));
    }

    public void transUseTx() {
        // 使用腾讯翻译api翻译
        transResult.put("plugin_name_cn", TxTransSplitApi.translate(csvRow.get("Name")));
        transResult.put("risk_cn", riskMap.get(csvRow.get("Risk")));
        transResult.put("synopsis_cn", delNewlines(TxTransSplitApi.translate(csvRow.get("Synopsis"))));
        transResult.put("description_cn", delNewlines(TxTransSplitApi.translate(csvRow.get("Description"))));
        transResult.put("solution_cn", delNewlines(TxTransSplitApi.translate(csvRow.get("Solution"))));
    }

    public Map<String, Object> transMain() {
        try {
            // 这里应添加数据库查询逻辑
             transUseDb();
        } catch (Exception e) {
            System.err.println("数据库翻译错误: " + e.getMessage());
            // 以下是原始英文漏洞信息，原封不动传递给transResult，准备存储到本地库中。
            transResult.put("plugin_id", csvRow.get("Plugin ID"));
            transResult.put("cve", riskMap.get(csvRow.get("CVE")));
            transResult.put("cvss", riskMap.get(csvRow.get("CVSS v2.0 Base Score")));
            transResult.put("risk", riskMap.get(csvRow.get("Risk")));
            transResult.put("plugin_name", riskMap.get(csvRow.get("Name")));
            transResult.put("synopsis", riskMap.get(csvRow.get("Synopsis")));
            transResult.put("description", riskMap.get(csvRow.get("Description")));
            transResult.put("solution", riskMap.get(csvRow.get("Solution")));
            transResult.put("upload_date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            try {
                transUseTenable();
                Thread.sleep(500);
            } catch (Exception ex) {
                System.err.println("官方API错误: " + ex.getMessage());
                try {
                    // 这里应添加腾讯翻译API调用
                    // 这里无论翻译是否成功均会返回原文
                    transUseTx();
                    Thread.sleep(1000);
                } catch (Exception exc) {
                    System.err.println("腾讯翻译错误: " + exc.getMessage());
                }
            }
            // 这里应添加数据库存储逻辑
        }
        return transResult;
    }

    public static void main(String[] args) {
//        测试
        Map<String, String> testData = new HashMap<String, String>() {{
                put("Plugin ID", "999999");
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
            Map<String, Object> result = translator.transMain();
            System.out.println("最终翻译结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}