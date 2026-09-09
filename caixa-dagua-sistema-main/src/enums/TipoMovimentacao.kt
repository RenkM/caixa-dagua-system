package enums

/**
 * Direcao do dinheiro no caixa da empresa:
 *  - ENTRADA: dinheiro entrou (venda paga, servico concluido).
 *  - SAIDA:   dinheiro saiu (compra de fornecedor, pagamento de funcionario).
 */
enum class TipoMovimentacao {
    ENTRADA,
    SAIDA
}
