package sistema

import financeiro.MovimentacaoFinanceira
import repositorio.CaixaRepository
import repositorio.FuncionarioRepository
import repositorio.MovimentacaoRepository
import util.Entrada
import util.Formato
import java.time.LocalDate
import java.time.LocalTime

/**
 * Submenu de FINANCEIRO: so consulta (nao gera dinheiro).
 * Mostra o saldo do caixa e o extrato de movimentacoes com os 6 dados exigidos:
 * valor, pagador, recebedor, data/hora, motivo e responsavel.
 */
object MenuFinanceiro {

    fun abrir() {
        while (true) {
            println(
                """
                |
                |--- FINANCEIRO ---
                |1 - Ver saldo do caixa
                |2 - Listar todas as movimentacoes
                |3 - Extrato de um mes (AAAA-MM)
                |0 - Voltar
                """.trimMargin()
            )
            when (Entrada.lerTexto("Opcao")) {
                "1" -> executarSeguro(::verSaldo)
                "2" -> executarSeguro { mostrar(MovimentacaoRepository.listar()) }
                "3" -> executarSeguro(::extratoDoMes)
                "0" -> return
                else -> println("Opcao invalida.")
            }
        }
    }

    private fun verSaldo() {
        println("Saldo atual do caixa: ${Formato.dinheiro(CaixaRepository.saldoAtual())}")
    }

    private fun extratoDoMes() {
        val competencia = Entrada.lerCompetencia()            // "AAAA-MM", ja validado
        val (ano, mes) = competencia.split("-").map { it.toInt() }
        val primeiroDia = LocalDate.of(ano, mes, 1)
        val inicio = primeiroDia.atStartOfDay()                                    // dia 1, 00:00
        val fim = primeiroDia.withDayOfMonth(primeiroDia.lengthOfMonth()).atTime(LocalTime.MAX) // ultimo dia, 23:59:59
        mostrar(MovimentacaoRepository.listarPorPeriodo(inicio, fim))
    }

    private fun mostrar(movimentacoes: List<MovimentacaoFinanceira>) {
        if (movimentacoes.isEmpty()) {
            println("Nenhuma movimentacao no periodo.")
            return
        }
        // mapa id->nome do responsavel, montado uma vez so
        val responsaveis = FuncionarioRepository.listar().associate { it.id to it.nomeCompleto }

        println("=========================================================")
        movimentacoes.forEach { m ->
            val nomeResp = responsaveis[m.responsavelId] ?: "funcionario #${m.responsavelId}"
            println("${Formato.dataHora(m.dataHora)} | ${m.tipo}  ${Formato.dinheiro(m.valor)}  (${m.operacao})")
            println("   pagador  : ${m.pagador}")
            println("   recebedor: ${m.recebedor}")
            println("   motivo   : ${m.descricao}")
            println("   responsavel pela transacao: $nomeResp")
            println("---------------------------------------------------------")
        }
        val entradas = movimentacoes.filter { it.tipo.name == "ENTRADA" }.sumOf { it.valor.toDouble() }
        val saidas = movimentacoes.filter { it.tipo.name == "SAIDA" }.sumOf { it.valor.toDouble() }
        println("Total de movimentacoes: ${movimentacoes.size}")
        println("Entradas: R$ %.2f  |  Saidas: R$ %.2f".format(entradas, saidas))
        println("Saldo atual do caixa: ${Formato.dinheiro(CaixaRepository.saldoAtual())}")
    }
}
