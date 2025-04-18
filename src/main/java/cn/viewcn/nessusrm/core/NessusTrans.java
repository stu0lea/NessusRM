package cn.viewcn.nessusrm.core;

import cn.viewcn.nessusrm.api.TenableTransApi;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class NessusTrans {
    private static final Map<String, String> RISK_MAP = new HashMap<String, String>() {{
        put("Critical", "严重");
        put("High", "高危");
        put("Medium", "中危");
        put("Low", "低危");
        put("Info", "信息");
        put("None", "信息");
    }};

    private Map<String, String> csvRow;
    private Map<String, Object> transResult = new HashMap<>();

    public NessusTrans(Map<String, String> csvRow) {
        this.csvRow = csvRow;
    }
    private static final Gson gson = new Gson();
    private String delNewlines(String text) {
        return text.replace("\n\n", "[@]")
                .replace("\n", "")
                .replace("[@]", "\r\n")
                .replace("\\xa0", "");
    }

    public void transUseTenable() throws IOException {
        String pluginId = csvRow.get("Plugin ID");
        Map<String, Object> tenableData = gson.fromJson(TenableTransApi.trans(pluginId), new TypeToken<Map<String, Object>>() {}.getType()
        );
        transResult.put("plugin_name_cn", tenableData.get("script_name"));
        transResult.put("synopsis_cn", delNewlines((String) tenableData.get("synopsis")));
        transResult.put("description_cn", delNewlines((String) tenableData.get("description")));
        transResult.put("solution_cn", delNewlines((String) tenableData.get("solution")));
    }

    public Map<String, Object> transMain() {
        try {
            // 这里应添加数据库查询逻辑
            // transUseDb();
        } catch (Exception e) {
            System.err.println("数据库翻译错误: " + e.getMessage());

            transResult.put("plugin_id", csvRow.get("Plugin ID"));
            transResult.put("risk_cn", RISK_MAP.get(csvRow.get("Risk")));

            try {
                transUseTenable();
                Thread.sleep(500);
            } catch (Exception ex) {
                System.err.println("官方API错误: " + ex.getMessage());
                try {
                    // 这里应添加腾讯翻译API调用
//                     transUseTx();
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
        Map<String, String> testData = new HashMap<>();
        testData.put("Plugin ID", "159826");
        testData.put("Risk", "High");

        NessusTrans translator = new NessusTrans(testData);
        try {
            Map<String, Object> result = translator.transMain();
            System.out.println("翻译结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}