package servico

import enums.TipoOperacao
import enums.TipoServico
import financeiro.Caixa
import models.operacoes.Compra
import models.operacoes.OrdemServico
import models.operacoes.PagamentoFuncionario
import models.operacoes.Venda
import models.pessoas.Cliente
import models.pessoas.Fornecedor
import models.pessoas.Funcionario
import models.produtos.CaixaDaAgua
import repositorio.ClienteRepository
import repositorio.CompraRepository
import repositorio.Conexao
import repositorio.OrdemServicoRepository
import repositorio.PagamentoFuncionarioRepository
import repositorio.ProdutoRepository
import repositorio.VendaRepository
import util.Validacao
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDateTime

/**
 * Regras de negocio das operacoes que mexem em estoque e/ou dinheiro.
 *
 * Cada operacao roda numa unica transacao: se qualquer passo falhar, faz `rollback()`
 * e nada e gravado (estoque, caixa e movimentacao ficam sempre consistentes).
 * O saldo so e alterado atraves do [Caixa], que exige uma movimentacao a cada mudanca.
 */
object OperacoesComerciais {

    /**
     * COMPRA DE FORNECEDOR: sai dinheiro do caixa e entra estoque.
     * Passos (tudo na mesma transacao): saida de caixa -> +estoque -> grava a compra.
     */
    fun realizarCompra(
        fornecedor: Fornecedor,
        produto: CaixaDaAgua,
        quantidade: Int,
        valorUnitario: BigDecimal,
        responsavel: Funcionario
    ) {
        require(quantidade > 0) { "Quantidade deve ser maior que zero." }
        Validacao.valorPositivo(valorUnitario, "Valor unitario")
        val produtoId = produto.id ?: throw RegistroNaoEncontradoException("Produto")
        val total = valorUnitario.multiply(BigDecimal(quantidade))

        emTransacao { conn ->
            // 1) tira o dinheiro do caixa (isso ja grava a movimentacao financeira)
            val caixa = Caixa.carregar(conn)
            val mov = caixa.registrarSaida(
                conn, total,
                pagador = Caixa.EMPRESA,
                recebedor = "Fornecedor ${fornecedor.nomeFantasia}",
                operacao = TipoOperacao.COMPRA,
                descricao = "Compra de $quantidade x ${produto.marca} ${produto.modelo}",
                responsavelId = responsavel.id!!
            )
            // 2) aumenta o estoque
            ProdutoRepository.entrarEstoque(conn, produtoId, quantidade)
            // 3) registra a compra, apontando para a movimentacao criada no passo 1
            CompraRepository.inserir(
                conn,
                Compra(null, fornecedor.id!!, produtoId, quantidade, valorUnitario, total, LocalDateTime.now(), responsavel.id!!, mov.id!!)
            )
        }
    }

    /**
     * VENDA PARA CLIENTE: sai estoque e, se paga a vista, entra dinheiro no caixa.
     * Se nao for paga agora ("fiado"), o cliente fica marcado com dividas em aberto.
     */
    fun realizarVenda(
        cliente: Cliente,
        produto: CaixaDaAgua,
        quantidade: Int,
        responsavel: Funcionario,
        pagoAgora: Boolean
    ) {
        require(quantidade > 0) { "Quantidade deve ser maior que zero." }
        val produtoId = produto.id ?: throw RegistroNaoEncontradoException("Produto")

        emTransacao { conn ->
            // le o estoque/preco ATUAL do banco (nao confia no objeto que veio do menu)
            val atual = ProdutoRepository.buscar(conn, produtoId) ?: throw RegistroNaoEncontradoException("Produto")
            if (atual.quantidadeEstoque < quantidade) {
                throw EstoqueInsuficienteException(atual.quantidadeEstoque, quantidade)
            }
            val total = atual.precoVenda.multiply(BigDecimal(quantidade))

            var movimentacaoId: Int? = null
            if (pagoAgora) {
                // pagou: entra dinheiro no caixa e guardamos o id da movimentacao
                val caixa = Caixa.carregar(conn)
                val mov = caixa.registrarEntrada(
                    conn, total,
                    pagador = "Cliente ${cliente.nomeCompleto}",
                    recebedor = Caixa.EMPRESA,
                    operacao = TipoOperacao.VENDA,
                    descricao = "Venda de $quantidade x ${atual.marca} ${atual.modelo}",
                    responsavelId = responsavel.id!!
                )
                movimentacaoId = mov.id
            } else {
                // fiado: sem entrada de caixa; marca o cliente como devedor
                ClienteRepository.marcarDividas(conn, cliente.id!!, true)
            }
            // sempre baixa o estoque e grava a venda
            ProdutoRepository.baixarEstoque(conn, produtoId, quantidade)
            VendaRepository.inserir(
                conn,
                Venda(null, cliente.id!!, produtoId, quantidade, atual.precoVenda, total, pagoAgora, LocalDateTime.now(), responsavel.id!!, movimentacaoId)
            )
        }
    }

