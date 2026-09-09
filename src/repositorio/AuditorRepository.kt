package repositorio

import models.pessoas.Auditor
import java.sql.ResultSet
import java.sql.Statement

/** Acesso ao banco para a tabela `auditor`. */
object AuditorRepository {

    fun inserir(a: Auditor): Int {
        val sql = "INSERT INTO auditor (nome, sobrenome, cpf, rg, idade, registro, orgao) VALUES (?, ?, ?, ?, ?, ?, ?)"
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, a.nome)
                stmt.setString(2, a.sobrenome)
                stmt.setString(3, a.cpf)
                stmt.setString(4, a.rg)
                stmt.setInt(5, a.idade)
                stmt.setString(6, a.registro)
                stmt.setString(7, a.orgao)
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    rs.next()
                    return rs.getInt(1)
                }
            }
        }
    }

    fun listar(): List<Auditor> {
        val lista = mutableListOf<Auditor>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement("SELECT * FROM auditor ORDER BY nome").use { stmt ->
                stmt.executeQuery().use { rs -> while (rs.next()) lista.add(mapear(rs)) }
            }
        }
        return lista
    }

    private fun mapear(rs: ResultSet) = Auditor(
        id = rs.getInt("id"),
        nome = rs.getString("nome"),
        sobrenome = rs.getString("sobrenome"),
        cpf = rs.getString("cpf"),
        rg = rs.getString("rg"),
        idade = rs.getInt("idade"),
        registro = rs.getString("registro"),
        orgao = rs.getString("orgao")
    )
}
