# Sistema Bancário — Persistência de Classes (Spring Boot + SQLite)

Projeto desenvolvido para a disciplina de **Design de Software** com o objetivo de implementar a persistência de classes em banco de dados utilizando **Spring Boot**, **Spring Data JPA**, **Hibernate** e **SQLite**, seguindo a arquitetura definida no diagrama de pacotes do grupo.

---

## 1. Visão Geral da Arquitetura: Monólito Modular

Para representar uma arquitetura inspirada em **8 microsserviços** sem exigir que 8 servidores Spring Boot separados rodem simultaneamente em uma única máquina durante a apresentação, o projeto adota o padrão **Monólito Modular (*Modular Monolith*)**.

Cada pacote dentro de `core/src/main/java/com/sistema/bancario/core/` representa um módulo/serviço independente, com **suas próprias entidades (`@Entity`), repositórios (`@Repository`) e regras de negócio (`@Service`)**, persistindo no mesmo arquivo SQLite (`sistema_bancario_geral.db`) em tabelas isoladas:

| Pacote (Módulo) | Papel na Arquitetura | Tipo de Serviço | Usa Lote? |
| :--- | :--- | :--- | :--- |
| `pagamento` | Orquestrador de saídas de dinheiro (compras/boletos) | Orquestrador | Não diretamente |
| `transferencia` | Orquestrador de movimentação entre contas | Orquestrador | Não diretamente |
| `saldo` | Único responsável por ler e alterar o saldo das contas | Serviço de Base (Cofre) | Não |
| `lotes` | Acumulador assíncrono para processamento em batch | Serviço de Base (Batch) | — |
| `pix` | Pagamento **e** Transferência instantânea por chave | Especialista | Não (tempo real) |
| `boleto` | Pagamento por código de barras | Especialista | **Sim (`"PENDENTE"`)** |
| `ted` | Transferência imediata por ID da conta | Especialista | Não (tempo real) |
| `doc` | Transferência de até R\$ 4.999,99 | Especialista | **Sim (`"PENDENTE"`)** |

---

## 2. Entendendo o Sistema (Visão Não Técnica — Analogia da Agência Bancária)

Imagine o sistema como uma **agência bancária** com **cadernos de registro**, **arquivistas** e **funcionários especializados**:

### Os Cadernos (Tabelas no Banco de Dados)
- **Caderno de Contas (`Conta`):** guarda a ficha de cada cliente (`id`, número da conta, saldo atual e chave Pix).
- **Cadernos de Comprovantes (`Pix`, `Boleto`, `Ted`, `Doc`):** cada modalidade tem o seu próprio caderno exclusivo para registrar apenas as operações que passaram por ela.
- **Cesto de Lotes (`ItemLote`):** uma caixa onde ficam guardados os comprovantes de **Boleto** e **DOC** marcados como `"PENDENTE"` até o horário do envio em grupo.

### Os Funcionários (Serviços)
1. **O Homem do Cofre (`SaldoService`):** é o **único** funcionário autorizado a abrir o cofre, somar ou subtrair dinheiro das contas e descobrir de quem é uma chave Pix. Nenhum outro serviço mexe no saldo diretamente.
2. **Os Balcões de Atendimento (`PagamentoService` e `TransferenciaService`):**
   - **Balcão de Pagamentos:** atende quando o dinheiro **só sai** da conta do cliente (ex: pagar uma loja no Pix ou pagar um Boleto).
   - **Balcão de Transferências:** atende quando o dinheiro **sai** de uma conta e **entra** em outra conta (ex: transferir por Pix, TED ou DOC).
3. **Os Crachás/Moldes (`FormaDePagamento` e `FormaDeTransferencia`):**
   - São contratos que garantem quem pode trabalhar em qual balcão e obrigam todos a responder aos mesmos comandos (`validar`, `registrar`, e no caso de transferência, `resolverContaDestino`).
   - **Boleto** só usa o crachá `FormaDePagamento` (nunca atende transferência).
   - **TED** e **DOC** só usam o crachá `FormaDeTransferencia` (nunca atendem pagamento).
   - **Pix** usa **os dois crachás**, pois funciona tanto para pagar quanto para transferir.
