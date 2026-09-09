package repositorio

import enums.TipoMovimentacao
import enums.TipoOperacao
import financeiro.MovimentacaoFinanceira
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * Acesso ao banco para a tabela `movimentacao_financeira` (o historico do caixa).
 * So o [financeiro.Caixa] chama o `inserir`; os menus usam so as consultas.
 */
object MovimentacaoRepository {

    /** Insere na transacao em andamento e devolve a movimentacao ja com id e data/hora. */
    fun inserir(
        conn: Connection,
        valor: BigDecimal,
        tipo: TipoMovimentacao,
        operacao: TipoOperacao,
        pagador: String,
        recebedor: String,
        descricao: String,
        responsavelId: Int
    ): MovimentacaoFinanceira {
        val agora = LocalDateTime.now()
        val sql = """
            INSERT INTO movimentacao_financeira
                (valor, tipo, operacao, pagador, recebedor, data_hora, descricao, responsavel_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setBigDecimal(1, valor)
            stmt.setString(2, tipo.name)
            stmt.setString(3, operacao.name)
            stmt.setString(4, pagador)
            stmt.setString(5, recebedor)
            stmt.setTimestamp(6, Timestamp.valueOf(agora))
            stmt.setString(7, descricao)
            stmt.setInt(8, responsavelId)
            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                rs.next()
                val id = rs.getInt(1)
                return MovimentacaoFinanceira(id, valor, tipo, operacao, pagador, recebedor, agora, descricao, responsavelId)
            }
        }
    }

    /** Todas as movimentacoes, da mais nova para a mais antiga. */
    fun listar(): List<MovimentacaoFinanceira> =
        consultar("SELECT * FROM movimentacao_financeira ORDER BY data_hora DESC, id DESC")

    /** Movimentacoes entre duas datas (usado no extrato mensal). */
    fun listarPorPeriodo(inicio: LocalDateTime, fim: LocalDateTime): List<MovimentacaoFinanceira> =
        consultar(
            "SELECT * FROM movimentacao_financeira WHERE data_hora BETWEEN ? AND ? ORDER BY data_hora DESC, id DESC"
        ) { stmt ->
            stmt.setTimestamp(1, Timestamp.valueOf(inicio))
            stmt.setTimestamp(2, Timestamp.valueOf(fim))
        }

    private fun consultar(
        sql: String,
        parametros: (java.sql.PreparedStatement) -> Unit = {}
    ): List<MovimentacaoFinanceira> {
        val lista = mutableListOf<MovimentacaoFinanceira>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                parametros(stmt)
                stmt.executeQuery().use { rs -> while (rs.next()) lista.add(mapear(rs)) }
            }
        }
        return lista
    }

    private fun mapear(rs: ResultSet) = MovimentacaoFinanceira(
        id = rs.getInt("id"),
        valor = rs.getBigDecimal("valor"),
        tipo = TipoMovimentacao.valueOf(rs.getString("tipo")),
        operacao = TipoOperacao.valueOf(rs.getString("operacao")),
        pagador = rs.getString("pagador"),
        recebedor = rs.getString("recebedor"),
        dataHora = rs.getTimestamp("data_hora").toLocalDateTime(),
        descricao = rs.getString("descricao"),
        responsavelId = rs.getInt("responsavel_id")
    )
}
