package repositorio

import models.operacoes.Venda
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp

/** Acesso ao banco para a tabela `venda`. */
object VendaRepository {

    /** Grava a venda na transacao em andamento. movimentacao_id vai NULL quando e no fiado. */
    fun inserir(conn: Connection, v: Venda): Int {
        val sql = """
            INSERT INTO venda (cliente_id, produto_id, quantidade, valor_unitario, valor_total, pago,
                               data_hora, responsavel_id, movimentacao_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setInt(1, v.clienteId)
            stmt.setInt(2, v.produtoId)
            stmt.setInt(3, v.quantidade)
            stmt.setBigDecimal(4, v.valorUnitario)
            stmt.setBigDecimal(5, v.valorTotal)
            stmt.setBoolean(6, v.pago)
            stmt.setTimestamp(7, Timestamp.valueOf(v.dataHora))
            stmt.setInt(8, v.responsavelId)
            if (v.movimentacaoId != null) stmt.setInt(9, v.movimentacaoId!!) else stmt.setNull(9, java.sql.Types.INTEGER)
            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                rs.next()
                return rs.getInt(1)
            }
        }
    }

    fun listar(): List<Venda> {
        val lista = mutableListOf<Venda>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement("SELECT * FROM venda ORDER BY data_hora DESC, id DESC").use { stmt ->
                stmt.executeQuery().use { rs -> while (rs.next()) lista.add(mapear(rs)) }
            }
        }
        return lista
    }

    private fun mapear(rs: ResultSet): Venda {
        // getInt devolve 0 quando a coluna e NULL; wasNull() diz se foi realmente NULL.
        val movId = rs.getInt("movimentacao_id").let { if (rs.wasNull()) null else it }
        return Venda(
            id = rs.getInt("id"),
            clienteId = rs.getInt("cliente_id"),
            produtoId = rs.getInt("produto_id"),
            quantidade = rs.getInt("quantidade"),
            valorUnitario = rs.getBigDecimal("valor_unitario"),
            valorTotal = rs.getBigDecimal("valor_total"),
            pago = rs.getBoolean("pago"),
            dataHora = rs.getTimestamp("data_hora").toLocalDateTime(),
            responsavelId = rs.getInt("responsavel_id"),
            movimentacaoId = movId
        )
    }
}