4. **Os 4 Especialistas (`PixService`, `BoletoService`, `TedService`, `DocService`):**
   - Cada um tem uma placa na mesa (`"PIX"`, `"BOLETO"`, `"TED"`, `"DOC"`). Quando um cliente chega ao balcão dizendo qual modalidade quer usar, o atendente abre uma lista (`Map`), encontra o especialista correspondente pela placa e manda ele validar e registrar a operação.
5. **O Despachante de Lotes (`LoteService`):**
   - Recebe os pedidos de **Boleto** e **DOC** ao longo do dia com status `"PENDENTE"`. Quando acionado (`processarLotePendente()`), envia tudo de uma vez e carimba todos os itens como `"PROCESSADO"`.

---

## 3. Entendendo o Sistema (Visão Técnica — Spring Boot, JPA e Padrão Strategy)

### A. Persistência com Spring Data JPA + Hibernate + SQLite
- **`@Entity` (`Conta`, `Pix`, `Boleto`, `Ted`, `Doc`, `ItemLote`):** mapeia cada classe Java para uma tabela no SQLite. `@Id` e `@GeneratedValue(strategy = GenerationType.IDENTITY)` delegam ao SQLite a geração automática da chave primária (`id`).
- **`@Repository` (`JpaRepository<Entidade, Long>`):** interfaces que fazem o Spring Data JPA gerar automaticamente em tempo de execução os comandos SQL (`save`, `findById`, `findAll`, `count`, `deleteAll`). Também usamos *Derived Queries* como `findByChavePix(String chavePix)` em `ContaRepository` e `findByStatus(String status)` em `ItemLoteRepository`.
- **Dialeto SQLite:** configurado via dependência `org.hibernate.orm:hibernate-community-dialects` no `core/pom.xml` e `spring.jpa.database-platform: org.hibernate.community.dialect.SQLiteDialect` em `core/src/main/resources/application.yaml` com `ddl-auto: update`.

### B. Padrão Strategy + Injeção Automática via `Map` do Spring
Em vez de escrever cadeias de `if / else` dentro dos orquestradores, o projeto aplica o **Padrão Comportamental Strategy**:

1. **Contratos Strategy:**
   - `FormaDePagamento`: define `validar(destino, valor)` e `registrar(idContaOrigem, tipoOperacao, destino, valor)`.
   - `FormaDeTransferencia`: define `validar(destino, valor)`, `resolverContaDestino(idContaDestino, chaveDestino)` e `registrar(idContaOrigem, tipoOperacao, destino, valor)`.
2. **Registro Automático pelo Spring (`@Service("NOME")` + `Map`):**
   - Cada estratégia é anotada com o nome da modalidade: `@Service("PIX")`, `@Service("BOLETO")`, `@Service("TED")` e `@Service("DOC")`.
   - O `PagamentoService` recebe `Map<String, FormaDePagamento> formasDePagamento` no construtor. O Spring detecta todos os beans que implementam `FormaDePagamento` e monta automaticamente o mapa: `{"PIX": PixService, "BOLETO": BoletoService}`.
   - O `TransferenciaService` recebe `Map<String, FormaDeTransferencia> formasDeTransferencia`, montando automaticamente: `{"PIX": PixService, "TED": TedService, "DOC": DocService}`.
   - Quando chega uma requisição, `buscarEstrategia(tipo)` normaliza a string (`tipo.trim().toUpperCase()`) e faz `mapa.get(...)`, lançando `IllegalArgumentException` caso o tipo seja nulo, vazio ou incompatível com aquela operação (ex: tentar `"BOLETO"` como transferência).

### C. Atomicidade (`@Transactional`) e Ordem Segura de Execução
Os métodos `PagamentoService.processarPagamento(...)` e `TransferenciaService.processarTransferencia(...)` são anotados com `@Transactional` e seguem sempre a ordem:
1. **Validar** a estratégia e os parâmetros antes de tocar no banco;
2. **Movimentar o saldo** via `SaldoService` (que também valida `valor != null && valor > 0` e saldo suficiente);
3. **Persistir** o comprovante da operação (e enfileirar no lote, quando aplicável).

