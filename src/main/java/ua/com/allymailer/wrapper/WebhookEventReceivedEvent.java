package ua.com.allymailer.wrapper;

import ua.com.allymailer.api.webhook.model.WebhookCallbackEventsByUserItemEventsItem;
import ua.com.allymailer.api.webhook.model.WebhookCallbackEventsByUserItem;
import org.springframework.context.ApplicationEvent;

/**
 * One event of a webhook notification (email status, spam block...) and the user/project of the event.
 */
public class WebhookEventReceivedEvent extends ApplicationEvent {
    private final WebhookCallbackEventsByUserItemEventsItem event;
    private final WebhookCallbackEventsByUserItem user;

    /**
     * @param source the controller
     * @param event the event
     * @param user user/project of the event
     */
    public WebhookEventReceivedEvent(Object source, WebhookCallbackEventsByUserItemEventsItem event, WebhookCallbackEventsByUserItem user) {
        super(source);
        this.event = event;
        this.user = user;
    }

    /** @return the event: getEventName(), getEventData() */
    public WebhookCallbackEventsByUserItemEventsItem getEvent() {
        return event;
    }

    /** @return user/project of the event */
    public WebhookCallbackEventsByUserItem getUser() {
        return user;
    }
}
