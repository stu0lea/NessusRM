package cn.viewcn.nessusrm.api;

import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;


public class TxTransApi {
    // 从配置文件读取的密钥
    private static String secretId;
    private static String secretKey;
    // 默认源语言设为自动检测，目标语言设为英语
    private static final String DEFAULT_SOURCE_LANG = "en";
    private static final String DEFAULT_TARGET_LANG = "zh";
    // 加载配置文件
    static {
        loadConfig();
    }

    // 加载config.ini
    private static void loadConfig() {
        Properties prop = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get("config.ini"))) {
            prop.load(input);
            secretId = prop.getProperty("secretId");
            secretKey = prop.getProperty("secretKey");
        } catch (IOException ex) {
            throw new RuntimeException("无法加载配置文件 config.ini", ex);
        }
    }

    /**
     * 对外提供的翻译方法
     * @param sourceText 需要翻译的文本
     * @param sourceLang 源语言代码（如"en"）
     * @param targetLang 目标语言代码（如"zh"）
     * @return API响应结果
     */
    public static String translate(String sourceText, String sourceLang, String targetLang)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {

        String body = String.format(
                "{\"SourceText\":\"%s\",\"Source\":\"%s\",\"Target\":\"%s\",\"ProjectId\":0}",
                sourceText, sourceLang, targetLang
        );

        return doRequest(
                secretId,
                secretKey,
                "tmt",        // 服务名称
                "2018-03-21", // API版本
                "TextTranslate",
                body,
                "ap-beijing", // 地域
                ""            // token（无token留空）
        );
    }

    // 重载版本1：自动检测源语言 + 英语目标语言
    public static String translate(String sourceText) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        return translate(sourceText, DEFAULT_SOURCE_LANG, DEFAULT_TARGET_LANG);
    }
    // 以下是原有逻辑保持不变（略作整理）

    private static final OkHttpClient client = new OkHttpClient();

    private static String doRequest(
            String secretId, String secretKey,
            String service, String version, String action,
            String body, String region, String token
    ) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        Request request = buildRequest(secretId, secretKey, service, version, action, body, region, token);
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    private static Request buildRequest(
            String secretId, String secretKey,
            String service, String version, String action,
            String body, String region, String token
    ) throws NoSuchAlgorithmException, InvalidKeyException {
        String host = "tmt.tencentcloudapi.com";
        String endpoint = "https://" + host;
        String contentType = "application/json; charset=utf-8";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String auth = getAuth(secretId, secretKey, host, contentType, timestamp, body);

        return new Request.Builder()
                .header("Host", host)
                .header("X-TC-Timestamp", timestamp)
                .header("X-TC-Version", version)
                .header("X-TC-Action", action)
                .header("X-TC-Region", region)
                .header("X-TC-Token", token)
                .header("X-TC-RequestClient", "SDK_JAVA_BAREBONE")
                .header("Authorization", auth)
                .url(endpoint)
                .post(RequestBody.create(body, MediaType.parse(contentType)))
                .build();
    }

    // 其他辅助方法保持不变...
    // (包括 getAuth, sha256Hex, printHexBinary, hmac256)
    private static String getAuth(
            String secretId, String secretKey, String host, String contentType,
            String timestamp, String body
    ) throws NoSuchAlgorithmException, InvalidKeyException {
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:" + contentType + "\nhost:" + host + "\n";
        String signedHeaders = "content-type;host";

        String hashedRequestPayload = sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String canonicalRequest = "POST"
                + "\n"
                + canonicalUri
                + "\n"
                + canonicalQueryString
                + "\n"
                + canonicalHeaders
                + "\n"
                + signedHeaders
                + "\n"
                + hashedRequestPayload;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(Long.valueOf(timestamp + "000")));
        String service = host.split("\\.")[0];
        String credentialScope = date + "/" + service + "/" + "tc3_request";
        String hashedCanonicalRequest =
                sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String stringToSign =
                "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;

        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, service);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature =
                printHexBinary(hmac256(secretSigning, stringToSign)).toLowerCase();
        return "TC3-HMAC-SHA256 "
                + "Credential="
                + secretId
                + "/"
                + credentialScope
                + ", "
                + "SignedHeaders="
                + signedHeaders
                + ", "
                + "Signature="
                + signature;
    }

    public static String sha256Hex(byte[] b) throws NoSuchAlgorithmException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(b);
        return printHexBinary(d).toLowerCase();
    }

    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }

    public static byte[] hmac256(byte[] key, String msg) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }


    public static void main(String[] args) {
        try {
            String result = TxTransApi.translate("hello", "en", "zh");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}