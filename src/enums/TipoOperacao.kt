package enums

/**
 * Qual operacao de negocio gerou a movimentacao financeira.
 * Serve para filtrar/relatar o caixa depois (ex.: "quanto gastei com folha?").
 */
enum class TipoOperacao {
    COMPRA,                 // compra de produto de um fornecedor
    VENDA,                  // venda de produto para um cliente
    PAGAMENTO_FUNCIONARIO,  // pagamento de salario
    SERVICO                 // recebimento por ordem de servico concluida
}
