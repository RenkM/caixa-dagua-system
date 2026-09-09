package models.operacoes

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Pagamento do salario de um funcionario referente a um mes (competencia AAAA-MM).
 * Efeito: sai dinheiro do caixa no valor do salario.
 */
class PagamentoFuncionario(
    var id: Int?,
    var funcionarioId: Int,      // quem recebeu o salario
    var valor: BigDecimal,       // igual ao salario do funcionario
    var competencia: String,     // mes de referencia, ex.: "2026-08"
    var dataHora: LocalDateTime,
    var responsavelId: Int,      // funcionario que autorizou/registrou o pagamento
    var movimentacaoId: Int      // a saida de caixa gerada por este pagamento
)
