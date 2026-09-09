package repositorio

import java.math.BigDecimal
import java.sql.Connection

/**
 * Acesso a linha unica da tabela `caixa` (id = 1), onde fica o saldo consolidado.
 * As alteracoes de saldo passam sempre pelo [financeiro.Caixa], dentro de uma transacao.
 */
object CaixaRepository {

    /** Le o saldo atual (dentro da transacao da operacao). */
    fun buscarSaldo(conn: Connection): BigDecimal {
        conn.prepareStatement("SELECT saldo FROM caixa WHERE id = 1").use { stmt ->
            stmt.executeQuery().use { rs ->
                check(rs.next()) { "Tabela caixa sem a linha inicial. Rode o seed de BANCO_DE_DADOS.md." }
                return rs.getBigDecimal("saldo")
            }
        }
    }

    /** Grava o novo saldo (chamado pelo Caixa depois de somar/subtrair). */
    fun atualizarSaldo(conn: Connection, novoSaldo: BigDecimal) {
        conn.prepareStatement("UPDATE caixa SET saldo = ? WHERE id = 1").use { stmt ->
            stmt.setBigDecimal(1, novoSaldo)
            stmt.executeUpdate()
        }
    }

    /** Consulta simples fora de transacao, para o menu financeiro. */
    fun saldoAtual(): BigDecimal {
        Conexao.abrir().use { conn -> return buscarSaldo(conn) }
    }
}
