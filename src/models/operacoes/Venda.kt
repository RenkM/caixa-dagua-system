package models.operacoes

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Venda de caixas d'agua para um cliente.
 * Efeito: sai estoque (-quantidade) e, se paga a vista, entra dinheiro no caixa.
 */
class Venda(
    var id: Int?,
    var clienteId: Int,          // quem comprou (tabela cliente)
    var produtoId: Int,          // qual caixa d'agua foi vendida (tabela produto)
    var quantidade: Int,
    var valorUnitario: BigDecimal,
    var valorTotal: BigDecimal,
    var pago: Boolean,           // true = pagou agora; false = ficou "no fiado"
    var dataHora: LocalDateTime,
    var responsavelId: Int,      // funcionario que registrou a venda
    var movimentacaoId: Int?     // a entrada de caixa; null quando a venda e no fiado
)
