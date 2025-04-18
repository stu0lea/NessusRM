package cn.viewcn.nessusrm.api;

import okhttp3.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;


public class TenableTransApi {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final Pattern BUILD_ID_PATTERN = Pattern.compile("\"buildId\":\"([a-zA-Z0-9_-]+)\"");

    public static String getToken() throws IOException {
        Request request = new Request.Builder()
                .url("https://www.tenable.com/plugins/search")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            Matcher matcher = BUILD_ID_PATTERN.matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                throw new IOException("Token not found in response");
            }
        }
    }

    public static String trans(String pluginId) throws IOException {
        String token = getToken();
        System.out.println("Token:"+token);
        String url = String.format("https://zh-cn.tenable.com/_next/data/%s/zh-CN/plugins/nessus/%s.json?type=nessus&id=%s",
                token, pluginId, pluginId);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code());
            }

            String jsonData = response.body().string();
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
            JsonObject pageProps = jsonObject.getAsJsonObject("pageProps");
            JsonObject pluginData = pageProps.getAsJsonObject("plugin");

            if (pluginData != null && !pluginData.isJsonNull()) {
                return pluginData.getAsString();
            } else {
                throw new IOException("Plugin data not found in response");
            }
        } catch (JsonSyntaxException e) {
            throw new IOException("Invalid JSON response", e);
        }
    }

    public static void main(String[] args) {
        try {
            String result = TenableTransApi.trans("159826");
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}