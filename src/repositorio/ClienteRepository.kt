package repositorio

import models.pessoas.Cliente
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/** Acesso ao banco para a tabela `cliente`. */
object ClienteRepository {

    /** Cadastra um cliente e devolve o id gerado. */
    fun inserir(c: Cliente): Int {
        val sql = """
            INSERT INTO cliente (nome, sobrenome, cpf, rg, idade, dividas_abertas)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, c.nome)
                stmt.setString(2, c.sobrenome)
                stmt.setString(3, c.cpf)
                stmt.setString(4, c.rg)
                stmt.setInt(5, c.idade)
                stmt.setBoolean(6, c.dividasAbertas)
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    rs.next()
                    return rs.getInt(1)
                }
            }
        }
    }

    fun listar(): List<Cliente> {
        val lista = mutableListOf<Cliente>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement("SELECT * FROM cliente ORDER BY nome").use { stmt ->
                stmt.executeQuery().use { rs -> while (rs.next()) lista.add(mapear(rs)) }
            }
        }
        return lista
    }

    /** Busca por id. Devolve null se nao existir (o chamador trata com ?: ). */
    fun buscar(id: Int): Cliente? {
        Conexao.abrir().use { conn ->
            conn.prepareStatement("SELECT * FROM cliente WHERE id = ?").use { stmt ->
                stmt.setInt(1, id)
                stmt.executeQuery().use { rs -> return if (rs.next()) mapear(rs) else null }
            }
        }
    }

    /**
     * Liga/desliga a marca "tem dividas em aberto". Recebe a `conn` da transacao em
     * andamento (chamado dentro da venda no fiado), para tudo commitar/rollar junto.
     */
    fun marcarDividas(conn: Connection, clienteId: Int, possuiDividas: Boolean) {
        conn.prepareStatement("UPDATE cliente SET dividas_abertas = ? WHERE id = ?").use { stmt ->
            stmt.setBoolean(1, possuiDividas)
            stmt.setInt(2, clienteId)
            stmt.executeUpdate()
        }
    }

    // Linha do banco -> objeto Cliente.
    private fun mapear(rs: ResultSet) = Cliente(
        id = rs.getInt("id"),
        nome = rs.getString("nome"),
        sobrenome = rs.getString("sobrenome"),
        cpf = rs.getString("cpf"),
        rg = rs.getString("rg"),
        idade = rs.getInt("idade"),
        dividasAbertas = rs.getBoolean("dividas_abertas")
    )
}
