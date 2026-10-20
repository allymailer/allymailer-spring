# AllyMailer Spring Boot 2.0.0

Web API 1.90

Покроковий посібник: [Надсилання листів на Java](https://docs.allymailer.com.ua/ua/send-with-java) · Документація для LLM: [llms.txt](https://docs.allymailer.com.ua/llms.txt) · [Довідник API](https://docs.allymailer.com.ua/ua/web-api-ref)

Інтеграція [Java SDK AllyMailer](https://github.com/allymailer/allymailer-java-sdk) (``) з Spring Boot: клієнт API в application context (auto-configuration), ендпоінт вебхуків із перевіркою підпису, транспорт JavaMailSender, що надсилає листи через `email/send`.

## Встановлення

```bash
# pom.xml
<dependency>
    <groupId>ua.com.allymailer</groupId>
    <artifactId>allymailer-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

Вимоги: Java 17+, Spring Boot 3.

## Отримайте API-ключ

1. Зареєструйтеся на [cp.allymailer.com](https://cp.allymailer.com).
2. Скопіюйте API-ключ у [панелі керування](https://cp.allymailer.com).
3. Покладіть його у змінну оточення:

```bash
export ALLYMAILER_API_KEY="your-api-key"
```

Пакет бере його з `ALLYMAILER_API_KEY` (див. налаштування нижче).

> **Лише на сервері.** Використовуйте цей пакет у серверному коді: API-маршрути, серверні функції, воркери.
> Ніколи не передавайте API-ключ у браузер чи мобільний застосунок і не комітьте його.

## Налаштування

За замовчуванням налаштування беруться зі змінних оточення:

```yaml
# application.yml (or the ALLYMAILER_* environment variables)
allymailer:
  api-key: ${ALLYMAILER_API_KEY}
  host: cp.allymailer.com
  timeout: 30
  webhook:
    path: /allymailer/webhook
  mail:
    enabled: true
```

| Параметр | Змінна оточення | За замовчуванням | Опис |
|---|---|---|---|
| `allymailer.api-key` | `ALLYMAILER_API_KEY` |  | API-ключ користувача або проєкту (обов'язковий). |
| `allymailer.host` | `ALLYMAILER_HOST` | `cp.allymailer.com` | Хост API (датацентр: `cp.allymailer.com`) або повний базовий URL. |
| `allymailer.timeout` | `ALLYMAILER_TIMEOUT` | `30` | Таймаут запиту, секунди. |
| `allymailer.webhook.key` | `ALLYMAILER_WEBHOOK_KEY` |  | API-ключ, яким підписані вебхуки (ключ користувача/проєкту вебхука), якщо він відрізняється від API-ключа. |
| `allymailer.webhook.path` | `ALLYMAILER_WEBHOOK_PATH` | `/allymailer/webhook` | Шлях ендпоінта вебхуків. |

Змінні `ALLYMAILER_*` читаються, якщо властивість `allymailer.*` не задана. `allymailer.webhook.enabled=false` вимикає контролер вебхуків, `allymailer.mail.enabled=false` — надсилання листів (воно підключається, якщо в classpath є `spring-boot-starter-mail` і немає іншого біна `MailSender`).

## Перший лист — без налаштування DNS

Кожен обліковий запис може одразу надсилати тестові листи із **sandbox-домену**: він уже підтверджений, DKIM і SPF налаштовані.
Листи доставляються лише на підтверджені вами адреси.

У прикладах — клієнт API з пакета (див. налаштування вище).

**1. Отримайте sandbox-домен** (`domain/get-sandbox-domain`). Перший виклик створює домен, наступні повертають той самий.

```java
import ua.com.allymailer.AllyMailerClient;

@Service
public class MailService {
    private final AllyMailerClient client;

    public MailService(AllyMailerClient client) {
        this.client = client;
    }

    public String getSandboxDomain() {
        var response = client.domain().getSandboxDomain();
        return response.getDomain();
    }
}
```

**2. Підтвердіть свою скриньку** (`confirmed-email/create`). AllyMailer надішле лист із підтвердженням; перейдіть за посиланням із нього протягом 48 годин.

```java
import ua.com.allymailer.AllyMailerClient;
import ua.com.allymailer.api.confirmedemail.model.ConfirmedEmailRequest;

@Service
public class MailService {
    private final AllyMailerClient client;

    public MailService(AllyMailerClient client) {
        this.client = client;
    }

    public String confirmEmail() {
        var response = client.confirmedEmail().create(new ConfirmedEmailRequest()
            .setEmail("you@example.com"));
        return response.getStatus();
    }
}
```

Статус перевірте через `confirmed-email/list` — у вашої адреси має бути `"status": "confirmed"`.

**3. Надішліть лист** (`email/send`) з будь-якої адреси sandbox-домену на підтверджену адресу.

```java
import ua.com.allymailer.AllyMailerClient;
import java.util.List;
import ua.com.allymailer.api.email.model.EmailSendRequest;
import ua.com.allymailer.api.email.model.MessageObject;
import ua.com.allymailer.api.email.model.MessageObjectRecipientsItem;
import ua.com.allymailer.common.model.BodyObject;

@Service
public class MailService {
    private final AllyMailerClient client;

    public MailService(AllyMailerClient client) {
        this.client = client;
    }

    public String send() {
        var response = client.email().send(new EmailSendRequest()
            .setMessage(new MessageObject()
                    .setRecipients(List.of(
                            new MessageObjectRecipientsItem()
                                .setEmail("you@example.com")))
                    .setBody(new BodyObject()
                            .setHtml("<p>Працює!</p>"))
                    .setSubject("Привіт від AllyMailer")
                    .setFromEmail("noreply@<sandbox-domain>")));
        return response.getJobId();
    }
}
```

### Що повертається

```json
{
  "status": "success",
  "job_id": "1ZymBc-00041N-9X",
  "emails": [
    "you@example.com"
  ]
}
```

У разі помилки API повертає `status: "error"`, `message` і числовий `code`:

```json
{
  "status": "error",
  "code": 102,
  "message": "Error ID:83274F98-C7D5-11F1-B221-6214FF8707B1. Can not decode key wrongkey"
}
```

### Те саме через curl

```bash
BASE=https://cp.allymailer.com/ua/transactional/api/v1

curl -s -X POST $BASE/domain/get-sandbox-domain.json \
  -H "Content-Type: application/json" -H "X-API-KEY: $ALLYMAILER_API_KEY" -d '{}'

curl -s -X POST $BASE/confirmed-email/create.json \
  -H "Content-Type: application/json" -H "X-API-KEY: $ALLYMAILER_API_KEY" \
  -d '{"email": "you@example.com"}'

curl -s -X POST $BASE/email/send.json \
  -H "Content-Type: application/json" -H "X-API-KEY: $ALLYMAILER_API_KEY" \
  -d '{
        "message": {
          "recipients": [
            {
              "email": "you@example.com"
            }
          ],
          "from_email": "noreply@<sandbox-domain>",
          "subject": "Привіт від AllyMailer",
          "body": {
            "html": "<p>Працює!</p>"
          }
        }
      }'
```

## Надсилання листів

Листи JavaMailSender надсилаються через [email/send](https://docs.allymailer.com.ua/ua/web-api-ref#email-send): відправник, to/cc/bcc, reply-to, тема, HTML і текстова частини, вкладення, inline-зображення та заголовки `X-*`.

```java
@Autowired
private JavaMailSender mailSender;

MimeMessage message = mailSender.createMimeMessage();
MimeMessageHelper helper = new MimeMessageHelper(message, true);
helper.setFrom("shop@example.com", "Shop");
helper.setTo("user@example.com");
helper.setSubject("Order shipped");
helper.setText("<b>Your order is on the way</b>", true);
message.setHeader("X-Order", "42");
mailSender.send(message);
```

## Вебхуки

Ендпоінт відповідає 200 OK на `GET` (перевірка URL під час [webhook/set](https://docs.allymailer.com.ua/ua/web-api-ref#webhook-set)) і приймає `POST`-сповіщення: перевіряє поле `auth` (MD5 тіла запиту, у якому значення `auth` замінено на API-ключ), на неправильний підпис відповідає 401, а сповіщення і кожну його подію передає застосунку.

```java
import ua.com.allymailer.wrapper.WebhookEventReceivedEvent;

@Component
public class DeliveryListener {
    @EventListener
    public void onEvent(WebhookEventReceivedEvent e) {
        var data = e.getEvent().getEventData();
        log.info("AllyMailer event {}: {} {}", e.getEvent().getEventName(), data.getEmail(), data.getStatus());
    }
}
```

Зареєструйте URL ендпоінта методом `webhook/set`, наприклад `https://example.com/allymailer/webhook`.

## Клієнт API

Усі розділи API доступні через клієнт, див. [документацію SDK](https://github.com/allymailer/allymailer-java-sdk).

```java
import ua.com.allymailer.AllyMailerClient;

@Service
public class UsageService {
    private final AllyMailerClient client;

    public UsageService(AllyMailerClient client) {
        this.client = client;
    }

    public Integer emailsSent() {
        var accounting = client.system().info().getAccounting();
        return accounting == null ? null : accounting.getEmailsSent();
    }
}
```

## Надсилання через API

Окрім JavaMailSender, листи можна надсилати напряму через клієнт API. Клієнт викликає [email/send](https://docs.allymailer.com.ua/ua/web-api-ref#email-send) з усіма його параметрами: шаблони, підстановки, метадані, трекінг. Поля запиту та інші приклади — у [документації Java SDK](https://github.com/allymailer/allymailer-java-sdk/blob/HEAD/docs/Email.md#send).

```java
import ua.com.allymailer.AllyMailerClient;
import java.util.List;
import java.util.Map;
import ua.com.allymailer.api.email.model.EmailSendRequest;
import ua.com.allymailer.api.email.model.MessageObject;
import ua.com.allymailer.api.email.model.MessageObjectRecipientsItem;
import ua.com.allymailer.common.model.BodyObject;

@Service
public class MailService {
    private final AllyMailerClient client;

    public MailService(AllyMailerClient client) {
        this.client = client;
    }

    public String send() {
        var response = client.email().send(new EmailSendRequest()
            .setMessage(new MessageObject()
                    .setRecipients(List.of(
                            new MessageObjectRecipientsItem()
                                .setEmail("user@example.com")
                                .setSubstitutions(Map.of(
                                        "CustomerId", "12452",
                                        "to_name", "John Smith"))))
                    .setBody(new BodyObject()
                            .setHtml("<b>Hello, {{to_name}}</b>")
                            .setPlaintext("Hello, {{to_name}}"))
                    .setSubject("AllyMailer test email ")
                    .setFromEmail("email@example.com")
                    .setFromName("John Smith")));
        return response.getJobId();
    }
}
```

## Обмеження sandbox

- Надсилання лише на підтверджені адреси: до 10 на обліковий запис.
- До 100 листів на день.
- Sandbox-домен спливає через 31 день; знову викличте `domain/get-sandbox-domain`, щоб отримати новий.
- Працює лише з API-ключем облікового запису, не з ключем проєкту.
- Не показується в `domain/list`; інші методи `domain/*` до нього не застосовуються.

Sandbox — для розробки та тестування. Не використовуйте sandbox-домен як відправника на проді.

## Перед продом: підтвердіть свій домен

1. `domain/get-dns-records` — повертає значення для DNS-записів.
2. Додайте ці записи у свого DNS-провайдера (їх також показує сторінка домену в панелі керування):

   | Тип | Хост | Значення |
   |---|---|---|
   | TXT | `@` | значення `verification-record` (`allymailer-validate-hash=…`) |
   | TXT | `<селектор>._domainkey` (селектор — на сторінці домену в панелі керування, наприклад `us`) | `v=DKIM1; k=rsa; p=<dkim>` |
   | TXT | `@` | `v=spf1 include:spf.allymailer.com ~all` |
   | CNAME | `_dmarc` | значення `dmarc` |

3. `domain/validate-verification-record`, потім `domain/validate-dkim`.
4. `domain/list` — дочекайтеся, поки запис підтвердження буде підтверджено, а DKIM — активний.
5. Беріть `from_email` з конфігурації, щоб перехід із sandbox не потребував змін у коді.

## Надійне надсилання

- **Повтори:** ставте `message.idempotence_key` у надсиланнях, які можуть повторюватися, щоб повторний запит не надіслав лист двічі.
- **Ліміти:** до 500 отримувачів і 10 МБ на запит; кожне вкладення до 7 МБ.
- **Тіло:** завжди передавайте `body.html` або `body.plaintext`.
- **Статус доставки:** зареєструйте вебхук (`webhook/set`) зі швидким ідемпотентним HTTPS-обробником замість опитування.
- **Типові налаштування:** не прибирайте стандартний блок відписки та трекінг; змінити їх можна через підтримку AllyMailer.
- **Помилки:** повторюйте `429` і `5xx` з backoff; інші помилки без змін не повторюйте.

## Посилання

- [Надсилання листів на Java](https://docs.allymailer.com.ua/ua/send-with-java) та інші [посібники з інтеграції](https://docs.allymailer.com.ua/ua/integrations/)
- [Довідник Web API](https://docs.allymailer.com.ua/ua/web-api-ref)
- [llms.txt](https://docs.allymailer.com.ua/llms.txt) для AI-асистентів
- [Підтримка](https://cp.allymailer.com/ua/support)

## Для розробників пакета

Див. [AGENTS.md](AGENTS.md): як цей пакет генерується з OpenAPI-специфікації, збирається, перевіряється та випускається.
