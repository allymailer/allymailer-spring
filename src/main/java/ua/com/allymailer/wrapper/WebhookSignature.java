package ua.com.allymailer.wrapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Signature of the webhook notifications: the "auth" field is the MD5 of the request body in which the "auth"
 * value is replaced by the API key of the user/project of the webhook.
 */
public final class WebhookSignature {
    private static final Pattern AUTH = Pattern.compile("\"auth\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final String PLACEHOLDER = "00000000000000000000000000000000";

    private WebhookSignature() {
    }

    /**
     * @param body raw body of the notification
     * @param apiKey API key of the user/project of the webhook
     * @return true when the signature is valid
     */
    public static boolean verify(String body, String apiKey) {
        if (apiKey == null || apiKey.isEmpty() || body == null) {
            return false;
        }
        Matcher match = AUTH.matcher(body);
        if (!match.find()) {
            return false;
        }
        String auth = match.group(1).toLowerCase(Locale.ROOT);
        String expected = md5(body.replace(match.group(1), apiKey));
        return MessageDigest.isEqual(auth.getBytes(StandardCharsets.US_ASCII),
            expected.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Signs a body that has "auth" set to the 32 zeros placeholder, e.g. for tests.
     *
     * @param body body with the placeholder
     * @param apiKey API key to sign with
     * @return the signed body
     */
    public static String sign(String body, String apiKey) {
        return body.replace(PLACEHOLDER, md5(body.replace(PLACEHOLDER, apiKey)));
    }

    private static String md5(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
