# Estrutura do Projeto — Sistema Caixa da Água

Sistema de console (Kotlin + PostgreSQL) para uma empresa que **compra, vende e instala
caixas d'água**. Controla produtos, estoque, pessoas, setores e o **fluxo de caixa** da
empresa, com toda a informação salva no banco.

- Como montar o banco: **`BANCO_DE_DADOS.md`**
- Como rodar: abrir no IntelliJ e executar `src/Main.kt` (▶). Sem Gradle/Maven.

---

## 1. Visão geral em camadas

O código é dividido em camadas. Cada uma só conversa com a de baixo:

```
   ┌───────────────────────────────────────────────┐
   │  sistema/   → MENUS de console                 │  fala com o usuário
   ├───────────────────────────────────────────────┤
   │  servico/   → REGRAS de negócio + TRANSAÇÕES   │  "o que pode e o que não pode"
   ├───────────────────────────────────────────────┤
   │  financeiro/→ CAIXA (saldo encapsulado)        │  todo dinheiro passa aqui
   ├───────────────────────────────────────────────┤
   │  repositorio/→ REPOSITÓRIOS (SQL / JDBC)       │  ler e gravar no banco
   ├───────────────────────────────────────────────┤
   │            PostgreSQL (12 tabelas)             │
   └───────────────────────────────────────────────┘

   models/ = objetos de dados usados por todas as camadas
   enums/  = valores fixos (Cor, Turno, TipoOperacao...)
   util/   = leitura de console + validação + formatação
```

**Por que separar assim?** Cada arquivo tem uma responsabilidade só. Se um dia mudar o
banco, mexe só em `repositorio/`. Se mudar uma regra, mexe só em `servico/`. Os menus nunca
escrevem SQL.

---

## 2. O que tem em cada pasta

### `src/Main.kt`
Ponto de entrada. Só chama `Menu.principal()`.

### `src/enums/` — valores fixos
Usar `enum` em vez de texto solto evita erro de digitação (o valor errado nem compila).

| Arquivo | Para que serve |
|---|---|
| `Cor`, `Material`, `Turno` | características de produto / funcionário |
| `Habilidade` | o que o funcionário sabe fazer |
| `TipoServico` | INSTALACAO / MANUTENCAO |
| `TipoMovimentacao` | ENTRADA / SAIDA de dinheiro |
| `TipoOperacao` | COMPRA / VENDA / PAGAMENTO_FUNCIONARIO / SERVICO (o que gerou a movimentação) |

### `src/models/` — os "dados" do sistema
Classes simples que carregam informação. O campo `id: Int?` fica `null` até salvar (o banco
gera o número). Campos terminados em `Id` guardam a chave estrangeira (o número da linha
relacionada em outra tabela).

| Arquivo | Representa |
|---|---|
| `Setor` | setor da empresa (Financeiro, Logística...) |
| `pessoas/Pessoa` | classe-base (`open`) com nome, cpf, rg, idade |
| `pessoas/Funcionario` | funcionário (herda Pessoa) + salário, turno, setor. **`salario` só muda por método** |
| `pessoas/Cliente` | cliente (herda Pessoa) + `dividasAbertas` |
| `pessoas/Auditor` | auditor externo (herda Pessoa) + registro/órgão |
| `pessoas/Fornecedor` | fornecedor (pessoa jurídica: tem CNPJ, fica fora da herança) |
| `produtos/CaixaDaAgua` | o produto: marca, modelo, dimensões, preço, `quantidadeEstoque` |
| `operacoes/Compra` `Venda` `OrdemServico` `PagamentoFuncionario` | o registro de cada operação |

### `src/repositorio/` — acesso ao banco (uma classe por tabela)
Cada repositório usa **JDBC puro** (`java.sql`) com o mesmo padrão:

```kotlin
Conexao.abrir().use { conn ->                 // abre e fecha a conexão sozinho
    conn.prepareStatement(sql).use { stmt ->  // PreparedStatement: protege de SQL injection
        stmt.setString(1, ...)                // preenche cada "?" pela posição
        stmt.executeQuery().use { rs -> ... }  // percorre o resultado com rs.next()
    }
}
```

