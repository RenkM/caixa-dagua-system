package enums

/**
 * Cores disponiveis para uma caixa d'agua.
 *
 * Usar enum (em vez de texto livre) evita erro de digitacao: so existem estes 4 valores,
 * tanto no codigo quanto no banco (a coluna `cor` tem CHECK com os mesmos nomes).
 */
enum class Cor {
    AZUL_ESCURO,
    AZUL_CLARO,
    CINZA,
    BRANCO
}
