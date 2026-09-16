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

1. **Saga Pattern (Coreografia)**:
   - Uma transferência bancária envolve mexer em saldos de origens diferentes (e microsserviços diferentes).
   - O `transfer-service` atua como o orquestrador de estados (`REQUESTED`, `DEBIT_RESERVED`, `COMPLETED`).
   - Se um débito ocorre mas o crédito falha (ex: conta destino inválida), a Saga compensatória é ativada (`COMPENSATING`) e um evento de estorno (`DebitReversed`) é emitido.

2. **Outbox Pattern**:
   - Para evitar inconsistências onde o banco comita mas o Kafka falha ao enviar o evento (Two-Phase Commit problem).
   - Eventos são salvos na tabela `outbox_events` do banco relacional na **mesma transação** em que o saldo é alterado.
   - Um `@Scheduled` em background lê essa tabela e despacha com segurança para o Kafka.

3. **Event Sourcing & Audit Trail**:
   - Todo depósito, saque ou estorno no `account-service` gera um registro imutável na tabela `account_events` antes de atualizar o saldo final.
   - Permite reconstruir o histórico financeiro completo (Ledger).

4. **Idempotência & Optimistic Locking**:
   - Mensagens duplicadas no Kafka são ignoradas graças a tabela `processed_events`.
   - Modificações concorrentes no saldo são prevenidas via trava otimista do JPA (`@Version`).

5. **Dead Letter Queues (DLQ)**:
   - Mensagens "envenenadas" que não puderam ser processadas após N tentativas caem em um tópico especial `.DLT`.
   - APIs REST manuais (`/dlq/reprocess/{topic}`) permitem corrigir a infraestrutura e reprocessar os eventos.

---

## 🧩 Estrutura do Projeto

O projeto é dividido em três blocos principais:

- **`/account-service`**: O verdadeiro dono do dinheiro. Não sabe o que é uma transferência, apenas atende comandos de `ReserveDebit`, `ApplyCredit` e `ReverseDebit`. Possui o banco `account_service`.
- **`/transfer-service`**: O maestro das Sagas de transferência. Não lida com saldos, apenas comanda os passos e reage aos eventos de sucesso ou falha. Possui o banco `transfer_service`.
- **`/frontend`**: Um WebApp visual em JavaScript puro que consome os dados e exibe a Saga (eventos e saldos mudando) animada em tempo real via Server-Sent Events (SSE).

> Para detalhes mais profundos sobre a máquina de estados e o fluxo de mensagens, consulte o nosso **[Documento de Arquitetura (architecture.md)](architecture.md)**.

---

## ⚙️ Como Rodar o Projeto (Localmente)

### 1. Subir a Infraestrutura (Postgres + Kafka)
Na raiz do projeto, inicie os containers base do Docker:
```bash
docker-compose up -d
```
*Isso vai iniciar o Apache Kafka (porta 9092), Kafka UI (porta 8090), Account DB (porta 5432) e Transfer DB (porta 5433).*

### 2. Compilar os Microsserviços
Na raiz do projeto, execute o build global:
```bash
mvn clean compile
```

### 3. Iniciar as Aplicações
Você precisará iniciar os dois serviços Spring Boot separadamente (via IDE ou linha de comando). 
Por padrão, o Flyway criará as tabelas do banco e fará a carga inicial de algumas contas fake (Alice, Bob, etc).

- Inicie o **`account-service`**: `mvn spring-boot:run -pl account-service` (rodará na porta `8080`)
- Inicie o **`transfer-service`**: `mvn spring-boot:run -pl transfer-service` (rodará na porta `8081`)

### 4. Abrir o Dashboard Visual em Tempo Real
Abra o arquivo `frontend/index.html` diretamente no seu navegador.
O Dashboard fará polling dos microsserviços e se conectará à stream SSE para mostrar bolinhas voando entre as colunas a cada pulso de evento do Kafka.

---

## 📡 API Endpoints

### Transfer Service (porta 8081)
- `POST /transfers` - Inicia uma nova transferência / saga
  ```json
  {
    "originAccountId": "uuid-da-conta-origem",
    "destinationAccountId": "uuid-da-conta-destino",
    "amount": 150.00
  }
  ```
- `GET /transfers` - Lista as transferências e seus status atuais.
- `POST /dlq/reprocess/{topic}` - Drena a DLQ de um tópico e tenta processar as falhas novamente.

### Account Service (porta 8080)
- `GET /accounts` - Lista as contas e os saldos atuais.
- `GET /accounts/{id}/events` - Lista a auditoria/ledger histórico da conta.
- `GET /events/stream` - Endpoint SSE contínuo de eventos do outbox para o Dashboard.
- `POST /dlq/reprocess/{topic}` - Drena a DLQ e tenta reprocessar.