Caso qualquer exceção ocorra (chave inválida, limite excedido ou saldo insuficiente), o Spring realiza o **rollback** completo da transação no SQLite.

---

## 4. Fluxo Detalhado de Cada Serviço

Ponto de partida na execução padrão (`SistemaBancarioApplication.demonstracaoInicial`):
Quando a tabela `conta` está vazia (`contaRepository.count() == 0`), a aplicação cria duas contas iniciais:
- **Conta 1 (`id = 1`):** número `"0001-1"`, saldo `R$ 2.000,00`, chave Pix `"joao@email.com"`
- **Conta 2 (`id = 2`):** número `"0002-2"`, saldo `R$ 500,00`, chave Pix `"12345678901"`

---

### 4.1. Fluxo do **PIX** (Pagamento e Transferência — Tempo Real)

#### Caso A: Pagamento via PIX
> **Não técnico:** João vai ao balcão de pagamentos pagar R\$ 150,00 para a chave `"loja@email.com"`. O fiscal do Pix confere se a chave tem formato válido, o homem do cofre tira R\$ 150,00 da conta do João e o fiscal anota o comprovante no caderno do Pix.

**Fluxo técnico (`PagamentoService.processarPagamento(1L, 150.0, "PIX", "loja@email.com")`):**
1. `PagamentoService.buscarEstrategia("PIX")` retorna `PixService` a partir de `formasDePagamento`.
2. `PixService.validar("loja@email.com", 150.0)` confirma que `valor > 0` e que a chave não é vazia e possui `@` (e-mail) ou 11 dígitos numéricos (CPF).
3. `SaldoService.debitarSaldo(1L, 150.0)` busca a Conta 1 no `ContaRepository`, verifica saldo suficiente e atualiza para `R$ 1.850,00`.
4. `PixService.registrar(1L, "PAGAMENTO", "loja@email.com", 150.0)` salva um novo registro na tabela `pix` via `PixRepository`.

#### Caso B: Transferência via PIX
> **Não técnico:** João quer mandar R\$ 200,00 para a chave `"12345678901"`, sem precisar saber o número da ficha da Maria. O fiscal valida o CPF, pergunta ao homem do cofre qual conta é dona dessa chave (`id = 2`), tira R\$ 200,00 do João, coloca R\$ 200,00 na Maria e anota no caderno do Pix.

**Fluxo técnico (`TransferenciaService.processarTransferencia(1L, null, 200.0, "PIX", "12345678901")`):**
1. `TransferenciaService.buscarEstrategia("PIX")` retorna `PixService` a partir de `formasDeTransferencia`.
2. `PixService.validar("12345678901", 200.0)` valida valor `> 0` e os 11 dígitos numéricos.
3. `PixService.resolverContaDestino(null, "12345678901")` chama `SaldoService.buscarIdPorChavePix("12345678901")` (`ContaRepository.findByChavePix`), descobrindo que o destino é `id = 2L`. *(Se um `idContaDestino` também tiver sido informado, valida se corresponde à chave).*
4. `SaldoService.debitarSaldo(1L, 200.0)` e `SaldoService.creditarSaldo(2L, 200.0)` atualizam ambas as contas.
5. `PixService.registrar(1L, "TRANSFERENCIA", "12345678901", 200.0)` salva o comprovante na tabela `pix`.

---

### 4.2. Fluxo do **BOLETO** (Apenas Pagamento — Assíncrono em Lote)

> **Não técnico:** João vai ao balcão de pagamentos pagar um boleto de R\$ 100,00 (`"12345678901234"`). O especialista de Boleto confere o código de barras, o homem do cofre tira R\$ 100,00 da conta do João, o comprovante é salvo no caderno de Boletos e o pedido vai para o cesto de Lotes como `"PENDENTE"`.

