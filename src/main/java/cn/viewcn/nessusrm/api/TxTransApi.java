package cn.viewcn.nessusrm.api;

import okhttp3.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Translator {
    private static String secretId;
    private static String secretKey;
    private static final Gson gson = new Gson();

    static {
        loadConfig();
    }

    // 加载配置文件（保持不变）
    private static void loadConfig() { /* 同上，略 */ }

    /**
     * 翻译方法（返回解析后的JsonObject）
     * @param sourceText 原文
     * @param sourceLang 源语言（如"en"）
     * @param targetLang 目标语言（如"zh"）
     * @return 解析后的JSON对象
     */
    public static JsonObject translate(String sourceText, String sourceLang, String targetLang)
            throws TranslationException {
        try {
            String body = String.format(
                    "{\"SourceText\":\"%s\",\"Source\":\"%s\",\"Target\":\"%s\",\"ProjectId\":0}",
                    sourceText, sourceLang, targetLang
            );

            String jsonResponse = doRequest(
                    secretId, secretKey,
                    "tmt", "2018-03-21", "TextTranslate",
                    body, "ap-beijing", ""
            );

            return parseJsonResponse(jsonResponse);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new TranslationException("请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析API返回的JSON字符串为JsonObject
     */
    private static JsonObject parseJsonResponse(String jsonResponse) throws TranslationException {
        try {
            return gson.fromJson(jsonResponse, JsonObject.class);
        } catch (JsonSyntaxException e) {
            throw new TranslationException("JSON解析失败: " + e.getMessage(), e);
        }
    }

    // 自定义异常类（封装所有可能的错误）
    public static class TranslationException extends Exception {
        public TranslationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 其余网络请求方法（doRequest/buildRequest/getAuth等保持不变）
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
}