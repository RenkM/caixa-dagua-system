package repositorio

import enums.TipoServico
import models.operacoes.OrdemServico
import servico.RegistroNaoEncontradoException
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp

/** Acesso ao banco para a tabela `ordem_servico`. */
object OrdemServicoRepository {

    /** Abre a OS (status ABERTA, sem dinheiro). Nao precisa de transacao. */
    fun inserir(os: OrdemServico): Int {
        val sql = """
            INSERT INTO ordem_servico (cliente_id, instalador_id, tipo, descricao, valor, status,
                                       data_hora, responsavel_id, movimentacao_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)
        """
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, os.clienteId)
                stmt.setInt(2, os.instaladorId)
                stmt.setString(3, os.tipo.name)
                stmt.setString(4, os.descricao)
                stmt.setBigDecimal(5, os.valor)
                stmt.setString(6, OrdemServico.ABERTA)
                stmt.setTimestamp(7, Timestamp.valueOf(os.dataHora))
                stmt.setInt(8, os.responsavelId)
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    rs.next()
                    return rs.getInt(1)
                }
            }
        }
    }

    fun listarAbertas(): List<OrdemServico> =
        consultar("SELECT * FROM ordem_servico WHERE status = '${OrdemServico.ABERTA}' ORDER BY data_hora")

    fun listarTodas(): List<OrdemServico> =
        consultar("SELECT * FROM ordem_servico ORDER BY data_hora DESC, id DESC")

    fun buscar(conn: Connection, id: Int): OrdemServico? {
        conn.prepareStatement("SELECT * FROM ordem_servico WHERE id = ?").use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs -> return if (rs.next()) mapear(rs) else null }
        }
    }

    /** Marca a OS como CONCLUIDA e amarra a movimentacao de entrada. Roda na transacao da conclusao. */
    fun concluir(conn: Connection, osId: Int, movimentacaoId: Int) {
        conn.prepareStatement(
            "UPDATE ordem_servico SET status = '${OrdemServico.CONCLUIDA}', movimentacao_id = ? WHERE id = ?"
        ).use { stmt ->
            stmt.setInt(1, movimentacaoId)
            stmt.setInt(2, osId)
            if (stmt.executeUpdate() == 0) throw RegistroNaoEncontradoException("Ordem de servico #$osId")
        }
    }

    private fun consultar(sql: String): List<OrdemServico> {
        val lista = mutableListOf<OrdemServico>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs -> while (rs.next()) lista.add(mapear(rs)) }
            }
        }
        return lista
    }

    private fun mapear(rs: ResultSet): OrdemServico {
        val movId = rs.getInt("movimentacao_id").let { if (rs.wasNull()) null else it }
        return OrdemServico(
            id = rs.getInt("id"),
            clienteId = rs.getInt("cliente_id"),
            instaladorId = rs.getInt("instalador_id"),
            tipo = TipoServico.valueOf(rs.getString("tipo")),
            descricao = rs.getString("descricao"),
            valor = rs.getBigDecimal("valor"),
            status = rs.getString("status"),
            dataHora = rs.getTimestamp("data_hora").toLocalDateTime(),
            responsavelId = rs.getInt("responsavel_id"),
            movimentacaoId = movId
        )
    }
}