**Fluxo técnico (`PagamentoService.processarPagamento(1L, 100.0, "BOLETO", "12345678901234")`):**
1. `PagamentoService.buscarEstrategia("BOLETO")` retorna `BoletoService`.
2. `BoletoService.validar("12345678901234", 100.0)` verifica `valor > 0` e se o código de barras possui ao menos 10 dígitos exclusivamente numéricos.
3. `SaldoService.debitarSaldo(1L, 100.0)` debita o saldo da Conta 1.
4. `BoletoService.registrar(1L, "PAGAMENTO", "12345678901234", 100.0)`:
   - Salva o comprovante na tabela `boleto` via `BoletoRepository`;
   - Chama `LoteService.adicionarAoLote("BOLETO", 1L, "12345678901234", 100.0)`, gravando um `ItemLote` na tabela `item_lote` com `status = "PENDENTE"`.

---

### 4.3. Fluxo do **TED** (Apenas Transferência — Tempo Real)

> **Não técnico:** João vai ao balcão de transferências mandar R\$ 300,00 direto para a Conta 2 via TED. Como TED cai na hora e usa o número da conta destino diretamente, o dinheiro sai da Conta 1, entra imediatamente na Conta 2 e fica registrado no caderno de TEDs, sem passar pelo cesto de lotes.

**Fluxo técnico (`TransferenciaService.processarTransferencia(1L, 2L, 300.0, "TED", null)`):**
1. `TransferenciaService.buscarEstrategia("TED")` retorna `TedService`.
2. `TedService.validar(null, 300.0)` valida que `valor > 0`.
3. `TedService.resolverContaDestino(2L, null)` exige `idContaDestino != null` e devolve `2L`.
4. `SaldoService.debitarSaldo(1L, 300.0)` e `SaldoService.creditarSaldo(2L, 300.0)` efetivam a transferência na hora.
5. `TransferenciaService` identifica que não há `chaveDestino`, define `destinoRegistrado = "2"` e chama `TedService.registrar(1L, "TRANSFERENCIA", "2", 300.0)`, salvando na tabela `ted` via `TedRepository`.

---

### 4.4. Fluxo do **DOC** (Apenas Transferência — Limite R\$ 4.999,99 + Assíncrono em Lote)

> **Não técnico:** João quer transferir R\$ 250,00 para a Conta 2 via DOC. O especialista confere se o valor não passa do teto de R\$ 4.999,99 (se passar, recusa na hora). Estando abaixo do limite, o saldo é movimentado, o registro vai para o caderno de DOCs e a operação entra no cesto de Lotes como `"PENDENTE"`.

**Fluxo técnico (`TransferenciaService.processarTransferencia(1L, 2L, 250.0, "DOC", null)`):**
1. `TransferenciaService.buscarEstrategia("DOC")` retorna `DocService`.
2. `DocService.validar(null, 250.0)` verifica `valor > 0` e `valor < 5000.0` (valores `>= 5000.0` lançam `IllegalArgumentException`).
3. `DocService.resolverContaDestino(2L, null)` exige `idContaDestino != null` e devolve `2L`.
4. `SaldoService.debitarSaldo(1L, 250.0)` e `SaldoService.creditarSaldo(2L, 250.0)` atualizam as contas.
5. `DocService.registrar(1L, "TRANSFERENCIA", "2", 250.0)`:
   - Salva o comprovante na tabela `doc` via `DocRepository`;
   - Chama `LoteService.adicionarAoLote("DOC", 1L, "2", 250.0)`, gravando um `ItemLote` na tabela `item_lote` com `status = "PENDENTE"`.

---

### 4.5. Fluxo do **LOTE** (`LoteService` — Processamento Batch Assíncrono)

> **Não técnico:** Ao final da rotina, o despachante abre o cesto, pega tudo o que foi acumulado por **Boleto** e **DOC** durante a operação com etiqueta `"PENDENTE"`, processa o lote inteiro de uma só vez e troca todas as etiquetas para `"PROCESSADO"`.

