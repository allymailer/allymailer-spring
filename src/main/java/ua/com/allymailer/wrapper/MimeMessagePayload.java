package ua.com.allymailer.wrapper;

import jakarta.mail.Address;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimePart;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;

/**
 * Converts a mail message to the "message" of email/send.
 * <ul>
 * <li>recipients: to, cc and bcc, the display name goes to the "to_name" substitution;</li>
 * <li>with cc the "To" and "CC" headers are set, so every recipient sees the same headers;</li>
 * <li>parts with a Content-ID and an inline disposition become inline_attachments named by it (cid:...);</li>
 * <li>X-* and List-* / In-Reply-To / References headers go to "headers".</li>
 * </ul>
 */
public final class MimeMessagePayload {
    private static final Set<String> PASSED_HEADERS = Set.of("list-unsubscribe", "list-subscribe", "list-help", "list-owner", "list-archive", "in-reply-to", "references");

    private MimeMessagePayload() {
    }

    /**
     * @param original message to send
     * @param session session to parse the serialized message with
     * @return the "message" of email/send
     * @throws MessagingException when the message can not be read
     * @throws IOException when the content can not be read
     */
    public static Map<String, Object> fromMimeMessage(MimeMessage original, Session session)
            throws MessagingException, IOException {
        InternetAddress from = first(original.getFrom());
        if (from == null) {
            throw new MailPreparationException("The message has no sender (From).");
        }
        List<InternetAddress> to = list(original.getRecipients(Message.RecipientType.TO));
        List<InternetAddress> cc = list(original.getRecipients(Message.RecipientType.CC));
        List<InternetAddress> bcc = list(original.getRecipients(Message.RecipientType.BCC));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("recipients", recipients(to, cc, bcc));
        message.put("subject", original.getSubject() == null ? "" : original.getSubject());
        sender(message, from, first(original.getReplyTo()));

        Map<String, Object> headers = new LinkedHashMap<>();
        if (!cc.isEmpty()) {
            headers.put("To", join(to));
            headers.put("CC", join(cc));
        }
        Enumeration<Header> all = original.getAllHeaders();
        while (all.hasMoreElements()) {
            Header header = all.nextElement();
            String name = header.getName().toLowerCase(Locale.ROOT);
            if (name.startsWith("x-") || PASSED_HEADERS.contains(name)) {
                headers.put(header.getName(), header.getValue());
            }
        }
        if (!headers.isEmpty()) {
            message.put("headers", headers);
        }

        // the serialized and parsed again message has the same structure whatever way it was built
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        original.writeTo(out);
        MimeMessage parsed = new MimeMessage(session, new ByteArrayInputStream(out.toByteArray()));
        Map<String, Object> body = new LinkedHashMap<>();
        List<Object> attachments = new ArrayList<>();
        List<Object> inline = new ArrayList<>();
        walk(parsed, body, attachments, inline);
        if (!body.isEmpty()) {
            message.put("body", body);
        }
        if (!attachments.isEmpty()) {
            message.put("attachments", attachments);
        }
        if (!inline.isEmpty()) {
            message.put("inline_attachments", inline);
        }
        return message;
    }

    /**
     * @param simple message to send
     * @return the "message" of email/send
     * @throws MessagingException when an address can not be parsed
     */
    public static Map<String, Object> fromSimpleMessage(SimpleMailMessage simple) throws MessagingException {
        if (simple.getFrom() == null) {
            throw new MailPreparationException("The message has no sender (From).");
        }
        List<InternetAddress> to = parse(simple.getTo());
        List<InternetAddress> cc = parse(simple.getCc());
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("recipients", recipients(to, cc, parse(simple.getBcc())));
        message.put("subject", simple.getSubject() == null ? "" : simple.getSubject());
        sender(message, new InternetAddress(simple.getFrom()),
            simple.getReplyTo() == null ? null : new InternetAddress(simple.getReplyTo()));
        if (!cc.isEmpty()) {
            Map<String, Object> headers = new LinkedHashMap<>();
            headers.put("To", join(to));
            headers.put("CC", join(cc));
            message.put("headers", headers);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plaintext", simple.getText() == null ? "" : simple.getText());
        message.put("body", body);
        return message;
    }

    private static void sender(Map<String, Object> message, InternetAddress from, InternetAddress replyTo) {
        message.put("from_email", from.getAddress());
        if (from.getPersonal() != null && !from.getPersonal().isEmpty()) {
            message.put("from_name", from.getPersonal());
        }
        if (replyTo != null) {
            message.put("reply_to", replyTo.getAddress());
            if (replyTo.getPersonal() != null && !replyTo.getPersonal().isEmpty()) {
                message.put("reply_to_name", replyTo.getPersonal());
            }
        }
    }

    private static List<Object> recipients(List<InternetAddress> to, List<InternetAddress> cc,
            List<InternetAddress> bcc) {
        List<Object> recipients = new ArrayList<>();
        List<InternetAddress> all = new ArrayList<>(to);
        all.addAll(cc);
        all.addAll(bcc);
        for (InternetAddress address : all) {
            Map<String, Object> recipient = new LinkedHashMap<>();
            recipient.put("email", address.getAddress());
            if (address.getPersonal() != null && !address.getPersonal().isEmpty()) {
                Map<String, Object> substitutions = new LinkedHashMap<>();
                substitutions.put("to_name", address.getPersonal());
                recipient.put("substitutions", substitutions);
            }
            recipients.add(recipient);
        }
        return recipients;
    }

    private static void walk(Part part, Map<String, Object> body, List<Object> attachments, List<Object> inline)
            throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                walk(multipart.getBodyPart(i), body, attachments, inline);
            }
            return;
        }
        String disposition = part.getDisposition();
        String contentId = part instanceof MimePart ? ((MimePart) part).getContentID() : null;
        boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition) || part.getFileName() != null
            || contentId != null;
        if (!isAttachment && part.isMimeType("text/plain") && !body.containsKey("plaintext")) {
            body.put("plaintext", part.getContent().toString());
            return;
        }
        if (!isAttachment && part.isMimeType("text/html") && !body.containsKey("html")) {
            body.put("html", part.getContent().toString());
            return;
        }
        boolean isInline = contentId != null && !Part.ATTACHMENT.equalsIgnoreCase(disposition);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", new ContentType(part.getContentType()).getBaseType().toLowerCase(Locale.ROOT));
        String name = isInline ? contentId.replaceAll("^<|>$", "") : part.getFileName();
        item.put("name", name == null ? "attachment" : name);
        try (var in = part.getInputStream()) {
            item.put("content", Base64.getEncoder().encodeToString(in.readAllBytes()));
        }
        (isInline ? inline : attachments).add(item);
    }

    private static InternetAddress first(Address[] addresses) {
        List<InternetAddress> list = list(addresses);
        return list.isEmpty() ? null : list.get(0);
    }

    private static List<InternetAddress> list(Address[] addresses) {
        List<InternetAddress> list = new ArrayList<>();
        if (addresses != null) {
            for (Address address : addresses) {
                if (address instanceof InternetAddress) {
                    list.add((InternetAddress) address);
                }
            }
        }
        return list;
    }

    private static List<InternetAddress> parse(String[] addresses) throws MessagingException {
        List<InternetAddress> list = new ArrayList<>();
        if (addresses != null) {
            for (String address : addresses) {
                list.add(new InternetAddress(address));
            }
        }
        return list;
    }

    private static String join(List<InternetAddress> addresses) {
        List<String> out = new ArrayList<>();
        for (InternetAddress address : addresses) {
            out.add(address.toUnicodeString());
        }
        return String.join(", ", out);
    }
}
