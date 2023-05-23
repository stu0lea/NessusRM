package cn.viewcn.nessusrm.core;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.*;

public class Trans {
    private Map<String, String> csvRowMap;
    private final Map<String, String> riskMap;
    private Map<String, String> transResult;

    public Trans(Map<String, String> csvRowDict) {
        this.csvRowMap = csvRowDict;
        this.riskMap = new HashMap<String, String>() {{
            put("Critical", "严重");
            put("High", "高危");
            put("Medium", "中危");
            put("Low", "低危");
            put("Info", "信息");
            put("None", "信息");
        }};
        this.transResult = new HashMap<String, String>();
    }


    public static String getToken() throws Exception {
        String url = "https://www.tenable.com/plugins/search";
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String respText = response.body().string();
            Pattern pattern = Pattern.compile("\"buildId\":\"([a-zA-Z0-9_-]+)\"");
            Matcher matcher = pattern.matcher(respText);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                throw new Exception("Token not found");
            }
        }
    }


    public static JSONObject transApiTenable(String plugin_id) throws Exception {
        String token = getToken();
        String url = String.format("https://zh-cn.tenable.com/_next/data/%s/zh-CN/plugins/nessus/%s.json?type=nessus&id=%s", token, plugin_id, plugin_id);
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).header("User-Agent", userAgent).build();
        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            ResponseBody responseBody = response.body();
            if (responseBody == null || statusCode != 200) {
                throw new IOException("Response is error");
            }
            String respText = responseBody.string();
            JSONObject respJson = JSONObject.parseObject(respText);
            return respJson.getJSONObject("pageProps").getJSONObject("plugin");
        }
    }


    public String transApiTx(String text) {

        return null;

    }
    public void transUseDb() {
        Connection conn = null;
        try {
            //连接SQLite数据库
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:nessus_vul_db.sqlite");
            System.out.println("Opened database successfully");
            //查询指定ID的数据
            String sql = "SELECT * FROM nessus_plugin WHERE plugin_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, 10180); //将123替换为您要查询的ID
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Column 1: " + rs.getString("plugin_name")); //将column1替换为您要查询的列名
                System.out.println("Column 2: " + rs.getString("cve")); //将column2替换为您要查询的列名
                //...
            }
            rs.close();
            pstmt.close();
            conn.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public void transUseTenable(Map<String, String> csvRowDict) {
        // 在这里实现"trans_use_tenable"方法
    }

    private String delN(String content) {
        String content_res;
        content_res= content.replace("\n\n", "[@]")
                .replace("\n", "")
                .replace("[@]", "\r\n")
                .replace("\u00a0", "");
        return content_res;
    }

    public Map<String, String> transMain(Map<String, String> csvRowDict) {
        try {
            // 1.本地库翻译
            transUseDb();
        } catch (Exception e1) {
            try {
                // 2.官方API翻译
                transUseTenable(csvRowDict);
                Thread.sleep(500);
            } catch (Exception e2) {
                try {
                    // 3.腾讯翻译API
                    transResult.put("plugin_name_cn", transApiTx(csvRowDict.get("Name")));
                    transResult.put("synopsis_cn", delN(transApiTx(csvRowDict.get("Synopsis"))));
                    transResult.put("description_cn", delN(transApiTx(csvRowDict.get("Description"))));
                    transResult.put("solution_cn", delN(transApiTx(csvRowDict.get("Solution"))));
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    System.out.println("All API is error");
                }
            }
        }
        return transResult;
    }

    public static void main(String[] args) throws Exception {
        // 测试token获取是否正常
        String t = Trans.getToken();
        System.out.println(t);
        // 测试获取漏洞是否正常
        // JSONObject res =  Trans.transApiTenable("157192");
        JSONObject res =  Trans.transApiTenable("56728");
        System.out.println(JSONObject.toJSONString(res));
    }

}