**Fluxo técnico (`LoteService.processarLotePendente()`):**
1. Busca no banco via `ItemLoteRepository.findByStatus("PENDENTE")` todos os itens aguardando compensação (o Boleto e o DOC).
2. Altera o campo `status` de cada `ItemLote` para `"PROCESSADO"`.
3. Persiste tudo de forma transacional (`@Transactional`) via `itemLoteRepository.saveAll(pendentes)` e retorna a quantidade de itens processados.

---

## 5. Estrutura de Tabelas Geradas no SQLite (`sistema_bancario_geral.db`)

| Tabela | Entidade Java | Colunas Principais |
| :--- | :--- | :--- |
| `conta` | `Conta` | `id`, `numero_conta`, `saldo_atual`, `chave_pix` *(única)* |
| `pix` | `Pix` | `id`, `id_conta_origem`, `tipo_operacao`, `chave_destino`, `valor`, `data_transacao` |
| `boleto` | `Boleto` | `id`, `id_conta_origem`, `codigo_barras`, `valor`, `data_pagamento` |
| `ted` | `Ted` | `id`, `id_conta_origem`, `conta_destino`, `valor`, `data_transferencia` |
| `doc` | `Doc` | `id`, `id_conta_origem`, `conta_destino`, `valor`, `data_transferencia` |
| `item_lote` | `ItemLote` | `id`, `tipo_modalidade`, `id_conta_origem`, `destino`, `valor`, `status`, `data_criacao` |

---

## 6. Como Executar, Testar e Visualizar o Banco

### Rodando a Aplicação
Execute a classe principal `SistemaBancarioApplication.java` (ou via Maven na pasta `core`):
```powershell
cd core
.\mvnw.cmd spring-boot:run
```
Na primeira execução (com o banco vazio), `demonstracaoInicial` popula as 2 contas e executa 1 operação de cada modalidade (`PIX` pagamento, `BOLETO`, `PIX` transferência, `TED` e `DOC`), disparando `loteService.processarLotePendente()` ao final.

### Rodando os Testes Automatizados (16 testes)
Os testes usam o perfil `test` (`core/src/test/resources/application-test.yaml`) com banco isolado em `core/target/test_bancario.db` para não alterar o banco principal:
```powershell
cd core
.\mvnw.cmd test
```
*Cobertura em `PixFluxoTests.java` e `CoreApplicationTests.java`:* carregamento do contexto Spring/SQLite, pagamento e transferência Pix com resolução de chave, transferência para mesma conta, rejeição de chave inválida/inexistente, saldo insuficiente com rollback, bloqueio de cruzamento inválido (`"TED"` em pagamento e `"BOLETO"` em transferência), validação de Boleto, TED sem lote, DOC com lote e limite de R\$ 4.999,99, além do processamento de lote pendente.

### Visualizando o Arquivo `.db` na IDE (VS Code / Antigravity)
1. Pressione `Ctrl + Shift + X` para abrir **Extensões**.
2. Instale a extensão **SQLite Viewer** (*Florian Klampfer*).
3. Clique diretamente no arquivo `sistema_bancario_geral.db` no explorador para visualizar todas as tabelas (`conta`, `pix`, `boleto`, `ted`, `doc`, `item_lote`) em formato de planilha.

---

## 7. Roteiro Rápido para Defesa de Autoria

1. **Por que um único projeto com 8 pacotes?**
   Adotamos um **Monólito Modular** para simular os 8 microsserviços do diagrama de pacotes rodando em um único processo Spring Boot, mantendo o isolamento de dados (*Database-per-Service* lógico: cada pacote possui sua própria `Entity` e `Repository`).
2. **Qual padrão de projeto foi usado?**
   **Padrão Strategy** com injeção automática por `Map<String, FormaDePagamento>` e `Map<String, FormaDeTransferencia>`, eliminando condicionais `if/else` nos orquestradores e separando claramente quem pode pagar (`PIX`, `BOLETO`) de quem pode transferir (`PIX`, `TED`, `DOC`).
3. **Como a consistência do saldo é garantida?**
   Toda alteração de saldo passa exclusivamente pelo `SaldoService`, dentro de métodos `@Transactional` nos orquestradores, validando primeiro a entrada, depois debitando/creditando e por último gravando o histórico e o lote.
