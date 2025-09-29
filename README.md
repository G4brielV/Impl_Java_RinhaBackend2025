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

🚀 Passo a Passo da compilação:
1. Plugin do Spring Boot com Buildpacks:
```pom.xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <name>g4brielv/rinhabackend2025_imp_1:latest</name>
            <builder>paketobuildpacks/builder-jammy-base:latest</builder>
            <env>
                <BP_NATIVE_IMAGE>true</BP_NATIVE_IMAGE>
                <BP_JVM_VERSION>21</BP_JVM_VERSION>
            </env>
        </image>
    </configuration>
    <executions>
        <execution>
            <id>process-aot</id>
            <goals>
                <goal>process-aot</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
2. Definir o Profile:
```pom.xml
<profiles>
    <profile>
        <id>native</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.graalvm.buildtools</groupId>
                    <artifactId>native-maven-plugin</artifactId>
                    ...
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```
3. Abra o terminal de desenvolvedor: É importante usar um terminal que tenha as ferramentas de build C++ configuradas, como o "x64 Native Tools Command Prompt for VS" no Windows.
4. Navegue até a pasta do projeto.
5. Execute o comando de build do Maven:
   ```Bash
   mvn clean
   mvn spring-boot:build-image -Pnative -DskipTests
   ```

---

### Versão 3: Concorrência

Inspirada em soluções de alta performance da comunidade, principalmente no [vídeo](https://www.youtube.com/watch?v=nfgHKkxJUP4) e [repo](https://github.com/matheuspieropan/rinhabackend2025/tree/redis)

* **Tecnologias:** `⚡ Concorrência Massiva com Threads Virtuais (ExecutorService)`, `⏰ Migração de while(true) para @Scheduled`.
* **Objetivo:** Eliminar o gargalo do processamento sequencial. A meta era processar dezenas de pagamentos em paralelo para aumentar a vazão (throughput).
* **Resultado:** A performance explodiu, processando um volume muito maior de transações. Porém, essa nova velocidade expôs um novo e severo gargalo: o **banco de dados**. O PostgreSQL ficou sobrecarregado com milhares de `INSERTs` individuais, causando contenção, timeouts na API de resumo e **grandes inconsistências de dados**.

---

### Versão 4: Banco de Dados 

* **Tecnologias:** `📦 Inserts em Lote (Batch Inserts)`, `⚙️ Otimizações de JPA/Hibernate`.
* **Objetivo:** Aliviar a pressão sobre o PostgreSQL, tornando as operações de escrita muito mais eficientes para eliminar as inconsistências e os timeouts.
* **Resultado:** Ao agrupar os `INSERTs` em lotes, a contenção no banco de dados melhorou, as **inconsistências foram zeradas** e o `total_transactions_amount` atingiu um valor muito bom.

---

### Versão 5: 🌐 Otimização da Rede e do Payload (`branch: main`)
* **Tecnologias:** 📡 HttpClient nativo do Java, 📜 Estratégia "JSON desde a chegada".
* **Objetivo:** Reduzir a latência e a sobrecarga da CPU na comunicação com os processadores de pagamento e no processamento assíncrono.
* **Resultado:** A comunicação de rede se tornou mais eficiente com o HttpClient. A estratégia de converter o payload para JSON uma única vez, no momento da chegada, diminuiu o trabalho repetitivo da CPU no fluxo assíncrono, contribuindo para a estabilidade geral do sistema sob carga.

## 📜 Pós Rinha: A Busca pela Perfeição
###Versão 6: 🚦 A Saga do Semáforo e do COPY no PostgreSQL
* **Tecnologias:** 🚦 Semáforos (ReentrantReadWriteLock), 🚀 COPY do PostgreSQL, 🐘 Tabelas UNLOGGED.
* **Objetivo:** Eliminar as inconsistências persistentes, sincronizando a leitura do summary com a escrita assíncrona, e maximizar a performance de inserção em massa no PostgreSQL.
* **Resultado:** Funcionou, mas ainda existiam gaps onde ocorriam inconsistências e o p(99) continuava sendo um problema.

### Versão 7: 🌐 Modernizando a Rede com WebClient
* **Tecnologias:** 🌐 Spring WebClient, ⚡ Reactor Netty.
* **Objetivo:** Substituir o HttpClient pelo WebClient reativo, buscando maior eficiência e controle sobre o pool de conexões em alta concorrência.
* **Resultado:** A API se tornou tão eficiente em aceitar requisições que criou um "congestionamento monstro", com a fila em memória crescendo mais rápido do que podia ser processada, trazendo as inconsistências de volta e uma latência também muito alta.

### Versão 8: 💨 Migração para o Redis
* **Tecnologias:** 💨 Redis, 🗃️ Sorted Sets, 🔐 Trava com SETNX.
* **Objetivo:** Eliminar completamente o gargalo de I/O de disco, substituindo o PostgreSQL pelo Redis como banco de dados principal para persistência e coordenação.
* **Resultado:** A performance de escrita e leitura melhorou absurdamente. A trava de eleição de líder com SETNX se mostrou muito mais simples e rápida que a trava transacional do PostgreSQL.

### Versão 9: ✨ Worker Reativo e ZCOUNT (`branch: pos-rinha`)
* **Tecnologias:** 👷 Worker reativo (@PostConstruct), 🧠 Retentativa com ScheduledExecutorService, 🔢 Redis ZCOUNT, ⏰ Timestamps Truncados.
* **Objetivo:** Refinar a arquitetura para sua forma final, combinando os aprendizados de todas as etapas para máxima performance e consistência.
* **Resultado:** Sucesso total. A troca do Job por um Worker reativo, a correção da precisão dos timestamps e o uso do ZCOUNT para os summaries zeraram as inconsistências e alcançaram a melhor performance possível.

---

## 🧩 Arquitetura
- nginx: Gateway de entrada das requisições.
- api1 e api2: Réplicas da API Java.
- postgres: Banco de dados relacional.
- payment-processor: Processador externo (Mocked na rinha).

## 🚦 Recusos 

| Serviço  | CPU | Memória |
| -------- | --- | ------- |
| nginx    | 0.3 | 40MB    |
| api1     | 0.5 | 105MB   |
| api2     | 0.5 | 105MB   |
| redis | 0.2 | 100MB   |

## 📁 Organização
- src/: Código-fonte da aplicação Java
- docker-compose.yml: Compose principal do projeto
- docker-compose-rinha.yaml: Compose fornecido pela organização da Rinha
- rinha-test/: Scripts de teste de carga com K6

## ▶️ Como Executar
⚠️ **Pré-requisito:** ter o **Docker** instalado na máquina.
  
1. **Subir o ambiente da Rinha** 
   ```bash
   docker compose -f docker-compose-rinha.yaml up -d
   ```
2. **Subir o projeto** (APIs, banco, nginx)
   ```bash
   docker compose up -d
   ```
## 🧪 Testes de Carga
Acesse a pasta dos scripts de teste:
```bash
cd rinha-test
```
- **Rodar prévias**:
   ```bash
    k6 run rinha.js
   ```
- **Rodar carga final**:
   ```bash
    k6 run rinha-final.js
    ``` 
## 🏁 Resultados

- 📌 **Resultado individual:** [Repositório Oficial](https://github.com/zanfranceschi/rinha-de-backend-2025/tree/main/participantes/g4brielV-java1)  
- 🔎 **Prévia dos Resultados:** [Ver aqui](https://github.com/zanfranceschi/rinha-de-backend-2025/blob/main/PREVIA_RESULTADOS.md)  
- 🏆 **Ranking Final:** [Resultados Finais](https://github.com/zanfranceschi/rinha-de-backend-2025/blob/main/RESULTADOS_FINAIS.md) → **97º lugar**
