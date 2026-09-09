package sistema

import enums.Cor
import enums.Material
import models.produtos.CaixaDaAgua
import repositorio.ProdutoRepository
import servico.RegistroNaoEncontradoException
import util.Entrada
import util.Formato
import java.math.BigDecimal

/**
 * Submenu de PRODUTOS: cadastra/lista/edita caixas d'agua e mostra o estoque.
 * O estoque nao e mexido aqui - ele sobe nas compras e desce nas vendas (ver MenuOperacoes).
 */
object MenuProdutos {

    fun abrir() {
        while (true) {
            println(
                """
                |
                |--- PRODUTOS E ESTOQUE ---
                |1 - Cadastrar caixa d'agua
                |2 - Listar produtos
                |3 - Editar dados / preco de um produto
                |4 - Ver estoque
                |0 - Voltar
                """.trimMargin()
            )
            when (Entrada.lerTexto("Opcao")) {
                "1" -> executarSeguro(::cadastrar)
                "2" -> executarSeguro { ProdutoRepository.listar().forEach(::println) }
                "3" -> executarSeguro(::editar)
                "4" -> executarSeguro(::verEstoque)
                "0" -> return
                else -> println("Opcao invalida.")
            }
        }
    }

    private fun cadastrar() {
        val marca = Entrada.lerTexto("Marca")
        val modelo = Entrada.lerTexto("Modelo")
        val cor = Entrada.escolherEnum("Cor:", Cor.values())
        val material = Entrada.escolherEnum("Material:", Material.values())
        val formato = Entrada.lerTexto("Formato (ex.: cilindrico, retangular)")
        val largura = Entrada.lerBigDecimal("Largura (m)", BigDecimal("0.01")).toDouble()
        val altura = Entrada.lerBigDecimal("Altura (m)", BigDecimal("0.01")).toDouble()
        val profundidade = Entrada.lerBigDecimal("Profundidade (m)", BigDecimal("0.01")).toDouble()
        val preco = Entrada.lerBigDecimal("Preco de venda", BigDecimal.ZERO)

        val id = ProdutoRepository.inserir(
            CaixaDaAgua(null, marca, modelo, cor, material, formato, largura, altura, profundidade, preco, quantidadeEstoque = 0)
        )
        println("Produto #$id cadastrado (estoque inicial 0 - use Operacoes > Compra para abastecer).")
    }

    private fun editar() {
        val produtos = ProdutoRepository.listar()
        val produto = Entrada.escolherDaLista("Escolha o produto:", produtos) { it.toString() }
        val id = produto.id ?: throw RegistroNaoEncontradoException("Produto")

        println("Deixe em branco para manter o valor atual.")
        produto.marca = Entrada.lerTextoOpcional("Marca [${produto.marca}]") ?: produto.marca
        produto.modelo = Entrada.lerTextoOpcional("Modelo [${produto.modelo}]") ?: produto.modelo
        produto.formato = Entrada.lerTextoOpcional("Formato [${produto.formato}]") ?: produto.formato
        if (Entrada.confirmar("Alterar cor (atual ${produto.cor})?")) {
            produto.cor = Entrada.escolherEnum("Cor:", Cor.values())
        }
        if (Entrada.confirmar("Alterar material (atual ${produto.material})?")) {
            produto.material = Entrada.escolherEnum("Material:", Material.values())
        }
        if (Entrada.confirmar("Alterar preco (atual ${Formato.dinheiro(produto.precoVenda)})?")) {
            produto.precoVenda = Entrada.lerBigDecimal("Novo preco de venda", BigDecimal.ZERO)
        }

        produto.id = id
        ProdutoRepository.atualizar(produto)
        println("Produto #$id atualizado.")
    }

    private fun verEstoque() {
        val produtos = ProdutoRepository.listar()
        if (produtos.isEmpty()) {
            println("Nenhum produto cadastrado.")
            return
        }
        println("ESTOQUE ATUAL:")
        produtos.forEach { p ->
            println("  #${p.id} ${p.marca} ${p.modelo} -> ${p.quantidadeEstoque} un. | preco ${Formato.dinheiro(p.precoVenda)}")
        }
    }
}
