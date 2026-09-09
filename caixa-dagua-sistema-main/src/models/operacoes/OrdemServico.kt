package models.operacoes

import enums.TipoServico
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Ordem de servico (OS) de instalacao ou manutencao para um cliente, feita por um instalador.
 *
 * Ciclo de vida:
 *  1. Aberta  -> status ABERTA, sem dinheiro ainda (movimentacaoId = null).
 *  2. Concluida -> status CONCLUIDA, gera a entrada de caixa e preenche movimentacaoId.
 */
class OrdemServico(
    var id: Int?,
    var clienteId: Int,          // para quem e o servico (tabela cliente)
    var instaladorId: Int,       // funcionario que executa o servico
    var tipo: TipoServico,       // INSTALACAO ou MANUTENCAO
    var descricao: String,
    var valor: BigDecimal,       // quanto o cliente vai pagar pelo servico
    var status: String,          // ABERTA / CONCLUIDA (ver constantes abaixo)
    var dataHora: LocalDateTime,
    var responsavelId: Int,      // funcionario que registrou a OS
    var movimentacaoId: Int?     // a entrada de caixa; preenchida so ao concluir
) {
    companion object {
        const val ABERTA = "ABERTA"
        const val CONCLUIDA = "CONCLUIDA"
    }
}
