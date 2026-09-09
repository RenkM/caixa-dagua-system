package financeiro

import enums.TipoMovimentacao
import enums.TipoOperacao
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Registro imutavel de uma movimentacao de dinheiro. Todos os campos sao `val`:
 * depois de criada, uma movimentacao nao muda (isso protege o historico financeiro).
 *
 * Guarda exatamente o que o requisito pede:
 *  - [valor]         quanto dinheiro foi usado
 *  - [pagador]       quem pagou
 *  - [recebedor]     quem recebeu
 *  - [dataHora]      data e hora
 *  - [descricao]     o motivo/descricao
 *  - [responsavelId] o funcionario responsavel pela transacao
 */
data class MovimentacaoFinanceira(
    val id: Int?,
    val valor: BigDecimal,
    val tipo: TipoMovimentacao,
    val operacao: TipoOperacao,
    val pagador: String,
    val recebedor: String,
    val dataHora: LocalDateTime,
    val descricao: String,
    val responsavelId: Int
)
