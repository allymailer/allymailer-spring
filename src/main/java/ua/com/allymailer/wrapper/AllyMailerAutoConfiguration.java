package ua.com.allymailer.wrapper;

import ua.com.allymailer.AllyMailerClient;
import ua.com.allymailer.client.Configuration;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * AllyMailer API client: {@link Configuration} and {@link AllyMailerClient} beans.
 */
@AutoConfiguration
@EnableConfigurationProperties(AllyMailerProperties.class)
public class AllyMailerAutoConfiguration {
    /**
     * A property, or the environment variable when the property is empty.
     *
     * @param value value of the property
     * @param environment environment of the application
     * @param variable name of the environment variable
     * @return the value, null when both are empty
     */
    static String setting(String value, Environment environment, String variable) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        String env = environment.getProperty(variable);
        return env == null || env.isBlank() ? null : env.trim();
    }

    /**
     * Connection settings of the API.
     *
     * @param properties {@code allymailer.*} properties
     * @param environment environment of the application
     * @return the configuration
     */
    @Bean
    @ConditionalOnMissingBean
    public Configuration allymailerConfiguration(AllyMailerProperties properties, Environment environment) {
        String apiKey = setting(properties.getApiKey(), environment, "ALLYMAILER_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException("AllyMailer: set the API key (allymailer.api-key or ALLYMAILER_API_KEY)");
        }
        Configuration config = new Configuration(apiKey).setPlatform("spring.2.0.0");
        String host = setting(properties.getHost(), environment, "ALLYMAILER_HOST");
        if (host != null) {
            config.setBaseUrl(Configuration.baseUrlFor(host));
        }
        String timeout = setting(properties.getTimeout() == null ? null : properties.getTimeout().toString(),
            environment, "ALLYMAILER_TIMEOUT");
        if (timeout != null) {
            config.setTimeout(Duration.ofMillis((long) (Double.parseDouble(timeout) * 1000)));
        }
        return config;
    }

    /**
     * The API client.
     *
     * @param config connection settings
     * @return the client
     */
    @Bean
    @ConditionalOnMissingBean
    public AllyMailerClient allymailerClient(Configuration config) {
        return new AllyMailerClient(config);
    }
}
