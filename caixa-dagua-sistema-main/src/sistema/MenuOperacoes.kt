package sistema

import enums.TipoServico
import models.pessoas.Funcionario
import repositorio.ClienteRepository
import repositorio.FornecedorRepository
import repositorio.FuncionarioRepository
import repositorio.OrdemServicoRepository
import repositorio.ProdutoRepository
import repositorio.VendaRepository
import servico.OperacoesComerciais
import util.Entrada
import util.Formato
import java.math.BigDecimal

/**
 * Submenu de OPERACOES: as acoes que mexem em estoque e/ou caixa.
 * Aqui o menu so PERGUNTA os dados; quem aplica as regras e as transacoes e o
 * [servico.OperacoesComerciais]. Toda operacao pergunta o "responsavel pela transacao".
 */
object MenuOperacoes {

    fun abrir() {
        while (true) {
            println(
                """
                |
                |--- OPERACOES ---
                |1 - Compra de fornecedor (entra estoque / sai caixa)
                |2 - Venda para cliente (sai estoque / entra caixa)
                |3 - Abrir ordem de servico (instalacao/manutencao)
                |4 - Concluir ordem de servico (entra caixa)
                |5 - Pagar funcionario (sai caixa)
                |6 - Listar vendas
                |7 - Listar ordens de servico
                |0 - Voltar
                """.trimMargin()
            )
            when (Entrada.lerTexto("Opcao")) {
                "1" -> executarSeguro(::compra)
                "2" -> executarSeguro(::venda)
                "3" -> executarSeguro(::abrirServico)
                "4" -> executarSeguro(::concluirServico)
                "5" -> executarSeguro(::pagarFuncionario)
                "6" -> executarSeguro(::listarVendas)
                "7" -> executarSeguro(::listarOrdens)
                "0" -> return
                else -> println("Opcao invalida.")
            }
        }
    }

    /** Pergunta o responsavel pela transacao a cada operacao (requisito). */
    private fun escolherResponsavel(): Funcionario {
        val funcionarios = FuncionarioRepository.listar()
        return Entrada.escolherDaLista("Responsavel pela transacao:", funcionarios) {
            "${it.nomeCompleto} (${it.setorNome})"
        }
    }

    private fun compra() {
        val fornecedor = Entrada.escolherDaLista("Fornecedor:", FornecedorRepository.listar()) { it.toString() }
        val produto = Entrada.escolherDaLista("Produto:", ProdutoRepository.listar()) { it.toString() }
        val quantidade = Entrada.lerInteiro("Quantidade", min = 1)
        val valorUnitario = Entrada.lerBigDecimal("Valor unitario de compra", BigDecimal("0.01"))
        val responsavel = escolherResponsavel()

        OperacoesComerciais.realizarCompra(fornecedor, produto, quantidade, valorUnitario, responsavel)
        println("Compra registrada. Estoque abastecido e saida de caixa lancada.")
    }

    private fun venda() {
        val cliente = Entrada.escolherDaLista("Cliente:", ClienteRepository.listar()) { it.toString() }
        val produto = Entrada.escolherDaLista("Produto:", ProdutoRepository.listar()) { it.toString() }
        val quantidade = Entrada.lerInteiro("Quantidade", min = 1)
        val pagoAgora = Entrada.confirmar("Pagamento a vista agora")
        val responsavel = escolherResponsavel()

        OperacoesComerciais.realizarVenda(cliente, produto, quantidade, responsavel, pagoAgora)
        if (pagoAgora) {
            println("Venda registrada e paga. Entrada de caixa lancada.")
        } else {
            println("Venda registrada no fiado. Cliente marcado com dividas em aberto (sem entrada de caixa).")
        }
    }

    private fun abrirServico() {
        val cliente = Entrada.escolherDaLista("Cliente:", ClienteRepository.listar()) { it.toString() }
        val instaladores = FuncionarioRepository.listar()
        val instalador = Entrada.escolherDaLista("Instalador responsavel:", instaladores) {
            "${it.nomeCompleto} (${it.setorNome} / ${it.habilidade})"
        }
        val tipo = Entrada.escolherEnum("Tipo de servico:", TipoServico.values())
        val descricao = Entrada.lerTexto("Descricao do servico")
        val valor = Entrada.lerBigDecimal("Valor do servico", BigDecimal("0.01"))
        val responsavel = escolherResponsavel()

        val id = OperacoesComerciais.abrirOrdemServico(cliente, instalador, tipo, descricao, valor, responsavel)
        println("Ordem de servico #$id aberta. Conclua depois para receber o pagamento.")
    }

    private fun concluirServico() {
        val abertas = OrdemServicoRepository.listarAbertas()
        val os = Entrada.escolherDaLista("Ordens de servico abertas:", abertas) {
            "#${it.id} ${it.tipo} - ${it.descricao} - ${Formato.dinheiro(it.valor)}"
        }
        val responsavel = escolherResponsavel()

        OperacoesComerciais.concluirOrdemServico(os.id!!, responsavel)
        println("OS #${os.id} concluida. Entrada de caixa lancada.")
    }

    private fun pagarFuncionario() {
        val funcionario = Entrada.escolherDaLista("Funcionario a pagar:", FuncionarioRepository.listar()) {
            "${it.nomeCompleto} - salario ${Formato.dinheiro(it.salario)}"
        }
        val competencia = Entrada.lerCompetencia()
        val responsavel = escolherResponsavel()

        OperacoesComerciais.pagarFuncionario(funcionario, competencia, responsavel)
        println("Pagamento de ${Formato.dinheiro(funcionario.salario)} para ${funcionario.nomeCompleto} lancado.")
    }

    private fun listarVendas() {
        val vendas = VendaRepository.listar()
        if (vendas.isEmpty()) {
            println("Nenhuma venda registrada.")
            return
        }
        vendas.forEach { v ->
            val situacao = if (v.pago) "PAGA" else "EM ABERTO"
            println(
                "#${v.id} ${Formato.dataHora(v.dataHora)} | cliente #${v.clienteId} | produto #${v.produtoId} " +
                    "| ${v.quantidade} un. | ${Formato.dinheiro(v.valorTotal)} | $situacao"
            )
        }
    }

    private fun listarOrdens() {
        val ordens = OrdemServicoRepository.listarTodas()
        if (ordens.isEmpty()) {
            println("Nenhuma ordem de servico registrada.")
            return
        }
        ordens.forEach { os ->
            println(
                "#${os.id} ${Formato.dataHora(os.dataHora)} | ${os.tipo} | cliente #${os.clienteId} " +
                    "| instalador #${os.instaladorId} | ${Formato.dinheiro(os.valor)} | ${os.status}"
            )
        }
    }
}
