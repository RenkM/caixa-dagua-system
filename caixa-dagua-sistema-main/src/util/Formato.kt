package util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatacao para exibicao no console. */
object Formato {

    private val MOEDA: NumberFormat = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"))
    private val DATA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun dinheiro(valor: BigDecimal): String = MOEDA.format(valor)

    fun dataHora(momento: LocalDateTime): String = momento.format(DATA_HORA)
}
