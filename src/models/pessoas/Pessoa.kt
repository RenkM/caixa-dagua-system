package models.pessoas

/**
 * Classe base de qualquer pessoa fisica do sistema (funcionario, cliente, auditor).
 * O fornecedor e pessoa juridica (tem CNPJ) e fica em [Fornecedor], fora desta hierarquia.
 *
 * `open` = pode ser herdada. As subclasses reaproveitam nome, cpf, idade, etc.
 */
open class Pessoa(
    var id: Int?,          // null enquanto nao foi salvo; o banco gera o numero (SERIAL)
    var nome: String,
    var sobrenome: String,
    var cpf: String,       // ja validado/formatado como 000.000.000-00 antes de chegar aqui
    var rg: String,
    var idade: Int
) {
    /** Propriedade calculada: junta nome + sobrenome sempre que for lida. */
    val nomeCompleto: String
        get() = "$nome $sobrenome"

    override fun toString(): String = "#${id ?: "-"} $nomeCompleto (CPF $cpf)"
}
