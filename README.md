# AllyMailer Spring Boot

[![Spring Boot](https://img.shields.io/maven-central/v/ua.com.allymailer/allymailer-spring-boot-starter)](https://central.sonatype.com/artifact/ua.com.allymailer/allymailer-spring-boot-starter) [![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Інтеграція [Java SDK AllyMailer](https://github.com/allymailer/allymailer-java-sdk) (``) з Spring Boot: клієнт API в application context (auto-configuration), ендпоінт вебхуків із перевіркою підпису, транспорт JavaMailSender, що надсилає листи через `email/send`.

[Довідник API](https://docs.allymailer.com.ua/ua/web-api-ref) · Документація для LLM: [llms.txt](https://docs.allymailer.com.ua/llms.txt)

## Встановлення

```bash
# pom.xml
<dependency>
    <groupId>ua.com.allymailer</groupId>
    <artifactId>allymailer-spring-boot-starter</artifactId>
    <version>2.1.2</version>
</dependency>
```

Вимоги: Java 17+, Spring Boot 3.

## Налаштування

Отримайте API-ключ у [панелі керування](https://cp.allymailer.com) і зберігайте його у змінній оточення, а не в коді:

```bash
export ALLYMAILER_API_KEY="your-api-key"
```

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

Домену ще немає? Надсилайте з [тестового sandbox-домену](#без-свого-домену-тестовий-sandbox-домен).

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

> **Відповідайте 200 протягом 3 секунд**, а події обробляйте у фоні (через чергу): інакше AllyMailer повторює запит кожні 10 хвилин протягом доби (зростає `retry_count`), а обробник, на якому за добу не пройшов жоден запит хоча б для 10 подій, вимикається. Обробник має витримувати **щонайменше 5 паралельних запитів до 100 подій у кожному**; повтор надсилає ті самі події ще раз — прибирайте дублікати.

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

### Надсилання через API

Окрім JavaMailSender, листи можна надсилати напряму через клієнт API. Клієнт викликає [email/send](https://docs.allymailer.com.ua/ua/web-api-ref#email-send) з усіма його параметрами: шаблони, підстановки, метадані, трекінг. Поля запиту та інші приклади — у [документації Java SDK](https://github.com/allymailer/allymailer-java-sdk/blob/HEAD/docs/Email.md#send).

```java
import java.util.List;
import org.springframework.stereotype.Service;
import ua.com.allymailer.AllyMailerClient;
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

    public void send() {
        var response = client.email().send(new EmailSendRequest().setMessage(new MessageObject()
            .setRecipients(List.of(MessageObjectRecipientsItem.fromAddress("Олексій <alex@example.com>")))
            .setFromEmail("shop@example.com")
            .setFromName("Магазин")
            .setSubject("Ваше замовлення")
            .setBody(BodyObject.html("<p>Вітаємо, {{to_name}}! Ваше замовлення надіслано.</p>"))));
        System.out.println(response.getJobId());
    }
}
```

## Без свого домену: тестовий sandbox-домен

Поки ваш домен не підтверджено, листи можна надсилати із **sandbox-домену** — тестового домену, який є в кожного облікового запису: він уже підтверджений, DKIM і SPF налаштовані. Листи з нього доставляються лише на підтверджені вами адреси (посилання для підтвердження приходить листом і діє 48 годин), до 100 на день. Через 31 день після створення sandbox-домен видаляється, але його можна отримати знову: наступний виклик `domain/get-sandbox-domain` створить новий (з іншою назвою, тому беріть її з відповіді, а не прописуйте в коді).

У прикладах — клієнт API з пакета (див. налаштування вище).

```java
import java.util.List;
import org.springframework.stereotype.Service;
import ua.com.allymailer.AllyMailerClient;
import ua.com.allymailer.api.confirmedemail.model.ConfirmedEmailRequest;
import ua.com.allymailer.api.email.model.EmailSendRequest;
import ua.com.allymailer.api.email.model.MessageObject;
import ua.com.allymailer.api.email.model.MessageObjectRecipientsItem;
import ua.com.allymailer.common.model.BodyObject;

@Service
public class SandboxService {
    private final AllyMailerClient client;

    public SandboxService(AllyMailerClient client) {
        this.client = client;
    }

    public void send() {
        // 1. Ваш sandbox-домен: створюється під час першого виклику, далі повертається той самий
        var domain = client.domain().getSandboxDomain().getDomain();

        // 2. Підтвердіть свою скриньку: AllyMailer надішле посилання, перейдіть за ним протягом 48 годин
        client.confirmedEmail().create(new ConfirmedEmailRequest().setEmail("you@example.com"));

        // 3. Надішліть із будь-якої адреси sandbox-домену на підтверджену адресу
        client.email().send(new EmailSendRequest().setMessage(new MessageObject()
            .setRecipients(List.of(MessageObjectRecipientsItem.fromAddress("you@example.com")))
            .setFromEmail("noreply@" + domain)
            .setSubject("Привіт від AllyMailer")
            .setBody(BodyObject.html("<p>Працює!</p>"))));
    }
}
```

<details>
<summary>Обмеження sandbox</summary>

- Надсилання лише на підтверджені адреси: до 10 на обліковий запис.
- До 100 листів на день.
- Sandbox-домен спливає через 31 день; знову викличте `domain/get-sandbox-domain`, щоб отримати новий.
- Працює лише з API-ключем облікового запису, не з ключем проєкту.
- Не показується в `domain/list`; інші методи `domain/*` до нього не застосовуються.

Sandbox — для розробки та тестування. Не використовуйте sandbox-домен як відправника на проді.

</details>

<details>
<summary>Те саме через curl</summary>

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

</details>

## Запуск у продакшен

**1. Підтвердіть домен надсилання.** `domain/get-dns-records` повертає запис підтвердження, DKIM-ключ і адресу для DMARC; додайте записи у свого DNS-провайдера, потім `domain/validate-verification-record` і `domain/validate-dkim`.

| Тип | Хост | Значення | Звідки взяти |
|---|---|---|---|
| TXT | `@` | `allymailer-validate-hash=…` | поле `verification-record` відповіді, як є |
| TXT | `<селектор>._domainkey` (селектор — на сторінці домену в панелі керування, наприклад `us`) | `v=DKIM1; k=rsa; p=<dkim>` | поле `dkim` відповіді після `v=DKIM1; k=rsa; p=` |
| TXT | `@` | `v=spf1 include:spf.allymailer.com ~all` | у відповіді немає; якщо SPF-запис уже є, додайте include до нього |
| CNAME | `_dmarc` | `<домен>.dmarc.…` | поле `dmarc` відповіді (DMARC-політику публікує AllyMailer); якщо в домену вже є свій TXT-запис DMARC, залиште його |

**2. Надійне надсилання.**

| | |
|---|---|
| Повтори | `429` і `5xx` з backoff і тим самим `idempotence_key`; інші помилки виправляйте, а не повторюйте |
| Ліміти | 500 одержувачів і 10 МБ на запит разом із вкладеннями: у base64 файл стає приблизно на третину більшим |
| Тіло | завжди `html` або `plaintext` (або із шаблону) |
| Статус доставки | вебхук, а не опитування |
| Черга | надсилайте з фонового завдання, а не всередині веб-запиту |
| Налаштування | `from_email` з конфігурації: перехід із sandbox на продакшен без змін у коді |
| Типові налаштування | не прибирайте блок відписки та трекінг; змінити їх можна через підтримку AllyMailer |

## Допомога

- [Java SDK](https://github.com/allymailer/allymailer-java-sdk)
- Для ШІ-агентів: [llms.txt](https://docs.allymailer.com.ua/llms.txt), скіли: `npx skills add https://github.com/allymailer/allymailer-skills.git`
- [Підтримка](https://cp.allymailer.com/ua/support) · [Задачі](https://github.com/allymailer/allymailer-spring/issues)
- Розробникам: [AGENTS.md](AGENTS.md) — пакет генерується з OpenAPI-специфікації.
