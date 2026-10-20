package ua.com.allymailer.wrapper;

import ua.com.allymailer.api.webhook.model.WebhookCallback;
import java.util.Map;
import org.springframework.context.ApplicationEvent;

/**
 * A webhook notification with a valid signature: all its events.
 */
public class WebhookReceivedEvent extends ApplicationEvent {
    private final WebhookCallback callback;
    private final Map<String, Object> payload;

    /**
     * @param source the controller
     * @param callback parsed notification
     * @param payload decoded body of the request
     */
    public WebhookReceivedEvent(Object source, WebhookCallback callback, Map<String, Object> payload) {
        super(source);
        this.callback = callback;
        this.payload = payload;
    }

    /** @return parsed notification */
    public WebhookCallback getCallback() {
        return callback;
    }

    /** @return decoded body of the request */
    public Map<String, Object> getPayload() {
        return payload;
    }
}
