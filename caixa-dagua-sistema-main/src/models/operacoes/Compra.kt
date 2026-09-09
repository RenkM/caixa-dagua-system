package models.operacoes

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Compra de caixas d'agua de um fornecedor.
 * Efeito: entra estoque (+quantidade) e sai dinheiro do caixa (valorTotal).
 *
 * Os campos "*Id" guardam so o numero (chave estrangeira) da linha relacionada no banco.
 */
class Compra(
    var id: Int?,               // null antes de salvar; o banco gera o numero
    var fornecedorId: Int,      // quem vendeu para a empresa (tabela fornecedor)
    var produtoId: Int,         // qual caixa d'agua foi comprada (tabela produto)
    var quantidade: Int,
    var valorUnitario: BigDecimal,
    var valorTotal: BigDecimal, // valorUnitario * quantidade
    var dataHora: LocalDateTime,
    var responsavelId: Int,     // funcionario que registrou a operacao
    var movimentacaoId: Int     // a saida de caixa gerada por esta compra
)
