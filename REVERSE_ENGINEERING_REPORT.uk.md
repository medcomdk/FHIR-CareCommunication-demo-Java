# REVERSE_ENGINEERING_REPORT.uk.md

**Репозиторій:** FHIR-CareCommunication-demo-Java  
**URL:** https://github.com/medcomdk/FHIR-CareCommunication-demo-Java  
**Аналізована гілка:** main (коміт 0a5a136c3751271ed9d0e87905f59af9ed3a5710)  
**Цільовий форк:** https://github.com/admeduardovieira/FHIR-CareCommunication-demo-Java  
**Дата аудиту:** 2026-11-08  
**Середовище:** Windows, PowerShell 5.1, Git 2.49.0, Java 17.0.20 (Maven не встановлено)  
**Агент:** Kilo (REA 6.7.0), ідентичність Git admeduardovieira / admeduardovieira@users.noreply.github.com  

> Примітка: переклад технічний, допомога автоматично; рекомендується перегляд носієм мови перед використанням як нормативного довідника. Перегляду носієм не отримано.

---

## A. Загальний діагноз

### Виявлена архітектура
FHIR-CareCommunication-demo-Java — це демо-проєкт на Java/Spring Boot, що демонструє використання офіційних бібліотек HL7 FHIR (HAPI FHIR) для парсингу та серіалізації повідомлень Care Communication згідно з implementation guide MedCom. Не має на меті бути повною реалізацією, а serve як орієнтовний зразок.

Це багатомодульний проєкт Maven (Spring Boot 2.6.3, Java 11):
- `common` — бібліотека валідації повідомлень Care Communication FHIR (Validator.java)
- `sender` — консольний додаток для створення повідомлення Care Communication FHIR (ResourceFactory, SenderApplication)
- `receiver` — консольний додаток для парсингу повідомлення Care Communication FHIR (ResourceParser, ReceiverApplication)
- `src` — дода тесту/демо (Demo1Application, Demo1ApplicationTests)

Залежності FHIR: HAPI FHIR 5.6.2 (hapi-fhir-validation, hapi-fhir-structures-r4, hapi-fhir-validation-resources-r4).

Точки входу: `SenderApplication` (створює та серіалізує), `ReceiverApplication` (парсить та друкує), `ResourceFactory` (будує ресурси FHIR), `Validator` (будує валідатор з NpmPackageValidationSupport), `ResourceParser` (витягує ресурси з bundle).

### Технології та залежності
- Java 11
- Spring Boot 2.6.3 (starter, starter-test)
- HAPI FHIR 5.6.2 (validation, structures-r4, validation-resources-r4)
- JUnit 5 (Spring Boot test)

### Стан підтримки
Коротка історія. Останній коміт `0a5a136` ("Added more documentation"). Демо-проєкт з 3 модулями + модуль тестів. Детальний README документує парсинг, серіалізацію та валідацію FHIR. Єдиний існуючий тест — `contextLoads` (порожній).

### Існуючі механізми тестування
- Один тест `Demo1ApplicationTests.contextLoads()` (порожній, @SpringBootTest).
- Немає тестів валідації/парсингу/серіалізації повідомлень Care Communication.
- Немає CI/CD (відсутність `.github/workflows`).

### Обмеження дослідження
- Maven не встановлено (`mvn` не розпізнається); неможливо запустити `mvn test` або `mvn clean install`.
- Доступно Java 17, але проєкт вимагає Java 11; не вдалося скомпілювати/запустити тести.
- Тести засновані на статичному читанні коду .java.
- GitHub Issues/PR не переглядалися крім вмісту клону.

---

## B. Виявлені проблеми

| ID | Назва | Severity | Статус | Локація |
|----|-------|----------|--------|---------|
| REA-012 | NPE при доступі до `.getValue()` розширення, яке може бути null | P1 | ПІДТВЕРДЖЕНО | `receiver/.../ReceiverApplication.java:90` |
| REA-013 | ClassCastException від прямого приведення `(Practitioner)` без instanceof | P1 | ПІДТВЕРДЖЕНО | `receiver/.../ReceiverApplication.java:94` |
| REA-014 | NoSuchElementException від `.get()` на Optional.empty() | P2 | ПІДТВЕРДЖЕНО | `receiver/.../ResourceParser.java:16` |

### Деталі

**REA-012 — NPE в `getExtensionByUrl().getValue()` (ПІДТВЕРДЖЕНО)**
- Проблема: `ReceiverApplication.java:90` викликає `payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue()`. Якщо розширення автора відсутнє, `getExtensionByUrl` повертає null, а `.getValue()` викидає NullPointerException.
- Довід: `ReceiverApplication.java:90` — `var valueReference = payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue();`
- Причина: виклик методу на потенційно null результаті без перевірки.
- Наслідок: receiver може вийти з NPE при обробці повідомлення без розширення автора, ламаючи операцію парсингу.
- Пропонована виправка: перевірити null розширення перед викликом `.getValue()`.

**REA-013 — ClassCastException від прямого приведення (ПІДТВЕРДЖЕНО)**
- Проблема: `ReceiverApplication.java:94` робить пряме приведення `(Practitioner) ((Reference) valueReference).getResource()` без перевірки `instanceof`. Якщо посиланочений ресурс не є Practitioner, виникає ClassCastException.
- Довід: `ReceiverApplication.java:94` — `var practitioner = (Practitioner) ((Reference) valueReference).getResource();`
- Причина: неявне довіряння тому, що тип ресурсу — Practitioner, без перевірки типу у часі виконання.
- Наслідок: receiver може вийти з ClassCastException, якщо тип ресурсу відрізняється від очікуваного.
- Пропонована виправка: перевірити `instanceof Practitioner` перед приведенням.

