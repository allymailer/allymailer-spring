package ua.com.allymailer.wrapper;

import ua.com.allymailer.client.ApiClient;
import ua.com.allymailer.client.ApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * {@link JavaMailSender} that sends through email/send of the AllyMailer API instead of SMTP.
 */
public class AllyMailerMailSender implements JavaMailSender {
    private final ApiClient client;
    private final Session session = Session.getInstance(new Properties());

    /** @param client low level API client ({@code AllyMailerClient.getApiClient()}) */
    public AllyMailerMailSender(ApiClient client) {
        this.client = client;
    }

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage(session);
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
        try {
            return new MimeMessage(session, contentStream);
        } catch (MessagingException e) {
            throw new MailParseException("Could not parse raw MIME content", e);
        }
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        Map<String, Object> message;
        try {
            message = MimeMessagePayload.fromMimeMessage(mimeMessage, session);
        } catch (MessagingException | IOException e) {
            throw new MailPreparationException("Could not read the message", e);
        }
        call(message);
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        for (MimeMessage mimeMessage : mimeMessages) {
            send(mimeMessage);
        }
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        MimeMessage message = createMimeMessage();
        try {
            mimeMessagePreparator.prepare(message);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            throw new MailPreparationException(e);
        }
        send(message);
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
        for (MimeMessagePreparator preparator : mimeMessagePreparators) {
            send(preparator);
        }
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        Map<String, Object> message;
        try {
            message = MimeMessagePayload.fromSimpleMessage(simpleMessage);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not read the message", e);
        }
        call(message);
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        for (SimpleMailMessage simpleMessage : simpleMessages) {
            send(simpleMessage);
        }
    }

    private void call(Map<String, Object> message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        try {
            client.call("/email/send.json", body);
        } catch (ApiException e) {
            throw new MailSendException("Unable to send the email: " + e.getMessage() + " (code "
                + e.getErrorCode() + ")", e);
        }
    }
}
