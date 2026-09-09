package repositorio

import models.operacoes.PagamentoFuncionario
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp

/** Acesso ao banco para a tabela `pagamento_funcionario` (a folha). */
object PagamentoFuncionarioRepository {

    /** Grava o pagamento na transacao em andamento (junto com a saida de caixa). */
    fun inserir(conn: Connection, p: PagamentoFuncionario): Int {
        val sql = """
            INSERT INTO pagamento_funcionario (funcionario_id, valor, competencia, data_hora,
                                               responsavel_id, movimentacao_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setInt(1, p.funcionarioId)
            stmt.setBigDecimal(2, p.valor)
            stmt.setString(3, p.competencia)
            stmt.setTimestamp(4, Timestamp.valueOf(p.dataHora))
            stmt.setInt(5, p.responsavelId)
            stmt.setInt(6, p.movimentacaoId)
            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                rs.next()
                return rs.getInt(1)
            }
        }
    }
}
