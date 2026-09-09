package repositorio

import models.Setor

/**
 * "Repositorio" = classe que conversa com o banco para UMA tabela (aqui, `setor`).
 * O resto do sistema chama estes metodos e nunca escreve SQL direto.
 *
 * Padrao usado em todos os repositorios:
 *   Conexao.abrir().use { conn ->            // abre conexao e fecha sozinho no fim
 *       conn.prepareStatement(sql).use { st ->// PreparedStatement evita SQL injection
 *           st.executeQuery().use { rs -> ... }
 *       }
 *   }
 */
object SetorRepository {

    /** Devolve todos os setores cadastrados (usado nos menus para escolher um setor). */
    fun listar(): List<Setor> {
        val sql = "SELECT id, nome FROM setor ORDER BY nome"
        val lista = mutableListOf<Setor>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {                       // rs.next() anda de linha em linha
                        lista.add(Setor(rs.getInt("id"), rs.getString("nome")))
                    }
                }
            }
        }
        return lista
    }
}