| Arquivo | Tabela / papel |
|---|---|
| `Conexao` | **configuração do banco (URL, usuário, senha) — edite aqui** |
| `SetorRepository` | `setor` (só lista) |
| `FuncionarioRepository` | `funcionario` (+ `listarPorSetor`) |
| `ClienteRepository` | `cliente` (+ `marcarDividas`) |
| `FornecedorRepository` / `AuditorRepository` | `fornecedor` / `auditor` |
| `ProdutoRepository` | `produto` (+ `entrarEstoque` / `baixarEstoque`) |
| `MovimentacaoRepository` | `movimentacao_financeira` (histórico do caixa) |
| `CaixaRepository` | linha única `caixa` (saldo consolidado) |
| `CompraRepository` `VendaRepository` `OrdemServicoRepository` `PagamentoFuncionarioRepository` | gravam cada operação |

Métodos que recebem `conn: Connection` participam de uma **transação** (ver seção 4).

### `src/financeiro/` — o dinheiro
| Arquivo | O que faz |
|---|---|
| `MovimentacaoFinanceira` | registro **imutável** (`val`) de um movimento de dinheiro: valor, pagador, recebedor, data/hora, motivo, responsável |
| `Caixa` | o "cofre". **Encapsulamento:** `saldo` tem `private set`, construtor privado. Só muda por `registrarEntrada()` / `registrarSaida()`, que **sempre** gravam uma `MovimentacaoFinanceira` e validam o valor. `registrarSaida` não deixa o saldo ficar negativo. |

### `src/servico/` — regras de negócio
| Arquivo | O que faz |
|---|---|
| `Erros` | exceções de negócio: `EstoqueInsuficienteException`, `SaldoInsuficienteException`, `RegistroNaoEncontradoException` |
| `OperacoesComerciais` | as 5 operações que mexem em estoque/caixa: `realizarCompra`, `realizarVenda`, `abrirOrdemServico`, `concluirOrdemServico`, `pagarFuncionario`. Cada uma roda numa **transação** (`emTransacao { }`) |

### `src/util/` — apoio
| Arquivo | O que faz |
|---|---|
| `Validacao` | valida CPF, CNPJ, e-mail, telefone, competência **sem regex** — checagem manual + `require`/`try-catch`. Devolve o valor limpo ou lança exceção |
| `Entrada` | lê do console **à prova de erro**: cada método tenta converter dentro de `try/catch` e **repete até o dado ficar certo**. Tem `escolherDaLista` e `escolherEnum` para menus numerados |
| `Formato` | formata dinheiro (`R$ 1.234,56`) e data/hora (`31/08/2026 14:30`) |

### `src/sistema/` — os menus
| Arquivo | Menu |
|---|---|
| `Menu` | menu principal + testa a conexão no início + função `executarSeguro { }` (captura erro e mantém o menu vivo) |
| `MenuPessoas` | cadastrar/listar funcionário, cliente, fornecedor, auditor; **listar por setor** |
| `MenuProdutos` | cadastrar/listar/editar produto; ver estoque |
| `MenuOperacoes` | compra, venda, abrir/concluir OS, pagar funcionário, listar vendas/OS |
| `MenuFinanceiro` | ver saldo, listar movimentações, extrato de um mês |

---

## 3. Como uma operação funciona (exemplo: VENDA)

Usuário escolhe `Operações > Venda para cliente`:

1. **`MenuOperacoes.venda()`** pergunta: cliente, produto, quantidade, se paga à vista, e
   **quem é o responsável** (escolhe da lista de funcionários). Só coleta os dados.
2. Chama **`OperacoesComerciais.realizarVenda(...)`**. Aí começa a transação:
   1. lê o **estoque atual** do produto no banco;
   2. se `estoque < quantidade` → lança `EstoqueInsuficienteException` (a transação é
      desfeita, nada é gravado);
   3. se for paga à vista → `Caixa.registrarEntrada(...)`, que grava a **movimentação
      financeira** (pagador = cliente, recebedor = empresa, motivo, data/hora, responsável)
      e soma no saldo; se for "fiado" → marca o cliente com dívidas em aberto;
   4. `ProdutoRepository.baixarEstoque(...)`;
   5. `VendaRepository.inserir(...)`.
3. Se todos os passos deram certo → **`commit()`** (grava tudo). Se qualquer um falhou →
   **`rollback()`** (desfaz tudo) e o menu mostra a mensagem de erro.

