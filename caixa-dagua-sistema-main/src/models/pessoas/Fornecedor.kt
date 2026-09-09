package models.pessoas

/**
 * Fornecedor (pessoa juridica) de onde a empresa compra as caixas d'agua.
 * [telefone] e [email] sao opcionais (nullable).
 */
class Fornecedor(
    var id: Int?,
    var nomeFantasia: String,
    var cnpj: String,
    var telefone: String? = null,
    var email: String? = null
) {
    override fun toString(): String =
        "#${id ?: "-"} $nomeFantasia (CNPJ $cnpj)" +
            (telefone?.let { " | tel: $it" } ?: "") +
            (email?.let { " | $it" } ?: "")
}
