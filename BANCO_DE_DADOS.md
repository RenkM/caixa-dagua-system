# Banco de dados — Sistema Caixa da Água

Tudo o que o PostgreSQL precisa está aqui. Ordem: **(1)** criar o banco, **(2)** rodar o
script de estrutura, **(3)** rodar o script de dados iniciais (seed), **(4)** ajustar a
conexão no código.

---

## 1. Pré-requisitos

- PostgreSQL instalado e rodando (porta padrão `5432`).
- O driver JDBC está incluído no próprio projeto em **`lib/postgresql-42.7.10.jar`** e já
  está registrado como biblioteca do módulo (`File > Project Structure > Libraries`). Não
  precisa baixar nada. Se o IntelliJ mostrar a biblioteca em vermelho, remova-a e adicione
  de novo apontando para `lib/postgresql-42.7.10.jar`.

Crie o banco (no `psql` ou no pgAdmin):

```sql
CREATE DATABASE caixa_da_agua;
```

Depois **conecte-se a esse banco** (`\c caixa_da_agua` no psql) antes de rodar os scripts abaixo.

---

## 2. Script de estrutura (tabelas)

```sql
-- Apaga tudo para poder rodar de novo sem erro
DROP TABLE IF EXISTS pagamento_funcionario, ordem_servico, venda, compra,
    movimentacao_financeira, caixa, produto, auditor, fornecedor, cliente,
    funcionario, setor CASCADE;

-- Setores (o sistema exige no mínimo 2)
CREATE TABLE setor (
    id   SERIAL PRIMARY KEY,
    nome VARCHAR(40) NOT NULL UNIQUE
);

-- Funcionários (sempre ligados a um setor)
CREATE TABLE funcionario (
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(60)   NOT NULL,
    sobrenome  VARCHAR(60)   NOT NULL,
    cpf        VARCHAR(14)   NOT NULL UNIQUE,
    rg         VARCHAR(20)   NOT NULL,
    idade      INT           NOT NULL CHECK (idade BETWEEN 0 AND 130),
    salario    NUMERIC(12,2) NOT NULL CHECK (salario >= 0),
    turno      VARCHAR(10)   NOT NULL CHECK (turno IN ('MANHA','TARDE','NOITE')),
    habilidade VARCHAR(20)   NOT NULL
               CHECK (habilidade IN ('INSTALACAO','FINANCEIRO','ADMINISTRATIVO','LOGISTICA','MANUTENCAO')),
    setor_id   INT           NOT NULL REFERENCES setor(id)
);

-- Clientes
CREATE TABLE cliente (
    id              SERIAL PRIMARY KEY,
    nome            VARCHAR(60) NOT NULL,
    sobrenome       VARCHAR(60) NOT NULL,
    cpf             VARCHAR(14) NOT NULL UNIQUE,
    rg              VARCHAR(20) NOT NULL,
    idade           INT         NOT NULL CHECK (idade BETWEEN 0 AND 130),
    dividas_abertas BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Fornecedores (pessoa jurídica)
CREATE TABLE fornecedor (
    id            SERIAL PRIMARY KEY,
    nome_fantasia VARCHAR(80)  NOT NULL,
    cnpj          VARCHAR(18)  NOT NULL UNIQUE,
    telefone      VARCHAR(20),
    email         VARCHAR(120)
);

-- Auditores externos
CREATE TABLE auditor (
    id        SERIAL PRIMARY KEY,
    nome      VARCHAR(60) NOT NULL,
    sobrenome VARCHAR(60) NOT NULL,
    cpf       VARCHAR(14) NOT NULL UNIQUE,
    rg        VARCHAR(20) NOT NULL,
    idade     INT         NOT NULL CHECK (idade BETWEEN 0 AND 130),
    registro  VARCHAR(30) NOT NULL,
    orgao     VARCHAR(60) NOT NULL
);

-- Produto: caixa d'água (catálogo + estoque)
CREATE TABLE produto (
    id                 SERIAL PRIMARY KEY,
    marca              VARCHAR(40)   NOT NULL,
    modelo             VARCHAR(40)   NOT NULL,
    cor                VARCHAR(20)   NOT NULL
                       CHECK (cor IN ('AZUL_ESCURO','AZUL_CLARO','CINZA','BRANCO')),
    material           VARCHAR(20)   NOT NULL
                       CHECK (material IN ('PLASTICO','FIBRA_DE_VIDRO')),
    formato            VARCHAR(20)   NOT NULL,
    largura            DOUBLE PRECISION NOT NULL CHECK (largura > 0),
    altura             DOUBLE PRECISION NOT NULL CHECK (altura > 0),
    profundidade       DOUBLE PRECISION NOT NULL CHECK (profundidade > 0),
    preco_venda        NUMERIC(12,2) NOT NULL CHECK (preco_venda >= 0),
    quantidade_estoque INT           NOT NULL DEFAULT 0 CHECK (quantidade_estoque >= 0)
);

-- Caixa: uma única linha (id = 1) com o saldo consolidado
CREATE TABLE caixa (
    id    INT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    saldo NUMERIC(14,2) NOT NULL DEFAULT 0
);

-- Movimentação financeira: o coração do controle de caixa
CREATE TABLE movimentacao_financeira (
    id             SERIAL PRIMARY KEY,
    valor          NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    tipo           VARCHAR(10)   NOT NULL CHECK (tipo IN ('ENTRADA','SAIDA')),
    operacao       VARCHAR(25)   NOT NULL
                   CHECK (operacao IN ('COMPRA','VENDA','PAGAMENTO_FUNCIONARIO','SERVICO')),
    pagador        VARCHAR(120)  NOT NULL,
    recebedor      VARCHAR(120)  NOT NULL,
    data_hora      TIMESTAMP     NOT NULL DEFAULT NOW(),
    descricao      VARCHAR(255)  NOT NULL,
    responsavel_id INT           NOT NULL REFERENCES funcionario(id)
);

-- Compra de fornecedor (entrada de estoque + saída de caixa)
CREATE TABLE compra (
    id              SERIAL PRIMARY KEY,
    fornecedor_id   INT           NOT NULL REFERENCES fornecedor(id),
    produto_id      INT           NOT NULL REFERENCES produto(id),
    quantidade      INT           NOT NULL CHECK (quantidade > 0),
    valor_unitario  NUMERIC(12,2) NOT NULL CHECK (valor_unitario >= 0),
    valor_total     NUMERIC(14,2) NOT NULL CHECK (valor_total >= 0),
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW(),
    responsavel_id  INT           NOT NULL REFERENCES funcionario(id),
    movimentacao_id INT           NOT NULL REFERENCES movimentacao_financeira(id)
);

-- Venda para cliente (saída de estoque + entrada de caixa se paga)
CREATE TABLE venda (
    id              SERIAL PRIMARY KEY,
    cliente_id      INT           NOT NULL REFERENCES cliente(id),
    produto_id      INT           NOT NULL REFERENCES produto(id),
    quantidade      INT           NOT NULL CHECK (quantidade > 0),
    valor_unitario  NUMERIC(12,2) NOT NULL CHECK (valor_unitario >= 0),
    valor_total     NUMERIC(14,2) NOT NULL CHECK (valor_total >= 0),
    pago            BOOLEAN       NOT NULL DEFAULT TRUE,
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW(),
    responsavel_id  INT           NOT NULL REFERENCES funcionario(id),
    movimentacao_id INT           REFERENCES movimentacao_financeira(id)  -- NULL quando no fiado
);

-- Ordem de serviço: instalação / manutenção
CREATE TABLE ordem_servico (
    id              SERIAL PRIMARY KEY,
    cliente_id      INT           NOT NULL REFERENCES cliente(id),
    instalador_id   INT           NOT NULL REFERENCES funcionario(id),
    tipo            VARCHAR(15)   NOT NULL CHECK (tipo IN ('INSTALACAO','MANUTENCAO')),
    descricao       VARCHAR(255)  NOT NULL,
    valor           NUMERIC(12,2) NOT NULL CHECK (valor >= 0),
    status          VARCHAR(15)   NOT NULL DEFAULT 'ABERTA'
                    CHECK (status IN ('ABERTA','CONCLUIDA')),
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW(),
    responsavel_id  INT           NOT NULL REFERENCES funcionario(id),
    movimentacao_id INT           REFERENCES movimentacao_financeira(id)  -- preenchido ao concluir
);

-- Folha: pagamento de salário de um funcionário
CREATE TABLE pagamento_funcionario (
    id              SERIAL PRIMARY KEY,
    funcionario_id  INT           NOT NULL REFERENCES funcionario(id),
    valor           NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    competencia     VARCHAR(7)    NOT NULL,   -- AAAA-MM
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW(),
    responsavel_id  INT           NOT NULL REFERENCES funcionario(id),
    movimentacao_id INT           NOT NULL REFERENCES movimentacao_financeira(id)
);
```

