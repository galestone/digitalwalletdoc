import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 单文件：发送 /v2/deposit/create，对 body 做 HMAC-SHA256 签名。
 * 直接修改下面「可配置变量」即可。
 */
public class DepositCreateTest {

    // ========== 可配置变量（按需修改） ==========
    private static final String SIGN_KEY = "您商户的签名密钥Signature Key";
    private static final String API_KEY = "您商户的API Key";
    private static final String URL = "http://154.82.113.141:21001/v2/deposit/create"; // 接口地址

    // body 参数
    private static final String CHAIN = "eth";
    private static final String ACCOUNT_ID = "100200300";
    private static final String AMOUNT = "10.00";
    private static final String ORDER_ID = "W302603188030";
    private static final String CONTRACT_ADDR = "0xb65f0057aee4e3d511607a050379b7558a15c67d";
    private static final long EXPIRE_AT = 0L;
    private static final String CALLBACK_URL = "http://127.0.0.1:28080/merchant/testdeposit/create";
    // ==========================================

    public static void main(String[] args) throws Exception {
        if (SIGN_KEY == null || SIGN_KEY.isEmpty() || "你的签名密钥".equals(SIGN_KEY)) {
            System.err.println("请先在文件里配置 SIGN_KEY");
            System.exit(1);
        }

        String body = buildBody();
        String signature = hmacSha256Hex(body, SIGN_KEY);

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("User-Agent", "insomnia/12.2.0")
                .header("Content-Type", "text/plain")
                .header("apikey", API_KEY)
                .header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }

    private static String buildBody() {
        return "{\n"
                + "    \"chain\": \"" + escapeJson(CHAIN) + "\",\n"
                + "    \"accountID\": \"" + escapeJson(ACCOUNT_ID) + "\",\n"
                + "    \"order\": {\n"
                + "        \"amount\": \"" + escapeJson(AMOUNT) + "\",\n"
                + "        \"orderID\": \"" + escapeJson(ORDER_ID) + "\",\n"
                + "        \"contractAddr\": \"" + escapeJson(CONTRACT_ADDR) + "\",\n"
                + "        \"expireAt\": " + EXPIRE_AT + ",\n"
                + "        \"callbackURL\": \"" + escapeJson(CALLBACK_URL) + "\"\n"
                + "    }\n"
                + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private static String hmacSha256Hex(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return bytesToHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}