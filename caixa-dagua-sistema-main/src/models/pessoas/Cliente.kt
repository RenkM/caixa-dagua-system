package models.pessoas

/**
 * Cliente que compra produtos ou contrata servicos.
 * [dividasAbertas] fica true quando uma venda e feita sem pagamento a vista.
 */
class Cliente(
    id: Int?,
    nome: String,
    sobrenome: String,
    cpf: String,
    rg: String,
    idade: Int,
    var dividasAbertas: Boolean = false
) : Pessoa(id, nome, sobrenome, cpf, rg, idade) {

    override fun toString(): String =
        "#${id ?: "-"} $nomeCompleto" + if (dividasAbertas) " (possui dividas em aberto)" else ""
}
