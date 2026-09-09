package repositorio

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Ponto unico de configuracao do banco.
 *
 * ================== EDITE AQUI ==================
 * Ajuste URL / USUARIO / SENHA conforme a sua instalacao do PostgreSQL.
 * O banco (database) precisa existir antes de rodar o sistema:
 *     CREATE DATABASE caixa_da_agua;
 * e as tabelas devem ser criadas com o script de BANCO_DE_DADOS.md.
 * ===============================================
 */
object Conexao {

    private const val URL = "jdbc:postgresql://localhost:5432/caixa_da_agua"
    private const val USUARIO = "postgres"
    private const val SENHA = "postgres"

    init {
        try {
            // Registra o driver do PostgreSQL (opcional em versoes novas, mas garante erro claro).
            Class.forName("org.postgresql.Driver")
        } catch (e: ClassNotFoundException) {
            println("[Conexao] Driver PostgreSQL nao encontrado. Confira a biblioteca postgresql-42.7.12.jar no modulo.")
        }
    }

    /** Abre uma nova conexao. Quem chama e responsavel por fechar (use .use { } sempre que possivel). */
    fun abrir(): Connection {
        try {
            return DriverManager.getConnection(URL, USUARIO, SENHA)
        } catch (e: SQLException) {
            throw IllegalStateException(
                "Nao foi possivel conectar ao banco em $URL. " +
                    "Verifique se o PostgreSQL esta rodando e se usuario/senha em Conexao.kt estao corretos.",
                e
            )
        }
    }

    /** Testa a conexao no boot do sistema e devolve true/false. */
    fun testar(): Boolean {
        return try {
            abrir().use { true }
        } catch (e: Exception) {
            println("[Conexao] ${e.message}")
            false
        }
    }
}
