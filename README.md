# Bank Transfer System

Um sistema de transferências bancárias distribuído e de alta disponibilidade, construído para demonstrar padrões arquiteturais avançados na comunicação entre microsserviços. 

O projeto simula a transferência de dinheiro entre contas bancárias utilizando **dois microsserviços autônomos** que se comunicam exclusivamente de forma assíncrona através de eventos no Apache Kafka, sem nenhuma chamada HTTP síncrona entre eles.

---

## 🚀 Tecnologias e Stack

- **Java 21**
- **Spring Boot 3.x**
- **Apache Kafka (KRaft)** para mensageria assíncrona
- **PostgreSQL** (um banco independente para cada serviço)
- **Flyway** para migrations de banco de dados
- **Maven** para build (Multi-module project)
- **HTML/CSS/JS Vanilla** + **SSE (Server-Sent Events)** para o Dashboard visual de tempo real.

---

## 🏗️ Arquitetura e Padrões de Projeto (Design Patterns)

O projeto foi construído seguindo estritamente as diretrizes de **Arquitetura Hexagonal (Ports and Adapters)** para garantir um core de domínio puro, isolado das complexidades de banco de dados e mensageria.

Além disso, implementamos padrões "nível bancário" para resiliência e consistência:

1. **Saga Pattern (Orquestração)**:
   - Uma transferência bancária envolve modificação de saldos entre contas de forma distribuída.
   - O `transfer-service` atua como o **Orquestrador da Saga**, controlando a máquina de estados (`REQUESTED` → `DEBIT_RESERVED` → `COMPLETED`).
   - Caso o débito ocorra mas o crédito falhe (ex: conta de destino inexistente ou inativa), o fluxo compensatório é acionado: estado `COMPENSATING`, publicação de `debit-reversal-requested`, execução do estorno (`ReverseDebit`) pelo `account-service`, emissão de `debit-reversed` e finalização da transferência como `CANCELLED`.

2. **Outbox Pattern**:
   - Para evitar inconsistências onde a transação do banco comita mas o Kafka falha ao publicar (Two-Phase Commit problem).
   - No `account-service`, eventos de domínio são salvos na tabela `outbox_events` na **mesma transação relacional** em que o saldo é alterado.
   - Um scheduler (`OutboxPublisherScheduler`) lê a tabela periodicamente e despacha os eventos para os tópicos do Kafka de forma segura.

3. **Event Sourcing & Audit Trail (Ledger)**:
   - Todas as operações financeiras (`DEBIT`, `CREDIT`, `DEBIT_REVERSAL`) geram registros imutáveis na tabela `account_events`.
   - Permite auditoria completa e reconstrução de histórico financeiro de cada conta.

4. **Idempotência & Optimistic Locking**:
   - Mensagens duplicadas no Kafka são tratadas de forma idempotente:
     - No `account-service`, por meio da tabela `processed_events`.
     - No `transfer-service`, por checagem de estado atual da transferência (`state machine idempotency`).
   - Modificações concorrentes de saldo utilizam bloqueio otimista (`@Version`).

5. **Dead Letter Queues (DLQ)**:
   - Mensagens com falha persistente de processamento são encaminhadas para tópicos `.DLT`.
   - Endpoints REST (`POST /dlq/reprocess/{topic}`) permitem reprocessamento controlado de mensagens não entregues.

6. **Real-Time Observability via SSE (Server-Sent Events)**:
   - Ambos os serviços expõem streams SSE (`/events/stream`) emitindo eventos publicados e processados com payload JSON formatado em tempo real no Dashboard.

---

## 🧩 Estrutura do Projeto

O projeto é dividido em três módulos:

- **`/account-service`** (Porta `8080`):
  - Responsável pela integridade das contas, saldos e histórico (Ledger).
  - Executa as operações atômicas: `ReserveDebit`, `ApplyCredit` e `ReverseDebit`.
  - Publica eventos via Outbox Pattern (`debit-reserved`, `debit-failed`, `credit-applied`, `credit-failed`, `debit-reversed`).
- **`/transfer-service`** (Porta `8081`):
  - Orquestrador de Sagas e máquina de estados das transferências.
  - Processa eventos de débito (`ProcessDebitEventsService`) e eventos de crédito (`ProcessCreditEventsService`).
  - Dispara comandos de crédito (`credit-requested`) e estorno (`debit-reversal-requested`).
- **`/frontend`**:
  - Dashboard web moderno (Vanilla HTML/CSS/JS) com visualização em tempo real das contas, saldos, máquinas de estados das transferências e streaming de eventos Kafka com payload JSON expansível via SSE.

---

## ⚙️ Como Rodar o Projeto (Localmente)

### 1. Subir a Infraestrutura (Postgres + Kafka)
Na raiz do projeto:
```bash
docker-compose up -d
```
*Inicia o Apache Kafka (porta `9092`), Kafka UI (porta `8090`), Account DB (porta `5432`) e Transfer DB (porta `5433`).*

### 2. Compilar os Microsserviços
```bash
mvn clean compile
```

### 3. Iniciar as Aplicações
Execute os dois serviços Spring Boot em terminais separados:

- **`account-service`**: `mvn spring-boot:run -pl account-service` (porta `8080`)
- **`transfer-service`**: `mvn spring-boot:run -pl transfer-service` (porta `8081`)

### 4. Abrir o Dashboard Visual
Abra o arquivo `frontend/index.html` no seu navegador (ou via Live Server).

---

## 📡 API Endpoints

### Transfer Service (porta 8081)
- `POST /transfers` - Inicia uma nova transferência (Saga)
  ```json
  {
    "originAccountId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "destinationAccountId": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
    "amount": 50.00
  }
  ```
- `GET /transfers` - Lista todas as transferências e seus estados atuais.
- `GET /events/stream` - Stream SSE em tempo real de eventos emitidos pelo orchestrator (`credit-requested`, `debit-reversal-requested`).
- `POST /dlq/reprocess/{topic}` - Drena a DLQ de um tópico e tenta reprocessar.

### Account Service (porta 8080)
- `GET /accounts` - Lista as contas e saldos atuais.
- `GET /accounts/{id}/events` - Lista o histórico financeiro (Ledger / Event Sourcing) de uma conta.
- `GET /events/stream` - Stream SSE em tempo real dos eventos publicados pelo Outbox.
- `POST /dlq/reprocess/{topic}` - Drena a DLQ de um tópico e tenta reprocessar.