    /** Abre a OS (sem dinheiro ainda). Devolve o id da ordem criada. */
    fun abrirOrdemServico(
        cliente: Cliente,
        instalador: Funcionario,
        tipo: TipoServico,
        descricao: String,
        valor: BigDecimal,
        responsavel: Funcionario
    ): Int {
        Validacao.valorPositivo(valor, "Valor do servico")
        val os = OrdemServico(
            id = null,
            clienteId = cliente.id!!,
            instaladorId = instalador.id!!,
            tipo = tipo,
            descricao = descricao,
            valor = valor,
            status = OrdemServico.ABERTA,
            dataHora = LocalDateTime.now(),
            responsavelId = responsavel.id!!,
            movimentacaoId = null
        )
        return OrdemServicoRepository.inserir(os)
    }

    /** CONCLUIR OS: marca como concluida e recebe o pagamento (entrada de caixa). */
    fun concluirOrdemServico(osId: Int, responsavel: Funcionario) {
        emTransacao { conn ->
            val os = OrdemServicoRepository.buscar(conn, osId)
                ?: throw RegistroNaoEncontradoException("Ordem de servico #$osId")
            check(os.status == OrdemServico.ABERTA) { "OS #$osId ja esta ${os.status}." }  // nao concluir 2x

            val nomeCliente = ClienteRepository.buscar(os.clienteId)?.nomeCompleto ?: "Cliente #${os.clienteId}"
            val caixa = Caixa.carregar(conn)
            val mov = caixa.registrarEntrada(
                conn, os.valor,
                pagador = "Cliente $nomeCliente",
                recebedor = Caixa.EMPRESA,
                operacao = TipoOperacao.SERVICO,
                descricao = "${os.tipo} concluida - ${os.descricao}",
                responsavelId = responsavel.id!!
            )
            OrdemServicoRepository.concluir(conn, osId, mov.id!!)
        }
    }

    /** PAGAR FUNCIONARIO: saida de caixa no valor do salario, registrada na folha. */
    fun pagarFuncionario(funcionario: Funcionario, competencia: String, responsavel: Funcionario) {
        val valor = Validacao.valorPositivo(funcionario.salario, "Salario do funcionario")
        emTransacao { conn ->
            val caixa = Caixa.carregar(conn)
            val mov = caixa.registrarSaida(
                conn, valor,
                pagador = Caixa.EMPRESA,
                recebedor = "Funcionario ${funcionario.nomeCompleto}",
                operacao = TipoOperacao.PAGAMENTO_FUNCIONARIO,
                descricao = "Pagamento de salario - competencia $competencia",
                responsavelId = responsavel.id!!
            )
            PagamentoFuncionarioRepository.inserir(
                conn,
                PagamentoFuncionario(null, funcionario.id!!, valor, competencia, LocalDateTime.now(), responsavel.id!!, mov.id!!)
            )
        }
    }

    /**
     * "Molde" de transacao usado por todas as operacoes acima:
     * abre a conexao, desliga o commit automatico, roda o bloco e da commit no fim.
     * Se qualquer linha do bloco lancar excecao -> rollback (desfaz tudo) e repassa o erro.
     */
    private inline fun emTransacao(bloco: (Connection) -> Unit) {
        Conexao.abrir().use { conn ->
            conn.autoCommit = false
            try {
                bloco(conn)
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }
}