As outras operações seguem a mesma ideia:

| Operação | Estoque | Caixa | Movimentação |
|---|---|---|---|
| Compra de fornecedor | **+** | **saída** | pagador = empresa, recebedor = fornecedor |
| Venda (à vista) | **−** | **entrada** | pagador = cliente, recebedor = empresa |
| Venda (fiado) | **−** | — | — (cliente fica devendo) |
| Concluir ordem de serviço | — | **entrada** | pagador = cliente, recebedor = empresa |
| Pagar funcionário | — | **saída** | pagador = empresa, recebedor = funcionário |

---

## 4. Transação (por que compra/venda não "quebram pela metade")

Uma compra faz 3 coisas no banco: tira dinheiro, aumenta estoque, grava a compra. Se o
programa parasse no meio, o banco ficaria inconsistente (dinheiro saiu mas estoque não
entrou).

Para evitar isso, `OperacoesComerciais` usa `emTransacao { }`:

```kotlin
Conexao.abrir().use { conn ->
    conn.autoCommit = false      // não grava nada ainda
    try {
        ...vários passos...
        conn.commit()            // deu tudo certo → grava
    } catch (e: Exception) {
        conn.rollback()          // deu erro → desfaz TUDO
        throw e
    }
}
```

É "tudo ou nada".

---

## 5. Validação à prova de erro humano

Três técnicas, sem regex:

- **`try / catch`** — toda conversão de texto para número passa por `try { texto.toInt() }
  catch (NumberFormatException) { ... }`. `util/Entrada` repete a pergunta até o valor ser
  válido; os repositórios e serviços tratam `SQLException`.
- **Tipos anuláveis (`?`)** — `buscar(id): X?` devolve `null` quando não acha; quem chama
  faz `?: throw RegistroNaoEncontradoException(...)`. `readLine()` (que pode ser `null`)
  vira `""` com `?: ""`.
- **Checagem manual** — CPF/CNPJ: tira a máscara e conta os dígitos; e-mail: exige um `@`
  com texto antes e um `.` no domínio; competência: quebra em `AAAA-MM` e valida mês 1–12.
  Sempre com `require(condição) { "mensagem clara" }`.

O banco tem uma última camada de defesa: `CHECK` (valor > 0, estoque ≥ 0), `UNIQUE` (CPF,
CNPJ) e `FOREIGN KEY`.

---

## 6. Onde cada requisito da disciplina está

| Requisito | Onde |
|---|---|
| Menus interativos de console | `src/sistema/` |
| Tudo persistido no PostgreSQL | `src/repositorio/` + `BANCO_DE_DADOS.md` |
| Fluxo de produto/serviço (compra, venda, estoque, manutenção/montagem) | `servico/OperacoesComerciais` + `ProdutoRepository` |
| Gerenciar pessoas (funcionário, cliente, fornecedor, auditor) | `models/pessoas/` + `MenuPessoas` |
| Funcionários divididos em setores (≥ 2) | tabela `setor` (4 setores) + `FuncionarioRepository.listarPorSetor` |
| Fluxo de caixa seguro com encapsulamento | `financeiro/Caixa` (`saldo` privado) e `Funcionario.salario` (`private set`) |
| Movimentação salva com valor, pagador, recebedor, data/hora, motivo e responsável | `MovimentacaoFinanceira` + tabela `movimentacao_financeira` |
| Responsável pela transação | perguntado a cada operação em `MenuOperacoes.escolherResponsavel()` |
| Validação (TRY / NULLABLE / checagem) | `util/Validacao`, `util/Entrada`, exceções em `servico/Erros` |

---

## 7. Fluxo de teste rápido (depois de montar o banco)

`Pessoas` → cadastrar fornecedor, cliente, funcionário (com setor) e auditor →
`Produtos` → cadastrar caixa d'água →
`Operações` → **compra** 10 un. (estoque sobe, caixa cai) →
**venda** 3 un. à vista (estoque cai, caixa sobe) →
tentar vender 999 (erro amigável, menu continua) →
**abrir OS** de instalação → **concluir OS** (caixa sobe) →
**pagar funcionário** competência `2026-08` (caixa cai) →
`Financeiro` → **extrato**: mostra as movimentações com os 6 campos e o saldo bate.
