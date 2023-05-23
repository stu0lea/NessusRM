package cn.viewcn.nessusrm.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSONObject;

public class TxTransApi {
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String secretId;
    private final String secretKey;
    private String region = "ap-beijing";
    private String source = "en";
    private String target = "zh";
    private Integer project_id = 0;

    public TxTransApi() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("config.ini")) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Error reading configuration file: " + e.getMessage());
        }
        secretId = properties.getProperty("txApiId");
        secretKey = properties.getProperty("txApiKey");
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setProjectId(int project_id) {
        this.project_id = project_id;
    }

    public JSONObject translate(String source_text) throws Exception {
        String service = "tmt";
        String algorithm = "TC3-HMAC-SHA256";
        String action = "TextTranslate";
        String version = "2018-03-21";
        String host = service + "." + region + ".tencentcloudapi.com";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(Long.parseLong(timestamp + "000")));

        // ************* 步骤 1：拼接规范请求串 *************
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                + "host:" + host + "\n" + "x-tc-action:" + action.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";

        String payloadTemplate = "{\"SourceText\":\"%s\",\"Source\":\"%s\",\"Target\":\"%s\",\"ProjectId\":%d}";
        String payload = String.format(payloadTemplate, source_text, source, target, project_id);

        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = httpRequestMethod + "\n" + canonicalUri + "\n" + canonicalQueryString + "\n"
                + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;

        // ************* 步骤 2：拼接待签名字符串 *************

        String credentialScope = date + "/" + service + "/" + "tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = algorithm + "\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;

        // ************* 步骤 3：计算签名 *************
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(UTF8), date);
        byte[] secretService = hmac256(secretDate, service);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        byte[] signature = hmac256(secretSigning, stringToSign);
        String hexSignature = DatatypeConverter.printHexBinary(signature).toLowerCase();

        // ************* 步骤 4：拼接 Authorization *************
        String authorization = algorithm + " " + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + hexSignature;

        // ************* 步骤 5：发送请求 *************
        HttpPost httpPost = new HttpPost("https://" + host + "/");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        httpPost.setHeader(HttpHeaders.HOST, host);
        httpPost.setHeader("X-TC-Action", action);
        httpPost.setHeader("Authorization", authorization);
        httpPost.setHeader("X-TC-Timestamp", timestamp);
        httpPost.setHeader("X-TC-Version", version);
        httpPost.setHeader("X-TC-Region", region);
        httpPost.setHeader("X-TC-Language", "zh-CN");

        httpPost.setEntity(new StringEntity(payload, UTF8));

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                return JSONObject.parseObject(EntityUtils.toString(entity, UTF8));

            } else {
                return null;
            }
        }
    }

    private static byte[] hmac256(byte[] key, String msg) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(msg.getBytes(UTF8));
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(s.getBytes(UTF8));
        return DatatypeConverter.printHexBinary(digest).toLowerCase();
    }

    public static void main(String[] args) throws Exception {
        TxTransApi t = new TxTransApi();
        JSONObject res = t.translate("test");
        System.out.println(res.toJSONString());
    }

}