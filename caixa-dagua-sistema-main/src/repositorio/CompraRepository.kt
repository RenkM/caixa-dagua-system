package repositorio

import models.operacoes.Compra
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp

/** Acesso ao banco para a tabela `compra`. So grava (as consultas de compra nao sao usadas nos menus). */
object CompraRepository {

    /** Grava a compra na transacao em andamento e devolve o id gerado. */
    fun inserir(conn: Connection, c: Compra): Int {
        val sql = """
            INSERT INTO compra (fornecedor_id, produto_id, quantidade, valor_unitario, valor_total,
                                data_hora, responsavel_id, movimentacao_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setInt(1, c.fornecedorId)
            stmt.setInt(2, c.produtoId)
            stmt.setInt(3, c.quantidade)
            stmt.setBigDecimal(4, c.valorUnitario)
            stmt.setBigDecimal(5, c.valorTotal)
            stmt.setTimestamp(6, Timestamp.valueOf(c.dataHora))
            stmt.setInt(7, c.responsavelId)
            stmt.setInt(8, c.movimentacaoId)
            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                rs.next()
                return rs.getInt(1)
            }
        }
    }
}
