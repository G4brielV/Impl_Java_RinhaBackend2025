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

## 📜 Histórico de Melhorias
---

### Versão 1: A Base (`branch: standard`)

* **Tecnologias:** `☕ JVM Padrão`, `🧵 Threads de Plataforma`, `🐌 Processamento Sequencial com while(true)`.
* **Objetivo:** Criar uma base funcional para entender os desafios do problema e validar a lógica de negócio principal.
* **Resultado:** A aplicação funcionava corretamente, mas tinha `total_transactions_amount` baixo e um alto `balance_inconsistency_amount`.

---

### Versão 2: GraalVM

* **Tecnologias:** `🚀 Imagem Nativa com GraalVM`, `🧵 Threads Virtuais (via config padrão do Spring)`, `🐌 Ainda com Processamento Sequencial`.
* **Objetivo:** Reduzir o consumo de memória e CPU. A JVM padrão consumia muitos recursos, dificultando o equilíbrio com o PostgreSQL dentro dos limites do desafio.
* **Resultado:** O uso de recursos melhorou significativamente. No entanto, o modelo de um *worker* sequencial limitava drasticamente a quantidade de transações processadas, impedindo um aumento significativo no volume de transações.

---

### Versão 3: Concorrência

Inspirada em soluções de alta performance da comunidade, principalmente no [vídeo](https://www.youtube.com/watch?v=nfgHKkxJUP4) e [repo](https://github.com/matheuspieropan/rinhabackend2025/tree/redis)

* **Tecnologias:** `⚡ Concorrência Massiva com Threads Virtuais (ExecutorService)`, `⏰ Migração de while(true) para @Scheduled`.
* **Objetivo:** Eliminar o gargalo do processamento sequencial. A meta era processar dezenas de pagamentos em paralelo para aumentar a vazão (throughput).
* **Resultado:** A performance explodiu, processando um volume muito maior de transações. Porém, essa nova velocidade expôs um novo e severo gargalo: o **banco de dados**. O PostgreSQL ficou sobrecarregado com milhares de `INSERTs` individuais, causando contenção, timeouts na API de resumo e **grandes inconsistências de dados**.

---

### Versão 4: Banco de Dados (`branch: main`)

* **Tecnologias:** `📦 Inserts em Lote (Batch Inserts)`, `⚙️ Otimizações de JPA/Hibernate`.
* **Objetivo:** Aliviar a pressão sobre o PostgreSQL, tornando as operações de escrita muito mais eficientes para eliminar as inconsistências e os timeouts.
* **Resultado:** Ao agrupar os `INSERTs` em lotes, a contenção no banco de dados melhorou, as **inconsistências foram zeradas** e o `total_transactions_amount` atingiu um valor muito bom.

## 🧩 Arquitetura
- nginx: Gateway de entrada das requisições.
- api1 e api2: Réplicas da API Java.
- postgres: Banco de dados relacional.
- payment-processor: Processador externo (Mocked na rinha).

## 🚦 Recusos 

| Serviço  | CPU | Memória |
| -------- | --- | ------- |
| nginx    | 0.2 | 20MB    |
| api1     | 0.5 | 105MB   |
| api2     | 0.5 | 105MB   |
| postgres | 0.3 | 120MB   |

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
