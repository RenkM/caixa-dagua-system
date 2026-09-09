package models.pessoas

import enums.Habilidade
import enums.Turno
import java.math.BigDecimal

/**
 * Funcionario da empresa. Sempre pertence a um setor (Financeiro, Administrativo,
 * Logistica, Instalacao...). O "instalador" e so um funcionario do setor INSTALACAO.
 *
 * ENCAPSULAMENTO: o [salario] tem `private set`. De fora da classe da pra LER o salario,
 * mas nao da pra escrever direto (`func.salario = 5000` nao compila). A unica forma de
 * mudar e chamar [reajustarSalario], que valida o valor. Isso evita alteracao acidental
 * de dinheiro espalhada pelo codigo.
 */
class Funcionario(
    id: Int?,
    nome: String,
    sobrenome: String,
    cpf: String,
    rg: String,
    idade: Int,
    salario: BigDecimal,
    var turno: Turno,
    var habilidade: Habilidade,
    var setorId: Int,             // chave estrangeira para a tabela setor
    var setorNome: String = ""    // nome do setor, so para exibir (vem de um JOIN)
) : Pessoa(id, nome, sobrenome, cpf, rg, idade) {

    var salario: BigDecimal = salario
        private set

    /** Unico jeito de alterar o salario. Rejeita valor negativo. */
    fun reajustarSalario(novoSalario: BigDecimal) {
        require(novoSalario >= BigDecimal.ZERO) { "Salario nao pode ser negativo." }
        salario = novoSalario
    }

    override fun toString(): String =
        "#${id ?: "-"} $nomeCompleto | setor: $setorNome | salario: $salario"
}
