package util

import java.math.BigDecimal

/**
 * Leitura de dados do console a prova de erro humano.
 *
 * Padrao de todos os metodos: mostra o rotulo -> le a linha -> tenta converter/validar
 * dentro de try/catch -> se der errado, avisa e pergunta DE NOVO (loop `while (true)`).
 * Nenhuma excecao de digitacao escapa daqui: o usuario nao consegue derrubar o programa.
 */
object Entrada {

    // readLine() devolve String? (pode ser null se a entrada acabar). "?: \"\"" troca null por vazio.
    private fun ler(): String = readLine()?.trim() ?: ""

    private fun avisar(e: Throwable) {
        println("  -> entrada invalida (${e.message ?: "formato incorreto"}); tente de novo.")
    }

    /** Texto obrigatorio (repete ate o usuario digitar algo). */
    fun lerTexto(rotulo: String): String {
        while (true) {
            print("$rotulo: ")
            try {
                return Validacao.textoObrigatorio(ler(), rotulo)
            } catch (e: Exception) {
                avisar(e)
            }
        }
    }

    /** Texto que pode ficar vazio: devolve null se o usuario so apertar Enter. */
    fun lerTextoOpcional(rotulo: String): String? {
        print("$rotulo (Enter para pular): ")
        return ler().ifBlank { null }
    }

    /** Numero inteiro, opcionalmente dentro de um intervalo [min, max]. */
    fun lerInteiro(rotulo: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
        while (true) {
            print("$rotulo: ")
            val texto = ler()
            try {
                val n = texto.toInt()               // "abc".toInt() lanca NumberFormatException
                require(n in min..max) { "informe um numero entre $min e $max" }
                return n
            } catch (e: NumberFormatException) {
                avisar(IllegalArgumentException("nao e um numero inteiro"))
            } catch (e: Exception) {
                avisar(e)
            }
        }
    }

    /** Numero com casas decimais (dinheiro/medidas). Aceita virgula ou ponto. */
    fun lerBigDecimal(rotulo: String, minimo: BigDecimal? = null): BigDecimal {
        while (true) {
            print("$rotulo: ")
            val texto = ler().replace(",", ".")   // "12,50" -> "12.50"
            try {
                val valor = texto.toBigDecimal()
                if (minimo != null) require(valor >= minimo) { "valor minimo e $minimo" }
                return valor
            } catch (e: NumberFormatException) {
                avisar(IllegalArgumentException("nao e um numero valido (ex.: 1250.00)"))
            } catch (e: Exception) {
                avisar(e)
            }
        }
    }

    /** Pergunta sim/nao. */
    fun confirmar(rotulo: String): Boolean {
        while (true) {
            print("$rotulo (s/n): ")
            when (ler().lowercase()) {
                "s", "sim" -> return true
                "n", "nao", "não" -> return false
                else -> avisar(IllegalArgumentException("responda com s ou n"))
            }
        }
    }

    // Campos com formato especifico: delegam a validacao para o util/Validacao.
    // "Validacao::cpf" e uma referencia de funcao (passa a funcao como argumento).
    fun lerCpf(rotulo: String = "CPF"): String = lerComValidador(rotulo, Validacao::cpf)
    fun lerCnpj(rotulo: String = "CNPJ"): String = lerComValidador(rotulo, Validacao::cnpj)
    fun lerRg(rotulo: String = "RG"): String = lerComValidador(rotulo, Validacao::rg)
    fun lerCompetencia(rotulo: String = "Competencia (AAAA-MM)"): String =
        lerComValidador(rotulo, Validacao::competencia)

    fun lerEmailOpcional(rotulo: String = "E-mail"): String? =
        lerComValidadorOpcional(rotulo, Validacao::email)

    fun lerTelefoneOpcional(rotulo: String = "Telefone"): String? =
        lerComValidadorOpcional(rotulo, Validacao::telefone)

    fun lerIdade(rotulo: String = "Idade"): Int {
        while (true) {
            val n = lerInteiro(rotulo)
            try {
                return Validacao.idade(n)
            } catch (e: Exception) {
                avisar(e)
            }
        }
    }

    /**
     * Mostra uma lista numerada (1, 2, 3...) e devolve o item que o usuario escolher.
     * `descricao` diz como mostrar cada item. Lanca se a lista estiver vazia (ex.: nenhum
     * cliente cadastrado ainda) - o menu captura e avisa.
     * <T> = funciona para qualquer tipo (Cliente, Produto, Funcionario...).
     */
    fun <T> escolherDaLista(titulo: String, itens: List<T>, descricao: (T) -> String): T {
        check(itens.isNotEmpty()) { "Nenhum registro disponivel em \"$titulo\". Cadastre antes de continuar." }
        println(titulo)
        itens.forEachIndexed { i, item -> println("  ${i + 1} - ${descricao(item)}") }
        val escolha = lerInteiro("Numero", min = 1, max = itens.size)
        return itens[escolha - 1]   // usuario ve 1..N; a lista comeca no indice 0
    }

    /** Igual ao anterior, mas para os valores de um enum (Cor, Turno, TipoServico...). */
    fun <T : Enum<T>> escolherEnum(titulo: String, valores: Array<T>): T {
        println(titulo)
        valores.forEachIndexed { i, v -> println("  ${i + 1} - ${v.name}") }
        val escolha = lerInteiro("Numero", min = 1, max = valores.size)
        return valores[escolha - 1]
    }

    // Le texto e passa pelo `validador`; repete enquanto ele reclamar.
    private fun lerComValidador(rotulo: String, validador: (String) -> String): String {
        while (true) {
            print("$rotulo: ")
            try {
                return validador(ler())
            } catch (e: Exception) {
                avisar(e)
            }
        }
    }

    // Igual, mas Enter vazio = null (campo opcional).
    private fun lerComValidadorOpcional(rotulo: String, validador: (String) -> String): String? {
        while (true) {
            print("$rotulo (Enter para pular): ")
            val texto = ler()
            if (texto.isBlank()) return null
            try {
                return validador(texto)
            } catch (e: Exception) {
                avisar(e)
            }
        }
    }
}
