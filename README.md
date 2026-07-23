# Transfer of Balances — микросервис загрузки переноса балансов

Микросервис для автоматической и ручной загрузки файлов переноса балансов формата `.045` в PostgreSQL.

## Технологии

- Java 21 (Virtual Threads)
- Spring Boot 3.3
- PostgreSQL + Flyway
- MapStruct, OpenCSV, Resilience4j, AspectJ (AOP)
- TestContainers, Micrometer/Prometheus
- SpringDoc OpenAPI (Swagger)

## Структура файлов

| Файл | Описание |
|------|----------|
| `epbYYYYMMDDHHMMSS.045` | Данные переноса балансов |
| `epbrYYYYMMDDHHMMSS.045` | Контрольный отчёт (имя файла + кол-во записей + контрольная сумма) |

### Формат данных (`epb*.045`)

```
ndog_billing_a;account_a;ndog_billing_b;fio_billing_a;summa;bill_date
```

Пример:
```
1707010487904;74813106;17070104879;Куренкова Светлана Ивановна;-11,2378;01.07.2026
```

### Формат отчёта (`epbr*.045`)

```
имя_файла_данных;количество_записей;контрольная_сумма;
```

Пример:
```
epb20260701090706.045;8;47,6346;
```

Контрольная сумма — арифметическая сумма всех значений поля `summa` из файла данных.

## Сборка

```bash
cd transfer_of_balances
mvn clean package
```

## Запуск

### Локально (PostgreSQL должен быть доступен)

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=transfer_balances
export DB_USER=transfer_user
export DB_PASSWORD=transfer_pass
export TRANSFER_INPUT_DIR=/data/incoming
export TRANSFER_PROCESSED_DIR=/data/processed
export TRANSFER_ERROR_DIR=/data/error

java -jar target/transfer-of-balances-1.0.0-SNAPSHOT.jar
```

### Docker

```bash
docker build -t transfer-of-balances:1.0.0 .
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e TRANSFER_INPUT_DIR=/data/incoming \
  -v /path/to/incoming:/data/incoming \
  transfer-of-balances:1.0.0
```

### Docker Compose (PostgreSQL + сервис)

```bash
docker compose up -d
```

## API

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/v1/transfer/process` | Ручной запуск обработки |
| GET | `/api/v1/transfer/status` | Статус последней загрузки |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/actuator/prometheus` | Метрики Prometheus |

### Пример запроса

```bash
curl -X POST http://localhost:8080/api/v1/transfer/process
curl http://localhost:8080/api/v1/transfer/status
```

## Тестирование

```bash
mvn test
```

Интеграционные тесты используют TestContainers (PostgreSQL).

## Примеры ошибок для тестирования

| Файл | Ошибка |
|------|--------|
| `epb_invalid_checksum.045` | Неверная контрольная сумма (Fail Fast) |
| `epb_invalid_fio.045` | Пустое ФИО |
| `epb_invalid_account.045` | Нечисловой номер счёта |
| `epb_invalid_date.045` | Неверный формат даты |

## Метрики

- `transfer.records.processed` — обработанные записи
- `transfer.errors.total` — количество ошибок
- `transfer.files.processed` — обработанные файлы
- `transfer.processing.duration` — время обработки

## Конфигурация

См. `src/main/resources/application.yml` и `docs/protocol.md`.
