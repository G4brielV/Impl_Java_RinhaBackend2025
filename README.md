# 🐓 Rinha de Backend 2025 - Java (Spring Boot)

Este projeto foi desenvolvido para participar da [Rinha de Backend 2025](https://github.com/zanfranceschi/rinha-de-backend-2025), utilizando Java 21 com Spring Boot.

## 📌 Descrição

A aplicação simula um serviço de processamento de pagamentos, composto por dois fluxos principais:

- **Fluxo 1 (sincrono)**: Recebe a requisição via `POST /payments` e enfileira como uma entidade.
- **Fluxo 2 (assíncrono)**: Processa a fila, tentando enviar o pagamento primeiro ao **Payment Processor Default**. Se falhar, tenta o **Payment Processor Fallback**. Após o sucesso, persiste a transação no banco PostgreSQL.

### ✉️ POST `/payments`

```json
{
  "correlationId": "4a7901b8-7d26-4d9d-aa19-4dc1c7cf60b3",
  "amount": 19.90
}
```
- Retorno: HTTP 2XX em caso de sucesso de enfileiramento.

### 📊 GET `/payments-summary`
Consulta o resumo dos pagamentos por período.

Exemplo:

```pgsql
GET /payments-summary?from=2020-07-10T12:34:56.000Z&to=2020-07-10T12:35:56.000Z
```

Resposta:

```json
{
  "default": {
    "totalRequests": 43236,
    "totalAmount": 415542345.98
  },
  "fallback": {
    "totalRequests": 423545,
    "totalAmount": 329347.34
  }
}
```

## ⚙️ Tecnologias Utilizadas
- Java 21
- Spring Boot
- JPA/Hibernate
- Flyway
- PostgreSQL
- Docker
- Nginx
- Maven

## 🧩 Arquitetura
- nginx: Gateway de entrada das requisições.
- api1 e api2: Réplicas da API Java.
- postgres: Banco de dados relacional.
- payment-processor: Processador externo (Mocked na rinha).

## 🚦 Recusos 

| Serviço  | CPU | Memória |
| -------- | --- | ------- |
| nginx    | 0.1 | 10MB    |
| api1     | 0.5 | 150MB   |
| api2     | 0.5 | 150MB   |
| postgres | 0.4 | 40MB    |

## ▶️ Como Executar
⚠️ É necessário ter o Docker instalado na máquina
  
1. Subir o ambiente da Rinha
   ```bash
   docker compose -f docker-compose-rinha.yaml up -d
   ```
2. Subir o projeto (APIs, banco, nginx)
   ```bash
   docker compose up --build -d
   ```
## 🧪 Testes de Carga
```bash
cd rinha-test
k6 run rinha.js
```

## 📁 Organização
- src/: Código-fonte da aplicação Java
- docker-compose.yml: Compose principal do projeto
- docker-compose-rinha.yaml: Compose fornecido pela organização da Rinha
- rinha-test/: Scripts de teste de carga com K6
