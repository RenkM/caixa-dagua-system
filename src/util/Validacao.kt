package util

import java.math.BigDecimal

/**
 * Validacoes de formato SEM regex - tudo com checagem manual + try/catch.
 *
 * Cada funcao devolve o valor ja "normalizado" (limpo/padronizado) ou lanca
 * IllegalArgumentException com uma mensagem clara. Quem chama (util/Entrada.kt) captura
 * a excecao e pede o dado de novo.
 *
 * `require(condicao) { "mensagem" }` = se a condicao for falsa, lanca IllegalArgumentException
 * com aquela mensagem. E o jeito curto de validar em Kotlin.
 */
object Validacao {

    /** Texto que nao pode ficar vazio. Devolve sem espacos nas pontas. */
    fun textoObrigatorio(valor: String, campo: String): String {
        val limpo = valor.trim()
        require(limpo.isNotEmpty()) { "$campo nao pode ficar em branco." }
        return limpo
    }

    fun idade(valor: Int): Int {
        require(valor in 0..130) { "Idade fora do intervalo permitido (0 a 130)." }
        return valor
    }

    /** Aceita zero ou positivo (ex.: preco, salario). */
    fun valorNaoNegativo(valor: BigDecimal, campo: String): BigDecimal {
        require(valor >= BigDecimal.ZERO) { "$campo nao pode ser negativo." }
        return valor
    }

    /** Exige maior que zero (ex.: valor de uma movimentacao). */
    fun valorPositivo(valor: BigDecimal, campo: String): BigDecimal {
        require(valor > BigDecimal.ZERO) { "$campo deve ser maior que zero." }
        return valor
    }

    /**
     * CPF: aceita com ou sem mascara. Tira tudo que nao e digito, exige 11 numeros
     * e devolve no formato 000.000.000-00 (nao valida os digitos verificadores - fora do escopo).
     */
    fun cpf(entrada: String): String {
        val digitos = entrada.filter { it.isDigit() }
        require(digitos.length == 11) { "CPF invalido: informe 11 digitos (ex.: 000.000.000-00)." }
        return "${digitos.substring(0, 3)}.${digitos.substring(3, 6)}.${digitos.substring(6, 9)}-${digitos.substring(9)}"
    }

    /** CNPJ -> devolve formatado 00.000.000/0000-00. */
    fun cnpj(entrada: String): String {
        val d = entrada.filter { it.isDigit() }
        require(d.length == 14) { "CNPJ invalido: informe 14 digitos (ex.: 00.000.000/0000-00)." }
        return "${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}"
    }

    /** RG -> apenas exige algo entre 5 e 20 caracteres (formatos variam por estado). */
    fun rg(entrada: String): String {
        val limpo = entrada.trim()
        require(limpo.length in 5..20) { "RG invalido: informe entre 5 e 20 caracteres." }
        return limpo
    }

    /** E-mail simples: exatamente um @, com texto antes e um ponto depois. */
    fun email(entrada: String): String {
        val e = entrada.trim()
        val partes = e.split("@")
        require(partes.size == 2 && partes[0].isNotEmpty()) { "E-mail invalido: falta o nome antes do @." }
        val dominio = partes[1]
        require(dominio.contains(".") && !dominio.startsWith(".") && !dominio.endsWith(".")) {
            "E-mail invalido: dominio mal formado (ex.: nome@empresa.com)."
        }
        return e
    }

    /** Telefone -> devolve so os digitos (10 ou 11). */
    fun telefone(entrada: String): String {
        val d = entrada.filter { it.isDigit() }
        require(d.length == 10 || d.length == 11) { "Telefone invalido: informe DDD + numero (10 ou 11 digitos)." }
        return d
    }

    /**
     * Competencia da folha no formato AAAA-MM (ex.: "2026-08").
     * Quebra no "-", converte ano e mes com try/catch e confere os limites.
     */
    fun competencia(entrada: String): String {
        val e = entrada.trim()
        val partes = e.split("-")
        require(partes.size == 2) { "Competencia invalida: use AAAA-MM (ex.: 2026-08)." }
        val ano: Int
        val mes: Int
        try {
            ano = partes[0].toInt()
            mes = partes[1].toInt()
        } catch (ex: NumberFormatException) {   // "abc".toInt() cai aqui
            throw IllegalArgumentException("Competencia invalida: AAAA e MM precisam ser numeros.")
        }
        require(ano in 2000..2100) { "Competencia invalida: ano fora do intervalo (2000-2100)." }
        require(mes in 1..12) { "Competencia invalida: mes precisa estar entre 01 e 12." }
        return "%04d-%02d".format(ano, mes)
    }
}
