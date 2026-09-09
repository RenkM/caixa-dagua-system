package repositorio

import enums.Habilidade
import enums.Turno
import models.pessoas.Funcionario
import java.sql.ResultSet

/** Acesso ao banco para a tabela `funcionario` (+ JOIN com `setor` para trazer o nome do setor). */
object FuncionarioRepository {

    // SELECT reaproveitado por listar/buscar. O JOIN traz s.nome como "setor_nome".
    private const val SELECT_BASE = """
        SELECT f.id, f.nome, f.sobrenome, f.cpf, f.rg, f.idade, f.salario,
               f.turno, f.habilidade, f.setor_id, s.nome AS setor_nome
        FROM funcionario f
        JOIN setor s ON s.id = f.setor_id
    """

    /** Insere um funcionario e devolve o id (numero) que o banco gerou. */
    fun inserir(f: Funcionario): Int {
        val sql = """
            INSERT INTO funcionario (nome, sobrenome, cpf, rg, idade, salario, turno, habilidade, setor_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { stmt ->
                // Cada "?" do SQL e preenchido pela posicao (1, 2, 3...).
                stmt.setString(1, f.nome)
                stmt.setString(2, f.sobrenome)
                stmt.setString(3, f.cpf)
                stmt.setString(4, f.rg)
                stmt.setInt(5, f.idade)
                stmt.setBigDecimal(6, f.salario)
                stmt.setString(7, f.turno.name)        // enum -> texto ("MANHA")
                stmt.setString(8, f.habilidade.name)
                stmt.setInt(9, f.setorId)
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    rs.next()
                    return rs.getInt(1)                 // id recem-criado
                }
            }
        }
    }

    fun listar(): List<Funcionario> = consultar("$SELECT_BASE ORDER BY s.nome, f.nome")

    /** Usado no requisito "dividir funcionarios em setores". */
    fun listarPorSetor(setorId: Int): List<Funcionario> =
        consultar("$SELECT_BASE WHERE f.setor_id = ? ORDER BY f.nome") { it.setInt(1, setorId) }

    fun buscar(id: Int): Funcionario? =
        consultar("$SELECT_BASE WHERE f.id = ?") { it.setInt(1, id) }.firstOrNull()

    // Executa um SELECT e transforma cada linha em Funcionario. O parametro "parametros"
    // e uma funcao que preenche os "?" quando existirem (listarPorSetor/buscar).
    private fun consultar(
        sql: String,
        parametros: (java.sql.PreparedStatement) -> Unit = {}
    ): List<Funcionario> {
        val lista = mutableListOf<Funcionario>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                parametros(stmt)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) lista.add(mapear(rs))
                }
            }
        }
        return lista
    }

    // Converte a linha atual do ResultSet em um objeto Funcionario.
    private fun mapear(rs: ResultSet) = Funcionario(
        id = rs.getInt("id"),
        nome = rs.getString("nome"),
        sobrenome = rs.getString("sobrenome"),
        cpf = rs.getString("cpf"),
        rg = rs.getString("rg"),
        idade = rs.getInt("idade"),
        salario = rs.getBigDecimal("salario"),
        turno = Turno.valueOf(rs.getString("turno")),          // texto -> enum
        habilidade = Habilidade.valueOf(rs.getString("habilidade")),
        setorId = rs.getInt("setor_id"),
        setorNome = rs.getString("setor_nome")
    )
}
