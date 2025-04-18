package cn.viewcn.nessusrm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class TxTransSplitApi {
    private static final Logger logger = LoggerFactory.getLogger(TxTransSplitApi.class);

    public static String translate(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            return text;
        }
        try {
            if (text.length() < 2000) {
                return TxTransApi.translate(text);
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
                        throw new IllegalArgumentException("Paragraph length exceeds API limit");
                    }
                    translatedParagraphs.add(TxTransApi.translate(para));
                }

                return String.join("\n\n", translatedParagraphs);
            }
        } catch (Exception e) {
            logger.error("Commercial translation API error: text='{}', error={}", text, e.getMessage(), e);
            return text;
        }
    }

    public static void main(String[] args) {
        try {
            String result = TxTransSplitApi.translate("hello");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}