package models.produtos

import enums.Cor
import enums.Material
import java.math.BigDecimal

/**
 * Produto do catalogo: uma caixa d'agua. O estoque ([quantidadeEstoque]) sobe nas compras
 * e desce nas vendas. As dimensoes viram tres colunas simples no banco (nada de array).
 */
class CaixaDaAgua(
    var id: Int?,
    var marca: String,
    var modelo: String,
    var cor: Cor,
    var material: Material,
    var formato: String,
    var largura: Double,
    var altura: Double,
    var profundidade: Double,
    var precoVenda: BigDecimal,
    var quantidadeEstoque: Int = 0
) {
    val dimensaoFormatada: String
        get() = "%.2f x %.2f x %.2f".format(largura, altura, profundidade)

    override fun toString(): String =
        "#${id ?: "-"} $marca $modelo | $cor / $material / $formato | " +
            "dim: $dimensaoFormatada | preco: $precoVenda | estoque: $quantidadeEstoque"
}
