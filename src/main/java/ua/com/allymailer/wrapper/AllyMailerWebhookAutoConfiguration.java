package ua.com.allymailer.wrapper;

import ua.com.allymailer.client.Configuration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Webhook endpoint of a servlet web application, see {@link AllyMailerWebhookController}.
 */
@AutoConfiguration(after = AllyMailerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnProperty(prefix = "allymailer", name = "webhook.enabled", havingValue = "true", matchIfMissing = true)
public class AllyMailerWebhookAutoConfiguration {
    /**
     * The webhook controller.
     *
     * @param properties {@code allymailer.*} properties
     * @param environment environment of the application
     * @param config connection settings (API key)
     * @param publisher publisher of the webhook events
     * @return the controller
     */
    @Bean
    @ConditionalOnMissingBean
    public AllyMailerWebhookController allymailerWebhookController(AllyMailerProperties properties,
            Environment environment, Configuration config, ApplicationEventPublisher publisher) {
        String key = AllyMailerAutoConfiguration.setting(properties.getWebhook().getKey(), environment,
            "ALLYMAILER_WEBHOOK_KEY");
        return new AllyMailerWebhookController(key == null ? config.getApiKey() : key, publisher);
    }
}
