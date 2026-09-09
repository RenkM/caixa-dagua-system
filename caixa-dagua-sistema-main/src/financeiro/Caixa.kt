package financeiro

import enums.TipoMovimentacao
import enums.TipoOperacao
import repositorio.CaixaRepository
import repositorio.MovimentacaoRepository
import servico.SaldoInsuficienteException
import java.math.BigDecimal
import java.sql.Connection

/**
 * Caixa (fluxo de dinheiro) da empresa - o "cofre" seguro do sistema.
 *
 * Encapsulamento:
 *  - [saldo] tem setter privado: ninguem de fora consegue somar/subtrair na mao.
 *  - o construtor e privado: so da pra obter um Caixa por [carregar], que le o saldo do banco.
 *  - toda alteracao passa por [registrarEntrada] / [registrarSaida], que SEMPRE:
 *      1. validam o valor;
 *      2. gravam uma [MovimentacaoFinanceira] (com pagador, recebedor, data/hora, motivo, responsavel);
 *      3. atualizam o saldo persistido.
 *
 * Todos os metodos recebem a [Connection] da operacao em andamento para participarem
 * da mesma transacao (commit/rollback ficam a cargo do servico que chamou).
 */
class Caixa private constructor(saldoInicial: BigDecimal) {

    // Saldo atual. Leitura livre; escrita so dentro desta classe (private set).
    var saldo: BigDecimal = saldoInicial
        private set

    /** Dinheiro entrando: soma no saldo e registra o motivo. Devolve a movimentacao gravada. */
    fun registrarEntrada(
        conn: Connection,
        valor: BigDecimal,
        pagador: String,
        recebedor: String,
        operacao: TipoOperacao,
        descricao: String,
        responsavelId: Int
    ): MovimentacaoFinanceira {
        exigirValorPositivo(valor)
        val mov = MovimentacaoRepository.inserir(
            conn, valor, TipoMovimentacao.ENTRADA, operacao, pagador, recebedor, descricao, responsavelId
        )
        saldo = saldo.add(valor)
        CaixaRepository.atualizarSaldo(conn, saldo)
        return mov
    }

    /** Dinheiro saindo: valida que ha saldo suficiente, subtrai e registra o motivo. */
    fun registrarSaida(
        conn: Connection,
        valor: BigDecimal,
        pagador: String,
        recebedor: String,
        operacao: TipoOperacao,
        descricao: String,
        responsavelId: Int
    ): MovimentacaoFinanceira {
        exigirValorPositivo(valor)
        if (saldo < valor) throw SaldoInsuficienteException(saldo, valor)  // nao deixa o caixa ficar negativo
        val mov = MovimentacaoRepository.inserir(
            conn, valor, TipoMovimentacao.SAIDA, operacao, pagador, recebedor, descricao, responsavelId
        )
        saldo = saldo.subtract(valor)
        CaixaRepository.atualizarSaldo(conn, saldo)
        return mov
    }

    private fun exigirValorPositivo(valor: BigDecimal) {
        require(valor > BigDecimal.ZERO) { "O valor da movimentacao deve ser maior que zero." }
    }

    companion object {
        const val EMPRESA = "Empresa (Caixa da Agua)"

        /** Unica forma de obter um Caixa: carregando o saldo atual do banco. */
        fun carregar(conn: Connection): Caixa = Caixa(CaixaRepository.buscarSaldo(conn))
    }
}
