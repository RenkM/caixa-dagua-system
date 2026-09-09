package sistema

import repositorio.Conexao
import util.Entrada

/**
 * Menu raiz do sistema. Todo item de menu roda dentro de [executarSeguro], entao um erro
 * (banco fora do ar, registro nao encontrado, saldo insuficiente...) vira uma mensagem
 * e o menu continua funcionando - nunca quebra na cara do usuario.
 */
object Menu {

    fun principal() {
        println("==============================================")
        println("        SISTEMA - CAIXA DA AGUA")
        println("==============================================")

        if (!Conexao.testar()) {
            println()
            println("Nao foi possivel conectar ao banco.")
            println("1) Confira usuario/senha/URL em src/repositorio/Conexao.kt")
            println("2) Rode os scripts de BANCO_DE_DADOS.md (estrutura + seed)")
            println("Encerrando.")
            return
        }
        println("Conexao com o banco OK.")

        while (true) {
            println(
                """
                |
                |----------- MENU PRINCIPAL -----------
                |1 - Pessoas (funcionarios, clientes, fornecedores, auditores)
                |2 - Produtos e estoque
                |3 - Operacoes (compra, venda, servicos, folha)
                |4 - Financeiro (caixa e movimentacoes)
                |0 - Sair
                |-------------------------------------
                """.trimMargin()
            )
            when (Entrada.lerTexto("Opcao")) {
                "1" -> MenuPessoas.abrir()
                "2" -> MenuProdutos.abrir()
                "3" -> MenuOperacoes.abrir()
                "4" -> MenuFinanceiro.abrir()
                "0" -> {
                    println("Ate mais!")
                    return
                }
                else -> println("Opcao invalida.")
            }
        }
    }
}

/** Executa uma acao de menu capturando qualquer excecao e mostrando mensagem amigavel. */
internal fun executarSeguro(acao: () -> Unit) {
    try {
        acao()
    } catch (e: Exception) {
        println()
        println("[ERRO] ${e.message ?: e.javaClass.simpleName}")
        println("Operacao cancelada. Nada foi gravado.")
        println()
    }
}
