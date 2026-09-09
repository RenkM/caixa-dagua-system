package models

/** Setor da empresa ao qual um funcionario pertence (Financeiro, Logistica, etc.). */
data class Setor(
    val id: Int,
    val nome: String
) {
    override fun toString(): String = "#$id $nome"
}