**REA-014 — NoSuchElementException від `.get()` (ПІДТВЕРДЖЕНО)**
- Проблема: `ResourceParser.java:16` викликає `.get()` на результаті `singleElement()`, який повертає `Optional.empty()`, коли список не містить рівно 1 елемент. `.get()` на Optional.empty() викидає NoSuchElementException.
- Довід: `ResourceParser.java:14-16` — `.collect(singleElement()).get()`
- Причина: використання `.get()` без перевірки `isPresent()`.
- Наслідок: парсинг виходить з винятком, якщо немає рівно одного ресурсу кожного типу у bundle.
- Пропонована виправка: перевірити `isPresent()` або використати `orElseThrow` з описуючим повідомленням.

---

## C. Повторювані проблеми

Повторювані проблеми, задокументовані публічно, не перевірялися (обмеження обсягу аналізу вмістом клону). Проєкт — демо з короткою історією, без Issue/PR, переглянутих під час цього аудиту.

---

## D. Не задокументовані помилки

Помилки, виявлені аудитом, які не з'являються в документації проєкту:
- REA-012, REA-013, REA-014 (всі вище).

---

## E. Продуктивність

Критичних можливостей продуктивності не виявлено при статичній інспекції. Проєкт — консольний демо. Обмеження: без Maven неможливо запустити бенчмарки.

---

## F. Технічні покращення

| Область | Покращення | Об҆умовлення |
|-------|------------|-------------|
| Надійність | Додати перевірку null розширення (REA-012) | Уникнути NPE при парсингу |
| Надійність | Додати `instanceof` перед приведенням (REA-013) | Уникнути ClassCastException |
| Надійність | Перевірити `isPresent()` перед `.get()` (REA-014) | Уникнути NoSuchElementException |
| Тести | Додати тести парсингу/валідації повідомлень Care Communication | Покриття основних поведінок (зараз лише порожній contextLoads) |

---

## G. План виконання

Самостійні виправлення, низький ризик, доказані кодом:
1. **REA-012** (P1): `ReceiverApplication.java:90` — перевірка null розширення
2. **REA-013** (P1): `ReceiverApplication.java:94` — `instanceof` перед приведенням
3. **REA-014** (P2): `ResourceParser.java:16` — `isPresent()` перед `.get()`

Рекомендований порядок: REA-012 → REA-013 → REA-014.

---

## H. Очікуваний результат

- Усунення 2 ризиків винятків у часі виконання (NPE, ClassCastException) у receiver.
- Усунення 1 ризику NoSuchElementException у парсері ресурсів.
- Більш надійний код без зміни ліки бізнесу або публічного API.
- Низькоризикові внески, придатні для малих переглядних PR.

---

## Обов'язкові питання завершення

1. **Які критичні помилки були підтверджені?** Жодних P0. Дві помилки P1 (NPE та ClassCastException у receiver) підтверджені інспекцією коду.
2. **Які повторювані проблеми ще не вирішені розробниками?** Жодних не перевірялося публічно (демо-проєкт, коротка історія).
3. **Які важливі помилки не були задокументовані?** NPE на відсутнє розширення, ClassCastException на прямому приведенні, NoSuchElementException на Optional.get().
4. **Які можливості продуктивності були знайдені?** Критичних не виявлено.
5. **Які покращення безпеки та надійності об҆умовлені?** Перевірки null та типу у receiver (надійність).
6. **Які виправлення можна зробити швидко?** REA-012, REA-013, REA-014 (малі, низький ризик, доказані кодом).
7. **Які три внески мають найбільший потенційний технічний сенс?** (1) Перевірка null розширення (уникнути NPE при парсингу), (2) instanceof перед приведенням (уникнути ClassCastException), (3) isPresent перед .get() (уникнути NoSuchElementException).
8. **Які виправлення слід уникнути через відсутність доказів?** Зміни логіки валідації або серіалізації FHIR без виконання тестів (без Maven неможливо валідувати).
9. **Які зміни ви рекомендуєте реалізувати першими?** REA-012, REA-013, REA-014 (малі, низький ризик, доказані кодом).

---

## Аналіз програмних загроз та загроз (статичний)

Виконано для всіх файлів проєкту (.java, .xml, .json, .md, .gitignore), за винятком `.git`.

**Результат: заходів не виявлено статичним аналізом, що було проведено.**

- Немає підозрілих виконуваних сценаріїв поза законним вихідним кодом Java.
- Немає вбудованих ключів, облікових даних, токенів, паролів або рядків з'єднання.
- Немає обфускації, упаковки, постійності, виконання процесів, запису/видалення файлів, збору/ексфільтрації даних або підозрілої зміни конфігурації.
- `SenderApplication.java` записує файл у `java.io.tmpdir` (леґítймна функціональність демо, не підозріла).
- Обмеження: лише статичний аналіз; Maven не встановлено для перевірки транзитивних залежностей. Не заявляється, що проєкт вільний від шкідливого коду.

---

**Примітка:** цей звіт — базова версія українською. Синхронізовані англійська (`.en.md`) та українська (`.uk.md`) версії супроводжують цей документ, зберігаючи ідентифікатори REA-*, важливості P0–P3, шляхи, команди, URL та фрагменти коду без змін.