---

## 3. Script de dados iniciais (seed)

Necessário para o sistema abrir: precisa existir a linha do `caixa` e pelo menos um
funcionário (para ser o "responsável" das operações).

```sql
-- Setores (mínimo 2 exigido; aqui vão 4)
INSERT INTO setor (nome) VALUES
    ('FINANCEIRO'),
    ('ADMINISTRATIVO'),
    ('LOGISTICA'),
    ('INSTALACAO');

-- Linha única do caixa
INSERT INTO caixa (id, saldo) VALUES (1, 0);

-- Funcionários iniciais (um por setor)
INSERT INTO funcionario (nome, sobrenome, cpf, rg, idade, salario, turno, habilidade, setor_id) VALUES
    ('Ana',   'Souza',   '111.111.111-11', '1111111', 34, 4200.00, 'MANHA', 'FINANCEIRO',
        (SELECT id FROM setor WHERE nome = 'FINANCEIRO')),
    ('Bruno', 'Lima',    '222.222.222-22', '2222222', 41, 3900.00, 'TARDE', 'ADMINISTRATIVO',
        (SELECT id FROM setor WHERE nome = 'ADMINISTRATIVO')),
    ('Carla', 'Nunes',   '333.333.333-33', '3333333', 29, 3500.00, 'MANHA', 'LOGISTICA',
        (SELECT id FROM setor WHERE nome = 'LOGISTICA')),
    ('Diego', 'Martins', '444.444.444-44', '4444444', 37, 3300.00, 'TARDE', 'INSTALACAO',
        (SELECT id FROM setor WHERE nome = 'INSTALACAO'));

-- Exemplos opcionais para testar rápido
INSERT INTO fornecedor (nome_fantasia, cnpj, telefone, email) VALUES
    ('Tigre Tubos e Conexoes', '11.111.111/0001-11', '4733412000', 'vendas@tigre.exemplo');

INSERT INTO cliente (nome, sobrenome, cpf, rg, idade) VALUES
    ('Joao', 'Pereira', '555.555.555-55', '5555555', 45);

INSERT INTO produto (marca, modelo, cor, material, formato, largura, altura, profundidade, preco_venda, quantidade_estoque)
VALUES ('Fortlev', '1000L', 'AZUL_CLARO', 'PLASTICO', 'cilindrico', 1.30, 1.05, 1.30, 650.00, 0);
```

---

## 4. Configurar a conexão no código

Abra **`src/repositorio/Conexao.kt`** e ajuste as três constantes do topo:

```kotlin
private const val URL     = "jdbc:postgresql://localhost:5432/caixa_da_agua"
private const val USUARIO = "postgres"     // seu usuário do PostgreSQL
private const val SENHA   = "postgres"     // sua senha
```

Rode a classe `Main.kt` (botão ▶ do IntelliJ). Se aparecer `Conexao com o banco OK.`,
está tudo certo.

---

## 5. Checklist rápido

No `psql`, conectado a `caixa_da_agua`:

```sql
\dt
```

Deve listar: `auditor, caixa, cliente, compra, fornecedor, funcionario,
movimentacao_financeira, ordem_servico, pagamento_funcionario, produto, setor, venda`.

```sql
SELECT * FROM caixa;              -- 1 linha, saldo 0.00
SELECT nome FROM setor;           -- 4 setores
SELECT nome, sobrenome FROM funcionario;  -- 4 funcionários
```

Para **zerar tudo e começar de novo**, basta rodar o script da seção 2 outra vez
(ele começa com `DROP TABLE ...`) e depois o da seção 3.
