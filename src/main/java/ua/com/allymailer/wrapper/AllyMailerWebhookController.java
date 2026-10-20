package ua.com.allymailer.wrapper;

import ua.com.allymailer.api.webhook.model.WebhookCallback;
import ua.com.allymailer.api.webhook.model.WebhookCallbackEventsByUserItemEventsItem;
import ua.com.allymailer.api.webhook.model.WebhookCallbackEventsByUserItem;
import ua.com.allymailer.api.webhook.WebhookApi;
import ua.com.allymailer.client.Json;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook endpoint: GET -&gt; 200 (URL check of webhook/set), POST -&gt; signature check, a
 * {@link WebhookReceivedEvent} and a {@link WebhookEventReceivedEvent} per event. Listeners must be fast: the API
 * waits for 200 OK for 3 seconds only.
 */
@RestController
@RequestMapping("${allymailer.webhook.path:${ALLYMAILER_WEBHOOK_PATH:/allymailer/webhook}}")
public class AllyMailerWebhookController {
    private final String key;
    private final ApplicationEventPublisher publisher;

    /**
     * @param key API key the notifications are signed with
     * @param publisher publisher of the events
     */
    public AllyMailerWebhookController(String key, ApplicationEventPublisher publisher) {
        this.key = key;
        this.publisher = publisher;
    }

    /** @return 200 OK: the URL check of webhook/set */
    @GetMapping
    public ResponseEntity<String> check() {
        return ResponseEntity.ok("OK");
    }

    /**
     * @param raw body of the notification
     * @return 200 OK, 401 for a wrong signature, 400 for a wrong body
     */
    @PostMapping
    public ResponseEntity<String> receive(@RequestBody(required = false) byte[] raw) {
        String body = raw == null ? "" : new String(raw, StandardCharsets.UTF_8);
        if (!WebhookSignature.verify(body, key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        WebhookCallback callback;
        Map<String, Object> payload;
        try {
            callback = WebhookApi.parseCallback(body);
            payload = Json.asMap(Json.parse(body));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Invalid body");
        }
        publisher.publishEvent(new WebhookReceivedEvent(this, callback, payload));
        if (callback.getEventsByUser() != null) {
            for (WebhookCallbackEventsByUserItem user : callback.getEventsByUser()) {
                if (user.getEvents() == null) {
                    continue;
                }
                for (WebhookCallbackEventsByUserItemEventsItem event : user.getEvents()) {
                    publisher.publishEvent(new WebhookEventReceivedEvent(this, event, user));
                }
            }
        }
        return ResponseEntity.ok("OK");
    }
}
