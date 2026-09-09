package models.pessoas

/**
 * Auditor externo que fiscaliza as operacoes e o fluxo de caixa.
 * [registro] e o numero do conselho/registro profissional; [orgao] o orgao emissor.
 */
class Auditor(
    id: Int?,
    nome: String,
    sobrenome: String,
    cpf: String,
    rg: String,
    idade: Int,
    var registro: String,
    var orgao: String
) : Pessoa(id, nome, sobrenome, cpf, rg, idade) {

    override fun toString(): String =
        "#${id ?: "-"} $nomeCompleto | registro: $registro ($orgao)"
}
