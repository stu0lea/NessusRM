package cn.viewcn.nessusrm.api;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
/**
 * 腾讯api限制翻译长度2000以内
 * 针对超长文本切分后翻译
 */
public class TxTransSplitApi {
    //
//    private static final Logger logger = LoggerFactory.getLogger(TxTransSplitApi.class);
    public static String translate(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            return text;
        }
        try {
            if (text.length() < 2000) {
                return getTranslationText(TxTransApi.translate(text));
            } else {
                // 按双换行符分割段落并清理空白
                List<String> paragraphs = new ArrayList<>();
                for (String paragraph : text.split("\\n\\n")) {
                    String trimmed = paragraph.trim();
                    if (!trimmed.isEmpty()) {
                        paragraphs.add(trimmed);
                    }
                }

                // 分段翻译处理
                List<String> translatedParagraphs = new ArrayList<>();
                for (String para : paragraphs) {
                    if (para.length() > 2000) {
                        throw new IllegalArgumentException("文本长度超过API限制");
                    }
                    translatedParagraphs.add(getTranslationText(TxTransApi.translate(para)));
                }

                return String.join("\n\n", translatedParagraphs);
            }
        } catch (Exception e) {
            System.err.println("腾讯api翻译错误: " + e.getMessage());
            // 如果翻译失败返回原始文本
            return text;
        }

    }

    // 辅助方法：解析 JSON 并提取 TargetText
    private static String getTranslationText(String jsonResponse) throws Exception {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonObject response = jsonObject.getAsJsonObject("Response");

        // 检查 TargetText 字段是否存在
        if (!response.has("TargetText")) {
            throw new Exception("腾讯API翻译错误，响应缺少TargetText字段");
        }
        return response.get("TargetText").getAsString();
    }


    public static void main(String[] args) {
            String test_text= "The lawsuit that resulted in Saturday's order said the Venezuelans detained in north Texas had been given notices about their imminent deportation in English \n\n  despite one detainee only speaking Spanish.";
            try {
            String result = TxTransSplitApi.translate(test_text);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}