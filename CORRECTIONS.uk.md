# CORRECTIONS.uk.md

**Репозиторій:** FHIR-CareCommunication-demo-Java  
**URL:** https://github.com/medcomdk/FHIR-CareCommunication-demo-Java  
**Аналізована гілка:** main (коміт 0a5a136c3751271ed9d0e87905f59af9ed3a5710)  
**Цільовий форк:** https://github.com/admeduardovieira/FHIR-CareCommunication-demo-Java  
**Дата:** 2026-11-08  
**Агент:** Kilo (REA 6.7.0), ідентичність Git admeduardovieira / admeduardovieira@users.noreply.github.com  

---

## Предложені виправлення та статус

| ID | Назва | Severity | Статус | Локація | Ризик | Пропонований PR |
|----|-------|----------|--------|---------|-------|-----------------|
| REA-012 | Перевірка null розширення перед .getValue() | P1 | Пропозиція (ще не реалізовано) | `receiver/.../ReceiverApplication.java:90` | Низький | `fix/rea-012-null-check-extension` |
| REA-013 | `instanceof` перед приведенням `(Practitioner)` | P1 | Пропозиція (ще не реалізовано) | `receiver/.../ReceiverApplication.java:94` | Низький | `fix/rea-013-instanceof-before-cast` |
| REA-014 | `isPresent()` перед `.get()` в парсері | P2 | Пропозиція (ще не реалізовано) | `receiver/.../ResourceParser.java:16` | Низький | `fix/rea-014-is-present-before-get` |

---

## Деталі по кожному виправленню

### REA-012 — Перевірка null розширення

**Проблема:** `ReceiverApplication.java:90` отримує доступ до `.getValue()` на результаті `getExtensionByUrl()`, який може бути null.

**Довід:** `ReceiverApplication.java:90` — `var valueReference = payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue();`

**Причина:** виклик методу на потенційно null результаті.

**Пропонована виправка:** перевірити null розширення; якщо відсутнє, пропустити або обробити належним чином.

**Файли:** `receiver/src/main/java/com/trifork/receiver/ReceiverApplication.java`

**Тест валідації:** запустити існуючі тести та додати тест парсингу повідомлення без розширення автора.

**Ризик:** низький.

**Польза:** уникнути NPE при парсингу.

**Пріоритет:** негайно.

---

### REA-013 — `instanceof` перед приведенням

**Проблема:** `ReceiverApplication.java:94` робить пряме приведення `(Practitioner)` без `instanceof`.

**Довід:** `ReceiverApplication.java:94` — `var practitioner = (Practitioner) ((Reference) valueReference).getResource();`

**Причина:** неявне довіряння типу ресурсу.

**Пропонована виправка:** перевірити `valueReference.getResource() instanceof Practitioner` перед приведенням.

**Файли:** `receiver/src/main/java/com/trifork/receiver/ReceiverApplication.java`

**Тест валідації:** запустити існуючі тести та додати тест з ресурсом іншого типу.

**Ризик:** низький.

**Польза:** уникнути ClassCastException.

**Пріоритет:** негайно.

---

### REA-014 — `isPresent()` перед `.get()`

**Проблема:** `ResourceParser.java:16` викликає `.get()` на Optional, який може бути порожнім.

**Довід:** `ResourceParser.java:14-16` — `.collect(singleElement()).get()`

**Причина:** використання `.get()` без перевірки `isPresent()`.

**Пропонована виправка:** перевірити `isPresent()` або використати `orElseThrow` з описуючим повідомленням.

**Файли:** `receiver/src/main/java/com/trifork/receiver/ResourceParser.java`

**Тест валідації:** запустити існуючі тести та додати тест з bundle без ресурсу даного типу.

**Ризик:** низький.

**Польза:** уникнути NoSuchElementException.

**Пріоритет:** негайно.

---

## Не виправлені проблеми та залишкові ризики

- Всі виправлення мають статус "Пропозиція" на цьому етапі аудиту (Фаза 1 завершена). Жодної зміни не застосовано до вихідного коду ще.
- Обмеження: Maven не встановлено; не вдалося запустити `mvn test` для динамічної валідації виправлень.
- Залишкові ризики: 3 проблеми вище все ще присутні в коді.

---

## Записи звітів

- База PT: `REVERSE_ENGINEERING_REPORT.md` та `CORRECTIONS.md` (це файл).
- Переклади: `REVERSE_ENGINEERING_REPORT.en.md`, `REVERSE_ENGINEERING_REPORT.uk.md`, `CORRECTIONS.en.md`, `CORRECTIONS.uk.md`.
- Всі в корені робочого клону / області аудиту, поруч з базовими звітами portugalsкою.

**Примітка перекладу (uk):** Технічний переклад з допомогою автоматики; рекомендується перегляд носієм мови перед використанням як нормативного довідника. Перегляду носієм не отримано.
