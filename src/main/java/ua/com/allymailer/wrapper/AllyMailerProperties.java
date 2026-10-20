package ua.com.allymailer.wrapper;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the AllyMailer API: {@code allymailer.*} properties, the {@code ALLYMAILER_*} environment variables
 * are used when a property is not set.
 */
@ConfigurationProperties("allymailer")
public class AllyMailerProperties {
    /** User or project API key. */
    private String apiKey;

    /** API host (datacenter) or a full base URL; empty: the default host. */
    private String host;

    /** Request timeout, seconds. */
    private Double timeout;

    private final Webhook webhook = new Webhook();

    private final Mail mail = new Mail();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Double getTimeout() {
        return timeout;
    }

    public void setTimeout(Double timeout) {
        this.timeout = timeout;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public Mail getMail() {
        return mail;
    }

    /** Webhook endpoint. */
    public static class Webhook {
        /** Register the webhook controller. */
        private boolean enabled = true;

        /** Path of the endpoint. */
        private String path = "/allymailer/webhook";

        /** API key the notifications are signed with; empty: the API key. */
        private String key;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    /** JavaMailSender over email/send. */
    public static class Mail {
        /** Register the mail sender. */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
