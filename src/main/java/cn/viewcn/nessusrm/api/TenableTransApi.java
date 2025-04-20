package cn.viewcn.nessusrm.api;

import okhttp3.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TenableTransApi {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Pattern BUILD_ID_PATTERN = Pattern.compile("\"buildId\":\"([a-zA-Z0-9_-]+)\"");

    public static String getToken() throws IOException {
        Request request = new Request.Builder()
                .url("https://www.tenable.com/plugins/search")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMessage = "获取token请求错误：" + response;
                throw new IOException(errorMessage);
            }
            String responseBody = response.body().string();
            Matcher matcher = BUILD_ID_PATTERN.matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                String errorMessage = "未在响应体中发现tenable的token";
                throw new IOException(errorMessage);
            }
        }
    }

    public static String translate(String pluginId) throws IOException {
        String token = getToken();
        System.out.println("Token:" + token);
        String url = String.format("https://zh-cn.tenable.com/_next/data/%s/zh-CN/plugins/nessus/%s.json?type=nessus&id=%s",
                token, pluginId, pluginId);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取Plugin数据请求错误：" + response);
            }
            try {
                String jsonData = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                JsonObject pageProps = jsonObject.getAsJsonObject("pageProps");
                JsonObject pluginData = pageProps.getAsJsonObject("plugin");
                return pluginData.toString();
            } catch (Exception e) {
                throw new IOException("响应体json数据格式错误", e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            String result = TenableTransApi.translate("159826");
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}