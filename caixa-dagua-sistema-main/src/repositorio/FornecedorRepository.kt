package repositorio

import models.pessoas.Fornecedor
import java.sql.ResultSet
import java.sql.Statement

/** Acesso ao banco para a tabela `fornecedor` (telefone e email podem ser null). */
object FornecedorRepository {

    fun inserir(f: Fornecedor): Int {
        val sql = "INSERT INTO fornecedor (nome_fantasia, cnpj, telefone, email) VALUES (?, ?, ?, ?)"
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, f.nomeFantasia)
                stmt.setString(2, f.cnpj)
                stmt.setString(3, f.telefone)   // aceita null
                stmt.setString(4, f.email)      // aceita null
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    rs.next()
                    return rs.getInt(1)
                }
            }
        }
    }

    fun listar(): List<Fornecedor> {
        val lista = mutableListOf<Fornecedor>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement("SELECT * FROM fornecedor ORDER BY nome_fantasia").use { stmt ->
                stmt.executeQuery().use { rs -> while (rs.next()) lista.add(mapear(rs)) }
            }
        }
        return lista
    }

    fun buscar(id: Int): Fornecedor? {
        Conexao.abrir().use { conn ->
            conn.prepareStatement("SELECT * FROM fornecedor WHERE id = ?").use { stmt ->
                stmt.setInt(1, id)
                stmt.executeQuery().use { rs -> return if (rs.next()) mapear(rs) else null }
            }
        }
    }

    private fun mapear(rs: ResultSet) = Fornecedor(
        id = rs.getInt("id"),
        nomeFantasia = rs.getString("nome_fantasia"),
        cnpj = rs.getString("cnpj"),
        telefone = rs.getString("telefone"),
        email = rs.getString("email")
    )
}
